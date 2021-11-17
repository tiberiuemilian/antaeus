package io.pleo.antaeus.core.services

import config.AppConfig
import config.Configuration
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

            batchJob = GlobalScope.async {
                processBatch()
            }
        }

        processCounter.get()
    }

    suspend fun processBatch() {
        batchInvoices!!.forEach() {
            val paymentProcesing = GlobalScope.async {
                paymentProvider.charge(it)
                it.status = InvoiceStatus.PAID
                invoiceService.updateInvoice(it)
            }
            paymentProcesing.await()
        }

        mutex.withLock {
            batchInvoices = invoiceService.nextInvoiceBatch(batchSize)

            if (batchInvoices!!.isNotEmpty()) {
                batchJob = GlobalScope.async { processBatch() }
            } else {
                batchInvoices = null
                batchJob = null
                isRunning = false
            }
        }
    }

    fun cancelCharging() {

    }

    fun getProgress() {

    }
}
