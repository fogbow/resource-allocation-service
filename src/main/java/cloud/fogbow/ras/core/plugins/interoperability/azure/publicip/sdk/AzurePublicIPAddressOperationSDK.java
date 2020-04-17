package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk;

import java.util.Optional;
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
    static final int FULL_CAPACITY = 3;

    private final String resourceGroupName;
    private final String securityGroupName;
    private final Scheduler scheduler;

    public AzurePublicIPAddressOperationSDK(String resourceGroupName, String securirtyGroupName) {
        ExecutorService executor = AzureSchedulerManager.getPublicIPAddressExecutor();
        this.scheduler = Schedulers.from(executor);
        this.resourceGroupName = resourceGroupName;
        this.securityGroupName = securirtyGroupName;
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
            LOGGER.info(Messages.Info.SECOND_STEP_ATTACH_RULE_ASYNC_BEHAVIOUR);
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

        Optional<NetworkSecurityGroup> optional = AzurePublicIPAddressSDK
                .getNetworkSecurityGroupFrom(networkInterface);

        String networkSecurityGroupName = getNetworkSecurityGroupName(optional);

        Creatable<NetworkSecurityGroup> creatable = AzurePublicIPAddressSDK
                .buildSecurityRuleCreatable(azure, publicIPAddress, networkSecurityGroupName);

        Observable<NetworkInterface> observable = AzurePublicIPAddressSDK
                .associateNetworkSecurityGroupAsync(networkInterface, creatable);

        subscribeUpdateNetworkInterface(observable);
    }

    @VisibleForTesting
    String getNetworkSecurityGroupName(Optional<NetworkSecurityGroup> optional) {
        if (optional != null) {
            NetworkSecurityGroup networkSecurityGroup = optional.get();
            return networkSecurityGroup.name();
        }
        return this.securityGroupName;
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
        doDeleteResources(observables, completable);
    }

    @VisibleForTesting
    void doDeleteResources(Stack<Observable> observables, Completable completable) {
        if (!observables.isEmpty()) {
            setDeleteResourcesBehaviour(observables, completable)
                .subscribeOn(this.scheduler)
                .subscribe();
        } else {
            setDeletePublicIPAddressBehaviour(completable)
                .subscribeOn(this.scheduler)
                .subscribe();
        }
    }

    @VisibleForTesting
    Completable setDeletePublicIPAddressBehaviour(Completable completable) {
        return completable.doOnError(error -> {
            LOGGER.error(Messages.Error.ERROR_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR, error);
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR);
        });
    }

    @VisibleForTesting
    Observable setDeleteResourcesBehaviour(Stack<Observable> observables, Completable completable) {
        return observables.pop().doOnNext(step -> {
            LOGGER.info(getStageMessage(observables));
            doDeleteResources(observables, completable);
        }).onErrorReturn(error -> {
            LOGGER.error(Messages.Error.ERROR_DELETE_RESOURCES_ASYNC_BEHAVIOUR, (Throwable) error);
            return null;
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DELETE_RESOURCES_ASYNC_BEHAVIOUR);
        });
    }

    @VisibleForTesting
    String getStageMessage(Stack<Observable> observables) {
        int stage = FULL_CAPACITY - observables.size();
        String message = null;
        switch (stage) {
        case 1:
            message = Messages.Info.FIRST_STEP_DELETE_RULE_ASYNC_BEHAVIOUR;
            break;
        case 2:
            message =  Messages.Info.SECOND_STEP_DETACH_PUBLIC_IP_ASYNC_BEHAVIOUR;
            break;
        case 3:
            message =  Messages.Info.THIRD_STEP_DETACH_SECURITY_GROUP_ASYNC_BEHAVIOUR;
            break;
        }
        return message;
    }

}
