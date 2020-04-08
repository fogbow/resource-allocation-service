package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip;

import java.util.Properties;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;

public class AzurePublicIpPlugin implements PublicIpPlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzurePublicIpPlugin.class);

    private final String defaultRegionName;
    private final String defaultResourceGroupName;

    public AzurePublicIpPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
    }
    
    @Override
    public boolean isReady(String instanceState) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER);
        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        String resourceName = AzureGeneralUtil.generateResourceName();
        String subscriptionId = azureUser.getSubscriptionId();
        String virtualMachineId = buildResourceId(subscriptionId, publicIpOrder.getComputeId());
        
        Creatable<PublicIPAddress> publicIPCreatable = azure.publicIPAddresses()
                .define(resourceName)
                .withRegion(this.defaultRegionName)
                .withExistingResourceGroup(this.defaultResourceGroupName)
                .withDynamicIP();
        
        return doRequestInstance(azure, virtualMachineId, publicIPCreatable);
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
    void doDeleteInstance(Azure azure, String virtualMachineId, String resourceId) {
        // TODO first: remove the security rule
        // TODO second: desassociate the public IP adddress from virtual machine
        // TODO third: remove the public IP address
    }

    @VisibleForTesting
    PublicIpInstance doGetInstance(Azure azure, String resourceId) {
        PublicIPAddress publicIPAddress = doGetPublicIPAddressSDK(azure, resourceId);
        return buildPublicIpInstance(publicIPAddress);
    }
    
    @VisibleForTesting
    PublicIpInstance buildPublicIpInstance(PublicIPAddress publicIPAddress) {
        // TODO Auto-generated method stub
        return null;
    }

    @VisibleForTesting
    PublicIPAddress doGetPublicIPAddressSDK(Azure azure, String resourceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @VisibleForTesting
    String doRequestInstance(Azure azure, String virtualMachineId, Creatable<PublicIPAddress> publicIPCreatable) {
        VirtualMachine virtualMachine = azure.virtualMachines().getById(virtualMachineId);
        NetworkInterface networkInterface = virtualMachine.getPrimaryNetworkInterface();
        networkInterface.update().withNewPrimaryPublicIPAddress(publicIPCreatable).apply();
        String resourceId = virtualMachine.getPrimaryPublicIPAddressId();
        // TODO add security rule to access the virtual machine and refactor this method.
        return resourceId;
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
