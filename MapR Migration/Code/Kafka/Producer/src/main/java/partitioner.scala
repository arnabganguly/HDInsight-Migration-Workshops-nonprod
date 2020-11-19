/**
  * Created by JohnS on 09/06/2017.
  */

import java.util

import org.apache.kafka.clients.producer.Partitioner
import org.apache.kafka.common.Cluster

import scala.util.Random

class partitioner extends Partitioner {

  // Randomly assign a partition
  override def partition(s: String, o: scala.Any, bytes: Array[Byte], o1: scala.Any, bytes1: Array[Byte], cluster: Cluster): Int = {
    val numPartitions: Int = eventdriver.inputOptions(Symbol(ClientArgumentKeys.NumPartitions)).asInstanceOf[Int]

    val rnd = Random.nextInt(numPartitions)
    return rnd
  }

  override def configure(map: util.Map[String, _]): Unit = {
    // Do nothing
  }

  override def close(): Unit = {
    // Do nothing
  }
}
