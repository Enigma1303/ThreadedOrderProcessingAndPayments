#!/bin/bash

LOAD=${1:-NORMAL}

echo "==================================="
echo " Resetting database..."
echo "==================================="
docker exec -i orders-mysql mysql -u orders -porders123 orderdb < src/main/resources/db/schema.sql

echo "==================================="
echo " Starting system in $LOAD mode..."
echo "==================================="
mvn exec:java -Dexec.mainClass="com.orders.Main" -Dexec.args="$LOAD"