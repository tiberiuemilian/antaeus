source .env && cat ../mysql-dump/antaeus-dump.sql | docker exec -i mysql mysql -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}"
