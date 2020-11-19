/**
 * Created by JohnS on 26/10/2020.
 */

import java.util.{Collections, Properties}

import scala.jdk.CollectionConverters._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer

object eventconsumer {

  var inputOptions: Map[Symbol, Any] = _

  def main(args: Array[String]): Unit = {

    // Parse the command line arguments
    inputOptions = ClientArgumentParser.parseArguments(Map(), args.toList)
    ClientArgumentParser.verifyArguments(inputOptions)
    println(inputOptions)

    // Retrieve the values of the command line arguments
    val bootstrapServers: String = inputOptions(Symbol(ClientArgumentKeys.BootstrapServers)).asInstanceOf[String]
    val topicName: String = inputOptions(Symbol(ClientArgumentKeys.TopicName)).asInstanceOf[String]
    val partition: Int = inputOptions(Symbol(ClientArgumentKeys.Partition)).asInstanceOf[Int]

    // Set up client properties
    val props = new Properties
    props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    // Just a user-defined string to identify the consumer group
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumers")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000")
    props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    try {
      val consumer = new KafkaConsumer[String, String](props)
      try { // List of topics to subscribe to
        //consumer.subscribe(Arrays.asList("taxi2"));
        val topicPartition = new TopicPartition(topicName, partition)
        consumer.assign(Collections.singletonList(topicPartition))
        consumer.seekToBeginning(Collections.singletonList(topicPartition))
        while ( {
          true
        }) try {
          val records = consumer.poll(100)
          for (record <- records.asScala) {
            System.out.printf("Offset = %d\n", record.offset)
            System.out.printf("Value  = %s\n", record.value)
          }
        } catch {
          case e: Exception =>
            e.printStackTrace()
        }
      } finally if (consumer != null) consumer.close()
    }
  }
}
