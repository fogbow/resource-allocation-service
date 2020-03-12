package cloud.fogbow.ras.core.plugins.interoperability.azure.network;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.AzureVirtualNetworkOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import java.util.Properties;

public class AzureNetworkPlugin implements NetworkPlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzureNetworkPlugin.class);

    private AzureVirtualNetworkOperationSDK azureVirtualNetworkOperationSDK;
    private final String defaultResourceGroupName;

    public AzureNetworkPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        String defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.defaultResourceGroupName = properties.getProperty(AzureConstants.DEFAULT_RESOURCE_GROUP_NAME_KEY);
        this.azureVirtualNetworkOperationSDK = new AzureVirtualNetworkOperationSDK(defaultRegionName);
    }

    @Override
    public boolean isReady(String instanceState) {
        return false;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    @Override
    public String requestInstance(NetworkOrder networkOrder, AzureUser azureUser) throws FogbowException {
        LOGGER.info(Messages.Info.REQUESTING_INSTANCE_FROM_PROVIDER);

        String resourceName = AzureGeneralUtil.generateResourceName();
        String cidr = networkOrder.getCidr();

        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .name(resourceName)
                .cidr(cidr)
                .resourceGroupName(this.defaultResourceGroupName)
                .checkAndBuild();
        this.azureVirtualNetworkOperationSDK.doCreateInstance(azureCreateVirtualNetworkRef, azureUser);

        return AzureGeneralUtil.defineInstanceId(resourceName);
    }

    @Override
    public NetworkInstance getInstance(NetworkOrder networkOrder, AzureUser azureUser) throws FogbowException {

        return null;
    }

    @Override
    public void deleteInstance(NetworkOrder networkOrder, AzureUser azureUser) throws FogbowException {
    }

    @VisibleForTesting
    void setAzureVirtualNetworkOperationSDK(AzureVirtualNetworkOperationSDK azureVirtualNetworkOperationSDK) {
        this.azureVirtualNetworkOperationSDK = azureVirtualNetworkOperationSDK;
    }
}
