package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import mu.KotlinLogging


class BillingService(
    private val paymentProvider: PaymentProvider,
    private val paymentService: PaymentService
) {
    private val logger = KotlinLogging.logger {}
    fun chargeAll() {
        val payments = paymentService.nextPayments(1000).filterNotNull()
        payments.forEach { logger.info { "$it" }}
    }

    fun cancelCharging() {

    }

    fun getProgress() {

    }
}
