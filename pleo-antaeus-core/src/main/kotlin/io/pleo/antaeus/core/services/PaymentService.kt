package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.PaymentDal
import io.pleo.antaeus.models.Payment

class PaymentService(private val dal: PaymentDal) {
    fun fetchAll(status: String?): List<Payment> {
        return dal.fetchPayments(status)
    }

    fun fetch(id: Int): Payment {
        return dal.fetchPayment(id) ?: throw InvoiceNotFoundException(id)
    }

    fun nextPayments(batchSize: Int = 1000): List<Payment?> {
        return dal.nextPayments(batchSize)
    }
}
