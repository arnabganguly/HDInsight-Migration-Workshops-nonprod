/**
  * Created by JohnS on 08/06/2017.
  */

package eventproducer

import java.util.Properties

import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import java.util.Calendar
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

object eventdriver {

  var inputOptions: Map[Symbol, Any] = _

  def main(inputArguments: Array[String]): Unit = {

    // Parse the command line arguments
    inputOptions = ClientArgumentParser.parseArguments(Map(), inputArguments.toList)
    ClientArgumentParser.verifyArguments(inputOptions)
    println(inputOptions)

    // Retrieve the values of the command line arguments
    val sourceFile: String = inputOptions(Symbol(ClientArgumentKeys.SourceFilename)).asInstanceOf[String]
    val bootstrapServers: String = inputOptions(Symbol(ClientArgumentKeys.BootstrapServers)).asInstanceOf[String]
    val topicName: String = inputOptions(Symbol(ClientArgumentKeys.TopicName)).asInstanceOf[String]

    // Construct a Properties object for the producer
    var props: Properties = new Properties
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(ProducerConfig.ACKS_CONFIG, "all")
    props.put(ProducerConfig.RETRIES_CONFIG, "5")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.LongSerializer")
    props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, "eventproducer.partitioner")

    // Construct the producer
    val producer = new KafkaProducer[Long, String](props)

    try {
      // Replay event data from the specified source file
      val csvData = io.Source.fromFile(sourceFile)
      var currentEventCount: Long = 0

      // Read the event data line by line
      for (line <- csvData.getLines) {

        // Format the next line of data
        val jsonData: String = formatLine(line)

        // Create a ProducerRecord from the data
        val data: ProducerRecord[Long, String] = new ProducerRecord(topicName, currentEventCount, jsonData)
        currentEventCount += 1
        println(s"Sending Key: $currentEventCount, Data: $jsonData")
        producer.send(data, sendercallback)
      }
    } catch {
      case e: Exception => println(s"Exception sending message: $e")
    } finally {
      producer.close()
    }

    // Format a line read from the CSV file as a JSON string
    def formatLine(line: String): String = {
      val currentTime = Calendar.getInstance().getTime
      val Array(vendorID, pickup, dropoff, passengers, distance, pickupLongitude, pickupLatitude, rateCode,
      storeAndForward, dropoffLongitude, dropoffLatitude, paymentType, fare, extra, mtaTax, tipAmount,
      tollsAmount, surcharge, total) = line.split(",")

      val eventPayload =
        "eventData" ->
          (("timestamp" -> System.currentTimeMillis) ~
           ("vendorID" -> vendorID) ~ ("pickup" -> pickup) ~ ("dropoff" -> dropoff) ~
           ("passengers" -> passengers) ~ ("distance" -> distance) ~ ("pickupLongitude" -> pickupLongitude) ~
           ("pickupLatitude" -> pickupLatitude) ~ ("rateCode" -> rateCode) ~ ("storeAndForward" -> storeAndForward) ~
           ("dropoffLongitude" -> dropoffLongitude) ~ ("dropoffLatitude" -> dropoffLatitude) ~
           ("paymentType" -> paymentType) ~ ("fare" -> fare) ~ ("extra" -> extra) ~ ("mtaTax" -> mtaTax) ~
           ("tipAmount" -> tipAmount) ~ ("tollsAmount" -> tollsAmount) ~ ("surcharge" -> surcharge) ~ ("total" -> total))

      val jsonPayload = compact(render(eventPayload))
      return jsonPayload
    }
  }


  // Handle the response from Kafka when the message has been sent
  private object sendercallback extends Callback {

    override def onCompletion(recordMetadata: RecordMetadata, e: Exception): Unit = {
      if (e != null) {
        println(s"Error sending message: $recordMetadata")
      } else {
        val topic = recordMetadata.topic
        val partition = recordMetadata.partition
        val position = recordMetadata.offset
        println(s"Message sent to topic $topic, partition $partition, position $position")
      }
    }
  }
}
