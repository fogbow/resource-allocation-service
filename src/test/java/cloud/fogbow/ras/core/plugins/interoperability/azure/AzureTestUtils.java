package cloud.fogbow.ras.core.plugins.interoperability.azure;

import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.models.AzureUser;

@RunWith(PowerMockRunner.class)
public class AzureTestUtils {

    public static final String AZURE_CLOUD_NAME = "azure";
    public static final String DEFAULT_RESOURCE_GROUP_NAME = "default-resource-group-name";
    public static final String DEFAULT_REGION_NAME = "eastus";
    public static final String DEFAULT_SUBSCRIPTION_ID = "default-subscription-id";
    public static final String ORDER_NAME = "order-name";
    public static final String RESOURCE_NAME = "resource-name";
    public static final String RESOUCE_ID_PREFIX = "prefix-";
    
    public static AzureUser createAzureUser() {
        AzureUser azureUser = Mockito.mock(AzureUser.class);
        Mockito.when(azureUser.getSubscriptionId()).thenReturn(DEFAULT_SUBSCRIPTION_ID);
        return azureUser;
    }

}
