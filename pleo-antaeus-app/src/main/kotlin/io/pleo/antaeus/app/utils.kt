
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.CustomerDal
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import java.util.*
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(customerDal: CustomerDal, invoiceDal: InvoiceDal) {
    val customers = (1..10).mapNotNull {
        customerDal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    val invoices = customers.flatMapTo(LinkedList<Invoice>()) { customer ->
        (1..5_000).mapNotNullTo(LinkedList<Invoice>()) {
            val invoice = invoiceDal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = if (it % 5 == 0) InvoiceStatus.PAID else InvoiceStatus.PENDING
            )
            println("Created invoice: $invoice")
            invoice
        }
    }

    val payments = invoices.mapNotNull { invoice ->
        paymentDal.createPayment(
            invoice = invoice
        )
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
                println("Charging invoice: $invoice")
                return Random.nextBoolean()
        }
    }
}
