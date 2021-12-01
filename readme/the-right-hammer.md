## Choose the right hammer

I have briefly listed here the alternatives I've found for implementing the batch job functionality required by
this code challenge assignment:

1. Quartz Scheduler ( https://github.com/quartz-scheduler/quartz )
   > Quartz is a richly featured, open source job scheduling library that can be integrated within virtually any Java
   > application - from the smallest stand-alone application to the largest e-commerce system.
   
2. Easy Batch ( https://github.com/j-easy/easy-batch )
   > Easy Batch is a framework that aims at simplifying batch processing with Java. It was specifically designed for 
   > simple, single-task ETL jobs. Writing batch applications requires a lot of boilerplate code: reading, writing, 
   > filtering, parsing and validating data, logging, reporting to name a few.. The idea is to free you from these
   > tedious tasks and let you focus on your batch application's logic.
   
3. Spring Batch ( https://spring.io/projects/spring-batch )
   > Spring Batch provides reusable functions that are essential in processing large volumes of records, including 
   > logging/tracing, transaction management, job processing statistics, job restart, skip, and resource management. 
   > It also provides more advanced technical services and features that will enable extremely high-volume and high performance batch jobs through optimization and partitioning techniques.

4. Write functionality from scratch with Java 8 multithreading classes like Executors and ScheduledFutures


5. Write functionality from scratch using coroutines in Kotlin specific manner

Using a stable library for batch work can help you get rid of a lot of hassle, as it brings a few solutions to common 
problems and a lot of goodies right from the start, such as recording/tracking, glue code for combining jobs, 
task management operations and so on.

When choosing between Java/Kotlin vanilla multithreaded (corutine) code and other frameworks, you need to ask yourself
how complex your line of processing is, how many connectors or types of transformers you need.
If your process is complex enough to use one of the existing frameworks, you might want to go with one of them. 
But keep in mind that any unused functionality can overwhelm your implementation.
Usually, this choice comes with unnecessary code to skip or disable various features and workarounds to reinvent, 
in some cases, the way the framework does things inside.

For my implementation, I chose to go with the 5th option based on the fact that this task requires a fairly simple 
process and because I found this challenge a good chance to learn more about Kotlin and its coroutine library.

As for the scheduler can be internal or external. I chose to use an external scheduler because this approach gives me 
more flexibility.

I'm not a big fan of the scheduler's tight coupled with batch processes, unless absolutely necessary, because I 
consider that gluing them is a source of headaches for integration and testing.
