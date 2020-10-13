/**
  * Created by JohnS on 08/06/2017.
  */

package eventconsumer

import java.util.Properties
import org.apache.kafka.clients.consumer.ConsumerConfig
import java.util.concurrent.{ExecutorService, Executors}


object eventreceiver {

  var inputOptions: Map[Symbol, Any] = _

  def main(inputArguments: Array[String]): Unit = {

    // Parse the command line arguments
    inputOptions = ClientArgumentParser.parseArguments(Map(), inputArguments.toList)
    ClientArgumentParser.verifyArguments(inputOptions)
    println(inputOptions)

    // Retrieve the values of the command line arguments
    val bootstrapServers: String = inputOptions(Symbol(ClientArgumentKeys.BootstrapServers)).asInstanceOf[String]
    val topicName: String = inputOptions(Symbol(ClientArgumentKeys.TopicName)).asInstanceOf[String]
    val groupid: String = inputOptions(Symbol(ClientArgumentKeys.GroupID)).asInstanceOf[String]
    val numConsumers: Int = inputOptions(Symbol(ClientArgumentKeys.NumConsumers)).asInstanceOf[Int]

    // Construct a Properties object for the consumer
    var props: Properties = new Properties
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupid)
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.LongDeserializer")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")

    // Create a thread pool for running the consumers
    var executor: ExecutorService = Executors.newFixedThreadPool(numConsumers)

    // Start each consumer in its own thread
    for (threadNum: Int <- 1 to numConsumers) {
      executor.submit(new threadconsumer(topicName, props, threadNum))
    }
  }
}
