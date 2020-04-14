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

public class AzurePublicIpAddressOperationSDK {
    
    private static final Logger LOGGER = Logger.getLogger(AzurePublicIpAddressOperationSDK.class);

    @VisibleForTesting
    static final int FULL_CAPACITY = 3;
    @VisibleForTesting
    static final String[] CARDINAL_NUMBERS = {"First", "Second", "Thrid"};

    private final String resourceGroupName;
    private final Scheduler scheduler;

    public AzurePublicIpAddressOperationSDK(String resourceGroupName) {
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
            LOGGER.info("First step: Public IP Address created and associated with the virtual machine."); // FIXME
            handleSecurityIssues(azure, instanceId, nic);
            LOGGER.info("Second step: Added rule into Security Group and associated with the network interface."); // FIXME
        }).onErrorReturn(error -> {
            LOGGER.error("Error while creating public IP address asynchronously.", error); // FIXME
            return null;
        }).doOnCompleted(() -> {
            LOGGER.info("End asynchronous create public IP address."); // FIXME
        });
    }

    @VisibleForTesting
    void handleSecurityIssues(Azure azure, String instanceId, NetworkInterface networkInterface) {
        PublicIPAddress publicIPAddress = azure.publicIPAddresses()
                .getByResourceGroup(this.resourceGroupName, instanceId);

        NetworkSecurityGroup networkSecurityGroup = AzurePublicIPAddressSDK
                .getNetworkSecurityGroupFrom(networkInterface);

        Creatable<NetworkSecurityGroup> creatable = AzurePublicIPAddressSDK
                .buildSecurityRuleCreatable(azure, publicIPAddress, networkSecurityGroup);

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
            LOGGER.error("Error while updating network interface asynchronously.", error); // FIXME
            return null;
        }).doOnCompleted(() -> {
            LOGGER.info("End asynchronous update network interface."); // FIXME
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
            LOGGER.error(Messages.Error.ERROR_DELETE_PUBLIC_IP_ADDRRES_ASYNC_BEHAVIOUR, error);
        }).doOnCompleted(() -> {
            LOGGER.info(Messages.Info.END_DELETE_PUBLIC_ADDRRES_IP_ASYNC_BEHAVIOUR);
        });
    }

    @VisibleForTesting
    Observable setDeleteResourcesBehaviour(Stack<Observable> observables, Completable completable) {
        return observables.pop().doOnNext(step -> {
            String message = getStageMessage(observables);
            LOGGER.info(message);
            doDeleteResources(observables, completable);
        }).onErrorReturn(error -> {
            LOGGER.error("Error while deleting resources asynchronously.", (Throwable) error); // FIXME
            return null;
        }).doOnCompleted(() -> {
            LOGGER.info("End asynchronous delete resources."); // FIXME
        });
    }

    @VisibleForTesting
    String getStageMessage(Stack<Observable> observables) {
        int stage = FULL_CAPACITY - observables.size();
        switch (stage) {
        case 1:
            return String.format("%s Step: Security rule deleted from network security group.", CARDINAL_NUMBERS[stage]); // FIXME
        case 2:
            return String.format("%s Step: Public IP addres disassociated from virtual machine.", CARDINAL_NUMBERS[stage]); // FIXME
        case 3:
            return String.format("%s Step: Network Security Group disassociated from network interface.", CARDINAL_NUMBERS[stage]); // FIXME
        }
        return null;
    }

}
