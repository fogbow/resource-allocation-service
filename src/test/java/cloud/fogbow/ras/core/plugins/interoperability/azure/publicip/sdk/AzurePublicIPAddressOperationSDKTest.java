package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk;

import cloud.fogbow.common.util.connectivity.cloud.azure.AzureClientCacheManager;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AsyncInstanceCreationManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceGroupOperationUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.PublicIPAddresses;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Azure.class,
    AzureClientCacheManager.class,
    AzureGeneralUtil.class,
    AzurePublicIPAddressSDK.class,
    AzureResourceGroupOperationUtil.class
})
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

        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = Mockito.mock(AsyncInstanceCreationManager.Callbacks.class);

        // exercise
        this.operation.subscribeAssociatePublicIPAddress(azure, instanceId, observable, finishCreationCallbacks);

        // verify
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .doAssociateNetworkSecurityGroupAsync(Mockito.eq(azure),
                Mockito.eq(instanceId), Mockito.eq(networkInterface));

        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Log.FIRST_STEP_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Log.SECOND_STEP_CREATE_AND_ATTACH_NSG_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Log.END_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR);
        Mockito.verify(finishCreationCallbacks, Mockito.times(TestUtils.RUN_ONCE)).runOnComplete();
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

        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = Mockito.mock(AsyncInstanceCreationManager.Callbacks.class);

        // exercise
        this.operation.subscribeAssociatePublicIPAddress(azure, instanceId, observable, finishCreationCallbacks);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Log.ERROR_CREATE_PUBLIC_IP_ASYNC_BEHAVIOUR);
        Mockito.verify(finishCreationCallbacks, Mockito.times(TestUtils.RUN_ONCE)).runOnError();
        Mockito.verify(finishCreationCallbacks, Mockito.times(TestUtils.RUN_ONCE)).runOnComplete();
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
                Mockito.eq(instanceId), Mockito.eq(networkInterface), Mockito.eq(observable));

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
                .subscribeUpdateNetworkInterface(Mockito.eq(azure), Mockito.eq(instanceId), Mockito.eq(networkInterface),
                Mockito.eq(observable));
    }

    // test case: When calling the subscribeUpdateNetworkInterface method and the
    // observable executes without any error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeUpdateNetworkInterfaceSuccessfully() {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String instanceId = AzureGeneralUtil.defineInstanceId(AzureTestUtils.RESOURCE_NAME);

        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        Observable<NetworkInterface> observable = Observable.defer(() -> {
            return Observable.just(networkInterface);
        });

        // exercise
        this.operation.subscribeUpdateNetworkInterface(azure, instanceId, networkInterface, observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Log.END_UPDATE_NIC_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeUpdateNetworkInterface method and the
    // observable executes with an error, it must verify than returns the right
    // logs.
    @Test
    public void testSubscribeUpdateNetworkInterfaceFail() {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String instanceId = AzureGeneralUtil.defineInstanceId(AzureTestUtils.RESOURCE_NAME);
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        Observable observable = AzureTestUtils.createSimpleObservableFail();

        Mockito.doNothing().when(this.operation).doDisassociateAndDeletePublicIPAddressAsync(Mockito.eq(azure),
                Mockito.eq(instanceId), Mockito.eq(networkInterface));

        // exercise
        this.operation.subscribeUpdateNetworkInterface(azure, instanceId, networkInterface, observable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Log.ERROR_UPDATE_NIC_ASYNC_BEHAVIOUR);

        Mockito.verify(this.operation, Mockito.timeout(TestUtils.RUN_ONCE))
                .doDisassociateAndDeletePublicIPAddressAsync(Mockito.eq(azure), Mockito.eq(instanceId),
                Mockito.eq(networkInterface));
    }

    // test case: When calling the doGetPublicIPAddress method, it must verify
    // that is call was successful.
    @Test
    public void testDoGetPublicIPAddressSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;

        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceGroupName).when(AzureGeneralUtil.class, "selectResourceGroupName",
                Mockito.eq(azure), Mockito.eq(resourceName), Mockito.eq(resourceGroupName));

        PublicIPAddresses publicIPAddresses = Mockito.mock(PublicIPAddresses.class);
        Mockito.when(azure.publicIPAddresses()).thenReturn(publicIPAddresses);

        PublicIPAddress publicIPAddress = Mockito.mock(PublicIPAddress.class);
        Mockito.when(publicIPAddresses.getByResourceGroup(Mockito.eq(this.resourceGroupName),
                Mockito.eq(resourceName))).thenReturn(publicIPAddress);

        // exercise
        this.operation.doGetPublicIPAddress(azure, resourceName);

        // verify
        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.selectResourceGroupName(Mockito.eq(azure), Mockito.eq(resourceName),
                Mockito.eq(resourceGroupName));

        Mockito.verify(azure, Mockito.times(TestUtils.RUN_ONCE)).publicIPAddresses();
        Mockito.verify(publicIPAddresses, Mockito.times(TestUtils.RUN_ONCE))
                .getByResourceGroup(Mockito.eq(this.resourceGroupName), Mockito.eq(resourceName));
    }

    // test case: When calling the doDisassociateAndDeletePublicIPAddressAsync
    // method, without the default resource group, it must verify among others
    // if the deleteResourceGroupAsync method has been called.
    @Test
    public void testDoDisassociateAndDeletePublicIPAddressAsyncWithoutDefaultResourceGroup() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);

        Observable<NetworkInterface> observable = Mockito.mock(Observable.class);
        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(observable).when(AzurePublicIPAddressSDK.class, "disassociatePublicIPAddressAsync",
                Mockito.eq(networkInterface));

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(true).when(AzureResourceGroupOperationUtil.class, "existsResourceGroup",
                Mockito.eq(azure), Mockito.eq(resourceName));

        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();
        PowerMockito.doReturn(completable).when(AzureResourceGroupOperationUtil.class, "deleteResourceGroupAsync",
                Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.doNothing().when(this.operation)
                .subscribeDisassociateAndDeletePublicIPAddress(Mockito.eq(observable), Mockito.eq(completable));

        // exercise
        this.operation.doDisassociateAndDeletePublicIPAddressAsync(azure, resourceName, networkInterface);

        // verify
        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.disassociatePublicIPAddressAsync(Mockito.eq(networkInterface));

        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.existsResourceGroup(Mockito.eq(azure), Mockito.eq(resourceName));

        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.deleteResourceGroupAsync(Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .subscribeDisassociateAndDeletePublicIPAddress(Mockito.eq(observable), Mockito.eq(completable));
    }

    // test case: When calling the doDisassociateAndDeletePublicIPAddressAsync
    // method, with the default resource group, it must verify among others if
    // the deletePublicIpAddressAsync method has been called.
    @Test
    public void testDoDisassociateAndDeletePublicIPAddressAsyncWithDefaultResourceGroup() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);

        Observable<NetworkInterface> observable = Mockito.mock(Observable.class);
        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
        PowerMockito.doReturn(observable).when(AzurePublicIPAddressSDK.class, "disassociatePublicIPAddressAsync",
                Mockito.eq(networkInterface));

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(false).when(AzureResourceGroupOperationUtil.class, "existsResourceGroup",
                Mockito.eq(azure), Mockito.eq(resourceName));

        String resourceId = AzureResourceIdBuilder.publicIpAddressId().build();
        PublicIPAddress publicIPAddress = Mockito.mock(PublicIPAddress.class);
        Mockito.when(publicIPAddress.id()).thenReturn(resourceId);
        Mockito.doReturn(publicIPAddress).when(this.operation).doGetPublicIPAddress(Mockito.eq(azure),
                Mockito.eq(resourceName));

        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();
        PowerMockito.doReturn(completable).when(AzurePublicIPAddressSDK.class, "deletePublicIpAddressAsync",
                Mockito.eq(azure), Mockito.eq(resourceId));

        Mockito.doNothing().when(this.operation)
                .subscribeDisassociateAndDeletePublicIPAddress(Mockito.eq(observable), Mockito.eq(completable));

        // exercise
        this.operation.doDisassociateAndDeletePublicIPAddressAsync(azure, resourceName, networkInterface);

        // verify
        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.disassociatePublicIPAddressAsync(Mockito.eq(networkInterface));

        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.existsResourceGroup(Mockito.eq(azure), Mockito.eq(resourceName));

        Mockito.verify(publicIPAddress, Mockito.times(TestUtils.RUN_ONCE)).id();
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).doGetPublicIPAddress(Mockito.eq(azure),
                Mockito.eq(resourceName));

        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.deletePublicIpAddressAsync(Mockito.eq(azure), Mockito.eq(resourceId));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .subscribeDisassociateAndDeletePublicIPAddress(Mockito.eq(observable), Mockito.eq(completable));
    }

    // test case: When calling the subscribeDisassociateAndDeletePublicIPAddress
    // method and the observable executes without any error, it must verify than
    // returns the right logs.
    @Test
    public void testSubscribeDisassociateAndDeletePublicIPAddressSuccessfully() {
        // set up
        Observable<NetworkInterface> observable = Observable.defer(() -> {
            NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
            return Observable.just(networkInterface);
        });

        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();
        Mockito.doNothing().when(this.operation).subscribeDeletePublicIPAddressAsync(Mockito.eq(completable));

        // exercise
        this.operation.subscribeDisassociateAndDeletePublicIPAddress(observable, completable);

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Log.FIRST_STEP_DETACH_PUBLIC_IP_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Log.END_DETACH_PUBLIC_IP_ASYNC_BEHAVIOUR);

        Mockito.verify(this.operation, Mockito.timeout(TestUtils.RUN_ONCE))
                .subscribeDeletePublicIPAddressAsync(Mockito.eq(completable));
    }

    // test case: When calling the subscribeDisassociateAndDeletePublicIPAddress
    // method and the observable executes with an error, it must verify than returns
    // the right logs.
    @Test
    public void testSubscribeDisassociateAndDeletePublicIPAddressFail() {
        // set up
        Observable observable = AzureTestUtils.createSimpleObservableFail();
        Completable completable = AzureTestUtils.createSimpleCompletableFail();

        // exercise
        this.operation.subscribeDisassociateAndDeletePublicIPAddress(observable, completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Log.ERROR_DETACH_PUBLIC_IP_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeDeletePublicIPAddressAsync method
    // and the observable executes without any error, it must verify than
    // returns the right logs.
    @Test
    public void testSubscribeDeletePublicIPAddressAsyncSuccessfully() {
        // set up
        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();

        // exercise
        this.operation.subscribeDeletePublicIPAddressAsync(completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Log.END_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the subscribeDeletePublicIPAddressAsync
    // method and the observable executes with an error, it must verify than
    // returns the right logs.
    @Test
    public void testSubscribeDeletePublicIPAddressAsyncFail() {
        // set up
        Completable completable = AzureTestUtils.createSimpleCompletableFail();

        // exercise
        this.operation.subscribeDeletePublicIPAddressAsync(completable);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Log.ERROR_DELETE_PUBLIC_IP_ASYNC_BEHAVIOUR);
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
                .assertEqualsInOrder(Level.INFO, Messages.Log.FIRST_STEP_DETACH_RESOURCES_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Log.END_DETACH_RESOURCES_ASYNC_BEHAVIOUR);
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
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Log.ERROR_DETACH_RESOURCES_ASYNC_BEHAVIOUR);
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
        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Log.END_DELETE_RESOURCES_ASYNC_BEHAVIOUR);
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
        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Log.ERROR_DELETE_RESOURCES_ASYNC_BEHAVIOUR);
    }

}
