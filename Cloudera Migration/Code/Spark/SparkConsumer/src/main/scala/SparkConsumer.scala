/**
 * Created by JohnS on 3/11/2020.
 */

import org.apache.spark._
import org.apache.spark.sql.types.{StructType, StringType, StructField}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, Dataset, SparkSession}

object SparkConsumer {

  var inputOptions: Map[Symbol, Any] = _

  def main(inputArguments: Array[String]) {

    // Get the command line arguments that specify:
    // - the app name (for monitoring Spark)
    // - the addresses of the Kafka bootstrap servers
    // - the Kafka topic providing the flight data
    inputOptions = ClientArgumentParser.parseArguments(Map(), inputArguments.toList)
    ClientArgumentParser.verifyArguments(inputOptions)

    // Application name, for monitoring and tracking
    val appName = inputOptions(Symbol(ClientArgumentKeys.AppName)).asInstanceOf[String];

    // Addresses of Kafka bootstrap servers for the cluster
    val kafkaBootstrapServers = inputOptions(Symbol(ClientArgumentKeys.BootstrapServers)).asInstanceOf[String]

    // Name of the topic containing incoming data
    val topic = inputOptions(Symbol(ClientArgumentKeys.TopicName)).asInstanceOf[String]

    try {
      // Create a Spark session
      val config: SparkConf = new SparkConf
      config.setAppName("SparkStreamer")
      config.set("spark.streaming.driver.writeAheadLog.allowBatching", "true")
      config.set("spark.streaming.driver.writeAheadLog.batchingTimeout", "60000")
      config.set("spark.streaming.receiver.writeAheadLog.enable", "true")
      config.set("spark.streaming.receiver.writeAheadLog.closeFileAfterWrite", "true")
      config.set("spark.streaming.stopGracefullyOnShutdown", "true")

      // Add support for Hive to the Spark session
      val sparkSession: SparkSession = SparkSession.builder.config(config)
      .enableHiveSupport()
      .config("hive.exec.dynamic.partition", "true")
      .config("hive.exec.dynamic.partition.mode", "nonstrict")
      .getOrCreate()

      // Connect to the stream of data arriving from Kafka
      val incomingStream = sparkSession
        .readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", kafkaBootstrapServers)
        .option("subscribe", topic)
        .load()

      // Format the incoming messages
      incomingStream.printSchema
      val jsonStream = incomingStream.selectExpr("CAST(value AS STRING)")

      val flightDataSchema = new StructType()
          .add ("eventData", new StructType()
            .add("timestamp", StringType, true)
            .add("dateinfo", new StructType()
              .add("year", StringType, true)
              .add("month", StringType, true)
              .add("dayofmonth", StringType, true)
              .add("dayofweek", StringType, true)
              .add("deptime", StringType, true)
              .add("crsdeptime", StringType, true)
              .add("arrtime", StringType, true)
              .add("crsarrtime", StringType, true))
            .add("flightinfo", new StructType()
              .add("carrier", StringType, true)
              .add("flightnum", StringType, true)
              .add("tailnum", StringType, true)
              .add("elapsedtime", StringType, true)
              .add("crselapsedtime", StringType, true)
              .add("airtime", StringType, true)
              .add("arrdelay", StringType, true)
              .add("depdelay", StringType, true)
              .add("origin", StringType, true)
              .add("dest", StringType, true)
              .add("distance", StringType, true))
            .add("delayinfo", new StructType()
              .add("taxiin", StringType, true)
              .add("taxiout", StringType, true)
              .add("cancelled", StringType, true)
              .add("cancellationcode", StringType, true)
              .add("diverted", StringType, true)
              .add("carrierdelay", StringType, true)
              .add("weatherdelay", StringType, true)
              .add("nasdelay", StringType, true)
              .add("securitydelay", StringType, true)
              .add("lateaircraftdelay", StringType, true))
          )

      // Flatten the data and select a few columns to write to Hive
      val formattedStream = jsonStream.select(from_json(col("value"), flightDataSchema).as("flightdata"))
      val pertinentDataStream = formattedStream.selectExpr(
          "flightdata.eventData.timestamp", 
          "flightdata.eventData.dateinfo.year", 
          "flightdata.eventData.dateinfo.month", 
          "flightdata.eventData.dateinfo.dayofmonth", 
          "flightdata.eventData.dateinfo.deptime", 
          "flightdata.eventData.flightinfo.depdelay", 
          "flightdata.eventData.dateinfo.arrtime", 
          "flightdata.eventData.flightinfo.arrdelay", 
          "flightdata.eventData.flightinfo.carrier", 
          "flightdata.eventData.flightinfo.flightnum", 
          "flightdata.eventData.flightinfo.elapsedtime", 
          "flightdata.eventData.flightinfo.origin", 
          "flightdata.eventData.flightinfo.dest", 
          "flightdata.eventData.flightinfo.distance")

      pertinentDataStream.printSchema()

      // Send the data to Hive
      import sparkSession.sql
      pertinentDataStream.writeStream
          .foreachBatch {(batchDF: DataFrame, batchID: Long) => { 
              batchDF.show()
              batchDF.write.insertInto("flightinfo")
          }}
          .start()
          .awaitTermination()
    }
    catch {
      case e: Exception => println(s"Exception processing message: $e")
    }
  }
}
