# Exercise 3: Migrate a Spark workload to HDInisght

## Introduction

In this exercise, you'll migrate a Spark workload from Cloudera to HDInsight. You'll perform the following tasks:

- Examine the existing Spark workload running on Cloudera.
- Create an HDInsight Spark cluster.
- Migrate the code for the Spark workload to the HDInsight cluster, and make any changes required.
- Test the Spark workload on the HDInsight cluster.

At the end of this process, the Spark applications will run on the HDInsight cluster, and retrieve data from the HDInsight cluster you created in the previous exercise.

## Task 1: Run the existing Spark workload

The existing Spark workload generates reports based on the flight information stored in the Hive database. Specifically, it creates the following reports:

- Origin airport, destination airport, and number of flights for this route ordered by the number of flights servicing this route
- Airline, and number of flights
- Origin airport and and departure delay in descending order of departure delay
- Destination airport and arrival delay in descending order of arrival delay

The data in the Hive database comprises selected fields from the data received from Kafka. The Kafka data stream includes the IATA codes for each airport, but not the names. Instead, airport names and other static information are stored in a separate CSV file that is accessed as an external table in Hive. 

The Spark workload runs as a script inside a Jupyter notebook:

![The structure of the existing Spark/Hive/Jupyter Notebooks system running on Cloudera](../Images/3-SparkSystem.png)
 
### Create the airportdata external table in Hive

1. If you haven't already done so, on your desktop, open a **Command Prompt** window and sign in to the Cloudera virtual machine. The username is **azureuser***. Replace *\<ip_address`>* with the IP address of the virtual machine.

    ```PowerShell
    ssh azureuser@<ip address>
    ```

1. Move to the **apps/reports** folder: 

    ```bash
    cd ~/apps/reports
    ```

1. Using an editor of your choice, open the file **airports.csv**. This file contains the static information about airports. The important fields for this exercise are **iata**, which contains the airport code, and **airport**, which holds the name of the airport.

    Close the editor when you have finished browsing the data.

1. Run the following commands to create a folder named **airportdata** in HDFS, and upload the **airports.csv** file to this folder:

    ```bash
    hdfs dfs -mkdir airportdata
    hdfs dfs -copyFromLocal airports.csv airportdata
    ```

1. Verify that the file has been uploaded correctly:

    ```bash
    hdfs dfs -ls airportdata
    ```

    The command should respond with a message similar to that shown below:

    ```text
    Found 1 items
    -rw-r--r--   3 azureuser azureuser     247809 2020-11-16 17:43 airportdata/airports.csv
    ```

1. Start the **hive** utility:

    ```bash
    hive
    ```

1. Create an external table named **airportdata** that references the CSV file in the  **airportdata** folder:

    ```sql
    CREATE EXTERNAL TABLE IF NOT EXISTS airportdata(
        iata STRING, 
        airport STRING, 
        city STRING,  
        state STRING,  
        country STRING, 
        lat DECIMAL(11,8),
        long DECIMAL(11,8)
    )
    COMMENT 'Airport names and locations'
    ROW FORMAT DELIMITED
    FIELDS TERMINATED BY ','
    STORED AS TEXTFILE
    LOCATION '/user/azureuser/airportdata';
    ```

1. Query the number of rows in the table to verify that it has been created successfully:

    ```sql
    SELECT COUNT(*)
    FROM airportdata;
    ```

    The table should contain 3377 rows

1. Quit the **hive** utility:

    ```sql
    exit;
    ```
### Run the **FlightStats** Jupyter notebook

1. Run the following command to start the Jupyter notebook server:

    ```bash
    pyspark2 --driver-cores 2 --executor-memory 512m
    ```

    ---

    **NOTE:** This command starts the Jupyter service running, and also displays a simple character mode interface which you can ignore for the time being.

    ---

1. Using the web browser, move to the URL <ip-address>:8888, where *\<ip-address\>* is the IP address of the Cloudera virtual machine. You should see the Jupyter Notebooks login page:

    ![The Jupyter Notebooks login page.](../Images/3-Jupyter-Notebooks-Login.png)

1. Enter the password **Pa55w.rdDemo**, and then select **Log in**. 

    The **Files** page will appear, listing the **FlightStats** notebook and the **airports.csv** file:

    ![The **Files** page in Jupyter Notebooks.](../Images/3-Jupyter-Notebooks-Files.png)


1. Select the **FlightStats.ipynb** notebook.

    The notebook should open in a new browser window. The cells in the workbook contain Python code that uses Spark to query the data in the Hive database and generate the various reports described earlier.

    ![The notebook, showing the Python code to run the various HIve queries for the report](../Images/3-Jupyter-Notebook.png)


1. In the **Cell** menu, select **Run All**:

    ![The **Cell** menu. The user has selected **Run All**](../Images/3-Jupyter-Run-All.png)


1. Wait while the notebook runs. It should generate the various reports, and display the results using barcharts.

    ![The notebook after it has run, displaying the reports and charts generated by the Spark queries](../Images/3-Jupyter-Results.png)

1. Close the browser window displaying the notebook.

1. Return to the SSH session running on the Cloudera virtual machine.

1. Press **q** to stop the Jupyter notebook server, and then press **y** to confirm that you want to halt te server.

1. When prompted, press **Ctrl-C** to shut down the kernels run by the server, and then press **y** to confirm.
## Task 5: Tidy up

1. In the Azure portal, go to the page for the HDInsight Spark cluster.

1. In the command bar, select **Delete**:

1. In the confirmation pane, enter the name of the cluster, and then select **Delete**.

1. Go to the page for the HDInsight LLAP cluster.

1. In the command bar, select **Delete**:

1. In the confirmation pane, enter the name of the cluster, and then select **Delete**.

### Create the Spark cluster

1. On the Azure Home page, select **Create a resource**.

1. On the **New** page, in the **Search the Marketplace** box, type **Azure HDInsight**, and then select **Azure HDInsight** from the list that appears.

1. On the **Azure HDInsight** page, select **Create**.

1. On the **Basics** tab of the **Create HDInsight cluster** page, enter the following settings, and then select **Next: Storage**:

    | Field | Value|
    |-|-|
    | Subscription | Select your subscription |
    | Resource group | clusterrg |
    | Cluster name | sparkcluster*nnnn*, where *nnnn* is the same random four digit number you used for the SQL Database (if necessary, you can use a different number, but for consistency try and reuse the same value if possible) |
    | Region | Select the same region used by the Cloudera virtual machine and the **clusterrg** resource group |
    | Cluster type | Spark |
    | Version | Spark 2.4 (HDI 4.0) |
    | Cluster login name | admin |
    | Cluster login password | Pa55w.rdDemo |
    | Confirm cluster login password | Pa55w.rdDemo |
    | Secure Shell (SSH) username | sshuser |
    | Use cluster login password for SSH | checked |

1. On the **Storage** tab, enter the following settings, and then select **Next: Security + networking**:

    | Field | Value|
    |-|-|
    | Primary storage type | Azure Data Lake Storage Gen2 |
    | Primary storage account | Select the storage account you previously created for the Kafka and Hive clusters (**clusterstorage*9999***)|
    | Filesystem | Reuse the same container that you created for the Kafka and Hive clusters (**cluster*9999***) |
    | Identity | clustermanagedid |
    | SQL database for Ambari | leave blank |
    | SQL database for Hive | hiveserver*9999*/hivedb*9999* |
    | Authenticate SQL Database | Select **Authenticate**. On the **Authenticate** page, enter **azuresa** for the username, provide the password you created for this user in the database, and then click **Test Connection**. You should receive a warning informing you that Azure couldn't currently validate the database credentials, but that you can proceed. Click **Select** to finish.
    | SQL database for Ooozie | leave blank |

1. On the **Security + networking** tab, enter the following settings, and then select **Next: Configuration + pricing**

    | Field | Value|
    |-|-|
    | Enable enterprise security package | Leave unchecked |
    | Minimum TLS version | 1.2 |
    | Virtual network | clustervnet/clusterrg |
    
    Leave all remaining settings on this tab with their default values.

1. On the **Configuration + pricing** tab, reduce the number of **Worker nodes** to 3 (to save costs for this exercise). Select **Review + create**.

1. On the validation page, select **Create**, and wait while the cluster is created.

    ---

    **NOTE:** 
    
    This operation may take 15 or 20 minutes to complete

    ---

# START HERE
### Configure the cluster network connectivity

1. On the Home page in the Azure portal, under **Recent resources**, select **sparkcluster*9999***.

1. On the **Overview** page for the cluster, under **Dashboards**, select **Ambari home**.

1. Sign in to Ambari as **admin** with password **Pa55w.rdDemo** when prompted. The Ambari page should show that the cluster is running the HDFS, Hive, and Zookeeper services (amongst others).:

    ![The Ambari home page, showing the running services for the Spark cluster.](../Images/3-Ambari-Home.png)

1. In the left-hand pane of the Ambari page, select **Hosts**. Make a note of the name prefixes and IP addresses of the worker nodes with the prefixes **wn0**, **wn1**, and **wn2**.

1. Return to the **Command Prompt** window displaying the SSH connection to the Cloudera virtual machine.

1. On the Cloudera virtual machine. run the following command to create a bash shell running as root.

    ```bash
    sudo bash
    ```

1. Edit the **/etc/hosts** file, and add entries for each of the worker nodes in the HDInsight Kafka cluster to the end of the file. The file below shows an example, based on the screenshot shown above:

    ```text
    127.0.0.1 localhost

    # The following lines are desirable for IPv6 capable hosts
    ::1 ip6-localhost ip6-loopback
    fe00::0 ip6-localnet
    ff00::0 ip6-mcastprefix
    ff02::1 ip6-allnodes
    ff02::2 ip6-allrouters
    ff02::3 ip6-allhosts

    # Entries for worker nodes
    10.3.0.11 wn0-sparkc
    10.3.0.4  wn2-sparkc
    10.3.0.14 wn4-sparkc
    ```

1. Run the **ifconfig** command, and make a note of the **inet addr** field for the **eth0** device. In the example shown below, the **inet addr** is 10.10.0.4.

    ```text
    root@onprem:~/apps/kafka# ifconfig
    eth0    Link encap:Ethernet  HWaddr 00:0d:3a:98:f9:70
            inet addr:10.10.0.4  Bcast:10.10.0.255  Mask:255.255.255.0
            inet6 addr: fe80::20d:3aff:fe98:f970/64 Scope:Link
            UP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1
            RX packets:45434 errors:0 dropped:15 overruns:0 frame:0
            TX packets:51027 errors:0 dropped:0 overruns:0 carrier:0
            collisions:0 txqueuelen:1000
            RX bytes:21979594 (21.9 MB)  TX bytes:11414674 (11.4 MB)

    lo      Link encap:Local Loopback
            inet addr:127.0.0.1  Mask:255.0.0.0
            ...
    ```

1. Run the following command to quit the root shell and return to the azureuser shell.

    ```bash
    exit
    ```

1. In the Azure portal, on the page for **llapcluster*9999***, under **Settings**, select **SSH + Cluster login**. In the **SSH + Cluster login** pane, in the **Hostname** list select your cluster, and then make a note of the **ssh** command you can use to connect to this cluster:

1. On the desktop, open another command prompt window, and run the SSH command you just noted, to sign in to the head node of the Kafka cluster. The password is **Pa55w.rdDemo**:

    ```bash
    ssh sshuser@llapcluster9999-ssh.azurehdinsight.net
    ```

1. Start a shell running as root:

    ```bash
    sudo bash
    ```
1. Edit the file **/etc/hosts**, and add an entry for the Cloudera virtual machine. You noted the IP address of the Cloudera virtual machine earlier. The virtual machine has the name **onprem**, with the FQDN of **onprem.internal.cloudapp.net**. The file below shows an example, using the IP address 10.10.0.4:

    ```text
    127.0.0.1 localhost

    # The following lines are desirable for IPv6 capable hosts
    ::1 ip6-localhost ip6-loopback
    fe00::0 ip6-localnet
    ff00::0 ip6-mcastprefix
    ff02::1 ip6-allnodes
    ff02::2 ip6-allrouters
    ff02::3 ip6-allhosts
    10.3.0.16 hn0-llapcl.kaetua2hhycevkq3hkawfmrwjh.bx.internal.cloudapp.net hn0-llapcl hn0-llapcl.kaetua2hhycevkq3hkawfmrwjh.bx.internal.cloudapp.net.
    ...
    # Cloudera virtual machine
    10.10.0.4 onprem.internal.cloudapp.net onprem
    ```

1. Run the following command to quit the root shell and return to the sshuser shell.

    ```bash
    exit
    ```

1. Connect to the first worker node. The password is **Pa55w.rdDemo**, as before (the example below uses **wn0-llapcl**, although the name of the first node in your cluster may be different):

    ```bash
    ssh wn0-llapcl
    ```

1. Start a shell running as root, edit the **/etc/hosts** file, add the entry for the **onprem** virtual machine as before, and then exit the root shell.

1. Disconnect from the **wn0-llapcl** node, and return to the head node of the cluster.

1. Repeat the previous three steps for the two remaining worker nodes, **wn1-llapcl** and **wn2-llapcl**.

    ---

    **NOTE:**
    Under some circumstances, the worker nodes might not be named sequentially. For example, you might that they are named **wn2-llapcl** and **wn4-llapcl**, as illustrated in the examples shown above. Check the entries in the **/etc/hosts** file of the head node for the names of these nodes.

    ---
