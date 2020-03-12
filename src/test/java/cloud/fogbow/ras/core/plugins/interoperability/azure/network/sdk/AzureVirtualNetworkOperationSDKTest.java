package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import ch.qos.logback.classic.Level;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
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
import rx.Completable;
import rx.Observable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AzureNetworkSDK.class})
public class AzureVirtualNetworkOperationSDKTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private LoggerAssert loggerAssert = new LoggerAssert(AzureVirtualNetworkOperationSDK.class);

    private AzureVirtualNetworkOperationSDK azureVirtualNetworkOperationSDK;
    private String regionName = AzureTestUtils.DEFAULT_REGION_NAME;
    private String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
    private Azure azure;

    @Before
    public void setUp() {
        this.azureVirtualNetworkOperationSDK = Mockito.spy(new AzureVirtualNetworkOperationSDK(this.regionName, this.resourceGroupName));
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
        PowerMockito.verifyStatic(AzureNetworkSDK.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
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


    // TODO(chico) - Remove this commentary after merge. ---------------------- Network ----------------------

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
                .assertEqualsInOrder(Level.INFO, Messages.Info.END_DELETE_VNET_ASYNC_BEHAVIOUR);
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
                .assertEqualsInOrder(Level.ERROR, Messages.Error.ERROR_DELETE_VNET_ASYNC_BEHAVIOUR);
    }

}
