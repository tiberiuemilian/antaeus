## Performance Considerations

The current solution architecture contains 2 agents who could process pending invoices, but the solution
allow their number to be scaled if necessary.

Because the logic is packaged in a container it gives us a lot of flexibility in how we deploy the solution to
production. We can easily convert it to a cloud native scalable solution.

### Gateway APIs

Usually, external service providers(payment provider in our case) are implemented in a microservice architecture as REST
endpoints accessible through a kind of API gateway that provides security and controls the bandwidth limit for the
exposed API.

Because the code lack of any unnecessary adornment, it runs very fast. 
In this particular case, you may need to adjust its speed for the API Gateway bandwidth limit.

This is why I have entered 2 configuration parameters that could be set both in the antaeus application configuration file
(config.yaml) and as environment variables. These are:
* batchSize: 200
* delayBetweenBatches: 0

These configurations could help us in this particular case to reduce the processing speed.

### Allocated database connections
For performance reasons, DBA administrators typically set a maximum number for competing connections. 
If we let the agent (or even worse, multiple agents) open a connection for each separate database transaction,
we could easily reach this limit. 

That's why I chose to use a database connection pool based on HikariCP
(https://github.com/brettwooldridge/HikariCP) which is a JDBC connection pool ready for "zero-overhead" production.

### Database transactions isolation level
The solution was implemented for competing agents that could work in parallel. They work on separate batches of 
invoices.

This architectural choice allows us to use the transaction isolation level of TRANSACTION_REPEATABLE_READ.

This level of isolation outperforms TRANSACTION_SERIALIZABLE and helps us avoid
rollback exceptions if both agents try to update the invoices statuses at the same time.

> **TRANSACTION_REPEATABLE_READ**
> : A constant indicating that dirty reads and non-repeatable reads are prevented; phantom reads can occur. This level prohibits a transaction from reading a row with uncommitted changes in it, and it also prohibits the situation where one transaction reads a row, a second transaction alters the row, and the first transaction rereads the row, getting different values the second time (a "non-repeatable read").

