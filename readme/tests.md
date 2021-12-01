From the JUnit and integration tests added to the project, I think the following deserves special attention:

### Tests for the DALs (Data Access Layer classes)

I've created **DatabaseExtension.class** as a JUnit5 Extension Lifecycle class in order to start-up and tear down the 
test environment for the database related tests.

By default, database tests are run quickly in memory using the H2 database driver. 
You can also troubleshoot the H2 database operations over the default H2 TCP port (9092) after authentication with 
root / test credentials.

If you want to test database operations on a MySQL server, you can set the environment variable "withMysql = true"
and switch the database driver and instance type from H2 to MySQL.
This behavior is provided by the Maven testcontainers docker plugin.
You can also troubleshoot the MySQL database operations over the default MySQL TCP port (3306) after authentication with
root / test credentials.
I have created both H2 and MySQL IntelliJ IDEA, shared running configurations that you can run from IntelliJ IDEA.

![H2 and MySQL IntelliJ IDEA shared run configurations](/readme/database-IntelliJ-configs.PNG "H2 and MySQL IntelliJ IDEA shared run configurations")

### Tests for the *BillingService*

* BillingService.class * contains logic for multi-threaded execution logic for agents.
  The library "org.jetbrains.kotlinx: kotlinx-coroutines-test: 1.5.2" helped me to easily test multithreaded
functionality with very little granularity. It was much easier than with Java 8-specific multithreading classes.

![Coroutine multithreading tests](/readme/coroutine-tests.PNG "Coroutine multithreading tests")

