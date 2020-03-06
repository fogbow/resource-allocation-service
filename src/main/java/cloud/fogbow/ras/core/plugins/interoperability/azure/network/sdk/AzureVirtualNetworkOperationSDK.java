package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureSchedulerManager;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import org.apache.log4j.Logger;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.concurrent.ExecutorService;

public class AzureVirtualNetworkOperationSDK {

    private static final Logger LOGGER = Logger.getLogger(AzureVirtualNetworkOperationSDK.class);

    private final String regionName;
    private Scheduler scheduler;

    public AzureVirtualNetworkOperationSDK(String regionName) {
        ExecutorService virtualNetworkExecutor = AzureSchedulerManager.getVirtualNetworkExecutor();
        this.scheduler = Schedulers.from(virtualNetworkExecutor);
        this.regionName = regionName;
    }

    public void doCreateInstance(AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef, AzureUser azureUser)
            throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        Observable<Indexable> virtualMachineAsync = setAzureVirtualNetworkBehaviour(azureCreateVirtualNetworkRef, azure);
        subscribeCreateVirtualMachine(virtualMachineAsync);
    }

    /**
     * Set full virtual network creation process behaviour that it consists in:
     * 1 - Create Security Group
     * 2 - Create Virtual Network based on security group created previously
     */
    @VisibleForTesting
    Observable<Indexable> setAzureVirtualNetworkBehaviour(AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef, Azure azure) {
        Observable<Indexable> securityGroupObservable = buildCreateSecurityGroupObservable(azureCreateVirtualNetworkRef, azure);
        return securityGroupObservable
                .doOnNext(indexableSecurityGroup -> {
                    LOGGER.info(Messages.Info.FIRST_STEP_CREATE_VNET_ASYNC_BEHAVIOUR);
                    doNetworkCreationStepTwo(indexableSecurityGroup, azureCreateVirtualNetworkRef, azure);
                    LOGGER.info(Messages.Info.SECOND_STEP_CREATE_VNET_ASYNC_BEHAVIOUR);
                })
                .doOnError(error -> {
                    LOGGER.error(Messages.Error.ERROR_CREATE_VNET_ASYNC_BEHAVIOUR);
                })
                .doOnCompleted(() -> {
                    LOGGER.info(Messages.Info.END_CREATE_VNET_ASYNC_BEHAVIOUR);
                });
    }

    @VisibleForTesting
    Observable<Indexable> buildCreateSecurityGroupObservable(AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef, Azure azure) {
        String name = azureCreateVirtualNetworkRef.getName();
        String resourceGroupName = azureCreateVirtualNetworkRef.getResourceGroupName();
        String cidr = azureCreateVirtualNetworkRef.getCidr();
        Region region = Region.fromName(this.regionName);

        return AzureNetworkSDK.createSecurityGroupAsync(azure, name, region, resourceGroupName, cidr);
    }

    @VisibleForTesting
    Observable<Indexable> buildCreateVirtualNetworkObservable(AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef,
                                                              NetworkSecurityGroup networkSecurityGroup, Azure azure) {

        String name = azureCreateVirtualNetworkRef.getName();
        String resourceGroupName = azureCreateVirtualNetworkRef.getResourceGroupName();
        String cidr = azureCreateVirtualNetworkRef.getCidr();
        Region region = Region.fromName(this.regionName);

        return AzureNetworkSDK.createNetworkAsync(azure, name, region, resourceGroupName, cidr, networkSecurityGroup);
    }

    @VisibleForTesting
    Indexable doNetworkCreationStepTwo(Indexable indexableSecurityGroup,
                                       AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef,
                                       Azure azure) {

        NetworkSecurityGroup networkSecurityGroup = (NetworkSecurityGroup) indexableSecurityGroup;
        Observable<Indexable> networkObservable = buildCreateVirtualNetworkObservable(azureCreateVirtualNetworkRef, networkSecurityGroup, azure);
        return networkObservable
                .toBlocking()
                .first();
    }

    @VisibleForTesting
    void subscribeCreateVirtualMachine(Observable<Indexable> virtualNetworkObservable) {
        virtualNetworkObservable
                .subscribeOn(this.scheduler)
                .subscribe();
    }

}
