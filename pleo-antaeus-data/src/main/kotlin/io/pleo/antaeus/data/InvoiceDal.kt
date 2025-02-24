package io.pleo.antaeus.data

import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class InvoiceDal(private val db: Database, private val agent: String) {

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

    fun nextInvoicesBatch(batchSize: Int = 1000): List<Invoice> {
        return transaction(db) {
            addLogger(StdOutSqlLogger)

            val nextBatch: List<Invoice> = InvoiceTable
                .select { InvoiceTable.status.eq(InvoiceStatus.PENDING.name) }
                .orderBy(InvoiceTable.id)
                .limit(batchSize)
                .map { it.toInvoice() }

            val batchIdList: List<Int> = nextBatch.map { it.id }

            InvoiceTable.update ( { InvoiceTable.id inList batchIdList } ) {
                it[status] = InvoiceStatus.IN_PROGRESS.name
                it[processedBy] = agent
            }

            nextBatch
        }

    }

    fun update(invoice: Invoice): Invoice {
        return transaction(db) {
            addLogger(StdOutSqlLogger)
            InvoiceTable.update({ InvoiceTable.id eq invoice.id }) {
                it[id] = invoice.id
                it[customerId] = invoice.customerId
                it[currency] = invoice.amount.currency.name
                it[value] = invoice.amount.value
                it[status] = invoice.status.name
            }

            fetchInvoice(invoice.id)!!
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

    fun getProgress(): Int = transaction(db) {
        val pending =
            InvoiceTable
                .select { InvoiceTable.status.eq(InvoiceStatus.PENDING.name) }
                .count();

        val total = InvoiceTable.selectAll().count();

        val processed = total - pending

        processed * 100 / total
    }

}
