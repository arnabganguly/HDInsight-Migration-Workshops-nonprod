# CONFIGURATION - MODIFY AS NECESSARY

# Provide the subscription Id
$subscriptionId = '<your-subscription-id>'

# SAS URL of pre-created image
$sourceDiskSAS = '<SAS>'   

# Specify the location for creating resources
$location = "East US"

# Specify the resource group for the MapR VM and resources
$resourceGroupName = 'maprworkshoprg'

# Provide the name of the virtual machine
$virtualMachineName = 'maprvm'

# Provide the size of the virtual machine
$virtualMachineSize = 'Standard_D8s_v4'

# Provide the name of a virtual network and subnet where virtual machine will be created
$virtualNetworkName = 'maprvmvnet'
$subnetName = 'maprsubnet'

#VNet prefix for the VM
$vnetprefix = '10.1.0.0'

#Network security group name for the VNet
$nsgName = 'maprnsg'

# **DON'T CHANGE ANYTHING BELOW THIS POINT**

$targetOS = 'Linux'
$osDiskName = 'maprdisk'
$vhdSizeBytes = 68719477248

# Create a resource group for holding the virtual machine
Select-AzSubscription -SubscriptionId $SubscriptionId
New-AzResourceGroup -Name $resourceGroupName -Location $location

# Create the managed disk
$targetDiskConfig = New-AzDiskConfig `
    -SkuName 'Standard_LRS' `
    -osType 'Linux' `
    -UploadSizeInBytes $vhdSizeBytes `
    -Location $location `
    -CreateOption 'Upload'

$targetDisk = New-AzDisk -ResourceGroupName $resourceGroupName `
    -DiskName $osDiskName `
    -Disk $targetDiskConfig

$targetDiskSas = Grant-AzDiskAccess -ResourceGroupName $resourceGroupName `
    -DiskName $osDiskName `
    -DurationInSecond 86400 -Access 'Write'

# Copy the contents of the pre-created disk to the managed disk
azcopy copy $sourceDiskSAS $targetDiskSas.AccessSAS `
    --blob-type PageBlob

Revoke-AzDiskAccess -ResourceGroupName $resourceGroupName `
    -DiskName $osDiskName
  
# Create a virtual network for the VM
$virtualNetwork = New-AzVirtualNetwork `
    -ResourceGroupName $resourceGroupName `
    -Location $location `
    -Name $virtualNetworkName `
    -AddressPrefix $vnetprefix/16

# Create a network security group for the VNet and add rules to allow ports required to access MapR
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
    -Name 'ClouderaManager' `
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

# Create a subnet for the VM, and associate the NSG with the subnet
$subnetConfig = Add-AzVirtualNetworkSubnetConfig `
    -Name $subnetName `
    -AddressPrefix $vnetprefix/24 `
    -VirtualNetwork $virtualNetwork `
    -NetworkSecurityGroup $nsg

$virtualNetwork | Set-AzVirtualNetwork

# Create a Gen1 storage account with a unique name
[string]$rnd = Get-Random -Maximum 10000
$storageAccount = New-AzStorageAccount `
    -AccountName ($VirtualMachineName.ToLower() + 'storage' + $rnd) `
    -ResourceGroupName $resourceGroupName `
    -Location $location `
    -Kind Storage `
    -SkuName Standard_GRS
    
# Initialize virtual machine configuration
$VirtualMachine = New-AzVMConfig -VMName $virtualMachineName `
    -VMSize $virtualMachineSize

# Use the Managed Disk Resource Id to attach it 
# to the virtual machine configuration
$disk = Get-AzDisk `
    -ResourceGroupName $resourceGroupName `
    -DiskName $osDiskName

$VirtualMachine = Set-AzVMOSDisk `
   -VM $VirtualMachine `
    -ManagedDiskId $disk.Id `
    -CreateOption Attach -Linux

# Create a public IP for the VM
$publicIp = New-AzPublicIpAddress `
    -Name ($VirtualMachineName.ToLower() + '_ip') `
    -ResourceGroupName $resourceGroupName `
    -Location $location `
    -AllocationMethod Static

# Get the virtual network where virtual machine will be hosted
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

# Set the boot diagnostics storage account for the VM
Set-AzVMBootDiagnostic `
    -VM $VirtualMachine `
    -Enable `
    -ResourceGroupName $resourceGroupName `
    -StorageAccountName $storageAccount.StorageAccountName

# Create the virtual machine
New-AzVM `
    -VM $VirtualMachine `
    -ResourceGroupName $resourceGroupName `
    -Location $location
    
$ipAddr = (Get-AzPublicIpAddress `
    -Name ($VirtualMachineName.ToLower() + '_ip') `
    -ResourceGroupName $resourceGroupName).IpAddress

echo "Virtual machine created. IP address is:" $ipAddr
