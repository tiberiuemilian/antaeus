package io.pleo.antaeus.core.services

import assertk.assertAll
import assertk.assertThat
import assertk.assertions.isEqualTo
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExperimentalCoroutinesApi //
@ExtendWith(MockKExtension::class)
internal class BillingServiceTest {
    private val batchSize = 10

    @MockK
    lateinit var paymentProvider: PaymentProvider

    @MockK
    lateinit var invoiceService: InvoiceService

    lateinit var billingService: BillingService

    private val dummyInvoice = Invoice(
        id = 1,
        customerId = 1,
        amount = Money(
            10.toBigDecimal(),
            Currency.SEK
        ),
        status = InvoiceStatus.PENDING
    )

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        billingService = spyk(
            objToCopy = BillingService(
                paymentProvider = paymentProvider,
                invoiceService = invoiceService,
                batchSize = batchSize,
                delayBetweenBatches = 0
            ),
            recordPrivateCalls = true // let us mock private methods
        )
    }

    @Test
    fun `getProgress delegate progress calculation to invoice service`() {
        every { invoiceService.getProgress() } returns 50
        billingService.getProgress()
        verify {
            invoiceService.getProgress()
        }
        confirmVerified(invoiceService)
    }

    @Test
    fun `chargeAll skip charging if it's still running`() {
        BillingService.Companion.isRunning = true
        billingService.chargeAll()
        verify { invoiceService wasNot called }
    }

    @Test
    fun `chargeAll launch charging processBatch if it does not run`() = runBlockingTest {
        every { invoiceService.nextInvoiceBatch(any()) } returns listOf()
        coEvery { billingService.processBatch() } returns Unit
        billingService.chargeAll()
        BillingService.batchJob?.join() // Wait for the batchJob to complete. Otherwise, we will have intermittent failures.
        verify(exactly = 1) { invoiceService.nextInvoiceBatch(any()) }
        coVerify (exactly = 1) { billingService.processBatch() }
    }

    @Test
    fun `processBatch charges each invoice in the batch, requests a new batch, and calls itself recursively`() = runBlockingTest {
        val firstBatch = List(10) {
            dummyInvoice.copy(id = it)
        }

        val secondBatch = List(5) {
            dummyInvoice.copy(id = batchSize + it)
        }

        val batchProcessingTime = 2_000L
        val delayBetweenBatches = 3_000L

        billingService = spyk(
            objToCopy = BillingService(
                paymentProvider = paymentProvider,
                invoiceService = invoiceService,
                batchSize = batchSize,
                delayBetweenBatches = delayBetweenBatches
            ),
            recordPrivateCalls = true // let us mock private methods
        )

        // given
        BillingService.Companion.batchInvoices = firstBatch
        every { invoiceService.nextInvoiceBatch(any()) } returns secondBatch andThen emptyList()
        coEvery { billingService.chargeBatchInvoices() } coAnswers {
            delay(batchProcessingTime)
        }

        // when -> immediately after processBatch call
        launch { billingService.processBatch() }
        // then
        coVerify(exactly = 1) { billingService.chargeBatchInvoices() }
        verify(exactly = 0) { invoiceService.nextInvoiceBatch(any()) }
        coVerify(exactly = 1) { billingService.processBatch() }

        // when -> after the first batch and the delay between batches have just ended
        advanceTimeBy(batchProcessingTime + delayBetweenBatches)
        // then
        coVerify(exactly = 2) { billingService.chargeBatchInvoices() }
        verify(exactly = 1) { invoiceService.nextInvoiceBatch(any()) }
        coVerify(exactly = 2) { billingService.processBatch() }

        // when -> in the end
        advanceUntilIdle()
        // then
        coVerify(exactly = 2) { billingService.chargeBatchInvoices() } // one for each non-empty batch
        verify(exactly = 2) { invoiceService.nextInvoiceBatch(any()) } // one for each non-empty batch
        coVerify(exactly = 2) { billingService.processBatch() }
    }

    @Test
    fun `processBatch is executed recursively with a delay`() = runBlockingTest {
        val firstBatch = List(10) {
            dummyInvoice.copy(id = it)
        }

        val secondBatch = List(5) {
            dummyInvoice.copy(id = batchSize + it)
        }

        val batchProcessingTime = 2_000L
        val delayBetweenBatches = 3_000L

        billingService = spyk(
            objToCopy = BillingService(
                paymentProvider = paymentProvider,
                invoiceService = invoiceService,
                batchSize = batchSize,
                delayBetweenBatches = delayBetweenBatches
            ),
            recordPrivateCalls = true // let us mock private methods
        )

        // given
        BillingService.Companion.batchInvoices = firstBatch
        every { invoiceService.nextInvoiceBatch(any()) } returns secondBatch andThen emptyList()
        coEvery { billingService.chargeBatchInvoices() } coAnswers {
            delay(batchProcessingTime)
        }

        // when -> immediately after processBatch call
        launch { billingService.processBatch() }
        // then
        coVerify (exactly = 0) { // immediately after processBatch call
            invoiceService.nextInvoiceBatch(any())
        }

        // when -> the first batch just finished
        advanceTimeBy(batchProcessingTime)
        // then
        coVerify (exactly = 0) { // the first batch just finished
            invoiceService.nextInvoiceBatch(any())
        }

        // when -> after the delay between batches
        advanceTimeBy(delayBetweenBatches)
        // then
        coVerify (exactly = 1) { // the first batch just finished
            invoiceService.nextInvoiceBatch(any())
        }
    }

    @Test
    fun `chargings are processed in parallel`() = runBlockingTest {
        val invoiceProcessingTime = 5_000L

        val firstBatch = List(batchSize) {
            dummyInvoice.copy(id = it)
        }

        // given
        BillingService.Companion.batchInvoices = firstBatch
        coEvery { billingService.chargeInvoice(any(), any(), any()) } coAnswers {
            delay(invoiceProcessingTime)
        }

        // when
        launch { billingService.chargeBatchInvoices() }
        advanceUntilIdle() // will run the child coroutine to completion

        // then
        assertThat(currentTime).isEqualTo(invoiceProcessingTime)
    }

    @Test
    fun `chargeAll is a non-blocking method`() = runBlockingTest {
        val invoiceProcessingTime = 5_000L

        val firstBatch = List(batchSize) {
            dummyInvoice.copy(id = it)
        }

        val secondBatch = List(5) {
            dummyInvoice.copy(id = batchSize + it)
        }

        // given
        every { invoiceService.update(any()) } returns dummyInvoice.copy()
        every { invoiceService.nextInvoiceBatch(any()) } returns firstBatch andThen secondBatch andThen emptyList()
        coEvery { billingService.chargeInvoice(any(), any(), any()) } coAnswers {
            delay(invoiceProcessingTime)
        }

        // when
        launch { billingService.chargeAll() }
        advanceUntilIdle() // will run the child coroutine to completion

        // then
        assertThat(currentTime).isEqualTo(0)
    }

    @Test
    fun `cancelCharging test`() = runBlockingTest {
        val invoiceProcessingTime = 5_000L

        val firstBatch = List(batchSize) {
            dummyInvoice.copy(id = it)
        }

        val secondBatch = List(5) {
            dummyInvoice.copy(id = batchSize + it)
        }

        // given
        BillingService.Companion.batchInvoices = firstBatch
        BillingService.Companion.isRunning = true
        every { invoiceService.nextInvoiceBatch(any()) } returns secondBatch andThen emptyList()
        coEvery { billingService.chargeInvoice(any(), any(), any()) } coAnswers {
            delay(invoiceProcessingTime)
        }
        every { invoiceService.fetch(any()) } returns
                dummyInvoice.copy(status = InvoiceStatus.IN_PROGRESS)

        // when
        BillingService.Companion.batchJob = launch { billingService.processBatch() }
        advanceTimeBy(1_000) // right after charging process starts (< 5000)
        val invoiceStates = mutableListOf<Invoice>()
        every { invoiceService.update(capture(invoiceStates)) } returns dummyInvoice.copy()
        billingService.cancelCharging()

        // then
        assertTrue(BillingService.Companion.batchJob!!.isCancelled)
        verify(exactly = batchSize) { invoiceService.update(any()) }
        assertAll {
            assertThat(invoiceStates.size).isEqualTo(batchSize)
            invoiceStates.forEach {
                assertThat(it.status).isEqualTo(InvoiceStatus.PENDING)
            }
        }
    }

    @Test
    fun `chargeInvoice sets the invoice status to PAID when paymentProvider charges it`() {
        val invoice = dummyInvoice.copy()

        val invoiceStates = mutableListOf<Invoice>()

        // given
        every { paymentProvider.charge(invoice) } returns true
        every { invoiceService.update(capture(invoiceStates)) } returns
                invoice.copy(status = InvoiceStatus.TO_BE_CHECKED) andThen
                invoice.copy(status = InvoiceStatus.PAID)

        // when
        runBlockingTest { billingService.chargeInvoice(invoice, 3, 3000) }

        //then
        verify(exactly = 1) { paymentProvider.charge(invoice) }
        verify(exactly = 2) { invoiceService.update(any()) }
        assertAll {
            assertThat(invoiceStates.size).isEqualTo(2)
            assertThat(invoiceStates[0].status).isEqualTo(InvoiceStatus.TO_BE_CHECKED) // transit state
            assertThat(invoiceStates[1].status).isEqualTo(InvoiceStatus.PAID) // the last status
        }
    }

    @Test
    fun `chargeInvoice sets the invoice status to ERR_UNAVAILABLE_FUNDS when call to paymentProvider returns false`() {
        val invoice = dummyInvoice.copy()

        val invoiceStates = mutableListOf<Invoice>()

        // given
        every { paymentProvider.charge(invoice) } returns false
        every { invoiceService.update(capture(invoiceStates)) } returns
                invoice.copy(status = InvoiceStatus.TO_BE_CHECKED) andThen
                invoice.copy(status = InvoiceStatus.ERR_UNAVAILABLE_FUNDS)

        // when
        runBlockingTest { billingService.chargeInvoice(invoice, 3, 3000) }

        //then
        verify(exactly = 1) { paymentProvider.charge(invoice) }
        verify(exactly = 2) { invoiceService.update(any()) }
        assertAll {
            assertThat(invoiceStates.size).isEqualTo(2)
            assertThat(invoiceStates[0].status).isEqualTo(InvoiceStatus.TO_BE_CHECKED) // transit state
            assertThat(invoiceStates[1].status).isEqualTo(InvoiceStatus.ERR_UNAVAILABLE_FUNDS) // the last status
        }
    }

    @Test
    fun `chargeInvoice sets the invoice status to ERR_CUSTOMER_NOT_FOUND when call to paymentProvider throws CustomerNotFoundException`() {
        val invoice = dummyInvoice.copy()

        val invoiceStates = mutableListOf<Invoice>()

        // given
        every { paymentProvider.charge(invoice) } throws CustomerNotFoundException(invoice.customerId)
        every { invoiceService.update(capture(invoiceStates)) } returns
                invoice.copy(status = InvoiceStatus.TO_BE_CHECKED) andThen
                invoice.copy(status = InvoiceStatus.ERR_CUSTOMER_NOT_FOUND)

        // when
        runBlockingTest { billingService.chargeInvoice(invoice, 3, 3000) }

        //then
        verify(exactly = 1) { paymentProvider.charge(invoice) }
        verify(exactly = 2) { invoiceService.update(any()) }
        assertAll {
            assertThat(invoiceStates.size).isEqualTo(2)
            assertThat(invoiceStates[0].status).isEqualTo(InvoiceStatus.TO_BE_CHECKED) // transit state
            assertThat(invoiceStates[1].status).isEqualTo(InvoiceStatus.ERR_CUSTOMER_NOT_FOUND) // the last status
        }
    }

    @Test
    fun `chargeInvoice sets the invoice status to ERR_CURRENCY_MISMATCH when call to paymentProvider throws CurrencyMismatchException`() {
        val invoice = dummyInvoice.copy()

        val invoiceStates = mutableListOf<Invoice>()

        // given
        every { paymentProvider.charge(invoice) } throws CurrencyMismatchException(invoice.id, invoice.customerId)
        every { invoiceService.update(capture(invoiceStates)) } returns
                invoice.copy(status = InvoiceStatus.TO_BE_CHECKED) andThen
                invoice.copy(status = InvoiceStatus.ERR_CURRENCY_MISMATCH)

        // when
        runBlockingTest { billingService.chargeInvoice(invoice, 3, 3000) }

        //then
        verify(exactly = 1) { paymentProvider.charge(invoice) }
        verify(exactly = 2) { invoiceService.update(any()) }
        assertAll {
            assertThat(invoiceStates.size).isEqualTo(2)
            assertThat(invoiceStates[0].status).isEqualTo(InvoiceStatus.TO_BE_CHECKED) // transit state
            assertThat(invoiceStates[1].status).isEqualTo(InvoiceStatus.ERR_CURRENCY_MISMATCH) // the last status
        }
    }

    @Test
    fun `chargeInvoice sets the invoice status to ERR_UNKNOWN when call to paymentProvider throws unexpected Exception`() {
        val invoice = dummyInvoice.copy()

        val invoiceStates = mutableListOf<Invoice>()

        // given
        every { paymentProvider.charge(invoice) } throws Exception()
        every { invoiceService.update(capture(invoiceStates)) } returns
                invoice.copy(status = InvoiceStatus.TO_BE_CHECKED) andThen
                invoice.copy(status = InvoiceStatus.ERR_UNKNOWN)

        // when
        runBlockingTest { billingService.chargeInvoice(invoice, 3, 3000) }

        //then
        verify(exactly = 1) { paymentProvider.charge(invoice) }
        verify(exactly = 2) { invoiceService.update(any()) }
        assertAll {
            assertThat(invoiceStates.size).isEqualTo(2)
            assertThat(invoiceStates[0].status).isEqualTo(InvoiceStatus.TO_BE_CHECKED) // transit state
            assertThat(invoiceStates[1].status).isEqualTo(InvoiceStatus.ERR_UNKNOWN) // the last status
        }
    }

    @Test
    fun `chargeInvoice retries charging when call to paymentProvider throws a NetworkException`() {
        val invoice = dummyInvoice.copy()

        val invoiceStates = mutableListOf<Invoice>()

        // given
        every { paymentProvider.charge(invoice) } throws NetworkException() andThen true
        every { invoiceService.update(capture(invoiceStates)) } returns
                invoice.copy(status = InvoiceStatus.TO_BE_CHECKED) andThen
                invoice.copy(status = InvoiceStatus.TO_BE_CHECKED) andThen
                invoice.copy(status = InvoiceStatus.PAID)

        // when
        runBlockingTest { billingService.chargeInvoice(invoice, 3, 3000) }

        //then
        verify(exactly = 2) { paymentProvider.charge(invoice) } // charging is attempted twice
        verify(exactly = 3) { invoiceService.update(any()) }
        assertAll {
            assertThat(invoiceStates.size).isEqualTo(3)
            assertThat(invoiceStates[0].status).isEqualTo(InvoiceStatus.TO_BE_CHECKED) // transit state
            assertThat(invoiceStates[1].status).isEqualTo(InvoiceStatus.TO_BE_CHECKED) // still transit state
            assertThat(invoiceStates[2].status).isEqualTo(InvoiceStatus.PAID) // the last status
        }
    }

    @Test
    fun `chargeInvoice sets the invoice status to ERR_NETWORK when call to paymentProvider throws NetworkException 3 times in a row`() {
        val invoice = dummyInvoice.copy()

        val invoiceStates = mutableListOf<Invoice>()

        // given
        every { paymentProvider.charge(invoice) } throws NetworkException() andThenThrows NetworkException() andThenThrows NetworkException()
        every { invoiceService.update(capture(invoiceStates)) } returns
                invoice.copy(status = InvoiceStatus.TO_BE_CHECKED) andThen
                invoice.copy(status = InvoiceStatus.TO_BE_CHECKED) andThen
                invoice.copy(status = InvoiceStatus.TO_BE_CHECKED) andThen
                invoice.copy(status = InvoiceStatus.ERR_NETWORK) // the last status

        // when
        runBlockingTest { billingService.chargeInvoice(invoice, 3, 3000) }

        //then
        verify(exactly = 3) { paymentProvider.charge(invoice) } // charging is attempted three times in a row
        verify(exactly = 4) { invoiceService.update(any()) }
        assertAll {
            assertThat(invoiceStates.size).isEqualTo(4)
            assertThat(invoiceStates[0].status).isEqualTo(InvoiceStatus.TO_BE_CHECKED) // transit state
            assertThat(invoiceStates[1].status).isEqualTo(InvoiceStatus.TO_BE_CHECKED) // still transit state
            assertThat(invoiceStates[2].status).isEqualTo(InvoiceStatus.TO_BE_CHECKED) // still transit state
            assertThat(invoiceStates[3].status).isEqualTo(InvoiceStatus.ERR_NETWORK) // the last status
        }
    }
}
