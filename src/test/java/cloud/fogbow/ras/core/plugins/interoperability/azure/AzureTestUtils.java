package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.apache.log4j.Logger;
import rx.Completable;
import rx.Observable;

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

    public static AzureUser createAzureUser() {
        AzureUser azureUser = Mockito.mock(AzureUser.class);
        Mockito.when(azureUser.getSubscriptionId()).thenReturn(DEFAULT_SUBSCRIPTION_ID);
        return azureUser;
    }

    public static Observable<Indexable> createSimpleObservableSuccess(Indexable indexable) {
        return Observable.defer(() -> Observable.just(indexable));
    }

    public static Observable<Indexable> createSimpleObservableSuccess() {
        Indexable indexable = Mockito.mock(Indexable.class);
        return createSimpleObservableSuccess(indexable);
    }

    public static Observable<Indexable> createSimpleObservableFail() {
        return Observable.defer(() -> {
            throw new RuntimeException();
        });
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

    public static Completable createSimpleCompletableFail(Logger logger, String message) {
        return Completable.create((completableSubscriber) -> {
            logger.debug(message);
            completableSubscriber.onError(new RuntimeException());
        });
    }

    public static void mockGetAzureClient(AzureUser azureUser, Azure azure) throws UnauthenticatedUserException {
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(azureUser)))
                .thenReturn(azure);
    }

}
