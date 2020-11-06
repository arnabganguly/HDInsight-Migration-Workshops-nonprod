# Exercise 2 - Introduction

In this exercise, you'll migrate an active Hive database from Cloudera to HDInsight. You'll perform the following tasks:

- Examine the workload and database currently running on Cloudera.
- Create the virtual infrastructure for an HDInsight Hive LLAP cluster, and then create the cluster.
- Configure Hive replication to transfer the Hive database from Cloudera to the HDInsight cluster.
- Verify that the data is being replicated correctly.

At the end of this process, the Hive database will have been relocated to the HDInsight cluster. Any applications that utilize the database can be reconfigured to connect to the instance on HDInsight.

## Task 1: Run the existing workload

In the existing on-premises system, a Spark application acts as a receiver for messages arriving on a Kafka topic. The Spark application posts selected parts of this data to a Hive database for analysis. In this exercise, you'll replicate the Cloudera Hive database to an HDInsight cluster while the Spark application is running. 

The first task is to examine the existing system

---

**NOTE:**
In a *fully migrated* system, the Spark application would run on an HDInsight Spark  cluster, and connect to Kafka running on an HDInsight Kafka cluster, as configured by the previous exercise. To save the costs of running multiple HDInsight clusters, in this exercise you'll revert to running Kafka on the original Cloudera cluster.

---

![The structure of the existing Kafka/Spark/Hive system running on Cloudera](../Images/3-HiveSystem.png)













When the migration is complete, you The existing Kafka workload receives incoming data about airports, flights, delays, and other tracking information from a variety of sources. The Kafka producer application posts this data to Kafka topics as messages. The Kafka consumer retrieves these messages and processes them so that the data can be analyzed. 
