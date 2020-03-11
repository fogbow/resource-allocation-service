package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureGetVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureSchedulerManager;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.implementation.VirtualNetworkInner;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import org.apache.log4j.Logger;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.Optional;
import java.util.concurrent.ExecutorService;

public class AzureVirtualNetworkOperationSDK {

    private static final Logger LOGGER = Logger.getLogger(AzureVirtualNetworkOperationSDK.class);

    private final String regionName;
    private final Scheduler scheduler;

    public AzureVirtualNetworkOperationSDK(String regionName) {
        ExecutorService virtualNetworkExecutor = AzureSchedulerManager.getVirtualNetworkExecutor();
        this.scheduler = Schedulers.from(virtualNetworkExecutor);
        this.regionName = regionName;
    }

    public void doCreateInstance(AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef, AzureUser azureUser)
            throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        Observable<Indexable> virtualNetworkCreationObservable = buildVirtualNetworkCreationObservable(azureCreateVirtualNetworkRef, azure);
        subscribeVirtualNetworkCreation(virtualNetworkCreationObservable);
    }

    /**
     * Build full virtual network creation process behaviour that it consists in:
     * 1 - Create Security Group
     * 2 - Create Virtual Network based on security group created previously
     */
    @VisibleForTesting
    Observable<Indexable> buildVirtualNetworkCreationObservable(AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef, Azure azure) {
        Observable<Indexable> securityGroupObservable = buildCreateSecurityGroupObservable(azureCreateVirtualNetworkRef, azure);
        return securityGroupObservable
                .doOnNext(indexableSecurityGroup -> {
                    LOGGER.info(Messages.Info.FIRST_STEP_CREATE_VNET_ASYNC_BEHAVIOUR);
                    doNetworkCreationStepTwoSync(indexableSecurityGroup, azureCreateVirtualNetworkRef, azure);
                    LOGGER.info(Messages.Info.SECOND_STEP_CREATE_VNET_ASYNC_BEHAVIOUR);
                })
                .onErrorReturn(error -> {
                    LOGGER.error(Messages.Error.ERROR_CREATE_VNET_ASYNC_BEHAVIOUR);
                    return null;
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
    void doNetworkCreationStepTwoSync(Indexable indexableSecurityGroup,
                                           AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef,
                                           Azure azure) {

        NetworkSecurityGroup networkSecurityGroup = (NetworkSecurityGroup) indexableSecurityGroup;
        String name = azureCreateVirtualNetworkRef.getName();
        String resourceGroupName = azureCreateVirtualNetworkRef.getResourceGroupName();
        String cidr = azureCreateVirtualNetworkRef.getCidr();
        Region region = Region.fromName(this.regionName);

        AzureNetworkSDK.createNetworkSync(azure, name, region, resourceGroupName, cidr, networkSecurityGroup);
    }

    private void subscribeVirtualNetworkCreation(Observable<Indexable> virtualNetworkObservable) {
        virtualNetworkObservable
                .subscribeOn(this.scheduler)
                .subscribe();
    }

    // TODO(chico) - Implement tests
    public AzureGetVirtualNetworkRef doGetInstance(String instanceId, AzureUser azureUser)
            throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        // TODO(chico) - Build azureVirtualNetworkId by AzureBuilder; Note: Waiting another PR be accepted
        String azureVirtualNetworkId = "" + instanceId;
        Network network = AzureNetworkSDK
                .getNetwork(azure, instanceId)
                .orElseThrow(InstanceNotFoundException::new);

        VirtualNetworkInner virtualNetworkInner = network.inner();
        String id = virtualNetworkInner.id();
        String provisioningState = virtualNetworkInner.provisioningState();
        String name = network.name();
        // TODO(chico) - refactor this
        String cird = network.addressSpaces().listIterator().next();

        return AzureGetVirtualNetworkRef.builder()
                .id(id)
                .cidr(cird)
                .name(name)
                .state(provisioningState)
                .build();
    }
}
