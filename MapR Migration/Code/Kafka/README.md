This folder contains two applications in the **Artifacts** folder:

1. **Producer.jar**, that simulates incoming data from flights and airports (this application reads data from a CSV file rather than receiving events). This app writes messages to the **flights** topic.

1. **Consumer.jar**, which subscribes to the **flights** topic and displays the contents of the messages it reads.

The source code is provided as a pair of IntelliJ IDEA projects in the **Consumer** and **Producer** folders.

Create the following Kafka topic before running these apps:

```bash
kafka-topics --create \
    --zookeeper onprem:2181 \
    --replication-factor 1 \
    --partitions 1 \
    --topic flights
```

To start the Producer app:

```bash
java -cp EventProducer.jar eventdriver \
    --bootstrap-servers onprem:9092 \
    --sourcefilename 2000.csv \
    --topic flights \
    --partitions 1 > /dev/null &
```

This command runs the Producer app silently in the background. You will receive a warning about the log4j configuration which you can safely ignore.

To start the consumer app:

```bash
java -cp Consumer.jar eventconsumer \
    --bootstrap-servers onprem:9092 \
    --topic flights
```

This app will echo the messages it receives to the screen:

```text
Offset = 352
Value  = {"eventData":{"timestamp":1604244845680,"dateinfo":{"year":"\"2000\"","month":"\"1\"","dayofmonth":"\"24\"","dayofweek":"\"1\"","deptime":"\"2000\"","crsdeptime":"\"2000\"","arrtime":"\"2000\"","crsarrtime":"\"2000\""},"flightinfo":{"carrier":"\"2000\"","flightnum":"\"2000\"","tailnum":"\"2000\"","elapsedtime":"\"2000\"","crselapsedtime":"\"2000\"","airtime":"\"2000\"","arrdelay":"\"2000\"","depdelay":"\"2000\"","origin":"\"2000\"","dest":"\"2000\"","distance":"\"2000\""},"delayinfo":{"taxiin":"\"2000\"","taxiout":"\"2000\"","cancelled":"\"2000\"","cancellationcode":"\"2000\"","diverted":"\"2000\"","carrierdelay":"\"2000\"","weatherdelay":"\"2000\"","nasdelay":"\"2000\"","securitydelay":"\"2000\"","lateaircraftdelay":"\"2000\""}}}
Offset = 353
Value  = {"eventData":{"timestamp":1604244846180,"dateinfo":{"year":"\"2000\"","month":"\"1\"","dayofmonth":"\"3\"","dayofweek":"\"1\"","deptime":"\"2000\"","crsdeptime":"\"2000\"","arrtime":"\"2000\"","crsarrtime":"\"2000\""},"flightinfo":{"carrier":"\"2000\"","flightnum":"\"2000\"","tailnum":"\"2000\"","elapsedtime":"\"2000\"","crselapsedtime":"\"2000\"","airtime":"\"2000\"","arrdelay":"\"2000\"","depdelay":"\"2000\"","origin":"\"2000\"","dest":"\"2000\"","distance":"\"2000\""},"delayinfo":{"taxiin":"\"2000\"","taxiout":"\"2000\"","cancelled":"\"2000\"","cancellationcode":"\"2000\"","diverted":"\"2000\"","carrierdelay":"\"2000\"","weatherdelay":"\"2000\"","nasdelay":"\"2000\"","securitydelay":"\"2000\"","lateaircraftdelay":"\"2000\""}}}
Offset = 354
Value  = {"eventData":{"timestamp":1604244846681,"dateinfo":{"year":"\"2000\"","month":"\"1\"","dayofmonth":"\"5\"","dayofweek":"\"3\"","deptime":"\"2000\"","crsdeptime":"\"2000\"","arrtime":"\"2000\"","crsarrtime":"\"2000\""},"flightinfo":{"carrier":"\"2000\"","flightnum":"\"2000\"","tailnum":"\"2000\"","elapsedtime":"\"2000\"","crselapsedtime":"\"2000\"","airtime":"\"2000\"","arrdelay":"\"2000\"","depdelay":"\"2000\"","origin":"\"2000\"","dest":"\"2000\"","distance":"\"2000\""},"delayinfo":{"taxiin":"\"2000\"","taxiout":"\"2000\"","cancelled":"\"2000\"","cancellationcode":"\"2000\"","diverted":"\"2000\"","carrierdelay":"\"2000\"","weatherdelay":"\"2000\"","nasdelay":"\"2000\"","securitydelay":"\"2000\"","lateaircraftdelay":"\"2000\""}}}
...
```
