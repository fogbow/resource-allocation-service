package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.PublicIPAddresses;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;

import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Azure.class, AzurePublicIPAddressSDK.class, AzureClientCacheManager.class})
public class AzurePublicIPAddressOperationSDKTest {

    private AzurePublicIPAddressOperationSDK operation;
    private LoggerAssert loggerAssert;
    private String resourceGroupName;

    @Before
    public void setup() {
        // The scheduler trampoline makes the subscriptions execute in the current thread
        Scheduler scheduler = Schedulers.trampoline();
        this.resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        this.operation = Mockito.spy(new AzurePublicIPAddressOperationSDK(this.resourceGroupName));
        this.operation.setScheduler(scheduler);
        this.loggerAssert = new LoggerAssert(AzurePublicIPAddressOperationSDK.class);
    }

    // test case: When calling the subscribeAssociatePublicIPAddress method and the
    // observable executes without any error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeAssociatePublicIPAddressSuccessfully() {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String instanceId = AzureGeneralUtil.defineInstanceId(AzureTestUtils.RESOURCE_NAME);

        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        Observable<NetworkInterface> observable = Observable.defer(() -> {
            return Observable.just(networkInterface);
        });

        Mockito.doNothing().when(this.operation).doAssociateNetworkSecurityGroupAsync(Mockito.eq(azure),
                Mockito.eq(instanceId), Mockito.eq(networkInterface));

        // exercise
        this.operation.subscribeAssociatePublicIPAddress(azure, instanceId, observable);

        // verify
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .doAssociateNetworkSecurityGroupAsync(Mockito.eq(azure),
                Mockito.eq(instanceId), Mockito.eq(networkInterface));

        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Info.FIRST_STEP_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Info.SECOND_STEP_CREATE_AND_ATTACH_NSG_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeAssociatePublicIPAddress method and the
    // observable executes with an error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeAssociatePublicIPAddressFail() {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String instanceId = AzureGeneralUtil.defineInstanceId(AzureTestUtils.RESOURCE_NAME);

        Observable observable = AzureTestUtils.createSimpleObservableFail();

        // exercise
        this.operation.subscribeAssociatePublicIPAddress(azure, instanceId, observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the doAssociateNetworkSecurityGroupAsync method,
    // it must verify that is call was successful.
    @Test
    public void testDoAssociateNetworkSecurityGroupAsyncSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String instanceId = AzureGeneralUtil.defineInstanceId(AzureTestUtils.RESOURCE_NAME);
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);

        PublicIPAddresses publicIPAddresses = Mockito.mock(PublicIPAddresses.class);
        Mockito.when(azure.publicIPAddresses()).thenReturn(publicIPAddresses);

        PublicIPAddress publicIPAddress = Mockito.mock(PublicIPAddress.class);
        Mockito.doReturn(publicIPAddress).when(this.operation).doGetPublicIPAddress(Mockito.eq(azure),
                Mockito.eq(instanceId));

        Creatable<NetworkSecurityGroup> creatable = Mockito.mock(Creatable.class);
        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(creatable).when(AzurePublicIPAddressSDK.class, "buildNetworkSecurityGroupCreatable",
                Mockito.eq(azure), Mockito.eq(publicIPAddress));

        Observable<NetworkInterface> observable = Mockito.mock(Observable.class);
        PowerMockito.doReturn(observable).when(AzurePublicIPAddressSDK.class, "associateNetworkSecurityGroupAsync",
                Mockito.eq(networkInterface), Mockito.eq(creatable));

        Mockito.doNothing().when(this.operation).subscribeUpdateNetworkInterface(Mockito.eq(azure),
                Mockito.eq(instanceId), Mockito.eq(observable));

        // exercise
        this.operation.doAssociateNetworkSecurityGroupAsync(azure, instanceId, networkInterface);

        // verify
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).doGetPublicIPAddress(Mockito.eq(azure),
                Mockito.eq(instanceId));

        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.buildNetworkSecurityGroupCreatable(Mockito.eq(azure), Mockito.eq(publicIPAddress));

        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.associateNetworkSecurityGroupAsync(Mockito.eq(networkInterface), Mockito.eq(creatable));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .subscribeUpdateNetworkInterface(Mockito.eq(azure), Mockito.eq(instanceId), Mockito.eq(observable));
    }

    // test case: When calling the subscribeUpdateNetworkInterface method and the
    // observable executes without any error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeUpdateNetworkInterfaceSuccessfully() {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String instanceId = AzureGeneralUtil.defineInstanceId(AzureTestUtils.RESOURCE_NAME);

        Observable<NetworkInterface> observable = Observable.defer(() -> {
            NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
            return Observable.just(networkInterface);
        });

        // exercise
        this.operation.subscribeUpdateNetworkInterface(azure, instanceId, observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Info.END_UPDATE_NIC_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeUpdateNetworkInterface method and the
    // observable executes with an error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeUpdateNetworkInterfaceFail() {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String instanceId = AzureGeneralUtil.defineInstanceId(AzureTestUtils.RESOURCE_NAME);
        Observable observable = AzureTestUtils.createSimpleObservableFail();

        Mockito.doNothing().when(this.operation).doDeletePublicIPAddressAsync(Mockito.eq(azure), Mockito.eq(instanceId));

        // exercise
        this.operation.subscribeUpdateNetworkInterface(azure, instanceId, observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_UPDATE_NIC_ASYNC_BEHAVIOUR);

        Mockito.verify(this.operation, Mockito.timeout(TestUtils.RUN_ONCE))
                .doDeletePublicIPAddressAsync(Mockito.eq(azure), Mockito.eq(instanceId));
    }

    // test case: When calling the doGetPublicIPAddress method, it must verify
    // that is call was successful.
    @Test
    public void testDoGetPublicIPAddressSuccessfully() {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String instanceId = AzureGeneralUtil.defineInstanceId(AzureTestUtils.RESOURCE_NAME);

        PublicIPAddresses publicIPAddresses = Mockito.mock(PublicIPAddresses.class);
        Mockito.when(azure.publicIPAddresses()).thenReturn(publicIPAddresses);

        PublicIPAddress publicIPAddress = Mockito.mock(PublicIPAddress.class);
        Mockito.when(publicIPAddresses.getByResourceGroup(Mockito.eq(this.resourceGroupName),
                Mockito.eq(instanceId))).thenReturn(publicIPAddress);

        // exercise
        this.operation.doGetPublicIPAddress(azure, instanceId);

        // verify
        Mockito.verify(azure, Mockito.times(TestUtils.RUN_ONCE)).publicIPAddresses();
        Mockito.verify(publicIPAddresses, Mockito.times(TestUtils.RUN_ONCE))
                .getByResourceGroup(Mockito.eq(this.resourceGroupName), Mockito.eq(instanceId));
    }

    // test case: When calling the doDeletePublicIPAddressAsync method, it must
    // verify that is call was successful.
    @Test
    public void testDoDeletePublicIPAddressAsyncSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String instanceId = AzureGeneralUtil.defineInstanceId(AzureTestUtils.RESOURCE_NAME);
        String resourceId = AzureResourceIdBuilder.publicIpAddressId().build();

        PublicIPAddress publicIPAddress = Mockito.mock(PublicIPAddress.class);
        Mockito.when(publicIPAddress.id()).thenReturn(resourceId);

        Mockito.doReturn(publicIPAddress).when(this.operation).doGetPublicIPAddress(Mockito.eq(azure),
                Mockito.eq(instanceId));

        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();
        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(completable).when(AzurePublicIPAddressSDK.class, "deletePublicIpAddressAsync",
                Mockito.eq(azure), Mockito.eq(resourceId));

        Mockito.doNothing().when(this.operation).subscribeDeletePublicIPAddress(completable);

        // exercise
        this.operation.doDeletePublicIPAddressAsync(azure, instanceId);

        // verify
        Mockito.verify(publicIPAddress, Mockito.times(TestUtils.RUN_ONCE)).id();
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).doGetPublicIPAddress(Mockito.eq(azure),
                Mockito.eq(instanceId));

        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.deletePublicIpAddressAsync(Mockito.eq(azure), Mockito.eq(resourceId));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).subscribeDeletePublicIPAddress(
                Mockito.eq(completable));
    }

    // test case: When calling the subscribeDeletePublicIPAddress method
    // and the observable executes without any error, it must verify than
    // returns the right logs.
    @Test
    public void testSubscribeDeletePublicIPAddressSuccessfully() {
        // set up
        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();

        // exercise
        this.operation.subscribeDeletePublicIPAddress(completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeDeletePublicIPAddress method
    // and the observable executes with an error, it must verify than returns
    // the right logs.
    @Test
    public void testSubscribeDeletePublicIPAddressFail() {
        // set up
        Completable completable = AzureTestUtils.createSimpleCompletableFail();

        // exercise
        this.operation.subscribeDeletePublicIPAddress(completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeDisassociateAndDeleteResources method
    // and the observable executes without any error, it must verify than returns
    // the right logs.
    @Test
    public void testSubscribeDisassociateAndDeleteResourcesSuccessfully() {
        // set up
        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();

        Observable<NetworkInterface> observable = Observable.defer(() -> {
            NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
            return Observable.just(networkInterface);
        });

        Mockito.doNothing().when(this.operation).subscribeDeleteResources(Mockito.eq(completable));

        // exercise
        this.operation.subscribeDisassociateAndDeleteResources(observable, completable);

        // verify
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .subscribeDeleteResources(Mockito.eq(completable));

        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Info.FIRST_STEP_DETACH_RESOURCES_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_DETACH_RESOURCES_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeDisassociateAndDeleteResources method
    // and the observable executes with an error, it must verify than returns the
    // right logs.
    @Test
    public void testSubscribeDisassociateAndDeleteResourcesFail() {
        // set up
        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();
        Observable observable = AzureTestUtils.createSimpleObservableFail();

        // exercise
        this.operation.subscribeDisassociateAndDeleteResources(observable, completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DETACH_RESOURCES_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeDeleteResources method and the
    // completable executes without any error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeDeleteDiskSuccessfully() {
        // set up
        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();

        // exercise
        this.operation.subscribeDeleteResources(completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_RESOURCES_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeDeleteDisk method and the completable
    // executes with an error, it must verify if It returns the right logs.
    @Test
    public void testSubscribeDeleteDiskFail() {
        // set up
        Completable completable = AzureTestUtils.createSimpleCompletableFail();

        // exercise
        this.operation.subscribeDeleteResources(completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DELETE_RESOURCES_ASYNC_BEHAVIOUR);
    }

}
