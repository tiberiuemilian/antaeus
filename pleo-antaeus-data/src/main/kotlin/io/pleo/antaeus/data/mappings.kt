/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Alias

fun ResultRow.toInvoice(): Invoice = Invoice(
    id = this[InvoiceTable.id],
    amount = Money(
        value = this[InvoiceTable.value],
        currency = Currency.valueOf(this[InvoiceTable.currency])
    ),
    status = InvoiceStatus.valueOf(this[InvoiceTable.status]),
    customerId = this[InvoiceTable.customerId]
)

fun ResultRow.toInvoice(alias: Alias<InvoiceTable>): Invoice = Invoice(
    id = this[alias[InvoiceTable.id]],
    amount = Money(
        value = this[alias[InvoiceTable.value]],
        currency = Currency.valueOf(this[alias[InvoiceTable.currency]])
    ),
    status = InvoiceStatus.valueOf(this[alias[InvoiceTable.status]]),
    customerId = this[alias[InvoiceTable.customerId]]
)

fun ResultRow.toCustomer(): Customer = Customer(
    id = this[CustomerTable.id],
    currency = Currency.valueOf(this[CustomerTable.currency])
)

fun ResultRow.toPayment(): Payment = Payment(
    id = this[PaymentTable.id],
    amount = Money(
        value = this[PaymentTable.value],
        currency = Currency.valueOf(this[PaymentTable.currency])
    ),
    date = this[PaymentTable.date].toDate(),
    invoiceId = this[PaymentTable.invoiceId],
    status = InvoiceStatus.valueOf(this[PaymentTable.status])
)
