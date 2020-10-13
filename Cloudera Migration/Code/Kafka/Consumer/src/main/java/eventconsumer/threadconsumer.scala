/**
  * Created by JohnS on 09/06/2017.
  */

package eventconsumer

import java.util.{Collections, Properties}

import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}

class threadconsumer(val topicName: String, val props: Properties, val threadNum: Int) extends Runnable {

  // Create the consumer
  private var consumer = new KafkaConsumer[Long, String](props)

  def run: Unit = {
    println(s"Starting consumer for thread $threadNum")

    // Subscribe to the specified topic
    consumer.subscribe(Collections.singleton(topicName))

    try {
      while (true) {
        // Poll for event data
        val records: ConsumerRecords[Long, String] = consumer.poll(1000)

        // Read the next batch of event records received
        val recordIterator = records.iterator
        while (recordIterator.hasNext) {
          // Display the data a record at a time
          val data: ConsumerRecord[Long, String] = recordIterator.next
          println(s"Key: ${data.key}\nValue: ${data.value}\nOffset: ${data.offset}\nPartition ${data.partition}\n")
        }
        // Advance the offset pointer for the consumer
        consumer.commitSync()
      }
    } catch {
      case e: Exception => println(s"Exception receiving message: $e")
    } finally {
      consumer.close()
    }
  }
}
