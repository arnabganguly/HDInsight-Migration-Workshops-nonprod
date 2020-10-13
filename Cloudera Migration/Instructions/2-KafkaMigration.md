# Exercise 1 - Introduction
In this exercise, you'll migrate a Kafka workload from Cloudera to HDInsight. You'll perform the following tasks:

- Examine the existing Kafka workload running on Cloudera, including the Kafka producer and consumer applications.
- Create the virtual infrastructure for an HDInsight Kafka cluster, and then create the cluster.
- Configure MirrorMaker to replicate topics on the Cloudera cluster to the HDInsight cluster.
- Reconfigure the Kafka consumer application to receive messages from Kafka running on the HDInsight cluster.
- Reconfigure the Kafka producer application to post messages to Kafka running on the HDInsight cluster.

At the end of this process, all incoming data will be received and processed through the Kafka cluster on HDInsight, and the corresponding functionality in the Cloudera cluster can be retired.

## Task 1: Examine the existing Kafka workload

The existing Kafka workload receives incoming data about airports, flights, delays, and other tracking information from a variety of sources. The Kafka producer application posts this data to Kafka topics as messages. The Kafka consumer retrieves these messages and processes them so that the data can be analyzed. 

---

**NOTE:**
In the *live* Cloudera system, the consumer is a Spark application running in a separate cluster, but for the purposes of this exercise, you'll run a Scala application that simply retrieves and displays the messages from the Kafka topics. Additionally, in this exercise, the Kafka producer simulates receiving data by reading it from a CSV file.

---

![The structure of the existing Kafka system running on Cloudera](../Images/2-KafkaSystem.png)

1. STEP 1
2. STEP 2

## Task 2: Create the HDInsight Kafka cluster

In this task, you'll create an Azure Data Lake Gen 2 storage account to be used as the cluster storage for the HDInsight Kafka cluster. Next, you'll add a user-assigned managed identity that HDInsight will use to access the cluster storage. You'll create a new virtual network and subnet to host the cluster. Finally, you'll create the Kafka cluster using this virtual infrastructure.

<img alt="The virtual infrastructure required by te HDInsight Kafka cluster" src="../Images/2-HDInsightKafka.png" width=50%>

1. STEP 1
2. STEP 2

## Task 3: Configure MirrorMaker to replicate topics

In this task, you'll configure peering between the virtual network containing the Cloudera cluster and the virtual network for the HDInsight Kafka cluster. You'll then use MirrorMaker to replicate Kafka topics from the Cloudera cluster to the HDInsight cluster:

![Kafka on Cloudera replicating data to the HDInsight cluster using MirrorMaker](../Images/2-MirrorMaker.png)

1. STEP 1
2. STEP 2

## Task 4: Reconfigure the Kafka consumer application

In this task, you'll reconfigure the Kafka consumer application to subscribe to topics in the HDInsight Kafka cluster.

![Kafka consumer subscribing to topics on the HDInsight cluster](../Images/2-HDInsightConsumer.png)

1. STEP 1
2. STEP 2

## Task 5: Reconfigure the Kafka producer application

In this task, you'll update the producer application to post messages to topics in the HDInsight cluster. After this step is complete, you can decommission Kafka in the Cloudera cluster.

![Kafka producer posting to topics on the HDInsight cluster](../Images/2-HDInsightProducer.png)

1. STEP 1
2. STEP 2
