/*
    Configures the rest app along with basic exception handling and URL endpoints.
 */

package io.pleo.antaeus.rest

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.pleo.antaeus.core.config.Configuration.config
import io.pleo.antaeus.core.config.ServerConfig
import io.pleo.antaeus.core.exceptions.EntityNotFoundException
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.services.PaymentService
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private val thisFile: () -> Unit = {}

class AntaeusRest(
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val paymentService: PaymentService,
    private val billingService: BillingService
) : Runnable {

    override fun run() {
        //port from configuration file
        val port = config[ServerConfig.port]
        app.start(port)
    }

    // Set up Javalin rest app
    private val app = Javalin
        .create()
        .apply {
            // InvoiceNotFoundException: return 404 HTTP status code
            exception(EntityNotFoundException::class.java) { _, ctx ->
                ctx.status(404)
            }
            // Unexpected exception: return HTTP 500
            exception(Exception::class.java) { e, _ ->
                logger.error(e) { "Internal server error" }
            }
            // On 404: return message
            error(404) { ctx -> ctx.json("not found") }
        }

    init {
        // Set up URL endpoints for the rest app
        app.routes {
            get("/") {
                it.result("Welcome to Antaeus! see AntaeusRest class for routes")
            }
            path("rest") {
                // Route to check whether the app is running
                // URL: /rest/health
                get("health") {
                    it.json("ok")
                }

                // V1
                path("v1") {
                    path("invoices") {
                        // URL: /rest/v1/invoices
                        get {
                            it.json(invoiceService.fetchAll())
                        }

                        // URL: /rest/v1/invoices/{:id}
                        get("{id}") {
                            it.json(invoiceService.fetch(it.pathParamAsClass<Int>("id").get()))
                        }
                    }

                    path("customers") {
                        // URL: /rest/v1/customers
                        get {
                            it.json(customerService.fetchAll())
                        }

                        // URL: /rest/v1/customers/{:id}
                        get("{id}") {
                            it.json(customerService.fetch(it.pathParamAsClass<Int>("id").get()))
                        }
                    }

                    path("payments") {
                        // URL: /rest/v1/payments
                        get {
                            val status = it.queryParam("status")
                            it.json(paymentService.fetchAll(status))
                        }

                        // URL: /rest/v1/payments/{:id}
                        get("{id}") {
                            it.json(paymentService.fetch(it.pathParamAsClass<Int>("id").get()))
                        }
                    }

                    path("billing") {
                        post {
                            billingService.chargeAll()
                        }

                        delete {
                            billingService.cancelCharging()
                        }

                        get {
                            // progress
                            billingService.getProgress()
                        }
                    }
                }
            }
        }
    }
}
