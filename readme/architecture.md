## Solution Architecture
![Architecture](/readme/architecture.png)

The implemented solution should run on Docker Compose 7on top of Docker Engine.
It consists of the following 5 docker containers as components:

| Component | Role | Port | Credentials |
| --------- | ---- | ---- | ----------- |
| MySQL database | Invoice & Customer data storage | 3306 | root / root |
| MySQL adminer | Database web client | 9090 | root / root |
| Node1 (Agent1) | Invoices processor | 7072 | N.A. |
| Node2 (Agent2) | Invoices processor | 7073 | N.A. |
| Cron UI - scheduler | Web UI for linux cron | 8082 | admin / admin |

<p>The two nodes, Node1 & Node2, run my invoice charging solution based on the initial Pleo project skeleton. The solution 
is packed in a docker container image.</p>
<p>The two nodes run in parallel in an active-active architecture, but charging functionality could be stopped and 
restarted at anytime.</p>
