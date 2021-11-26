package io.pleo.antaeus.data

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsOnly
import io.pleo.antaeus.models.Currency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions

@Extensions(
    ExtendWith(DatabaseExtension::class)
)
internal class CustomerDalTest {

    @Test
    fun `created customer follows currency specification`() {
        val customerDal = CustomerDal(DatabaseExtension.testDb)
        val customer = customerDal.createCustomer(currency = Currency.DKK)

        assertEquals(Currency.DKK, customer?.currency, "Created customer has wrong currency.");
    }

    @Test
    fun `fetchCustomers return all customers from the database`() {
        val customerDal = CustomerDal(DatabaseExtension.testDb)
        val customer1 = customerDal.createCustomer(currency = Currency.DKK)
        val customer2 = customerDal.createCustomer(currency = Currency.EUR)
        val customer3 = customerDal.createCustomer(currency = Currency.SEK)

        assertThat(customerDal.fetchCustomers())
            .containsExactlyInAnyOrder(customer1, customer2, customer3)
    }
}