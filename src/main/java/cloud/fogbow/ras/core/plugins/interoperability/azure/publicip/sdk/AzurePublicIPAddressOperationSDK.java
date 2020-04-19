package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk;

import java.util.Stack;
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
    private final Scheduler scheduler;

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
            handleSecurityIssues(azure, instanceId, nic);
            LOGGER.info(Messages.Info.SECOND_STEP_CREATE_AND_ATTACH_NSG_ASYNC_BEHAVIOUR);
        }).onErrorReturn(error -> {
            LOGGER.error(Messages.Error.ERROR_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR, error);
            return null;
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR);
        });
    }

    @VisibleForTesting
    void handleSecurityIssues(Azure azure, String instanceId, NetworkInterface networkInterface) {
        PublicIPAddress publicIPAddress = azure.publicIPAddresses()
                .getByResourceGroup(this.resourceGroupName, instanceId);

        Creatable<NetworkSecurityGroup> creatable = AzurePublicIPAddressSDK
                .buildNetworkSecurityGroupCreatable(azure, publicIPAddress);

        Observable<NetworkInterface> observable = AzurePublicIPAddressSDK
                .associateNetworkSecurityGroupAsync(networkInterface, creatable);

        subscribeUpdateNetworkInterface(observable);
    }

    @VisibleForTesting
    void subscribeUpdateNetworkInterface(Observable<NetworkInterface> observable) {
        setUpdateNetworkInterfaceBehaviour(observable)
            .subscribeOn(this.scheduler)
            .subscribe();
    }

    @VisibleForTesting
    Observable<NetworkInterface> setUpdateNetworkInterfaceBehaviour(Observable<NetworkInterface> observable) {
        return observable.onErrorReturn(error -> {
            LOGGER.error(Messages.Error.ERROR_UPDATE_NIC_ASYNC_BEHAVIOUR, error);
            return null;
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_UPDATE_NIC_ASYNC_BEHAVIOUR);
        });
    }

    public void subscribeDeleteResources(Stack<Observable> observables, Completable completable) {
        doDisassociateAndDeleteResources(observables, completable);
    }

    @VisibleForTesting
    void doDisassociateAndDeleteResources(Stack<Observable> observables, Completable completable) {
        if (!observables.isEmpty()) {
            setDisassociateResourcesBehaviour(observables, completable)
                .subscribeOn(this.scheduler)
                .subscribe();
        } else {
            setDeleteResourcesBehaviour(completable)
                .subscribeOn(this.scheduler)
                .subscribe();
        }
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
    Observable setDisassociateResourcesBehaviour(Stack<Observable> observables, Completable completable) {
        return observables.pop().doOnNext(step -> {
            LOGGER.info(getStageMessage(observables));
            doDisassociateAndDeleteResources(observables, completable);
        }).onErrorReturn(error -> {
            LOGGER.error(Messages.Error.ERROR_DETACH_RESOURCES_ASYNC_BEHAVIOUR, (Throwable) error);
            return null;
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DETACH_RESOURCES_ASYNC_BEHAVIOUR);
        });
    }

    @VisibleForTesting
    String getStageMessage(Stack<Observable> observables) {
        int stage = FULL_CAPACITY - observables.size();
        String message = null;
        switch (stage) {
        case 1:
            message = Messages.Info.FIRST_STEP_DETACH_NSG_ASYNC_BEHAVIOUR;
            break;
        case 2:
            message =  Messages.Info.SECOND_STEP_DETACH_PUBLIC_IP_ASYNC_BEHAVIOUR;
            break;
        }
        return message;
    }

}
