The first time you run the MySQL database container, it loads a backup sql file (dump): antaeus-dump.sql
This backup was generated using a pair of 2 bash utility scripts added to the project that can provide backup and 
restore functionality for the MySQL container:
* backup-antaeus-data-from-docker-compose-mysql-container.sh
* restore-antaeus-data-to-docker-compose-mysql-container.sh

You can run the following ackup & restore commands inside "antaeus/mysql/scripts" folder.

#### Backup

![DB backup](/readme/db-backup.PNG "DB backup")

```bash
source ./backup-antaeus-data-from-docker-compose-mysql-container.sh
```

#### Restore

![DB restore](/readme/db-restore.PNG "DB restore")

```bash
source ./restore-antaeus-data-to-docker-compose-mysql-container.sh
```




