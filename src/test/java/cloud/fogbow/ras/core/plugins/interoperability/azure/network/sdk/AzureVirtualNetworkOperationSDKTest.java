package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import ch.qos.logback.classic.Level;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.AzureVirtualMachineSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureGetVirtualNetworkRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureClientCacheManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.implementation.VirtualNetworkInner;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
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
import rx.Observable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AzureNetworkSDK.class, AzureNetworkSDK.class, AzureClientCacheManager.class})
public class AzureVirtualNetworkOperationSDKTest {

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
    }

    // test case: When calling the buildVirtualNetworkCreationObservable method and the observables execute
    // without any error, it must verify if It returns the right logs.
    @Test
    public void testBuildVirtualNetworkCreationObservableSuccessfully() {
        // set up
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = Mockito.mock(AzureCreateVirtualNetworkRef.class);

        Indexable securityGroupIndexable = Mockito.mock(Indexable.class);
        Observable<Indexable> observableSecurityGroupSuccess = AzureTestUtils.createSimpleObservableSuccess(securityGroupIndexable);
        Mockito.doReturn(observableSecurityGroupSuccess).when(this.azureVirtualNetworkOperationSDK)
                .buildCreateSecurityGroupObservable(Mockito.eq(azureCreateVirtualNetworkRef), Mockito.eq(this.azure));

        Mockito.doNothing().when(this.azureVirtualNetworkOperationSDK)
                .doNetworkCreationStepTwoSync(Mockito.eq(securityGroupIndexable),
                        Mockito.eq(azureCreateVirtualNetworkRef), Mockito.eq(this.azure));

        // exercise
        Observable<Indexable> virtualNetworkCreationObservable = this.azureVirtualNetworkOperationSDK
                .buildVirtualNetworkCreationObservable(azureCreateVirtualNetworkRef, this.azure);
        virtualNetworkCreationObservable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Info.FIRST_STEP_CREATE_VNET_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Info.SECOND_STEP_CREATE_VNET_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_CREATE_VNET_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildVirtualNetworkCreationObservable method and the observables execute
    // with an error, it must verify if It returns the right logs with an error log.
    @Test
    public void testBuildVirtualNetworkCreationObservableFail() {
        // set up
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = Mockito.mock(AzureCreateVirtualNetworkRef.class);

        Indexable securityGroupIndexable = Mockito.mock(Indexable.class);
        Observable<Indexable> observableSecurityGroupSuccess = AzureTestUtils.createSimpleObservableSuccess(securityGroupIndexable);
        Mockito.doReturn(observableSecurityGroupSuccess).when(this.azureVirtualNetworkOperationSDK)
                .buildCreateSecurityGroupObservable(Mockito.eq(azureCreateVirtualNetworkRef), Mockito.eq(this.azure));

        Mockito.doThrow(new RuntimeException()).when(this.azureVirtualNetworkOperationSDK)
                .doNetworkCreationStepTwoSync(Mockito.eq(securityGroupIndexable),
                        Mockito.eq(azureCreateVirtualNetworkRef), Mockito.eq(this.azure));

        // exercise
        Observable<Indexable> virtualNetworkCreationObservable = this.azureVirtualNetworkOperationSDK
                .buildVirtualNetworkCreationObservable(azureCreateVirtualNetworkRef, this.azure);
        virtualNetworkCreationObservable.subscribe();

        // verify
        this.loggerAssert
                .assertEqualsInOrder(Level.INFO, Messages.Info.FIRST_STEP_CREATE_VNET_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_CREATE_VNET_ASYNC_BEHAVIOUR)
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_CREATE_VNET_ASYNC_BEHAVIOUR);
    }

    // test case: When calling the buildVirtualNetworkCreationObservable method and the observables execute
    // without any error, it must verify if It returns the right observable.
    @Test
    public void testBuildCreateSecurityGroupObservableSuccessfully() {
        // set up
        String nameExpected = TestUtils.ANY_VALUE;
        String cidrExpected = TestUtils.ANY_VALUE;
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .name(nameExpected)
                .cidr(cidrExpected)
                .build();
        Region regionExpected = Region.fromName(this.regionName);

        Observable<Indexable> observableExpected = Mockito.mock(Observable.class);
        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK.createSecurityGroupAsync(Mockito.eq(this.azure), Mockito.eq(nameExpected)
                , Mockito.eq(regionExpected), Mockito.eq(this.resourceGroupName), Mockito.eq(cidrExpected)))
                .thenReturn(observableExpected);

        // exercise
        Observable<Indexable> observable = this.azureVirtualNetworkOperationSDK.buildCreateSecurityGroupObservable(azureCreateVirtualNetworkRef, this.azure);

        // verify
        Assert.assertEquals(observableExpected, observable);
    }

    // test case: When calling the buildVirtualNetworkCreationObservable method and the observables execute
    // with an error, it must verify if It rethrows the same error.
    @Test
    public void testBuildCreateSecurityGroupObservableFail() {
        // set up
        String nameExpected = TestUtils.ANY_VALUE;
        String cidrExpected = TestUtils.ANY_VALUE;
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .name(nameExpected)
                .cidr(cidrExpected)
                .build();
        Region regionExpected = Region.fromName(this.regionName);

        RuntimeException exceptionExpected = new RuntimeException(TestUtils.ANY_VALUE);
        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK.createSecurityGroupAsync(Mockito.eq(this.azure), Mockito.eq(nameExpected)
                , Mockito.eq(regionExpected), Mockito.eq(this.resourceGroupName), Mockito.eq(cidrExpected)))
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
    public void testDoNetworkCreationStepTwoSyncSuccessfully() {
        // set up
        String nameExpected = TestUtils.ANY_VALUE;
        String cidrExpected = TestUtils.ANY_VALUE;
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .name(nameExpected)
                .cidr(cidrExpected)
                .build();
        Region regionExpected = Region.fromName(this.regionName);

        PowerMockito.mockStatic(AzureNetworkSDK.class);

        NetworkSecurityGroup indexableExpected = Mockito.mock(NetworkSecurityGroup.class);

        // exercise
        this.azureVirtualNetworkOperationSDK.doNetworkCreationStepTwoSync(indexableExpected, azureCreateVirtualNetworkRef, this.azure);

        // verify
        PowerMockito.verifyStatic(AzureVirtualMachineSDK.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        AzureNetworkSDK.createNetworkSync(Mockito.eq(this.azure), Mockito.eq(nameExpected), Mockito.eq(regionExpected)
                , Mockito.eq(this.resourceGroupName), Mockito.eq(cidrExpected), Mockito.eq(indexableExpected));
    }

    // test case: When calling the doNetworkCreationStepTwoSync method and the observables execute
    // with an error, it must verify if It rethrows the same exception.
    @Test
    public void testDoNetworkCreationStepTwoSyncFail() {
        // set up
        String nameExpected = TestUtils.ANY_VALUE;
        String cidrExpected = TestUtils.ANY_VALUE;
        AzureCreateVirtualNetworkRef azureCreateVirtualNetworkRef = AzureCreateVirtualNetworkRef.builder()
                .name(nameExpected)
                .cidr(cidrExpected)
                .build();

        RuntimeException exceptionExpected = new RuntimeException(TestUtils.ANY_VALUE);
        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK.createNetworkSync(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(exceptionExpected);

        NetworkSecurityGroup indexableExpected = Mockito.mock(NetworkSecurityGroup.class);

        // verify
        this.expectedException.expect(exceptionExpected.getClass());
        this.expectedException.expectMessage(exceptionExpected.getMessage());

        // exercise
        this.azureVirtualNetworkOperationSDK.doNetworkCreationStepTwoSync(indexableExpected, azureCreateVirtualNetworkRef, this.azure);
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
        Mockito.when(network.name()).thenReturn(name);
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
        AzureGetVirtualNetworkRef azureGetVirtualNetworkRef = this.azureVirtualNetworkOperationSDK.doGetInstance(resourceName, this.azureUser);

        // verify
        Assert.assertEquals(azureGetVirtualNetworkRefExpected, azureGetVirtualNetworkRef);
    }

    // test case: When calling the doGetInstance method with mocked methods and cird beeing null
    // , it must verify if It execute the right AzureGetVirtualNetworkRef.
    @Test
    public void testDoGetInstanceSuccessfullyWithCIRDNull() throws FogbowException {
        // set up
        String resourceName = "resourceName";
        String provisioningState = "provisioningState";
        String id = "id";
        String name = "name";

        VirtualNetworkInner virtualNetworkInner = Mockito.mock(VirtualNetworkInner.class);
        Mockito.when(virtualNetworkInner.provisioningState()).thenReturn(provisioningState);
        Mockito.when(virtualNetworkInner.id()).thenReturn(id);

        Network network = Mockito.mock(Network.class);
        Mockito.when(network.name()).thenReturn(name);
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
        AzureGetVirtualNetworkRef azureGetVirtualNetworkRef = this.azureVirtualNetworkOperationSDK.doGetInstance(resourceName, this.azureUser);

        // verify
        Assert.assertEquals(azureGetVirtualNetworkRefExpected, azureGetVirtualNetworkRef);
    }

    // test case: When calling the doGetInstance method with mocked methods and throws an exception
    // , it must verify if It rethrows the same exception.
    @Test
    public void testDoGetInstanceFail() throws FogbowException {
        // set up
        String resourceName = "resourceName";
        String provisioningState = "provisioningState";
        String id = "id";
        String name = "name";

        VirtualNetworkInner virtualNetworkInner = Mockito.mock(VirtualNetworkInner.class);
        Mockito.when(virtualNetworkInner.provisioningState()).thenReturn(provisioningState);
        Mockito.when(virtualNetworkInner.id()).thenReturn(id);

        Network network = Mockito.mock(Network.class);
        Mockito.when(network.name()).thenReturn(name);
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
    public void getNetworkSuccessfully() throws FogbowException {
        // set up
        AzureTestUtils.mockGetAzureClient(this.azureUser, this.azure);
        String resourceName = "resourceName";

        String azureVirtualNetworkIdExpected = AzureResourceIdBuilder.virtualNetworkId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(this.resourceGroupName)
                .withResourceName(resourceName)
                .build();

        Network networkExpected = Mockito.mock(Network.class);
        Optional<Network> optionalExpected = Optional.of(networkExpected);
        PowerMockito.mockStatic(AzureNetworkSDK.class);
        PowerMockito.when(AzureNetworkSDK.getNetwork(Mockito.eq(this.azure), Mockito.eq(azureVirtualNetworkIdExpected)))
                .thenReturn(optionalExpected);

        // exercise
        Network network = this.azureVirtualNetworkOperationSDK.getNetwork(resourceName, this.azureUser);

        // verify
        Assert.assertEquals(networkExpected, network);
    }

    // test case: When calling the getNetwork method with mocked methods and network is not found
    // , it must verify if It returns an exception.
    @Test
    public void testGetNetworkFail() throws FogbowException {
        // set up
        AzureTestUtils.mockGetAzureClient(this.azureUser, this.azure);
        String resourceName = "resourceName";

        String azureVirtualNetworkIdExpected = AzureResourceIdBuilder.virtualNetworkId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(this.resourceGroupName)
                .withResourceName(resourceName)
                .build();

        PowerMockito.mockStatic(AzureNetworkSDK.class);
        Optional<Network> optionalNetwork = Optional.empty();
        PowerMockito.when(AzureNetworkSDK.getNetwork(Mockito.eq(this.azure), Mockito.eq(azureVirtualNetworkIdExpected)))
                .thenReturn(optionalNetwork);

        // verify
        this.expectedException.expect(InstanceNotFoundException.class);

        // exercise
        this.azureVirtualNetworkOperationSDK.getNetwork(resourceName, this.azureUser);
    }

}
