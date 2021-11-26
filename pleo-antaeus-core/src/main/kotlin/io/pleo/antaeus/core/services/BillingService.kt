package io.pleo.antaeus.core.services

import config.AppConfig
import config.Configuration
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val batchSize: Int = Configuration.config[AppConfig.batchSize],
    private val delayBetweenBatches: Long = Configuration.config[AppConfig.delayBetweenBatches]
) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val mutex = Mutex()
        var isRunning = false
        var batchInvoices: List<Invoice>? = null
        var batchJob: Job? = null
        const val RETRY_WAITING_TIME = 3_000L
    }

    fun chargeAll(dispatcher: CoroutineDispatcher = Dispatchers.Default) = runBlocking {
        logger.info { "Charge all PENDING invoices." }

        mutex.withLock {
            if (isRunning) return@withLock

            batchInvoices = invoiceService.nextInvoiceBatch(batchSize)
            isRunning = true
            batchJob = GlobalScope.launch(dispatcher) { processBatch() }
        }
    }

    internal suspend fun processBatch(): Unit = coroutineScope {
        chargeBatchInvoices()

        delay(delayBetweenBatches)

        mutex.withLock {
            batchInvoices = invoiceService.nextInvoiceBatch(batchSize)

            if (batchInvoices?.isNotEmpty() == true) {
                batchJob = launch { processBatch() }
            } else {
                batchInvoices = null
                batchJob = null
                isRunning = false
            }
        }
    }

    internal suspend fun chargeBatchInvoices() = coroutineScope {
        batchInvoices?.forEach {
            launch { chargeInvoice(it, 3, RETRY_WAITING_TIME) }
        }
    }

    internal suspend fun chargeInvoice(invoice: Invoice, retries: Int, wait: Long): Unit {
        val (id, customerId) = invoice
        try {
            invoiceService.update(invoice.copy(status = InvoiceStatus.TO_BE_CHECKED))
            val paid = paymentProvider.charge(invoice)

            if (paid) {
                logger.info { "Invoice with ID $id was PAID." }
                invoiceService.update(invoice.copy(status = InvoiceStatus.PAID))
            } else {
                logger.warn { "Customer $customerId account balance did not allow the charge." }
                invoiceService.update(invoice.copy(status = InvoiceStatus.ERR_UNAVAILABLE_FUNDS))
            }
        } catch (error: CustomerNotFoundException) {
            logger.error { "No customer has the id $customerId. ${error.message}" }
            invoiceService.update(invoice.copy(status = InvoiceStatus.ERR_CUSTOMER_NOT_FOUND))
        } catch (error: CurrencyMismatchException) {
            logger.error { error.message }
            invoiceService.update(invoice.copy(status = InvoiceStatus.ERR_CURRENCY_MISMATCH))
        } catch (error: NetworkException) {
            logger.error { "Network issue encountered when processing the invoice $id." }
            if (retries == 1) {
                invoiceService.update(invoice.copy(status = InvoiceStatus.ERR_NETWORK))
            } else {
                delay(wait)
                chargeInvoice(invoice, retries - 1, wait)
            }
        } catch (error: Exception) {
            logger.error { "An unexpected error occurred while processing the invoice ID : $id" }
            invoiceService.update(invoice.copy(status = InvoiceStatus.ERR_UNKNOWN))
        }
    }

    fun cancelCharging():Int = runBlocking {
        var cancelledInvoices = 0;
        mutex.withLock {
            if (!isRunning) return@withLock

            batchJob?.cancel()
            batchInvoices?.forEach() {
                val currentInvoiceState = invoiceService.fetch(it.id)
                if (currentInvoiceState.status == InvoiceStatus.IN_PROGRESS) {
                    invoiceService.update(it.copy(status = InvoiceStatus.PENDING))
                    cancelledInvoices++
                }
            }
            isRunning = false
        }
        cancelledInvoices
    }

    fun getProgress():Int = runBlocking {
        invoiceService.getProgress()
    }
}
