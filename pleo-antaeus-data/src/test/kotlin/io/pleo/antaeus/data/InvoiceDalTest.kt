package io.pleo.antaeus.data

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsOnly
import assertk.assertions.isEqualByComparingTo
import assertk.assertions.isEqualTo
import io.mockk.InternalPlatformDsl.toArray
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import java.math.BigDecimal
import kotlin.random.Random

@Extensions(
    ExtendWith(DatabaseExtension::class)
)
internal class InvoiceDalTest {

    private lateinit var customer1: Customer
    private lateinit var customer2: Customer
    private lateinit var invoiceDal: InvoiceDal

    @BeforeEach
    fun init() {
        val customerDal = CustomerDal(DatabaseExtension.testDb)
        customer1 = customerDal.createCustomer(currency = Currency.DKK)!!
        customer2 = customerDal.createCustomer(currency = Currency.EUR)!!
        invoiceDal = InvoiceDal(DatabaseExtension.testDb, agent)
    }

    private val agent = "Agent1"

    @Test
    fun `progress value is calculated OK as %`() {
        val firstInvoice = invoiceDal.createInvoice(
            amount = Money(
                value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                currency = customer1.currency
            ),
            customer = customer1,
            status = InvoiceStatus.PENDING
        )

        val secondInvoice = invoiceDal.createInvoice(
            amount = Money(
                value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                currency = customer1.currency
            ),
            customer = customer1,
            status = InvoiceStatus.PAID
        )

        assertThat(invoiceDal.getProgress()).equals(50)
    }

    @Test
    fun `invoice updated properties are durable`() {
        val initialInvoice = invoiceDal.createInvoice(
            amount = Money(
                value = 100.toBigDecimal(),
                currency = Currency.SEK
            ),
            customer = customer1,
            status = InvoiceStatus.PENDING
        )!!

        val newValue = "200"
        val newCurrency = Currency.EUR
        val newAmount = Money(
            value = newValue.toBigDecimal(),
            currency = newCurrency
        )

        val newStatus = InvoiceStatus.PAID

        invoiceDal.update(initialInvoice.copy(customerId = customer2.id, amount = newAmount, status = newStatus))

        val updatedInvoice = invoiceDal.fetchInvoice(initialInvoice.id)!!

        assertAll {
            assertThat(updatedInvoice::customerId).isEqualTo(customer2.id)

            // test BigDecimal equality ignoring scale
            assertThat(updatedInvoice.amount.value).isEqualByComparingTo(newValue)

            assertThat(updatedInvoice.amount.currency).isEqualTo(newCurrency)
            assertThat(updatedInvoice::status).isEqualTo(newStatus)
        }
    }

    @Test
    fun `nextInvoicesBatch selects next "batchSize" X invoices with status Pending`() {
        val pendingInvoices: MutableList<Invoice> = mutableListOf()

        (1..30).forEach {

            var invoiceStatus: InvoiceStatus = if (it % 2 == 0) {
                InvoiceStatus.PENDING
            } else {
                InvoiceStatus.PAID
            }

            val invoice = invoiceDal.createInvoice(
                amount = Money(
                    value = 100.toBigDecimal(),
                    currency = Currency.SEK
                ),
                customer = customer1,
                status = invoiceStatus
            )!!

            if (it % 2 == 0) {
                pendingInvoices.add(invoice)
            }
        }

        assertAll {
            val nextBatch = invoiceDal.nextInvoicesBatch(20)
            assertThat(nextBatch.size).isEqualTo(30/2)
            assertThat(nextBatch).containsExactlyInAnyOrder(*pendingInvoices.toTypedArray())
        }
    }

    @Test
    fun `nextInvoicesBatch sets status to IN_PROGRESS for selected invoices`() {
        val pendingInvoices: MutableList<Invoice> = mutableListOf()

        (1..30).forEach {

            var invoiceStatus: InvoiceStatus = if (it % 2 == 0) {
                InvoiceStatus.PENDING
            } else {
                InvoiceStatus.PAID
            }

            val invoice = invoiceDal.createInvoice(
                amount = Money(
                    value = 100.toBigDecimal(),
                    currency = Currency.SEK
                ),
                customer = customer1,
                status = invoiceStatus
            )!!

            if (it % 2 == 0) {
                pendingInvoices.add(invoice)
            }
        }

        assertAll {
            val nextBatch = invoiceDal.nextInvoicesBatch(20)
            assertThat(nextBatch.size).isEqualTo(30/2)
            assertThat(nextBatch).containsExactlyInAnyOrder(*pendingInvoices.toTypedArray())
            nextBatch.forEach {
                assertThat(invoiceDal.fetchInvoice(it.id)!! :: status).isEqualTo(InvoiceStatus.IN_PROGRESS)
            }
        }
    }

    @Test
    fun `fetchInvoices return all invoices from the database`() {
        val invoice1 = invoiceDal.createInvoice(
            amount = Money(
                value = 100.toBigDecimal(),
                currency = Currency.DKK
            ),
            customer = customer1,
            status = InvoiceStatus.PENDING
        )

        val invoice2 = invoiceDal.createInvoice(
            amount = Money(
                value = 200.toBigDecimal(),
                currency = Currency.EUR
            ),
            customer = customer1,
            status = InvoiceStatus.PENDING
        )

        val invoice3 = invoiceDal.createInvoice(
            amount = Money(
                value = 300.toBigDecimal(),
                currency = Currency.SEK
            ),
            customer = customer1,
            status = InvoiceStatus.PENDING
        )

        assertThat(invoiceDal.fetchInvoices())
            .containsExactlyInAnyOrder(invoice1, invoice2, invoice3)
    }

}