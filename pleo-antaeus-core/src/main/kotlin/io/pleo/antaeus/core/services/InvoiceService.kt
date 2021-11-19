/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.models.Invoice

class InvoiceService(private val dal: InvoiceDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun nextInvoiceBatch(batchSize: Int): List<Invoice> {
        return dal.nextInvoicesBatch(batchSize)
    }

    fun update(invoice: Invoice): Invoice {
        return dal.update(invoice)
    }

    fun getProgress():Int {
        return dal.getProgress()
    }
}
