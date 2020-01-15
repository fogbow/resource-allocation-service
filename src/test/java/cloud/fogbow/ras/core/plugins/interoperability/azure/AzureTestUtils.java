package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.common.models.AzureUser;
import org.mockito.Mockito;

public class AzureTestUtils {

    private static final String SUBSCRIPTION_ID_DEFAULT = "subscriptionId";
    private static final String RESOURCE_GROUP_NAME_DEFAULT = "resourceGroupName";
    private static final String REGION_NAME_DEFAULT = "regionName";

    public static AzureUser createAzureCloudUser() {
        AzureUser azureUser = Mockito.mock(AzureUser.class);
        Mockito.when(azureUser.getSubscriptionId()).thenReturn(SUBSCRIPTION_ID_DEFAULT);
        Mockito.when(azureUser.getRegionName()).thenReturn(REGION_NAME_DEFAULT);
        Mockito.when(azureUser.getResourceGroupName()).thenReturn(RESOURCE_GROUP_NAME_DEFAULT);
        return azureUser;
    }

}
