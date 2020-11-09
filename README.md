# HDInsight Migration Workshops

---

**IMPORTANT:**

This lab creates several HDInsight clusters and a virtual machine for running Cloudera. Make sure that you delete these resources when you have finished, otherwise you will generate considerable charges.

Additionally, an HDInsight cluster comprises a number of virtual machines. You may need to request an increase in the quota for the number of CPU cores for your subscription (64 cores recommended). For more information, read [Standard quota: Increase limits by VM series](https://docs.microsoft.com/azure/azure-portal/supportability/per-vm-quota-requests).

---

Before starting this lab, [read the introduction and perform the setup steps](Cloudera%20Migration/Instructions/Introduction.md).

This lab contains four exercises showing how to:

1. [Migrate a Cloudera Kafka workload to an Azure HDInsight Kafka cluster by using Kafka Mirroring](Cloudera%20Migration/Instructions/2-KafkaMigration.md),

1. [Migrate a Cloudera Hive workload to an Azure HDInsight LLAP cluster by using Hive replication](Cloudera%20Migration/Instructions/3-HIveMigration.md),

1. [Migrate a Cloudera HBase database to an Azure HDInsight HBase cluster by using HBase snapshots](Cloudera%20Migration/Instructions/2-HBaseMigration.md), and

1. [Migrate a Cloudera Spark workload to an Azure HDInsight Spark cluster, using HDFS tools and AzCopy to transfer data](Cloudera%20Migration/Instructions/2-SparkMigration.md). 
