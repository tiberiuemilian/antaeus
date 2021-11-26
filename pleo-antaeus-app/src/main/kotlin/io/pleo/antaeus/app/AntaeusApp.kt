package io.pleo.antaeus.app

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import config.AgentConfig
import config.Configuration
import getPaymentProvider
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.CustomerDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceDal
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.rest.AntaeusRest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    val databaseHost = Configuration.config[AgentConfig.databaseHost]
    val databasePoolSize = Configuration.config[AgentConfig.databasePoolSize]
    val connectionURL = "jdbc:mysql://$databaseHost:3306/antaeus?useUnicode=true&characterEncoding=utf8&useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC&createDatabaseIfNotExist=true"

    val config = HikariConfig().apply {
        jdbcUrl         = connectionURL
        driverClassName = "com.mysql.cj.jdbc.Driver"
        username        = "root"
        password        = "root"
        maximumPoolSize = databasePoolSize
    }
    val dataSource = HikariDataSource(config)

    val db = Database
        .connect(dataSource)
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
//            transaction(it) {
//                addLogger(StdOutSqlLogger)
//                // Drop all existing tables to ensure a clean slate on each run
//                SchemaUtils.drop(*tables)
//                // Create all tables
//                SchemaUtils.create(*tables)
//            }
        }

    // Set up data access layer.
    val customerDal = CustomerDal(db = db)
    val invoiceDal = InvoiceDal(db = db, agent = Configuration.config[AgentConfig.agentName])

    // Insert example data in the database.
//    setupInitialData(
//        customerDal = customerDal,
//        invoiceDal = invoiceDal,
//    )

//    val testList = invoiceDal.nextInvoiceBatch();

    // Get third parties
    val paymentProvider = getPaymentProvider()

    // Create core services
    val invoiceService = InvoiceService(dal = invoiceDal)
    val customerService = CustomerService(dal = customerDal)

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(paymentProvider = paymentProvider, invoiceService = invoiceService)

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService,
        billingService = billingService
    ).run()

}
