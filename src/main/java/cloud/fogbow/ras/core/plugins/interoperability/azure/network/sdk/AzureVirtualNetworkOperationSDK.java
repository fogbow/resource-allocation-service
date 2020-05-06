package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureGetVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceGroupOperationUtil;
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

    private final String defaultResourceGroupName;
    private Scheduler scheduler;
    private final String regionName;

    public AzureVirtualNetworkOperationSDK(String regionName, String defaultResourceGroupName) {
        ExecutorService virtualMachineExecutor = AzureSchedulerManager.getVirtualNetworkExecutor();
        this.scheduler = Schedulers.from(virtualMachineExecutor);

        this.regionName = regionName;
        this.defaultResourceGroupName = defaultResourceGroupName;
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
        String resourceName = azureCreateVirtualNetworkRef.getResourceName();
        String resourceGroupName = AzureGeneralUtil
                .defineResourceGroupName(azure, this.regionName, resourceName, this.defaultResourceGroupName);

        String cidr = azureCreateVirtualNetworkRef.getCidr();
        Region region = Region.findByLabelOrName(this.regionName);
        Map tags = azureCreateVirtualNetworkRef.getTags();

        return AzureNetworkSDK.createSecurityGroupAsync(azure, resourceName, region, resourceGroupName, cidr, tags);
    }


    @VisibleForTesting
    void doNetworkCreationStepTwoSync(Indexable indexableSecurityGroup,
                                           AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef,
                                           Azure azure) {

        NetworkSecurityGroup networkSecurityGroup = (NetworkSecurityGroup) indexableSecurityGroup;
        String resourceName = azureCreateVirtualNetworkRef.getResourceName();
        String resourceGroupName = AzureGeneralUtil
                .selectResourceGroupName(azure, resourceName, this.defaultResourceGroupName);

        String cidr = azureCreateVirtualNetworkRef.getCidr();
        Region region = Region.findByLabelOrName(this.regionName);
        Map tags = azureCreateVirtualNetworkRef.getTags();

        AzureNetworkSDK.createNetworkSync(
                azure, resourceName, region, resourceGroupName, cidr, networkSecurityGroup, tags);
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
        String name = network.tags().get(AzureConstants.TAG_NAME);
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
        String resourceGroupName = AzureGeneralUtil
                .selectResourceGroupName(azure, resourceName, this.defaultResourceGroupName);

        String azureVirtualNetworkId = getAzureVirtualNetworkId(subscriptionId, resourceGroupName, resourceName);

        return AzureNetworkSDK
                .getNetwork(azure, azureVirtualNetworkId)
                .orElseThrow(InstanceNotFoundException::new);
    }

    public void doDeleteInstance(String resourceName, AzureUser azureUser) throws FogbowException {
        Azure azure = AzureClientCacheManager.getAzure(azureUser);

        if (AzureResourceGroupOperationUtil.existsResourceGroup(azure, resourceName)) {
            Completable deteteResourceGroupCompletable = AzureResourceGroupOperationUtil.deleteResourceGroupAsync(azure, resourceName);
            setDeleteVirtualNetworkBehaviour(deteteResourceGroupCompletable)
                    .subscribeOn(this.scheduler)
                    .subscribe();
        } else {
            String subscriptionId = azureUser.getSubscriptionId();
            String resourceGroupName = AzureGeneralUtil
                    .selectResourceGroupName(azure, resourceName, this.defaultResourceGroupName);

            String azureVirtualNetworkId = getAzureVirtualNetworkId(subscriptionId, resourceGroupName, resourceName);
            Completable firstDeleteVirtualNetwork = buildDeleteVirtualNetworkCompletable(azure, azureVirtualNetworkId);

            String azureSecurityGroupId = getAzureNetworkSecurityGroupId(subscriptionId, resourceGroupName, resourceName);
            Completable secondDeleteSecurityGroup = buildDeleteSecurityGroupCompletable(azure, azureSecurityGroupId);

            Completable.concat(firstDeleteVirtualNetwork, secondDeleteSecurityGroup)
                    .subscribeOn(this.scheduler)
                    .subscribe();
        }
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

    @VisibleForTesting
    String getAzureVirtualNetworkId(String subscriptionId, String resourceGroupName, String resourceName) {
        return AzureResourceIdBuilder.virtualNetworkId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(resourceGroupName)
                .withResourceName(resourceName)
                .build();
    }

    @VisibleForTesting
    String getAzureNetworkSecurityGroupId(String subscriptionId, String resourceGroupName, String resourceName) {
        return AzureResourceIdBuilder.networkSecurityGroupId()
                .withSubscriptionId(subscriptionId)
                .withResourceGroupName(resourceGroupName)
                .withResourceName(resourceName)
                .build();
    }

    @VisibleForTesting
    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}
