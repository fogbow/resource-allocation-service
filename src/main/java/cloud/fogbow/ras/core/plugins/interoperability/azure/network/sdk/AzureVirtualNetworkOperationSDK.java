package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureGetVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureSchedulerManager;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.implementation.VirtualNetworkInner;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import org.apache.log4j.Logger;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

public class AzureVirtualNetworkOperationSDK {

    private static final Logger LOGGER = Logger.getLogger(AzureVirtualNetworkOperationSDK.class);

    private final String resourceGroupName;
    private Scheduler scheduler;
    private final String regionName;

    public AzureVirtualNetworkOperationSDK(String regionName, String defaultResourceGroupName) {
        ExecutorService virtualMachineExecutor = AzureSchedulerManager.getVirtualNetworkExecutor();
        this.scheduler = Schedulers.from(virtualMachineExecutor);

        this.regionName = regionName;
        this.resourceGroupName = defaultResourceGroupName;
    }

    public void doCreateInstance(AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef,
                                 AzureUser azureUser, Runnable defineAsCreatedInstanceCallback)
            throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);
        Observable<Indexable> virtualNetworkCreationObservable = buildVirtualNetworkCreationObservable(
                azureCreateVirtualNetworkRef, azure, defineAsCreatedInstanceCallback);
        subscribeVirtualNetworkCreation(virtualNetworkCreationObservable);
    }

    /**
     * Build full virtual network creation process behaviour that it consists in:
     * 1 - Create Security Group
     * 2 - Create Virtual Network based on security group created previously
     */
    @VisibleForTesting
    Observable<Indexable> buildVirtualNetworkCreationObservable(AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef,
                                                                Azure azure, Runnable defineAsCreatedInstanceCallback) {
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
                    defineAsCreatedInstanceCallback.run();
                    LOGGER.info(Messages.Info.END_CREATE_VNET_ASYNC_BEHAVIOUR);
                });
    }

    @VisibleForTesting
    Observable<Indexable> buildCreateSecurityGroupObservable(AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef, Azure azure) {
        String name = azureCreateVirtualNetworkRef.getResourceName();
        String resourceGroupName = this.resourceGroupName;
        String cidr = azureCreateVirtualNetworkRef.getCidr();
        Region region = Region.fromName(this.regionName);
        Map tags = azureCreateVirtualNetworkRef.getTags();

        return AzureNetworkSDK.createSecurityGroupAsync(azure, name, region, resourceGroupName, cidr, tags);
    }


    @VisibleForTesting
    void doNetworkCreationStepTwoSync(Indexable indexableSecurityGroup,
                                           AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef,
                                           Azure azure) {

        NetworkSecurityGroup networkSecurityGroup = (NetworkSecurityGroup) indexableSecurityGroup;
        String name = azureCreateVirtualNetworkRef.getResourceName();
        String resourceGroupName = this.resourceGroupName;
        String cidr = azureCreateVirtualNetworkRef.getCidr();
        Region region = Region.fromName(this.regionName);
        Map tags = azureCreateVirtualNetworkRef.getTags();

        AzureNetworkSDK.createNetworkSync(
                azure, name, region, resourceGroupName, cidr, networkSecurityGroup, tags);
    }

    private void subscribeVirtualNetworkCreation(Observable<Indexable> virtualNetworkObservable) {
        virtualNetworkObservable
                .subscribeOn(this.scheduler)
                .subscribe();
    }

    public AzureGetVirtualNetworkRef doGetInstance(String resourceName, AzureUser azureUser)
            throws FogbowException {

        Network network = getNetwork(resourceName, azureUser);

        VirtualNetworkInner virtualNetworkInner = network.inner();
        String provisioningState = virtualNetworkInner.provisioningState();
        String id = virtualNetworkInner.id();
        String name = network.name();
        String cird = getCIRD(network);

        return AzureGetVirtualNetworkRef.builder()
                .state(provisioningState)
                .cidr(cird)
                .name(name)
                .id(id)
                .build();
    }

    @Nullable
    @VisibleForTesting
    String getCIRD(Network network) {
        try {
            return network.addressSpaces().listIterator().next();
        } catch (NoSuchElementException e) {
            LOGGER.warn(e.getMessage(), e);
            return AzureGeneralUtil.NO_INFORMATION;
        }
    }

    @VisibleForTesting
    Network getNetwork(String resourceName, AzureUser azureUser) throws FogbowException {

        Azure azure = AzureClientCacheManager.getAzure(azureUser);

        String subscriptionId = azureUser.getSubscriptionId();
        String azureVirtualNetworkId = AzureResourceIdBuilder.virtualNetworkId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.resourceGroupName)
                .withResourceName(resourceName)
                .build();

        return AzureNetworkSDK
                .getNetwork(azure, azureVirtualNetworkId)
                .orElseThrow(InstanceNotFoundException::new);
    }

    public void doDeleteInstance(String resourceName, AzureUser azureUser) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(azureUser);

        String azureVirtualNetworkId = getAzureVirtualNetworkId(resourceName, azureUser);
        Completable firstDeleteVirtualNetwork = buildDeleteVirtualNetworkCompletable(azure, azureVirtualNetworkId);

        String azureSecurityGroupId = getAzureNetworkSecurityGroupId(resourceName, azureUser);
        Completable secondDeleteSecurityGroup = buildDeleteSecurityGroupCompletable(azure, azureSecurityGroupId);

        Completable.concat(firstDeleteVirtualNetwork, secondDeleteSecurityGroup)
                .subscribeOn(this.scheduler)
                .subscribe();
    }

    @VisibleForTesting
    Completable buildDeleteSecurityGroupCompletable(Azure azure, String azureVirtualNetworkId) {
        Completable buildDeleteNetworkSecurityGroupCompletable =
                AzureNetworkSDK.buildDeleteNetworkSecurityGroupCompletable(azure, azureVirtualNetworkId);

        return setDeleteSecurityGroupBehaviour(buildDeleteNetworkSecurityGroupCompletable);
    }

    private Completable setDeleteSecurityGroupBehaviour(Completable deleteNetworkSecurityGroupCompletable) {
        return deleteNetworkSecurityGroupCompletable
                .doOnError((error -> {
                    LOGGER.error(Messages.Error.ERROR_DELETE_SECURITY_GROUP_ASYNC_BEHAVIOUR);
                }))
                .doOnCompleted(() -> {
                    LOGGER.info(Messages.Info.END_DELETE_SECURITY_GROUP_ASYNC_BEHAVIOUR);
                });
    }

    @VisibleForTesting
    Completable buildDeleteVirtualNetworkCompletable(Azure azure, String azureVirtualNetworkId) {
        Completable deleteVirtualNetworkCompletable = AzureNetworkSDK
                .buildDeleteVirtualNetworkCompletable(azure, azureVirtualNetworkId);

        return setDeleteVirtualNetworkBehaviour(deleteVirtualNetworkCompletable);
    }

    private Completable setDeleteVirtualNetworkBehaviour(Completable deleteVirtualNetworkCompletable) {
        return deleteVirtualNetworkCompletable
                .doOnError((error -> {
                    LOGGER.error(Messages.Error.ERROR_DELETE_VNET_ASYNC_BEHAVIOUR);
                }))
                .doOnCompleted(() -> {
                    LOGGER.info(Messages.Info.END_DELETE_VNET_ASYNC_BEHAVIOUR);
                });
    }

    private String getAzureVirtualNetworkId(String resourceName, AzureUser azureUser) {
        String subscriptionId = azureUser.getSubscriptionId();
        return AzureResourceIdBuilder.virtualNetworkId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.resourceGroupName)
                .withResourceName(resourceName)
                .build();
    }

    private String getAzureNetworkSecurityGroupId(String resourceName, AzureUser azureUser) {
        String subscriptionId = azureUser.getSubscriptionId();
        return AzureResourceIdBuilder.networkSecurityGroupId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(this.resourceGroupName)
                .withResourceName(resourceName)
                .build();
    }

    @VisibleForTesting
    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}
