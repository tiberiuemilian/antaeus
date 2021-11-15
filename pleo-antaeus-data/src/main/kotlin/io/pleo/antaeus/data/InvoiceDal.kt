package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

class InvoiceDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                    .select { InvoiceTable.id.eq(id) }
                    .firstOrNull()
                    ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                    .selectAll()
                    .map { it.toInvoice() }
        }
    }

    fun nextInvoiceBatch(batchSize: Int = 1000): List<Invoice> {
        return transaction(db) {
            addLogger(StdOutSqlLogger)

            val allInvoices = InvoiceTable.alias("allInvoices")
            val allPayments = PaymentTable.alias("allPayments")

            val currentPayments = allInvoices
                .innerJoin(allPayments, { allInvoices[InvoiceTable.id] }, { allPayments[PaymentTable.invoiceId] })
                .slice(allPayments[PaymentTable.invoiceId], allPayments[PaymentTable.date])
                .select { allPayments[PaymentTable.date].eq(DateTime.now()) }
                .alias("currentPayments")

            val nextInvoicesBatch = InvoiceTable.alias("nextInvoicesBatch")

            nextInvoicesBatch
                .leftJoin(currentPayments, { nextInvoicesBatch[InvoiceTable.id] }, { currentPayments[allPayments[PaymentTable.invoiceId]] })
                .select { currentPayments[allPayments[PaymentTable.invoiceId]].isNull() }
                .orderBy(nextInvoicesBatch[InvoiceTable.id] to SortOrder.ASC)
                .limit(batchSize)
                .mapNotNull { it.toInvoice(nextInvoicesBatch) }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                    .insert {
                        it[this.value] = amount.value
                        it[this.currency] = amount.currency.toString()
                        it[this.status] = status.toString()
                        it[this.customerId] = customer.id
                    } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }
}
