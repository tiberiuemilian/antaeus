package io.pleo.antaeus.data

import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import io.pleo.antaeus.models.Payment
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import java.util.Date

class PaymentDal(private val db: Database, private val invoiceDal: InvoiceDal) {
    fun fetchPayment(id: Int): Payment? {
        return transaction(db) {
            PaymentTable
                    .select { PaymentTable.id.eq(id) }
                    .firstOrNull()
                    ?.toPayment()
        }
    }

    fun fetchPayments(status: String?): List<Payment> {
        return transaction(db) {
            addLogger(StdOutSqlLogger)
            val condition = when {
                (status != null) -> Op.build { PaymentTable.status eq status }
                else -> null
            }

            val query = condition?.let { PaymentTable.select(condition) } ?: PaymentTable.selectAll()

            query.map { it.toPayment() }
        }
    }

    fun createPayment(invoice: Invoice): Payment? {
        val id = transaction(db) {
            addLogger(StdOutSqlLogger)
            // Insert the payment and return its new id.
            PaymentTable.insert {
                it[this.invoiceId] = invoice.id
                it[this.currency] = invoice.amount.currency.toString()
                it[this.value] = invoice.amount.value
                it[this.date] = DateTime(Date())
                it[this.status] = InvoiceStatus.PENDING.toString();
            } get PaymentTable.id
        }

        return fetchPayment(id)
    }

    fun nextPayments(batchSize: Int): List<Payment?> {
        return transaction(db) {
            addLogger(StdOutSqlLogger)
            invoiceDal.nextInvoiceBatch(batchSize).map { createPayment(it) }
        }
    }

}
