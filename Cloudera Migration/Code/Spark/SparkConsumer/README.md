This app subscribes to the Kafka **flights** topic and writes data to the **flightinfo** table in Hive.

Create the following Hive table before building and running the app:

```sql
CREATE TABLE IF NOT EXISTS flightinfo ( 
    timestamp string,
    year string,
    month string,
    dayofmonth string,
    deptime string,
    depdelay int,
    arrtime string,
    arrdelay int,
    carrier string,
    flightnum string,
    elapsedtime int,
    origin string,
    dest string,
    distance int)
COMMENT 'Flight information'
CLUSTERED BY (flightnum) INTO 10 BUCKETS
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS ORC
TBLPROPERTIES('transactional'='true');
```
Run **sbt assembly** to rebuild the application from source code in the **src** folder. 

The application jar file, **SparkConsumer.jar** is written to the **target/scala-2.11** folder

Run the app with:

```bash
spark2-submit \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.1.0 \
  --class SparkConsumer \
  --master yarn \
  --total-executor-cores 2 \
  --executor-memory 512m \
  SparkConsumer.jar \
    --application testapp \
    --bootstrap onprem:9092 \
    --topic flights
```
