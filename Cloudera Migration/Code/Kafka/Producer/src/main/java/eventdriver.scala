/**
  * Created by JohnS on 26/10/2020.
  */

import java.util.{Calendar, Properties}

import org.apache.kafka.clients.producer._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

object eventdriver {

  var inputOptions: Map[Symbol, Any] = _

  def main(args: Array[String]) = {

    // Parse the command line arguments
    inputOptions = ClientArgumentParser.parseArguments(Map(), args.toList)
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
    props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, "partitioner")

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
        Thread.sleep(500)
      }
    } catch {
      case e: Exception => println(s"Exception sending message: $e")
    } finally {
      producer.close()
    }

    // Format a line read from the CSV file as a JSON string
    def formatLine(line: String): String = {
      val currentTime = Calendar.getInstance().getTime
      val data = line.split(",")

      val eventPayload =
        "eventData" ->
          (("timestamp" -> System.currentTimeMillis) ~
           ("dateinfo" ->
            (("year" -> data(1)) ~ ("month" -> data(2)) ~ ("dayofmonth" -> data(3)) ~ ("dayofweek" -> data(4)) ~
             ("deptime" -> data(1)) ~ ("crsdeptime" -> data(1)) ~ ("arrtime" -> data(1)) ~ ("crsarrtime" -> data(1)))) ~
           ("flightinfo" ->
            (("carrier" -> data(1)) ~ ("flightnum" -> data(1)) ~ ("tailnum" -> data(1)) ~ ("elapsedtime" -> data(1)) ~
             ("crselapsedtime" -> data(1)) ~ ("airtime" -> data(1)) ~ ("arrdelay" -> data(1)) ~
             ("depdelay" -> data(1)) ~ ("origin" -> data(1)) ~ ("dest" -> data(1)) ~ ("distance" -> data(1)))) ~
           ("delayinfo" ->
            (("taxiin" -> data(1)) ~ ("taxiout" -> data(1)) ~ ("cancelled" -> data(1)) ~ ("cancellationcode" -> data(1)) ~
             ("diverted" -> data(1)) ~ ("carrierdelay" -> data(1)) ~ ("weatherdelay" -> data(1)) ~ ("nasdelay" -> data(1)) ~
             ("securitydelay" -> data(1)) ~ ("lateaircraftdelay" -> data(1)))))

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
