# Overview

In this workshop, you'll learn how to:

- Migrate a Kafka workload from MapR to an HDInsight 4.0 Kafka cluster.
- Migrate a Hive workload and data from MapR to an HDInsight 4.0 LLAP cluster.
- Migrate a Spark workload from MapR to an HDInsight 4.0 Spark cluster.
- Migrate HBase data from MapR to an HDInsight 4.0 HBase cluster.

This workshop follows the techniques and strategies described in the document **Migrate your Big Data Workloads to HDInsight**.

# Scenario

Imagine that you work for Contoso, an organization that performs Big Data analytics for the transport industry. The organization is currently engaged in a project that examines the data for commercial airlines, tracking which airlines fly to and from which airports, and analysing the flight times and frequency of flight delays. The data arrives from a number of sources, and includes real-time tracking information provided by the airlines and the various airports. The raw data is captured using Kafka and passed to a number of clients for processing using Spark and Hive. The processed data is stored in an HBase database for subsequent analysis. The current system is based on a MapR cluster, running  Hive, Spark, HBase, and Kafka services. The following image shows the high-level architecture of the existing system:

![The high-level architecture of the existing MapR-based system](../Images/0-MapRSystem.png)

Contoso wish to move operations from MapR to HDInsight. However, the system cannot be shutdown while migration occurs, so the transfer must e performed in a manner that allows operations to continue during the switchover, although some temporary degradation in performance is permissible while this occurs.

You will migrate from the MapR cluster to four HDInsight clusters, utilizing shared cluster storage and metadata databases. This architecture enables you to decouple and tune each cluster for a specific workload, and allows you to scale the clusters independently. The solution architecture looks like this:

![The high-level architecture of the new system comprising four HDInsight clusters](../Images/0-HDInsightSystem.png)

The HBase cluster utilizes its own storage account. The rationale behind this approach is that while Azure Data Lake Gen 2 storage gives the best performance for Hive, Spark, and Kafka clusters, HBase works best with Azure Blob storage.

---

**NOTE:** 

Due to time constraints, in this workshop you will not deploy the clusters using the Enterprise Security Package. In a production system, you must always ensure that your clusters run in a safe and secure environment.

---

# Setup

Before starting the workshop, set up the Contoso environment containing the apps and services that you will migrate to HDInsight. To save costs in this workshop, the Contoso environment consists of a single node cluster. This cluster runs MapR 5.16.2 and Kafka 3.1, to simulate a legacy system.

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

1. Run the commands shown below to create the managed disk used by the virtual machine that will be used to run MapR. The script creates the managed disk from a pre-created image. Replace *\<your-subscription-id\>* with the subscription ID you noted previously, and set *\<SAS\>* to the SAS token for the pre-created image (your instructor will provide this URL). You should also change the *location* variable to match your nearest Azure region:

    ```PowerShell
    #Provide the subscription Id
    $subscriptionId = '<your-subscription-id>'

    #Specify the location for creating resources
    $location = "East US"

    #SAS URL of pre-created image
    $sourceDiskSAS = '<SAS>'    

    $targetRG = 'MapRrg'
    $targetLocation = $location
    $targetOS = 'Linux'
    $osDiskName = 'MapRdisk'
    $vhdSizeBytes = 68719477248

    #Create a resource group (MapRrg) for holding the managed disk
    Select-AzSubscription -SubscriptionId $SubscriptionId
    New-AzResourceGroup -Name $targetRG -Location $targetLocation

    # Create the managed disk
    $targetDiskConfig = New-AzDiskConfig `
        -SkuName 'Standard_LRS' `
        -osType 'Linux' `
        -UploadSizeInBytes $vhdSizeBytes `
        -Location $targetLocation `
        -CreateOption 'Upload'

    $targetDisk = New-AzDisk -ResourceGroupName $targetRG `
        -DiskName $osDiskName `
        -Disk $targetDiskConfig

    $targetDiskSas = Grant-AzDiskAccess -ResourceGroupName $targetRG `
        -DiskName $osDiskName `
        -DurationInSecond 86400 -Access 'Write'

    #Copy the contents of the pre-created disk to the managed disk
    azcopy copy $sourceDiskSAS $targetDiskSas.AccessSAS `
        --blob-type PageBlob

    Revoke-AzDiskAccess -ResourceGroupName $targetRG `
        -DiskName $osDiskName
    ```

    ---

    **NOTE**

    The MapR virtual machine created in the following steps uses its own managed disk based on a copy of the disk you have just created. If you need to rebuild or reset the virtual machine at any point, you can start at the following step. You don't need to copy the disk from the SAS URL again.

    ---

1. Run the following commands to set up the parameters and configuration settings for the Contoso virtual machine and its associated resources. 

    ```PowerShell
    #Provide the name of your resource group
    $resourceGroupName = 'workshoprg'

    #Provide the name of the resource group and name of the the managed disk 
    # on which the VM is based.
    #These steps assume that the student hasn't modified the values for the 
    # resource group or disk name in the previous step
    $diskResourceGroupName = 'MapRrg'
    $sourceDiskName = 'MapRdisk'

    #Provide the name of the OS disk that will be created using the snapshot
    $osDiskName = 'MapRdisk'

    #Provide the name of a virtual network and subnet 
    # where virtual machine will be created
    $virtualNetworkName = 'MapRvmvnet'
    $subnetName = 'MapRsubnet'

    #VNet prefix for the VM
    $vnetprefix = '10.10.0.0'

    #Network security group name for the VNet
    $nsgName = 'MapRnsg'

    #Provide the name of the virtual machine
    $virtualMachineName = 'MapRvm'

    #Provide the size of the virtual machine
    $virtualMachineSize = 'Standard_D8s_v4'
    ```
1. Create the resource group that will hold the virtual machine and resources:

    ```PowerShell
    #Set the context to the subscription Id where the resources will be created
    Select-AzSubscription -SubscriptionId $SubscriptionId

    #Create the resource group
    New-AzResourceGroup -Name $resourceGroupName -Location $location
    ```

1. Create and configure a virtual network for the virtual machine

    ```PowerShell
    #Create a virtual network for the VM
    $virtualNetwork = New-AzVirtualNetwork `
        -ResourceGroupName $resourceGroupName `
        -Location $location `
        -Name $virtualNetworkName `
        -AddressPrefix $vnetprefix/16

    #Create a network security group for the VNet 
    # and add rules to allow ports required to access MapR
    $nsg = New-AzNetworkSecurityGroup `
        -Name $nsgName `
        -ResourceGroupName $resourceGroupName `
        -Location $location
    
    $nsg | Add-AzNetworkSecurityRuleConfig `
        -Name 'SSHRule' `
        -Access Allow `
        -Protocol Tcp `
        -Direction Inbound `
        -Priority 100 `
        -SourceAddressPrefix Internet `
        -SourcePortRange * `
        -DestinationAddressPrefix * `
        -DestinationPortRange 22 | Set-AzNetworkSecurityGroup

    $nsg | Add-AzNetworkSecurityRuleConfig `
        -Name 'MapRManager' `
        -Access Allow `
        -Protocol Tcp `
        -Direction Inbound `
        -Priority 350 `
        -SourceAddressPrefix Internet `
        -SourcePortRange * `
        -DestinationAddressPrefix * `
        -DestinationPortRange 7180 | Set-AzNetworkSecurityGroup

    $nsg | Add-AzNetworkSecurityRuleConfig `
        -Name 'SCM' `
        -Access Allow `
        -Protocol Tcp `
        -Direction Inbound `
        -Priority 360 `
        -SourceAddressPrefix Internet `
        -SourcePortRange * `
        -DestinationAddressPrefix * `
        -DestinationPortRange 7182 | Set-AzNetworkSecurityGroup

    $nsg | Add-AzNetworkSecurityRuleConfig `
        -Name 'KafkaBroker' `
        -Access Allow `
        -Protocol Tcp `
        -Direction Inbound `
        -Priority 370 `
        -SourceAddressPrefix Internet `
        -SourcePortRange * `
        -DestinationAddressPrefix * `
        -DestinationPortRange 9092 | Set-AzNetworkSecurityGroup

    $nsg | Add-AzNetworkSecurityRuleConfig `
        -Name 'Zookeeper' `
        -Access Allow `
        -Protocol Tcp `
        -Direction Inbound `
        -Priority 380 `
        -SourceAddressPrefix Internet `
        -SourcePortRange * `
        -DestinationAddressPrefix * `
        -DestinationPortRange 2181 | Set-AzNetworkSecurityGroup

    $nsg | Add-AzNetworkSecurityRuleConfig `
        -Name 'Port8080' `
        -Access Allow `
        -Protocol Tcp `
        -Direction Inbound -Priority 390 `
        -SourceAddressPrefix Internet `
        -SourcePortRange * `
        -DestinationAddressPrefix * `
        -DestinationPortRange 8080 | Set-AzNetworkSecurityGroup

    $nsg | Add-AzNetworkSecurityRuleConfig `
        -Name 'Jupyter' `
        -Access Allow `
        -Protocol Tcp `
        -Direction Inbound -Priority 400 `
        -SourceAddressPrefix Internet `
        -SourcePortRange * `
        -DestinationAddressPrefix * `
        -DestinationPortRange 8888 | Set-AzNetworkSecurityGroup

    #Create a subnet for the VM, and associate the NSG with the subnet
    $subnetConfig = Add-AzVirtualNetworkSubnetConfig `
        -Name $subnetName `
        -AddressPrefix $vnetprefix/24 `
        -VirtualNetwork $virtualNetwork `
        -NetworkSecurityGroup $nsg

    $virtualNetwork | Set-AzVirtualNetwork
    ```

1. Create a storage account for holding boot diagnostics for the virtual machine

    ```PowerShell
    #Create a Gen1 storage account with a unique name
    [string]$rnd = Get-Random -Maximum 10000
    $storageAccount = New-AzStorageAccount `
        -AccountName ($VirtualMachineName.ToLower() + 'storage' + $rnd) `
        -ResourceGroupName $resourceGroupName `
        -Location $location `
        -Kind Storage `
        -SkuName Standard_GRS 
    ```

1. Create a disk containing the image for the virtual machine:

    ```PowerShell
    #TBD: THIS MAY NEED TO CHANGE, DEPENDING ON WHERE THE SOURCE DISK IS HOSTED
    #Get the details of the disk containing the image for the virtual machine
    $sourceDisk = Get-AzDisk `
        -ResourceGroupName $diskResourceGroupName `
        -DiskName $sourceDiskName

    #Create a new disk for the virtual machine.
    #The disk must be big enough to hold the image
    $targetDiskConfig = New-AzDiskConfig `
        -SkuName 'Standard_LRS' `
        -osType 'Linux' `
        -UploadSizeInBytes $($sourceDisk.DiskSizeBytes + 512) `
        -Location $Location `
        -CreateOption 'Upload'

    $targetDisk = New-AzDisk -ResourceGroupName $resourceGroupName `
        -DiskName $osDiskName `
        -Disk $targetDiskConfig

    #Copy the image from the source disk to the new disk
    $sourceDiskSas = Grant-AzDiskAccess -ResourceGroupName $diskResourceGroupName `
        -DiskName $sourceDiskName `
        -DurationInSecond 86400 -Access 'Read'

    $targetDiskSas = Grant-AzDiskAccess -ResourceGroupName $resourceGroupName `
        -DiskName $osDiskName `
        -DurationInSecond 86400 -Access 'Write'

    azcopy copy $sourceDiskSas.AccessSAS $targetDiskSas.AccessSAS `
        --blob-type PageBlob

    Revoke-AzDiskAccess -ResourceGroupName $diskResourceGroupName `
        -DiskName $sourceDiskName

    Revoke-AzDiskAccess -ResourceGroupName $resourceGroupName `
        -DiskName $osDiskName
   
    $disk = Get-AzDisk `
        -ResourceGroupName $resourceGroupName `
        -DiskName $osDiskName
    ```

1. Create the virtual machine using the disk and network resources your just configured:

    ```PowerShell
    #Initialize virtual machine configuration
    $VirtualMachine = New-AzVMConfig -VMName $virtualMachineName `
        -VMSize $virtualMachineSize

    #Use the Managed Disk Resource Id to attach it 
    # to the virtual machine configuration
    $VirtualMachine = Set-AzVMOSDisk `
       -VM $VirtualMachine `
        -ManagedDiskId $disk.Id `
        -CreateOption Attach -Linux

    #Create a public IP for the VM
    $publicIp = New-AzPublicIpAddress `
        -Name ($VirtualMachineName.ToLower() + '_ip') `
        -ResourceGroupName $resourceGroupName `
        -Location $location `
        -AllocationMethod Static

    #Get the virtual network where virtual machine will be hosted
    $vnet = Get-AzVirtualNetwork `
        -Name $virtualNetworkName `
        -ResourceGroupName $resourceGroupName

    # Create NIC for the first subnet of the virtual network
    $nic = New-AzNetworkInterface `
        -Name ($VirtualMachineName.ToLower() + '_nic') `
        -ResourceGroupName $resourceGroupName `
        -Location $location `
        -SubnetId $vnet.Subnets[0].Id -PublicIpAddressId $publicIp.Id

    # Add the NIC to the virtual machine configuration
    $VirtualMachine = Add-AzVMNetworkInterface `
        -VM $VirtualMachine `
        -Id $nic.Id
    
    #Set the boot diagnostics storage account for the VM
    Set-AzVMBootDiagnostic `
        -VM $VirtualMachine `
        -Enable `
        -ResourceGroupName $resourceGroupName `
        -StorageAccountName $storageAccount.StorageAccountName

    #Create the virtual machine
    New-AzVM `
        -VM $VirtualMachine `
        -ResourceGroupName $resourceGroupName `
        -Location $location
    ```

1. When the virtual machine has been created, find the public IP address. Make a note of the IP address because you will need it later.

    ```PowerShell
    $ipAddr = (Get-AzPublicIpAddress `
        -Name ($VirtualMachineName.ToLower() + '_ip') `
        -ResourceGroupName $resourceGroupName).IpAddress

    echo $ipAddr
    ```

1. Connect using SSH as the **root** user. The password is **Pa55w.rdDemo**. Enter **yes** when prompted to connect.

    ---

    **NOTE:** 
    
    You may need to wait for a minute while the virtual machine services start before continuing

    ---

    ```PowerShell
    ssh root@$ipAddr
    ```

1. At the *bash* prompt, run the following commands to set the password for the **azureuser** account. Provide a password of your own choosing. You'll use this account rather than root for running the MapR services.

    ```bash
    passwd azureuser
    ```

1. Run the following command to sign out from the virtual machine and return to the PowerShell prompt:

    ```bash
    exit
    ```

1. On the desktop, open a Web browser, and navigate to the URL <ip-address>:7180, where *\<ip-address\>* is the IP address of the virtual machine you noted earlier. You should see the MapR Manager login page.

    ---

    **NOTE:** 
    Again. you may need to wait for a minute while the MapR Manager is initialized.
    
    ---
    
    ![The MapR Manager login page in the web browser.](../Images/0-MapR-Login.png)

1. Log in with the username **admin** with password **admin**.

1. In the MapR Manager, select the drop-down menu for the **MapR Management Service**, select **Start**, and wait for the management service to start up correctly.

    ![The MapR Management Service menu. The user has selected **Start**.](../Images/0-Start-MapR-Manager.png)

1. Select the drop-down menu for the **Cluster 1** cluster, and wait for the various services (Zookeeper, HDFS, Kafka, HBase, Yarn, Spark, and Hive) to start.

    ![The Cluster menu. The user has selected **Start**.](../Images/0-Start-MapR-Services.png)

1. Verify that all services are shown as running correctly.

    ---

    **NOTE:** 
    You may receive a warning from HDFS and/or Zookeeper initially, but they should clear after a minute or so.

    ---

    ![The Cluster Manager. All services have started successfully.](../Images/0-MapR-Services-Running.png)

1. In the Azure portal, close the PowerShell pane.









