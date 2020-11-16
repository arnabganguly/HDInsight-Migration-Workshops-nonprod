# Exercise 3: Migrate a Spark workload to HDInisght

## Introduction

In this exercise, you'll migrate a Spark workload from Cloudera to HDInsight. You'll perform the following tasks:

- Examine the existing Spark workload running on Cloudera.
- Create an HDInsight Spark cluster.
- Migrate the code for the Spark workload to the HDInsight cluster, and make any changes required.
- Test the Spark workload on the HDInsight cluster.

At the end of this process, the Spark applications will run on the HDInsight cluster, and retrieve data from the HDInsight cluster you created in the previous exercise.

## Task 1: Run the existing Kafka workload

The existing Spark workload generates reports based on the flight information stored in the Hive database. Specifically, it creates the following reports:

- Origin airport, destination airport, and number of flights for this route
- Airline, and number of flights
- Origin airport, departure delay, and average departure delay
- Destination airport, arrival delay, and average arrival delay

The data in the Hive database comprises selected fields from the data received from Kafka. The Kafka data stream includes the IATA codes for each airport, but not the names. Instead, airport names and other static information are stored in a separate CSV file that is accessed as an external table in Hive.
 




## Task 5: Tidy up

1. In the Azure portal, go to the page for the HDInsight Spark cluster.

1. In the command bar, select **Delete**:

1. In the confirmation pane, enter the name of the cluster, and then select **Delete**.

1. Go to the page for the HDInsight LLAP cluster.

1. In the command bar, select **Delete**:

1. In the confirmation pane, enter the name of the cluster, and then select **Delete**.

