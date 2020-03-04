package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureSchedulerManager;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.concurrent.ExecutorService;

public class AzureVirtualNetworkOperationSDK {

    private final String regionName;
    private Scheduler scheduler;

    public AzureVirtualNetworkOperationSDK(String regionName) {
        ExecutorService virtualNetworkExecutor = AzureSchedulerManager.getVirtualNetworkExecutor();
        this.scheduler = Schedulers.from(virtualNetworkExecutor);
        this.regionName = regionName;
    }


    public void doCreateInstance(AzureCreateVirtualNetworkRef virtualNetworkRef, AzureUser azureUser)
            throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        Observable<Indexable> virtualMachineAsync = buildAzureVirtualNetworkObservable(virtualNetworkRef, azure);
        subscribeCreateVirtualMachine(virtualMachineAsync);
    }

    // TODO implement
    private Observable<Indexable> buildAzureVirtualNetworkObservable(AzureCreateVirtualNetworkRef virtualNetworkRef, Azure azure) {

        return AzureNetworkSDK.createSecurityGroupAsync()
                .doOnNext(indexable -> {
                    doNetworkCreationStepTwo(indexable);
                })
                .doOnError(error -> {

                });
    }

    @VisibleForTesting
    Indexable doNetworkCreationStepTwo(Indexable indexableSecurityGroup) {
        try {
            NetworkSecurityGroup networkSecurityGroup = (NetworkSecurityGroup) indexableSecurityGroup;
            Observable<Indexable> networkObservable = AzureNetworkSDK.createNetworkAsync();
            return networkObservable
                    .toBlocking()
                    .first();
        } catch (RuntimeException e) {
            // TODO implement
            throw new RuntimeException("", e);
        }
    }

    /**
     * Execute create Virtual Machine observable and set its behaviour.
     */
    @VisibleForTesting
    private void subscribeCreateVirtualMachine(Observable<Indexable> virtualNetworkObservable) {
        setCreateVirtualNetworkBehaviour(virtualNetworkObservable)
                .subscribeOn(this.scheduler)
                .subscribe();
    }

    // TODO implement
    private Observable<Object> setCreateVirtualNetworkBehaviour(Observable<Indexable> virtualNetworkObservable) {
        return null;
    }

}
