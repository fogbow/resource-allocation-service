package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.connectivity.cloud.azure.AzureClientCacheManager;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureGetVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AsyncInstanceCreationManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceGroupOperationUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.implementation.VirtualNetworkInner;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import rx.Completable;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AzureNetworkSDK.class, AzureClientCacheManager.class, AzureGeneralUtil.class, AzureResourceGroupOperationUtil.class })
public class AzureVirtualNetworkOperationSDKTest {

    private static final Logger LOGGER_CLASS_MOCK = Logger.getLogger(AzureVirtualNetworkOperationSDK.class);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private LoggerAssert loggerAssert = new LoggerAssert(AzureVirtualNetworkOperationSDK.class);

    private AzureVirtualNetworkOperationSDK azureVirtualNetworkOperationSDK;
    private String regionName = AzureTestUtils.DEFAULT_REGION_NAME;
    private String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
    private AzureUser azureUser;
    private Azure azure;

    @Before
    public void setUp() {
        this.azureVirtualNetworkOperationSDK = Mockito.spy(new AzureVirtualNetworkOperationSDK(this.regionName, this.resourceGroupName));
        this.azureUser = AzureTestUtils.createAzureUser();
        this.azure = null;

        makeTheObservablesSynchronous();
    }

    // test case: When calling the buildVirtualNetworkCreationObservable method and the observables execute
    // without any error, it must verify if It returns the right logs and define instance as ready.
    @Test
    public void testBuildVirtualNetworkCreationObservableSuccessfully() {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .resourceName(resourceName)
                .build();
        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = Mockito.mock(AsyncInstanceCreationManager.Callbacks.class);

        Indexable securityGroupIndexable = Mockito.mock(Indexable.class);
        Observable<Indexable> observableSecurityGroupSuccess = AzureTestUtils.createSimpleObservableSuccess(securityGroupIndexable);
        Mockito.doReturn(observableSecurityGroupSuccess).when(this.azureVirtualNetworkOperationSDK)
                .buildCreateSecurityGroupObservable(Mockito.eq(azureCreateVirtualNetworkRef), Mockito.eq(this.azure));

        Mockito.doNothing().when(this.azureVirtualNetworkOperationSDK)
                .doNetworkCreationStepTwoSync(Mockito.eq(securityGroupIndexable),
                        Mockito.eq(azureCreateVirtualNetworkRef), Mockito.eq(this.azure));

        // exercise

        Observable<Indexable> virtualNetworkCreationObservable = this.azureVirtualNetworkOperationSDK
                .buildVirtualNetworkCreationObservable(azureCreateVirtualNetworkRef, this.azure, finishCreationCallbacks);
        virtualNetworkCreationObservable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Log.FIRST_STEP_CREATE_VNET_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Log.SECOND_STEP_CREATE_VNET_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Log.END_CREATE_VNET_ASYNC_BEHAVIOUR);
        Mockito.verify(finishCreationCallbacks, Mockito.times(TestUtils.RUN_ONCE)).runOnComplete();
    }

    // test case: When calling the buildVirtualNetworkCreationObservable method and the observables execute
    // with an error, it must verify if It returns the right logs with an error log.
    @Test
    public void testBuildVirtualNetworkCreationObservableFail() {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .resourceName(resourceName)
                .build();
        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = Mockito.mock(AsyncInstanceCreationManager.Callbacks.class);

        Indexable securityGroupIndexable = Mockito.mock(Indexable.class);
        Observable<Indexable> observableSecurityGroupSuccess = AzureTestUtils.createSimpleObservableSuccess(securityGroupIndexable);
        Mockito.doReturn(observableSecurityGroupSuccess).when(this.azureVirtualNetworkOperationSDK)
                .buildCreateSecurityGroupObservable(Mockito.eq(azureCreateVirtualNetworkRef), Mockito.eq(this.azure));

        Mockito.doThrow(new RuntimeException()).when(this.azureVirtualNetworkOperationSDK)
                .doNetworkCreationStepTwoSync(Mockito.eq(securityGroupIndexable),
                        Mockito.eq(azureCreateVirtualNetworkRef), Mockito.eq(this.azure));

        // exercise
        Observable<Indexable> virtualNetworkCreationObservable = this.azureVirtualNetworkOperationSDK
                .buildVirtualNetworkCreationObservable(azureCreateVirtualNetworkRef, this.azure, finishCreationCallbacks);
        virtualNetworkCreationObservable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Log.FIRST_STEP_CREATE_VNET_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.ERROR, Messages.Log.ERROR_CREATE_VNET_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Log.END_CREATE_VNET_ASYNC_BEHAVIOUR);
        Mockito.verify(finishCreationCallbacks, Mockito.times(TestUtils.RUN_ONCE)).runOnError();
        Mockito.verify(finishCreationCallbacks, Mockito.times(TestUtils.RUN_ONCE)).runOnComplete();
    }

    // test case: When calling the buildCreateSecurityGroupObservable method and the observables execute
    // without any error, it must verify if It returns the right observable.
    @Test
    public void testBuildCreateSecurityGroupObservableSuccessfully() {
        // set up
        String resourceNameExpected = TestUtils.ANY_VALUE;
        String cidrExpected = TestUtils.ANY_VALUE;
        Map tagsExpected = Collections.singletonMap(AzureConstants.TAG_NAME, TestUtils.ANY_VALUE);
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .resourceName(resourceNameExpected)
                .cidr(cidrExpected)
                .tags(tagsExpected)
                .build();
        Region regionExpected = Region.findByLabelOrName(this.regionName);

        Observable<Indexable> observableExpected = Mockito.mock(Observable.class);
        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK.createSecurityGroupAsync(Mockito.eq(this.azure),
                Mockito.eq(resourceNameExpected), Mockito.eq(regionExpected),
                Mockito.eq(this.resourceGroupName), Mockito.eq(cidrExpected), Mockito.eq(tagsExpected)))
                .thenReturn(observableExpected);

        // exercise
        Observable<Indexable> observable = this.azureVirtualNetworkOperationSDK.buildCreateSecurityGroupObservable(azureCreateVirtualNetworkRef, this.azure);

        // verify
        Assert.assertEquals(observableExpected, observable);
    }

    // test case: When calling the buildCreateSecurityGroupObservable method and the observables execute
    // with an error, it must verify if It rethrows the same error.
    @Test
    public void testBuildCreateSecurityGroupObservableFail() {
        // set up
        String resouceNameExpected = TestUtils.ANY_VALUE;
        String cidrExpected = TestUtils.ANY_VALUE;
        Map tagsExpected = Collections.singletonMap(AzureConstants.TAG_NAME, TestUtils.ANY_VALUE);
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .resourceName(resouceNameExpected)
                .cidr(cidrExpected)
                .tags(tagsExpected)
                .build();
        Region regionExpected = Region.findByLabelOrName(this.regionName);

        RuntimeException exceptionExpected = new RuntimeException(TestUtils.ANY_VALUE);
        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK.createSecurityGroupAsync(Mockito.eq(this.azure),
                Mockito.eq(resouceNameExpected), Mockito.eq(regionExpected), Mockito.eq(this.resourceGroupName),
                Mockito.eq(cidrExpected), Mockito.eq(tagsExpected)))
                .thenThrow(exceptionExpected);

        // verify
        this.expectedException.expect(exceptionExpected.getClass());
        this.expectedException.expectMessage(exceptionExpected.getMessage());

        // exercise
        this.azureVirtualNetworkOperationSDK.buildCreateSecurityGroupObservable(azureCreateVirtualNetworkRef, this.azure);
    }

    // test case: When calling the doNetworkCreationStepTwoSync method and the observables execute
    // without any error, it must verify if It execute the right method.
    @Test
    public void testDoNetworkCreationStepTwoSyncSuccessfully() throws Exception {
        // set up
        String resourceNameExpected = TestUtils.ANY_VALUE;
        String cidrExpected = TestUtils.ANY_VALUE;
        Map tagsExpected = Collections.singletonMap(AzureConstants.TAG_NAME, TestUtils.ANY_VALUE);
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .resourceName(resourceNameExpected)
                .cidr(cidrExpected)
                .tags(tagsExpected)
                .build();
        Region regionExpected = Region.findByLabelOrName(this.regionName);

        PowerMockito.mockStatic(AzureNetworkSDK.class);

        NetworkSecurityGroup indexableExpected = Mockito.mock(NetworkSecurityGroup.class);

        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceGroupName).when(AzureGeneralUtil.class, "selectResourceGroupName",
                Mockito.any(Azure.class), Mockito.anyString(), Mockito.anyString());

        // exercise
        this.azureVirtualNetworkOperationSDK.doNetworkCreationStepTwoSync(indexableExpected, azureCreateVirtualNetworkRef, this.azure);

        // verify
        PowerMockito.verifyStatic(AzureNetworkSDK.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AzureNetworkSDK.createNetworkSync(Mockito.eq(this.azure), Mockito.eq(resourceNameExpected),
                Mockito.eq(regionExpected), Mockito.eq(this.resourceGroupName),
                Mockito.eq(cidrExpected), Mockito.eq(indexableExpected), Mockito.eq(tagsExpected));
    }

    // test case: When calling the doNetworkCreationStepTwoSync method and the observables execute
    // with an error, it must verify if It rethrows the same exception.
    @Test
    public void testDoNetworkCreationStepTwoSyncFail() throws Exception {
        // set up
        String resourceNameExpected = TestUtils.ANY_VALUE;
        String cidrExpected = TestUtils.ANY_VALUE;
        Map tagsExpected = Collections.singletonMap(AzureConstants.TAG_NAME, TestUtils.ANY_VALUE);
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .resourceName(resourceNameExpected)
                .cidr(cidrExpected)
                .tags(tagsExpected)
                .build();

        RuntimeException exceptionExpected = new RuntimeException(TestUtils.ANY_VALUE);
        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK.createNetworkSync(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyMap()))
                .thenThrow(exceptionExpected);

        NetworkSecurityGroup indexableExpected = Mockito.mock(NetworkSecurityGroup.class);

        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceGroupName).when(AzureGeneralUtil.class, "selectResourceGroupName",
                Mockito.any(Azure.class), Mockito.anyString(), Mockito.anyString());

        // verify
        this.expectedException.expect(exceptionExpected.getClass());
        this.expectedException.expectMessage(exceptionExpected.getMessage());

        // exercise
        this.azureVirtualNetworkOperationSDK.doNetworkCreationStepTwoSync(
                indexableExpected, azureCreateVirtualNetworkRef, this.azure);
    }

    // test case: When calling the buildDeleteVirtualNetworkCompletable method and the completable executes
    // without any error, it must verify if It returns the right logs.
    @Test
    public void testBuildDeleteVirtualNetworkCompletableSuccessfully() {
        // set up
        String instanceId = "instanceId";
        Completable virtualNetworkCompletableSuccess = AzureTestUtils.createSimpleCompletableSuccess();

        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK
                .buildDeleteVirtualNetworkCompletable(Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenReturn(virtualNetworkCompletableSuccess);

        // exercise
        Completable completable = this.azureVirtualNetworkOperationSDK
                .buildDeleteVirtualNetworkCompletable(this.azure, instanceId);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Log.END_DELETE_VNET_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteVirtualNetworkCompletable method and the completable executes
    // with error, it must verify if It returns the right logs.
    @Test
    public void testBuildDeleteVirtualNetworkCompletableFail() {
        // set up
        String instanceId = "instanceId";
        Completable virtualNetworkCompletableFail = AzureTestUtils.createSimpleCompletableFail();

        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK
                .buildDeleteVirtualNetworkCompletable(Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenReturn(virtualNetworkCompletableFail);

        // exercise
        Completable completable = this.azureVirtualNetworkOperationSDK
                .buildDeleteVirtualNetworkCompletable(this.azure, instanceId);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.ERROR, Messages.Log.ERROR_DELETE_VNET_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteSecurityGroupCompletable method and the completable executes
    // without any error, it must verify if It returns the right logs.
    @Test
    public void testBuildDeleteSecurityGroupCompletableSuccessfully() {
        // set up
        String instanceId = "instanceId";
        Completable networkSecurityGroupCompletableSuccess = AzureTestUtils.createSimpleCompletableSuccess();

        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK
                .buildDeleteNetworkSecurityGroupCompletable(Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenReturn(networkSecurityGroupCompletableSuccess);

        // exercise
        Completable completable = this.azureVirtualNetworkOperationSDK
                .buildDeleteSecurityGroupCompletable(this.azure, instanceId);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Log.END_DELETE_SECURITY_GROUP_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildDeleteSecurityGroupCompletable method and the completable executes
    // with error, it must verify if It returns the right logs.
    @Test
    public void testBuildDeleteSecurityGroupCompletableFail() {
        // set up
        String instanceId = "instanceId";
        Completable networkSecurityGroupCompletableFail = AzureTestUtils.createSimpleCompletableFail();

        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK
                .buildDeleteNetworkSecurityGroupCompletable(Mockito.eq(this.azure), Mockito.eq(instanceId)))
                .thenReturn(networkSecurityGroupCompletableFail);

        // exercise
        Completable completable = this.azureVirtualNetworkOperationSDK
                .buildDeleteSecurityGroupCompletable(this.azure, instanceId);
        completable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.ERROR, Messages.Log.ERROR_DELETE_SECURITY_GROUP_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the doGetInstance method with mocked methods
    // , it must verify if It execute the right AzureGetVirtualNetworkRef.
    @Test
    public void testDoGetInstanceSuccessfully() throws FogbowException {
        // set up
        String resourceName = "resourceName";
        String provisioningState = "provisioningState";
        String id = "id";
        String name = "name";
        String cird = "cird";

        VirtualNetworkInner virtualNetworkInner = Mockito.mock(VirtualNetworkInner.class);
        Mockito.when(virtualNetworkInner.provisioningState()).thenReturn(provisioningState);
        Mockito.when(virtualNetworkInner.id()).thenReturn(id);

        Network network = Mockito.mock(Network.class);
        Map<String, String> tags = new HashMap<>();
        tags.put(AzureConstants.TAG_NAME, name);
        Mockito.when(network.tags()).thenReturn(tags);
        Mockito.when(network.inner()).thenReturn(virtualNetworkInner);

        Mockito.doReturn(network)
                .when(this.azureVirtualNetworkOperationSDK).getNetwork(Mockito.eq(resourceName), Mockito.eq(this.azureUser));

        AzureGetVirtualNetworkRef azureGetVirtualNetworkRefExpected = AzureGetVirtualNetworkRef.builder()
                .state(provisioningState)
                .cidr(cird)
                .name(name)
                .id(id)
                .build();

        Mockito.doReturn(cird)
                .when(this.azureVirtualNetworkOperationSDK).getCIRD(Mockito.eq(network));

        // exercise
        AzureGetVirtualNetworkRef azureGetVirtualNetworkRef = this.azureVirtualNetworkOperationSDK
                .doGetInstance(resourceName, this.azureUser);

        // verify
        Assert.assertEquals(azureGetVirtualNetworkRefExpected, azureGetVirtualNetworkRef);
    }

    // test case: When calling the doGetInstance method with mocked methods and cird being null
    // , it must verify if It execute the right AzureGetVirtualNetworkRef.
    @Test
    public void testDoGetInstanceSuccessfullyWithCIRDNull() throws FogbowException {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String provisioningState = "provisioningState";
        String id = "id";
        String name = AzureTestUtils.ORDER_NAME;

        VirtualNetworkInner virtualNetworkInner = Mockito.mock(VirtualNetworkInner.class);
        Mockito.when(virtualNetworkInner.provisioningState()).thenReturn(provisioningState);
        Mockito.when(virtualNetworkInner.id()).thenReturn(id);

        Network network = Mockito.mock(Network.class);
        Map<String, String> tags = new HashMap<>();
        tags.put(AzureConstants.TAG_NAME, name);
        Mockito.when(network.tags()).thenReturn(tags);
        Mockito.when(network.inner()).thenReturn(virtualNetworkInner);

        Mockito.doReturn(network)
                .when(this.azureVirtualNetworkOperationSDK).getNetwork(Mockito.eq(resourceName), Mockito.eq(this.azureUser));

        AzureGetVirtualNetworkRef azureGetVirtualNetworkRefExpected = AzureGetVirtualNetworkRef.builder()
                .state(provisioningState)
                .cidr(null)
                .name(name)
                .id(id)
                .build();

        Mockito.doReturn(null)
                .when(this.azureVirtualNetworkOperationSDK).getCIRD(Mockito.eq(network));

        // exercise
        AzureGetVirtualNetworkRef azureGetVirtualNetworkRef = this.azureVirtualNetworkOperationSDK
                .doGetInstance(resourceName, this.azureUser);

        // verify
        Assert.assertEquals(azureGetVirtualNetworkRefExpected, azureGetVirtualNetworkRef);
    }

    // test case: When calling the doGetInstance method with mocked methods and throws an exception
    // , it must verify if It rethrows the same exception.
    @Test
    public void testDoGetInstanceFail() throws FogbowException {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String provisioningState = "provisioningState";
        String id = "id";
        String name = AzureTestUtils.ORDER_NAME;

        VirtualNetworkInner virtualNetworkInner = Mockito.mock(VirtualNetworkInner.class);
        Mockito.when(virtualNetworkInner.provisioningState()).thenReturn(provisioningState);
        Mockito.when(virtualNetworkInner.id()).thenReturn(id);

        Network network = Mockito.mock(Network.class);
        Map<String, String> tags = new HashMap<>();
        tags.put(AzureConstants.TAG_NAME, name);
        Mockito.when(network.tags()).thenReturn(tags);
        Mockito.when(network.inner()).thenReturn(virtualNetworkInner);

        FogbowException exceptionExpected = new FogbowException(TestUtils.ANY_VALUE);
        Mockito.doThrow(exceptionExpected).when(this.azureVirtualNetworkOperationSDK)
                .getNetwork(Mockito.eq(resourceName), Mockito.eq(this.azureUser));

        // verify
        this.expectedException.expect(exceptionExpected.getClass());
        this.expectedException.expectMessage(exceptionExpected.getMessage());

        // exercise
        this.azureVirtualNetworkOperationSDK.doGetInstance(resourceName, this.azureUser);
    }

    // test case: When calling the getCIRD method with mocked methods
    // , it must verify if It returns the right cird.
    @Test
    public void testGetCIRDSuccessfully() {
        // set up
        String cirdExpected = "cirdExpected";
        Network network = Mockito.mock(Network.class);
        List<String> addessSpaces = Arrays.asList(cirdExpected);
        Mockito.when(network.addressSpaces()).thenReturn(addessSpaces);

        // exercise
        String cird = this.azureVirtualNetworkOperationSDK.getCIRD(network);

        // verify
        Assert.assertEquals(cirdExpected, cird);
    }

    // test case: When calling the getCIRD method with mocked methods and empty list
    // , it must verify if It returns null.
    @Test
    public void testGetCIRDFail() {
        // set up
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.addressSpaces()).thenReturn(new ArrayList<>());

        // exercise
        String cird = this.azureVirtualNetworkOperationSDK.getCIRD(network);

        // verify
        Assert.assertNull(cird);
    }

    // test case: When calling the getNetwork method with mocked methods
    // , it must verify if It returns the right Network.
    @Test
    public void getNetworkSuccessfully() throws Exception {
        // set up
        AzureTestUtils.mockGetAzureClient(this.azureUser, this.azure);
        String resourceName = AzureTestUtils.RESOURCE_NAME;

        String azureVirtualNetworkIdExpected = AzureResourceIdBuilder.networkId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(this.resourceGroupName)
                .withResourceName(resourceName)
                .build();

        Network networkExpected = Mockito.mock(Network.class);
        Optional<Network> optionalExpected = Optional.of(networkExpected);
        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK.getNetwork(Mockito.eq(this.azure), Mockito.eq(azureVirtualNetworkIdExpected)))
                .thenReturn(optionalExpected);

        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceGroupName).when(AzureGeneralUtil.class, "selectResourceGroupName",
                Mockito.any(Azure.class), Mockito.anyString(), Mockito.anyString());

        // exercise
        Network network = this.azureVirtualNetworkOperationSDK.getNetwork(resourceName, this.azureUser);

        // verify
        Assert.assertEquals(networkExpected, network);
    }

    // test case: When calling the getNetwork method with mocked methods and network is not found
    // , it must verify if It returns an exception.
    @Test
    public void testGetNetworkFail() throws Exception {
        // set up
        AzureTestUtils.mockGetAzureClient(this.azureUser, this.azure);
        String resourceName = AzureTestUtils.RESOURCE_NAME;

        String azureVirtualNetworkIdExpected = AzureResourceIdBuilder.networkId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(this.resourceGroupName)
                .withResourceName(resourceName)
                .build();

        PowerMockito.mockStatic(AzureNetworkSDK.class);
        Optional<Network> optionalNetwork = Optional.empty();
        PowerMockito.when(AzureNetworkSDK.getNetwork(Mockito.eq(this.azure), Mockito.eq(azureVirtualNetworkIdExpected)))
                .thenReturn(optionalNetwork);

        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceGroupName).when(AzureGeneralUtil.class, "selectResourceGroupName",
                Mockito.any(Azure.class), Mockito.anyString(), Mockito.anyString());

        // verify
        this.expectedException.expect(InstanceNotFoundException.class);

        // exercise
        this.azureVirtualNetworkOperationSDK.getNetwork(resourceName, this.azureUser);
    }

    // test case: When calling the doDeleteInstance method with mocked methods
    // , it must verify if It shows the right log.
    @Test
    public void testDoDeleteInstanceSuccessfully() throws Exception {
        // set up
        AzureTestUtils.mockGetAzureClient(this.azureUser, this.azure);
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String azureVirtualNetworkIdExpected = AzureResourceIdBuilder.networkId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(this.resourceGroupName)
                .withResourceName(resourceName)
                .build();
        String azureSecurityGroupIdExpected = AzureResourceIdBuilder.networkSecurityGroupId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(this.resourceGroupName)
                .withResourceName(resourceName)
                .build();

        String msgDeleteVirtualNetworkOk = "msgDeleteVirtualNetworkOk";
        String msgDeleteNetworkSecurityGroupOk = "msgDeleteNetworkSecurityGroupOk";
        Completable deleteVirtualNetworkCompletable = AzureTestUtils
                .createSimpleCompletableSuccess(LOGGER_CLASS_MOCK, msgDeleteVirtualNetworkOk);
        Completable completableTwo = AzureTestUtils
                .createSimpleCompletableSuccess(LOGGER_CLASS_MOCK, msgDeleteNetworkSecurityGroupOk);
        Mockito.doReturn(deleteVirtualNetworkCompletable).when(this.azureVirtualNetworkOperationSDK)
                .buildDeleteVirtualNetworkCompletable(Mockito.eq(this.azure), Mockito.eq(azureVirtualNetworkIdExpected));
        Mockito.doReturn(completableTwo).when(this.azureVirtualNetworkOperationSDK)
                .buildDeleteSecurityGroupCompletable(Mockito.eq(this.azure), Mockito.eq(azureSecurityGroupIdExpected));

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(false).when(AzureResourceGroupOperationUtil.class, "existsResourceGroup",
                Mockito.any(Azure.class), Mockito.anyString());

        // exercise
        this.azureVirtualNetworkOperationSDK.doDeleteInstance(resourceName, this.azureUser);

        // verify
        this.loggerAssert.assertEqualsInOrder(Level.DEBUG, msgDeleteVirtualNetworkOk)
                .assertEqualsInOrder(Level.DEBUG, msgDeleteNetworkSecurityGroupOk);
    }

    // test case: When calling the doDeleteInstance method with mocked methods and throws an exception
    // , it must verify if It rethrows the same exception.
    @Test
    public void testDoDeleteInstanceFail() throws FogbowException {
        // set up
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        UnauthenticatedUserException exceptionExpected = new UnauthenticatedUserException(TestUtils.ANY_VALUE);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(azureUser)))
                .thenThrow(exceptionExpected);

        // verify
        this.expectedException.expect(exceptionExpected.getClass());
        this.expectedException.expectMessage(exceptionExpected.getMessage());

        // exercise
        this.azureVirtualNetworkOperationSDK.doDeleteInstance(TestUtils.ANY_VALUE, this.azureUser);
    }

    private void makeTheObservablesSynchronous() {
        // The scheduler trampolime makes the subscriptions execute in the current thread
        this.azureVirtualNetworkOperationSDK.setScheduler(Schedulers.trampoline());
    }

    // test case: When calling the doDeleteInstance method whose existing
    // resource group has the same name as the resource, and the completable
    // executes without any error, it must verify than returns the right logs.
    @Test
    public void testDoDeleteInstanceThanExistsResourceGroupWithSameResourceNameSuccessfully() throws Exception {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;

        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(this.azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(true).when(AzureResourceGroupOperationUtil.class, "existsResourceGroup",
                Mockito.any(Azure.class), Mockito.eq(resourceName));

        Completable completable = AzureTestUtils.createSimpleCompletableSuccess();
        PowerMockito.doReturn(completable).when(AzureResourceGroupOperationUtil.class, "deleteResourceGroupAsync",
                Mockito.eq(this.azure), Mockito.eq(resourceName));

        // exercise
        this.azureVirtualNetworkOperationSDK.doDeleteInstance(resourceName, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.existsResourceGroup(Mockito.eq(this.azure), Mockito.eq(resourceName));

        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.deleteResourceGroupAsync(Mockito.eq(this.azure), Mockito.eq(resourceName));

        this.loggerAssert.assertEqualsInOrder(Level.INFO, Messages.Log.END_DELETE_VNET_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the doDeleteInstance method whose existing
    // resource group has the same name as the resource, and the completable
    // executes with an error, it must verify if It returns the right logs.
    @Test
    public void testDoDeleteInstanceThanExistsResourceGroupWithSameResourceNameFail() throws Exception {
        // set up
        String resourceName = AzureTestUtils.RESOURCE_NAME;

        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(this.azure).when(AzureClientCacheManager.class, "getAzure", Mockito.eq(this.azureUser));

        PowerMockito.mockStatic(AzureResourceGroupOperationUtil.class);
        PowerMockito.doReturn(true).when(AzureResourceGroupOperationUtil.class, "existsResourceGroup",
                Mockito.any(Azure.class), Mockito.eq(resourceName));

        Completable completable = AzureTestUtils.createSimpleCompletableFail();
        PowerMockito.doReturn(completable).when(AzureResourceGroupOperationUtil.class, "deleteResourceGroupAsync",
                Mockito.eq(this.azure), Mockito.eq(resourceName));

        // exercise
        this.azureVirtualNetworkOperationSDK.doDeleteInstance(resourceName, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.existsResourceGroup(Mockito.eq(this.azure), Mockito.eq(resourceName));

        PowerMockito.verifyStatic(AzureResourceGroupOperationUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureResourceGroupOperationUtil.deleteResourceGroupAsync(Mockito.eq(this.azure), Mockito.eq(resourceName));

        this.loggerAssert.assertEqualsInOrder(Level.ERROR, Messages.Log.ERROR_DELETE_VNET_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the doCreateInstance method, it must verify that is
    // call was successful.
    @Test
    public void testDoCreateInstanceSuccessfully() throws Exception {
        // set up
        AzureCreateVirtualNetworkRef virtualNetworkRef = Mockito.mock(AzureCreateVirtualNetworkRef.class);
        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = Mockito.mock(AsyncInstanceCreationManager.Callbacks.class);

        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.doReturn(this.azure).when(AzureClientCacheManager.class, "getAzure",
                Mockito.eq(this.azureUser));

        Observable observable = AzureTestUtils.createSimpleObservableSuccess();
        Mockito.doReturn(observable).when(this.azureVirtualNetworkOperationSDK)
                .buildVirtualNetworkCreationObservable(Mockito.eq(virtualNetworkRef),
                Mockito.any(Azure.class), Mockito.eq(finishCreationCallbacks));

        // exercise
        this.azureVirtualNetworkOperationSDK.doCreateInstance(virtualNetworkRef , this.azureUser, finishCreationCallbacks);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        Mockito.verify(this.azureVirtualNetworkOperationSDK, Mockito.times(TestUtils.RUN_ONCE))
                .buildVirtualNetworkCreationObservable(Mockito.eq(virtualNetworkRef),
                Mockito.any(Azure.class), Mockito.eq(finishCreationCallbacks));

        Mockito.verify(this.azureVirtualNetworkOperationSDK, Mockito.times(TestUtils.RUN_ONCE))
                .subscribeVirtualNetworkCreation(Mockito.eq(observable));
    }

}
