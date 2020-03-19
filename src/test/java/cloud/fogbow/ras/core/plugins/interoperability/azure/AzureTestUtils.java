package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.common.models.AzureUser;
import rx.Observable;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

/*
 * This class is intended to reuse code components to assist other unit test classes
 * but does not contemplate performing any tests. The @Ignore annotation is being used
 * in this context to prevent it from being initialized as a test class.
 */
@Ignore
@RunWith(PowerMockRunner.class)
public class AzureTestUtils {

    public static final String AZURE_CLOUD_NAME = "azure";
    public static final String DEFAULT_RESOURCE_GROUP_NAME = "default-resource-group-name";
    public static final String DEFAULT_REGION_NAME = "eastus";
    public static final String DEFAULT_SUBSCRIPTION_ID = "default-subscription-id";
    public static final String ORDER_NAME = "order-name";
    public static final String RESOURCE_NAME = "resource-name";
    public static final String RESOUCE_ID_PREFIX = "prefix-";
    public static final String UNDEFINED_STATE = "undefined";
    
    public static AzureUser createAzureUser() {
        AzureUser azureUser = Mockito.mock(AzureUser.class);
        Mockito.when(azureUser.getSubscriptionId()).thenReturn(DEFAULT_SUBSCRIPTION_ID);
        return azureUser;
    }
    
    public static Observable<Indexable> createSimpleObservableSuccess() {
        return Observable.defer(() -> {
            Indexable indexable = Mockito.mock(Indexable.class);
            return Observable.just(indexable);
        });
    }

    public static Observable<Indexable> createSimpleObservableFail() {
        return Observable.defer(() -> {
            throw new RuntimeException();
        });
    }
    
}
