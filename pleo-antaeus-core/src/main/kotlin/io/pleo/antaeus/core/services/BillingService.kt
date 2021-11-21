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
import java.util.concurrent.atomic.AtomicInteger


class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {
    companion object {
        private val mutex = Mutex()
        private var isRunning = false
        private var batchInvoices: List<Invoice>? = null
        private var batchJob: Job? = null
        private val childrenCounter = AtomicInteger(0)
        private val processCounter = AtomicInteger(0)
    }


    private val logger = KotlinLogging.logger {}

    private val batchSize = Configuration.config[AppConfig.batchSize]

    fun chargeAll() = runBlocking {

        println("Charge All")

        mutex.withLock {
            if (isRunning) return@withLock

            batchInvoices = invoiceService.nextInvoiceBatch(batchSize)
            isRunning = true
            batchJob = GlobalScope.launch { processBatch() }
        }

        processCounter.get()
    }

    private suspend fun processBatch() {

            batchInvoices?.forEach() {
                chargeInvoice(it, 3, 3000)
            }

            mutex.withLock {
                batchInvoices = invoiceService.nextInvoiceBatch(batchSize)

                if (batchInvoices?.isNotEmpty() == true) {
                    batchJob = GlobalScope.launch { processBatch() }
                } else {
                    batchInvoices = null
                    batchJob = null
                    isRunning = false
                }
            }
    }

    private suspend fun chargeInvoice(invoice: Invoice, retries: Int, wait: Long): Unit = coroutineScope {
        launch {
            val (id, customerId) = invoice
            try {
                invoiceService.update(invoice.copy(status = InvoiceStatus.PAID_OR_CANCELLED))
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
                if (retries == 0) {
                    invoiceService.update(invoice.copy(status = InvoiceStatus.ERR_NETWORK))
                } else {
                    delay(wait)
                    chargeInvoice(invoice, retries-1, wait)
                }
            } catch (error: Exception) {
                logger.error { "An unexpected error occurred while processing the invoice ID : $id" }
                invoiceService.update(invoice.copy(status = InvoiceStatus.ERR_UNKNOWN))
            }
        }
    }

    fun cancelCharging():Int = runBlocking {
        var cancelledInvoices: Int = 0;
        mutex.withLock {
            if (!isRunning) return@withLock

            batchJob?.cancel()
            batchInvoices?.forEach() {
                if (it.status == InvoiceStatus.IN_PROGRESS) {
                    invoiceService.update(it.copy(status = InvoiceStatus.CANCELLED))
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
