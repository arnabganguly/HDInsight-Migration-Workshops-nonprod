# Overview

In this workshop, you'll learn how to:

- Migrate a Kafka workload from Cloudera to an HDInsight 4.0 Kafka cluster.
- Migrate a Hive workload and data from Cloudera to an HDInsight 4.0 LLAP cluster.
- Migrate a Spark workload from Cloudera to an HDInsight 4.0 Spark cluster.
- Migrate HBase data from Cloudera to an HDInsight 4.0 HBase cluster.

This workshop follows the techniques and strategies described in the document **Migrate your Big Data Workloads to HDInsight**.

# Scenario

Imagine that you work for Contoso, an organization that performs Big Data analytics for the transport industry. The organization is currently engaged in a project that examines the data for commercial airlines, tracking which airlines fly to and from which airports, and analysing the flight times and frequency of flight delays. The data arrives from a number of sources, and includes real-time tracking information provided by the airlines and the various airports. The raw data is captured using Kafka and passed to a number of clients for processing using Spark and Hive. The processed data is stored in an HBase database for subsequent analysis. The current system is based on a Cloudera cluster, running  Hive, Spark, HBase, and Kafka services. The following image shows the high-level architecture of the existing system:

![The high-level architecture of the existing Cloudera-based system](../Images/0-ClouderaSystem.png)

Contoso wish to move operations from Cloudera to HDInsight. However, the system cannot be shutdown while migration occurs, so the transfer must e performed in a manner that allows operations to continue during the switchover, although some temporary degradation in performance is permissible while this occurs.

You will migrate from the Cloudera cluster to four HDInsight clusters, utilizing shared cluster storage and metadata databases. This architecture enables you to decouple and tune each cluster for a specific workload, and allows you to scale the clusters independently. The solution architecture looks like this:

![The high-level architecture of the new system comprising four HDInsight clusters](../Images/0-HDInsightSystem.png)

The HBase cluster utilizes its own storage account. The rationale behind this approach is that while Azure Data Lake Gen 2 storage gives the best performance for Hive, Spark, and Kafka clusters, HBase works best with Azure Blob storage.

---

**NOTE:** 

Due to time constraints, in this workshop you will not deploy the clusters using the Enterprise Security Package. In a production system, you must always ensure that your clusters run in a safe and secure environment.

---

# Setup

Before starting the workshop, set up the Contoso environment containing the apps and services that you will migrate to HDInsight. To save costs in this workshop, the Contoso environment consists of a single node cluster. This cluster runs Cloudera 5.16.2 and Kafka 3.1, to simulate a legacy system.

Perform the following tasks:

1. Sign in to the Azure portal using a web browser.

1. On the **Home** page, click **Subscriptions**.

    ![The **Home** page in the Azure portal. The user has selected **Subscriptions**.](../Images/0-PortalHome.png)

1. Make a note of the **Subscription ID** associated with your account.

    ![The **Subscriptions** page in the Azure portal. The user's subscription is highlighted.](../Images/0-Subscriptions.png)

1. In the toolbar, click **Cloud Shell**.

    ![The toolbar page in the Azure portal. The user has selected **Cloud Shell**.](../Images/0-CloudShell.png)

1. In the Cloud Shell dropdown list, select **PowerShell**. Click **Confirm** if prompted.

    ![The Cloud Shell dropdown. The user has selected **PowerShell**.](../Images/0-PowerShell.png)

1. In the Cloud Shell toolbar, select **Open new session**.

    ![The Cloud Shell toolbar in the Azure portal. The user has selected **Open a new session**.](../Images/0-NewSession.png)

    This command opens a new browser window, and gives you more space to run and view scripts.

1. Run the command shown below to retrieve the script that create the resources and the virtual machine that will be used to run Cloudera.

    **NOTE: BEFORE PUBLISHING THIS WORKSHOP, REPLACE THE URL BELOW WITH THE ADDRESS OF THE GITHUB REPO HOLDING THE SCRIPT**
    ```PowerShell
    wget https://raw.githubusercontent.com/JohnPWSharp/MigrationWorkshop/main/clouderasetup.ps1
    ```

1. In the Cloud Shell toolbar, select **Open editor**.

    ![The Cloud Shell toolbar in the Azure portal. The user has selected **Open editor**.](../Images/0-OpenEditor.png)

1. In the **Files** pane of the editor, select **clouderasetup.ps1** to open the setup script. In the script, replace **\<your-subscription-id\>** with your subscription id, and replace **\<SAS\>** with the SAS URL of the cloudera disk that will be used to create the virtual machine. **Your instructor should provide you with this URL**.


    ![The Cloud Shell editor. The user has opened the **clouderasetup.ps1** script.](../Images/0-EditFile.png)


1. Press **CTRL-S** to save the file, and then press **CTRL-Q** to leave the editor.

1. Run the script with the following command:

    ```PowerShell
    .\clouderasetup.ps1
    ```

    As the script runs, you will see various messages when the resources are created. The script will take about 5 minutes to complete. When it has finished, it will display the IP address of the new virtual machine. Make a note of this address.

    ![The Azure Cloud Shell in the Azure portal. The script has completed. The IP address of the new virtual machine is highlighted.](../Images/0-ScriptCompleted.png)

1. Connect using SSH as the **root** user as shown below. Replace **\<ip address\>** with the IP address of the virtual machine. The password is **Pa55w.rdDemo**. Enter **yes** when prompted to connect.

    ---

    **NOTE:** 
    
    You may need to wait for a minute while the virtual machine services start before continuing

    ---

    ```PowerShell
    ssh root@<ip address>
    ```

1. At the *bash* prompt, run the following command to set the password for the **azureuser** account. Provide a password of your own choosing. You'll use this account rather than root for running the Cloudera services.

    ```bash
    passwd azureuser
    ```

1. Run the following command to sign out from the virtual machine and return to the PowerShell prompt:

    ```bash
    exit
    ```


1. In the Azure portal, close the PowerShell pane.

1. On the desktop, open a Web browser, and navigate to the URL \<ip-address\>:7180, where *\<ip-address\>* is the IP address of the virtual machine you noted earlier. You should see the Cloudera Manager login page.

    ---

    **NOTE:** 
    Again. you may need to wait for a minute while the Cloudera Manager is initialized.
    
    ---
    
    ![The Cloudera Manager login page in the web browser.](../Images/0-Cloudera-Login.png)

1. Log in with the username **admin** with password **admin**.

2. In the Cloudera Manager, select the drop-down menu for the **Cloudera Management Service**, select **Start**, wait for the management service to start up correctly, and then select **Close**.

    ![The Cloudera Management Service menu. The user has selected **Start**.](../Images/0-Start-Cloudera-Manager.png)

3. Select the drop-down menu for the **Cluster 1** cluster, select **Start**, wait for the various services (Zookeeper, HDFS, Kafka, HBase, Yarn, Spark, and Hive) to start, and then select **Close**.

    ![The Cluster menu. The user has selected **Start**.](../Images/0-Start-Cloudera-Services.png)

4. Verify that all services are shown as running correctly.

    ---

    **NOTE:** 
    You may receive a warning from HDFS and/or Zookeeper initially, but they should clear after a minute or so.

    ---

    ![The Cluster Manager. All services have started successfully.](../Images/0-Cloudera-Services-Running.png)
