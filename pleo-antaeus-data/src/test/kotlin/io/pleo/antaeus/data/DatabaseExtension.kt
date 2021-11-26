package io.pleo.antaeus.data

import mu.KotlinLogging
import org.h2.tools.Server
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.Connection

class DatabaseExtension : BeforeAllCallback, ExtensionContext.Store.CloseableResource, BeforeEachCallback,
    AfterEachCallback {

    companion object {
        private val logger = KotlinLogging.logger {}

        @JvmStatic
        private var withMysql: Boolean = System.getenv("withMysql")?.toBoolean() ?: false

        @JvmStatic
        private var started = false

        private const val testDatabase = "antaeus"

        // you can use root/test controller to connect to the test mysql container
        private const val testUser = "root"
        private const val testPass = "test"

        private val tables = arrayOf(CustomerTable, InvoiceTable)
        lateinit var mysqlContainer: MySQLContainer<Nothing>
        lateinit var h2DbServer: Server
        lateinit var testDb: Database
    }

    override fun beforeAll(context: ExtensionContext?) {

        if (!started) {
            started = true
            logger.debug { "\"before all tests\" startup logic goes here" }

            // The following line registers a callback hook when the root test context is shut down
//            context!!.root.getStore(GLOBAL).put("any unique name", this);

            if (withMysql) {
                logger.info { "Create mysql test container." }
                mysqlContainer = MySQLContainer<Nothing>(DockerImageName.parse("mysql:8.0.27")).apply {
                    withDatabaseName(testDatabase)
                    withUsername(testUser)
                    withPassword(testPass)

                    // to allow test database client connections
                    withEnv("MYSQL_ROOT_HOST", "%")
                }

                mysqlContainer.start()

                testDb = Database.connect(
                    url = mysqlContainer.jdbcUrl,
                    driver = "com.mysql.cj.jdbc.Driver",
                    user = testUser,
                    password = testPass
                ).also {
                    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                }
            } else {
                // with H2 mem database (super-fast)
                testDb = Database.connect(
                    // Concurrency control mechanism and transaction isolation can be disabled so that we can check out the state of database
                    // in the middle of a transaction from a database browser. (https://nimatrueway.github.io/2017/03/01/h2-inmem-browsing.html)
                    //  url = "jdbc:h2:mem:test;MODE=Mysql;MVCC=FALSE;MV_STORE=FALSE;LOCK_MODE=0;DB_CLOSE_DELAY=-1",

                    url = "jdbc:h2:mem:$testDatabase;MODE=Mysql;DB_CLOSE_DELAY=-1",
                    driver = "org.h2.Driver",
                    user = testUser,
                    password = testPass
                ).also {
                    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                }

                h2DbServer = Server.createTcpServer ("-tcp", "-tcpAllowOthers", "-tcpPort", "9092", "-tcpDaemon").start()
            }
        }
    }

    override fun beforeEach(context: ExtensionContext?) {
        transaction(testDb) {
            addLogger(StdOutSqlLogger)
            logger.debug { "Create all tables" }
            SchemaUtils.create(*tables)
        }
    }

    override fun afterEach(context: ExtensionContext?) {
        transaction(testDb) {
            addLogger(StdOutSqlLogger)
            logger.debug { "Drop all tables to ensure a clean slate on each run" }
            SchemaUtils.drop(*tables)
        }
    }

    override fun close() {
        if (withMysql) {
            logger.info { "Destroy mysql test container." }
            mysqlContainer.stop()
        } else { // H2 database
            h2DbServer.shutdown()
        }
    }
}