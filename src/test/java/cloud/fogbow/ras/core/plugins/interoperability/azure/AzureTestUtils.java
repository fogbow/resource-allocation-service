package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import org.apache.log4j.Logger;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import rx.Completable;
import rx.Observable;

public class AzureTestUtils {

    public static final String AZURE_CLOUD_NAME = "azure";
    public static final String DEFAULT_RESOURCE_GROUP_NAME = "default-resource-group-name";
    public static final String DEFAULT_REGION_NAME = "eastus";
    public static final String DEFAULT_SUBSCRIPTION_ID = "default-subscription-id";
    public static final String ORDER_NAME = "order-name";
    public static final String RESOURCE_NAME = "resource-name";

    public static AzureUser createAzureUser() {
        AzureUser azureUser = Mockito.mock(AzureUser.class);
        Mockito.when(azureUser.getSubscriptionId()).thenReturn(DEFAULT_SUBSCRIPTION_ID);
        return azureUser;
    }

    public static Observable<Indexable> createSimpleObservableSuccess(Indexable indexable) {
        return Observable.defer(() -> Observable.just(indexable));
    }

    public static Completable createSimpleCompletableSuccess() {
        return Completable.complete();
    }

    public static Completable createSimpleCompletableSuccess(Logger logger, String message) {
        return Completable.create((completableSubscriber) -> {
            logger.debug(message);
            completableSubscriber.onCompleted();
        });
    }

    public static Completable createSimpleCompletableFail() {
        return Completable.error(new RuntimeException());
    }

    public static void mockGetAzureClient(AzureUser azureUser, Azure azure) throws UnauthenticatedUserException {
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(azureUser)))
                .thenReturn(azure);
    }

}
