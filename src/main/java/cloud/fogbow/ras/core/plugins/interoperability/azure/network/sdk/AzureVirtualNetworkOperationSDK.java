package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnexpectedException;
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
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

public class AzureVirtualNetworkOperationSDK {

    private static final Logger LOGGER = Logger.getLogger(AzureVirtualNetworkOperationSDK.class);

    private final Scheduler scheduler;
    private final String resourceGroupName;
    private final String regionName;

    public AzureVirtualNetworkOperationSDK(String regionName, String defaultResourceGroupName) {
        ExecutorService virtualNetworkExecutor = AzureSchedulerManager.getVirtualNetworkExecutor();
        this.scheduler = Schedulers.from(virtualNetworkExecutor);
        this.regionName = regionName;
        this.resourceGroupName = defaultResourceGroupName;
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
        String resourceGroupName = this.resourceGroupName;
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
        String resourceGroupName = this.resourceGroupName;
        String cidr = azureCreateVirtualNetworkRef.getCidr();
        Region region = Region.fromName(this.regionName);

        AzureNetworkSDK.createNetworkSync(azure, name, region, resourceGroupName, cidr, networkSecurityGroup);
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
}
