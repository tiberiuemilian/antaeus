source .env && docker exec mysql mysqldump -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" --databases antaeus > ../mysql-dump/antaeus-dump.sql
