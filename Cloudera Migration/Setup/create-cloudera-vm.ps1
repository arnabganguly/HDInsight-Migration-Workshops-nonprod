#Provide the subscription Id
$subscriptionId = 'b581336d-00a0-41f6-bacd-a4f6b8779001'

#Provide the name of your resource group
$resourceGroupName ='workshoprg'

#Provide the name of the snapshot that will be used to create OS disk
$snapshotName = 'clouderasnapshot'

#Provide the name of the OS disk that will be created using the snapshot
$osDiskName = 'clouderadisk'

#Provide the name of a virtual network and subnet where virtual machine will be created
$virtualNetworkName = 'clouderavmvnet'
$subnetName = 'clouderasubnet'

#VNet prefix for the VM
$vnetprefix = '10.10.0.0'

#Network security group name for the VNet
$nsgName = 'clouderansg'

#Provide the name of the virtual machine
$virtualMachineName = 'clouderavm'

#Provide the size of the virtual machine
#e.g. Standard_DS3
#Get all the vm sizes in a region using below script:
#e.g. Get-AzVMSize -Location westus
$virtualMachineSize = 'Standard_D8s_v3'

#Sign in to Azure
Connect-AzAccount

#Set the context to the subscription Id where Managed Disk will be created
Select-AzSubscription -SubscriptionId $SubscriptionId

#Retrieve details of the snapshot
$snapshot = Get-AzSnapshot -ResourceGroupName $resourceGroupName -SnapshotName $snapshotName

#Create a virtual network for the VM
$virtualNetwork = New-AzVirtualNetwork -ResourceGroupName $resourceGroupName -Location $snapshot.Location -Name $virtualNetworkName -AddressPrefix $vnetprefix/16

#Create a network security group for the VNet and add rules to allow ports required to access Cloudera
$nsg = New-AzNetworkSecurityGroup -Name $nsgName -ResourceGroupName $resourceGroupName -Location $snapshot.Location
$nsg | Add-AzNetworkSecurityRuleConfig -Name 'SSHRule' -Access Allow -Protocol Tcp -Direction Inbound -Priority 100 -SourceAddressPrefix Internet -SourcePortRange * -DestinationAddressPrefix * -DestinationPortRange 22 | Set-AzNetworkSecurityGroup
$nsg | Add-AzNetworkSecurityRuleConfig -Name 'ClouderaManager' -Access Allow -Protocol Tcp -Direction Inbound -Priority 350 -SourceAddressPrefix Internet -SourcePortRange * -DestinationAddressPrefix * -DestinationPortRange 7180 | Set-AzNetworkSecurityGroup
$nsg | Add-AzNetworkSecurityRuleConfig -Name 'SCM' -Access Allow -Protocol Tcp -Direction Inbound -Priority 360 -SourceAddressPrefix Internet -SourcePortRange * -DestinationAddressPrefix * -DestinationPortRange 7182 | Set-AzNetworkSecurityGroup
$nsg | Add-AzNetworkSecurityRuleConfig -Name 'KafkaBroker' -Access Allow -Protocol Tcp -Direction Inbound -Priority 370 -SourceAddressPrefix Internet -SourcePortRange * -DestinationAddressPrefix * -DestinationPortRange 9092 | Set-AzNetworkSecurityGroup
$nsg | Add-AzNetworkSecurityRuleConfig -Name 'Zookeeper' -Access Allow -Protocol Tcp -Direction Inbound -Priority 380 -SourceAddressPrefix Internet -SourcePortRange * -DestinationAddressPrefix * -DestinationPortRange 2181 | Set-AzNetworkSecurityGroup
$nsg | Add-AzNetworkSecurityRuleConfig -Name 'Port8080' -Access Allow -Protocol Tcp -Direction Inbound -Priority 390 -SourceAddressPrefix Internet -SourcePortRange * -DestinationAddressPrefix * -DestinationPortRange 8080 | Set-AzNetworkSecurityGroup

#Create a subnet for the VM, and associate the NSG with the subnet
$subnetConfig = Add-AzVirtualNetworkSubnetConfig -Name $subnetName -AddressPrefix $vnetprefix/24 -VirtualNetwork $virtualNetwork -NetworkSecurityGroup $nsg
$virtualNetwork | Set-AzVirtualNetwork

#Create the VM disk from the snapshot
$diskConfig = New-AzDiskConfig -Location $snapshot.Location -SourceResourceId $snapshot.Id -CreateOption Copy

$disk = New-AzDisk -Disk $diskConfig -ResourceGroupName $resourceGroupName -DiskName $osDiskName

#Initialize virtual machine configuration
$VirtualMachine = New-AzVMConfig -VMName $virtualMachineName -VMSize $virtualMachineSize

#Use the Managed Disk Resource Id to attach it to the virtual machine configuration
$VirtualMachine = Set-AzVMOSDisk -VM $VirtualMachine -ManagedDiskId $disk.Id -CreateOption Attach -Linux

#Create a public IP for the VM
$publicIp = New-AzPublicIpAddress -Name ($VirtualMachineName.ToLower()+'_ip') -ResourceGroupName $resourceGroupName -Location $snapshot.Location -AllocationMethod Dynamic

#Get the virtual network where virtual machine will be hosted
$vnet = Get-AzVirtualNetwork -Name $virtualNetworkName -ResourceGroupName $resourceGroupName

# Create NIC for the first subnet of the virtual network
$nic = New-AzNetworkInterface -Name ($VirtualMachineName.ToLower()+'_nic') -ResourceGroupName $resourceGroupName -Location $snapshot.Location -SubnetId $vnet.Subnets[0].Id -PublicIpAddressId $publicIp.Id

# Add the NIC to the virtual machine configuration
$VirtualMachine = Add-AzVMNetworkInterface -VM $VirtualMachine -Id $nic.Id

#Create the virtual machine with Managed Disk
New-AzVM -VM $VirtualMachine -ResourceGroupName $resourceGroupName -Location $snapshot.Location