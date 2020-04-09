package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip;

import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.SecurityRuleProtocol;
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
import cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk.AzurePublicIPAddressSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk.AzurePublicIpAddressOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import rx.Completable;

public class AzurePublicIpPlugin implements PublicIpPlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzurePublicIpPlugin.class);

    private final String defaultRegionName;
    private final String defaultResourceGroupName;

    private AzurePublicIpAddressOperationSDK operation;

    public AzurePublicIpPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.operation = new AzurePublicIpAddressOperationSDK();
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
        String virtualMachineId = buildResourceId(subscriptionId, publicIpOrder.getComputeId());
        VirtualMachine virtualMachine = doGetVirtualMachineSDK(azure, virtualMachineId);
        
        Creatable<PublicIPAddress> publicIPCreatable = azure.publicIPAddresses()
                .define(resourceName)
                .withRegion(this.defaultRegionName)
                .withExistingResourceGroup(this.defaultResourceGroupName)
                .withDynamicIP();
        
        return doRequestInstance(virtualMachine, publicIPCreatable);
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
        String virtualMachineId = buildResourceId(subscriptionId, publicIpOrder.getComputeId());
        
        doDeleteInstance(azure, virtualMachineId, resourceId);
    }
    
    @VisibleForTesting
    void doDeleteInstance(Azure azure, String virtualMachineId, String resourceId) throws FogbowException {
        // TODO remove the security rule
        doDeleteSecurityRule(azure, virtualMachineId);
        doDisassociatePublicIpAddress(azure, virtualMachineId);

        Completable completable = AzurePublicIPAddressSDK.buildDeletePublicIpAddressCompletable(azure, resourceId);
        this.operation.subscribeDeletePublicIPAddress(completable);
    }

    @VisibleForTesting
    void doDeleteSecurityRule(Azure azure, String virtualMachineId) throws FogbowException {
        VirtualMachine virtualMachine = doGetVirtualMachineSDK(azure, virtualMachineId);
        AzurePublicIPAddressSDK.deleteSecurityRuleFrom(virtualMachine);
    }

    @VisibleForTesting
    void doDisassociatePublicIpAddress(Azure azure, String virtualMachineId) throws FogbowException {
        VirtualMachine virtualMachine = doGetVirtualMachineSDK(azure, virtualMachineId);
        AzurePublicIPAddressSDK.disassociatePublicIPAddressFrom(virtualMachine);
    }

    @VisibleForTesting
    PublicIpInstance doGetInstance(Azure azure, String publicIPAddressId) throws FogbowException {
        PublicIPAddress publicIPAddress = doGetPublicIPAddressSDK(azure, publicIPAddressId);
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
    PublicIPAddress doGetPublicIPAddressSDK(Azure azure, String publicIPAddressId) throws FogbowException {
        return AzurePublicIPAddressSDK
                .getPublicIpAddress(azure, publicIPAddressId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
    }

    @VisibleForTesting
    VirtualMachine doGetVirtualMachineSDK(Azure azure, String virtualMachineId) throws FogbowException {
        return AzureVirtualMachineSDK
                .getVirtualMachine(azure, virtualMachineId)
                .orElseThrow(() -> new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND));
    }

    @VisibleForTesting
    String doRequestInstance(VirtualMachine virtualMachine, Creatable<PublicIPAddress> publicIPCreatable)
            throws FogbowException {

        AzurePublicIPAddressSDK.associatePublicIPAddressCreatable(virtualMachine, publicIPCreatable);

        // TODO add security rule to access the virtual machine
        String ipAddress = virtualMachine.getPrimaryPublicIPAddress().ipAddress();

        NetworkInterface networkInterface = virtualMachine.getPrimaryNetworkInterface();
        NetworkSecurityGroup securityGroup = networkInterface.getNetworkSecurityGroup();

        securityGroup.update()
                .defineRule("ruleName")
                    .allowInbound()
                    .fromAddress(ipAddress)
                    .fromPort(22)
                    .toAddress(ipAddress)
                    .toPort(22)
                    .withProtocol(SecurityRuleProtocol.TCP)
                    .attach()
                .apply();

        return AzureGeneralUtil.defineInstanceId(publicIPCreatable.name());
    }
    
    @VisibleForTesting
    String buildResourceId(String subscriptionId, String resourceName) {
        String resourceIdUrl = AzureResourceIdBuilder.publicIpId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.defaultResourceGroupName)
                .withResourceName(resourceName)
                .build();

        return resourceIdUrl;
    }

}
