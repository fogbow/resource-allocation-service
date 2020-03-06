package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import ch.qos.logback.classic.Level;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk.model.AzureCreateVirtualNetworkRef;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import rx.Observable;

public class AzureVirtualNetworkOperationSDKTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private LoggerAssert loggerAssert = new LoggerAssert(AzureVirtualNetworkOperationSDK.class);

    private AzureVirtualNetworkOperationSDK azureVirtualNetworkOperationSDK;
    private Azure azure;

    @Before
    public void setUp() {
        this.azureVirtualNetworkOperationSDK =
                Mockito.spy(new AzureVirtualNetworkOperationSDK(AzureTestUtils.DEFAULT_REGION_NAME));
        this.azure = null;
    }

    // test case: When calling the testBuildVirtualNetworkCreationObservable method and the observables execute
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


}
