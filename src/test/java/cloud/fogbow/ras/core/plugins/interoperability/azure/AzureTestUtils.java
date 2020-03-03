package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.common.models.AzureUser;
import org.mockito.Mockito;

public class AzureTestUtils {

    public static final String AZURE_CLOUD_NAME = "azure";
    public static final String DEFAULT_RESOURCE_GROUP_NAME = "default-resource-group-name";
    public static final String DEFAULT_REGION_NAME = "eastus";
    private static final String SUBSCRIPTION_ID_DEFAULT = "subscriptionId";

    public static AzureUser createAzureUser() {
        AzureUser azureUser = Mockito.mock(AzureUser.class);
        Mockito.when(azureUser.getSubscriptionId()).thenReturn(SUBSCRIPTION_ID_DEFAULT);
        return azureUser;
    }

}
