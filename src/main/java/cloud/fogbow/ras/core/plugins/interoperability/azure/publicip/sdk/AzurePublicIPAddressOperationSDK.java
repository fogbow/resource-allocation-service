package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk;

import java.util.concurrent.ExecutorService;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureSchedulerManager;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

public class AzurePublicIPAddressOperationSDK {
    
    @VisibleForTesting
    static final Logger LOGGER = Logger.getLogger(AzurePublicIPAddressOperationSDK.class);
    @VisibleForTesting
    static final int FULL_CAPACITY = 2;

    private final String resourceGroupName;

    private Scheduler scheduler;

    public AzurePublicIPAddressOperationSDK(String resourceGroupName) {
        ExecutorService executor = AzureSchedulerManager.getPublicIPAddressExecutor();
        this.scheduler = Schedulers.from(executor);
        this.resourceGroupName = resourceGroupName;
    }

    public void subscribeAssociatePublicIPAddress(Azure azure, String instanceId,
            Observable<NetworkInterface> observable) {
        
        setAssociatePublicIPAddressBehaviour(azure, instanceId, observable)
            .subscribeOn(this.scheduler)
            .subscribe();
    }

    @VisibleForTesting
    Observable<NetworkInterface> setAssociatePublicIPAddressBehaviour(Azure azure, String instanceId,
            Observable<NetworkInterface> observable) {

        return observable.doOnNext(nic -> {
            LOGGER.info(Messages.Info.FIRST_STEP_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR);
            doAssociateNetworkSecurityGroupAsync(azure, instanceId, nic);
            LOGGER.info(Messages.Info.SECOND_STEP_CREATE_AND_ATTACH_NSG_ASYNC_BEHAVIOUR);
        }).onErrorReturn(error -> {
            LOGGER.error(Messages.Error.ERROR_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR, error);
            return null;
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR);
        });
    }

    @VisibleForTesting
    void doAssociateNetworkSecurityGroupAsync(Azure azure, String instanceId, NetworkInterface nic) {
        PublicIPAddress publicIPAddress = doGetPublicIPAddress(azure, instanceId);
        Creatable<NetworkSecurityGroup> creatable = AzurePublicIPAddressSDK
                .buildNetworkSecurityGroupCreatable(azure, publicIPAddress);

        Observable<NetworkInterface> observable = AzurePublicIPAddressSDK
                .associateNetworkSecurityGroupAsync(nic, creatable);

        subscribeUpdateNetworkInterface(azure, instanceId, nic, observable);
    }

    @VisibleForTesting
    void subscribeUpdateNetworkInterface(Azure azure, String instanceId,
            NetworkInterface nic, Observable<NetworkInterface> observable) {

        setUpdateNetworkInterfaceBehaviour(azure, instanceId, nic, observable)
                .subscribeOn(this.scheduler)
                .subscribe();
    }

    @VisibleForTesting
    Observable<NetworkInterface> setUpdateNetworkInterfaceBehaviour(Azure azure,
            String instanceId, NetworkInterface nic, Observable<NetworkInterface> observable) {

        return observable.onErrorReturn(error -> {
            LOGGER.error(Messages.Error.ERROR_UPDATE_NIC_ASYNC_BEHAVIOUR, error);
            doDisassociateAndDeletePublicIPAddressAsync(azure, instanceId, nic);
            return null;
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_UPDATE_NIC_ASYNC_BEHAVIOUR);
        });
    }

    @VisibleForTesting
    PublicIPAddress doGetPublicIPAddress(Azure azure, String instanceId) {
        PublicIPAddress publicIPAddress = azure.publicIPAddresses()
                .getByResourceGroup(this.resourceGroupName, instanceId);

        return publicIPAddress;
    }

    @VisibleForTesting
    void doDisassociateAndDeletePublicIPAddressAsync(Azure azure,
            String instanceId, NetworkInterface nic) {

        PublicIPAddress publicIPAddress = doGetPublicIPAddress(azure, instanceId);
        String resourceId = publicIPAddress.id();

        Observable<NetworkInterface> observable = AzurePublicIPAddressSDK
                .disassociatePublicIPAddressAsync(nic);

        Completable completable = AzurePublicIPAddressSDK
                .deletePublicIpAddressAsync(azure, resourceId);

        subscribeDisassociateAndDeletePublicIPAddress(observable, completable);
    }

    @VisibleForTesting
    void subscribeDisassociateAndDeletePublicIPAddress(
            Observable<NetworkInterface> observable, Completable completable) {

        setDisassociateAndDeletePublicIPAddress(observable, completable)
                .subscribeOn(this.scheduler)
                .subscribe();
    }

    @VisibleForTesting
    Observable<NetworkInterface> setDisassociateAndDeletePublicIPAddress(
            Observable<NetworkInterface> observable, Completable completable) {

        return observable.doOnNext(nic -> {
            LOGGER.info(Messages.Info.FIRST_STEP_DETACH_PUBLIC_IP_ASYNC_BEHAVIOUR);
            subscribeDeletePublicIPAddressAsync(completable);
        }).onErrorReturn(error -> {
            LOGGER.error(Messages.Error.ERROR_DETACH_PUBLIC_IP_ASYNC_BEHAVIOUR, error);
            return null;
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DETACH_PUBLIC_IP_ASYNC_BEHAVIOUR);
        });
    }

    @VisibleForTesting
    void subscribeDeletePublicIPAddressAsync(Completable completable) {
        setDeletePublicIPAddressBehaviour(completable)
                .subscribeOn(this.scheduler)
                .subscribe();
    }

    @VisibleForTesting
    Completable setDeletePublicIPAddressBehaviour(Completable completable) {
        return completable.doOnError(error -> {
            LOGGER.error(Messages.Error.ERROR_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR, error);
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR);
        });
    }

    public void subscribeDisassociateAndDeleteResources(Observable observable, Completable completable) {
        setDisassociateAndDeleteResourcesBehaviour(observable, completable)
            .subscribeOn(this.scheduler)
            .subscribe();
    }

    @VisibleForTesting
    Observable setDisassociateAndDeleteResourcesBehaviour(Observable observable, Completable completable) {
        return observable.doOnNext(step -> {
            LOGGER.info(Messages.Info.FIRST_STEP_DETACH_RESOURCES_ASYNC_BEHAVIOUR);
            subscribeDeleteResources(completable);
        }).onErrorReturn(error -> {
            LOGGER.error(Messages.Error.ERROR_DETACH_RESOURCES_ASYNC_BEHAVIOUR, (Throwable) error);
            return null;
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DETACH_RESOURCES_ASYNC_BEHAVIOUR);
        });
    }

    @VisibleForTesting
    void subscribeDeleteResources(Completable completable) {
        setDeleteResourcesBehaviour(completable)
            .subscribeOn(this.scheduler)
            .subscribe();
    }

    @VisibleForTesting
    Completable setDeleteResourcesBehaviour(Completable completable) {
        return completable.doOnError(error -> {
            LOGGER.error(Messages.Error.ERROR_DELETE_RESOURCES_ASYNC_BEHAVIOUR, error);
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DELETE_RESOURCES_ASYNC_BEHAVIOUR);
        });
    }

    @VisibleForTesting
    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

}
