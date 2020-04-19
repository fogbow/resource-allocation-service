package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip;

import java.util.Properties;
import java.util.Stack;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.implementation.PublicIPAddressInner;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.AzureVirtualMachineSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk.AzurePublicIPAddressOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk.AzurePublicIPAddressSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import rx.Completable;
import rx.Observable;

public class AzurePublicIpPlugin implements PublicIpPlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzurePublicIpPlugin.class);

    private final String defaultRegionName;
    private final String defaultResourceGroupName;

    private AzurePublicIPAddressOperationSDK operation;

    public AzurePublicIpPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.operation = new AzurePublicIPAddressOperationSDK(this.defaultResourceGroupName);
    }
    
    @Override
    public boolean isReady(String instanceState) {
        return AzureStateMapper.map(ResourceType.PUBLIC_IP, instanceState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return AzureStateMapper.map(ResourceType.PUBLIC_IP, instanceState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER);
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String resourceName = AzureGeneralUtil.generateResourceName();
        String subscriptionId = azureUser.getSubscriptionId();
        String virtualMachineId = buildVirtualMachineId(subscriptionId, publicIpOrder.getComputeId());
        
        Creatable<PublicIPAddress> publicIPAddressCreatable = azure.publicIPAddresses()
                .define(resourceName)
                .withRegion(this.defaultRegionName)
                .withExistingResourceGroup(this.defaultResourceGroupName)
                .withDynamicIP();
        
        return doRequestInstance(azure, virtualMachineId, publicIPAddressCreatable);
    }

    @Override
    public PublicIpInstance getInstance(PublicIpOrder publicIpOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, publicIpOrder.getInstanceId()));
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String subscriptionId = azureUser.getSubscriptionId();
        String resourceId = buildResourceId(subscriptionId, publicIpOrder.getInstanceId());
        
        return doGetInstance(azure, resourceId);
    }
    
    @Override
    public void deleteInstance(PublicIpOrder publicIpOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE_S, publicIpOrder.getInstanceId()));
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String subscriptionId = azureUser.getSubscriptionId();
        String resourceId = buildResourceId(subscriptionId, publicIpOrder.getInstanceId());
        String virtualMachineId = buildVirtualMachineId(subscriptionId, publicIpOrder.getComputeId());
        
        doDeleteInstance(azure, resourceId, virtualMachineId);
    }
    
    @VisibleForTesting
    void doDeleteInstance(Azure azure, String resourceId, String virtualMachineId) throws FogbowException {
        VirtualMachine virtualMachine = doGetVirtualMachineSDK(azure, virtualMachineId);
        NetworkInterface networkInterface = doGetPrimaryNetworkInterfaceFrom(virtualMachine);
        doDeleteResources(azure, resourceId, networkInterface);
    }

    @VisibleForTesting
    void doDeleteResources(Azure azure, String publicIPAddressId, NetworkInterface networkInterface)
            throws FogbowException {

        NetworkSecurityGroup networkSecurityGroup = doGetNetworkSecurityGroup(networkInterface);
        String networkSecurityGroupId = networkSecurityGroup.id();
        Stack<Observable> observables = stackResourcesForDisassociation(networkInterface);
        Completable completable = concatResourcesForDeletion(azure, publicIPAddressId, networkSecurityGroupId);
        this.operation.subscribeDeleteResources(observables, completable);
    }

    @VisibleForTesting
    Completable concatResourcesForDeletion(Azure azure, String publicIPAddressId, String networkSecurityGroupId) {
        Completable firstDeleteNetworkSecurityGroup = AzurePublicIPAddressSDK
                .deleteNetworkSecurityGroupAsync(azure, networkSecurityGroupId);

        Completable secondDeletePublicIPAddress = AzurePublicIPAddressSDK
                .deletePublicIpAddressAsync(azure, publicIPAddressId);

        return Completable.concat(firstDeleteNetworkSecurityGroup, secondDeletePublicIPAddress);
    }

    @VisibleForTesting
    Stack<Observable> stackResourcesForDisassociation(NetworkInterface networkInterface) {
        Observable<NetworkInterface> disassociateSecurityGroup = AzurePublicIPAddressSDK
                .disassociateNetworkSecurityGroupAsync(networkInterface);

        Observable<NetworkInterface> disassociatePublicIPAddress = AzurePublicIPAddressSDK
                .disassociatePublicIPAddressAsync(networkInterface);

        Stack<Observable> observables = new Stack<>();
        observables.push(disassociatePublicIPAddress);
        observables.push(disassociateSecurityGroup);
        return observables;
    }

    @VisibleForTesting
    NetworkSecurityGroup doGetNetworkSecurityGroup(NetworkInterface networkInterface) throws FogbowException {
        return AzurePublicIPAddressSDK
                .getNetworkSecurityGroupFrom(networkInterface)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
    }

    @VisibleForTesting
    NetworkInterface doGetPrimaryNetworkInterfaceFrom(VirtualMachine virtualMachine) throws FogbowException {
        return AzurePublicIPAddressSDK
                .getPrimaryNetworkInterfaceFrom(virtualMachine)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
    }

    @VisibleForTesting
    PublicIpInstance doGetInstance(Azure azure, String resourceId) throws FogbowException {
        PublicIPAddress publicIPAddress = doGetPublicIPAddressSDK(azure, resourceId);
        return buildPublicIpInstance(publicIPAddress);
    }
    
    @VisibleForTesting
    PublicIpInstance buildPublicIpInstance(PublicIPAddress publicIPAddress) {
        PublicIPAddressInner publicIPAddressInner = publicIPAddress.inner();
        String id = publicIPAddressInner.id();
        String cloudState = publicIPAddressInner.provisioningState();
        String ip = publicIPAddressInner.ipAddress();
        return new PublicIpInstance(id, cloudState, ip);
    }

    @VisibleForTesting
    PublicIPAddress doGetPublicIPAddressSDK(Azure azure, String resourceId) throws FogbowException {
        return AzurePublicIPAddressSDK
                .getPublicIpAddress(azure, resourceId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
    }

    @VisibleForTesting
    String buildResourceId(String subscriptionId, String resourceName) {
        String resourceIdUrl = AzureResourceIdBuilder.publicIpAddressId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();

        return resourceIdUrl;
    }

    @VisibleForTesting
    String doRequestInstance(Azure azure, String virtualMachineId,
            Creatable<PublicIPAddress> publicIPAddressCreatable) throws FogbowException {

        VirtualMachine virtualMachine = doGetVirtualMachineSDK(azure, virtualMachineId);
        NetworkInterface networkInterface = doGetPrimaryNetworkInterfaceFrom(virtualMachine);

        Observable<NetworkInterface> observable = AzurePublicIPAddressSDK
                .associatePublicIPAddressAsync(networkInterface, publicIPAddressCreatable);

        String resourceId = publicIPAddressCreatable.name();
        String instanceId = AzureGeneralUtil.defineInstanceId(resourceId);
        this.operation.subscribeAssociatePublicIPAddress(azure, instanceId, observable);

        return instanceId;
    }

    @VisibleForTesting
    VirtualMachine doGetVirtualMachineSDK(Azure azure, String virtualMachineId) throws FogbowException {
        return AzureVirtualMachineSDK
                .getVirtualMachine(azure, virtualMachineId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
    }
    
    @VisibleForTesting
    String buildVirtualMachineId(String subscriptionId, String resourceName) {
        String resourceIdUrl = AzureResourceIdBuilder.virtualMachineId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();

        return resourceIdUrl;
    }

    @VisibleForTesting
    void setOperation(AzurePublicIPAddressOperationSDK operation) {
        this.operation = operation;
    }

}
