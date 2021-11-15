package io.pleo.antaeus.models

import java.util.*

data class Payment(
    val id: Int,
    val invoiceId: Int,
    val amount: Money,
    val date: Date,
    val status: InvoiceStatus
)
