package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import rx.Completable;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterfaces;
import com.microsoft.azure.management.network.Networks;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Azure.class, AzureNetworkSDK.class })
public class AzureNetworkSDKTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        PowerMockito.mockStatic(AzureNetworkSDK.class);
    }

    // test case: When calling the getNetworkInterface method and finds a network interface,
    // it must verify if It returns a Optional with a network interface.
    @Test
    public void testGetNetworkInterfaceSuccessfullyWhenFindNetworkInterface() throws Exception {
        // set up
        Azure azure = null;
        String networkInterfaceId = "networkInterfaceId";
        NetworkInterfaces networkInterfacesObject = Mockito.mock(NetworkInterfaces.class);
        NetworkInterface networkInterfaceExpected = Mockito.mock(NetworkInterface.class);
        Mockito.when(networkInterfacesObject.getById(Mockito.eq(networkInterfaceId)))
                .thenReturn(networkInterfaceExpected);
        PowerMockito.spy(AzureNetworkSDK.class);
        PowerMockito.doReturn(networkInterfacesObject)
                .when(AzureNetworkSDK.class, "getNetworkInterfacesSDK", Mockito.eq(azure));

        // exercise
        Optional<NetworkInterface> networkInterface =
                AzureNetworkSDK.getNetworkInterface(azure, networkInterfaceId);

        // verify
        Assert.assertTrue(networkInterface.isPresent());
        Assert.assertEquals(networkInterfaceExpected, networkInterface.get());
    }

    // test case: When calling the getNetworkInterface method and do not find a network interface,
    // it must verify if It returns a Optional without a network interface.
    @Test
    public void testGetNetworkInterfaceSuccessfullyWhenNotFindNetworkInterface() throws Exception {
        // set up
        Azure azure = null;
        String networkInterfaceId = "networkInterfaceId";
        NetworkInterfaces networkInterfacesObject = Mockito.mock(NetworkInterfaces.class);
        NetworkInterface networkInterfaceExpected = null;
        Mockito.when(networkInterfacesObject.getById(Mockito.eq(networkInterfaceId)))
                .thenReturn(networkInterfaceExpected);
        PowerMockito.spy(AzureNetworkSDK.class);
        PowerMockito.doReturn(networkInterfacesObject)
                .when(AzureNetworkSDK.class, "getNetworkInterfacesSDK", Mockito.eq(azure));

        // exercise
        Optional<NetworkInterface> networkInterface = AzureNetworkSDK.getNetworkInterface(azure, networkInterfaceId);

        // verify
        Assert.assertFalse(networkInterface.isPresent());
    }

    // test case: When calling the getNetworkInterface method and throws any exception,
    // it must verify if It throws an UnexpectedException.
    @Test
    public void testGetNetworkInterfacesFail() throws Exception {
        // set up
        Azure azure = null;
        String networkInterfaceId = "networkInterfaceId";
        PowerMockito.spy(AzureNetworkSDK.class);
        String errorMessage = "error";
        PowerMockito.doThrow(new RuntimeException(errorMessage))
                .when(AzureNetworkSDK.class, "getNetworkInterfacesSDK", Mockito.eq(azure));

        // verify
        this.expectedException.expect(UnexpectedException.class);
        this.expectedException.expectMessage(errorMessage);

        // exercise
        AzureNetworkSDK.getNetworkInterface(azure, networkInterfaceId);
    }

    // test case: When calling the buildDeleteNetworkInterfaceCompletable method
    // with a valid resource ID, it must verify that is call was successful.
    @Test
    public void testBuildDeleteNetworkInterfaceCompletableSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.networkInterfaceId().build();
        PowerMockito.doCallRealMethod().when(AzureNetworkSDK.class, "buildDeleteNetworkInterfaceCompletable",
                Mockito.eq(azure), Mockito.eq(resourceId));

        NetworkInterfaces networkInterfaces = Mockito.mock(NetworkInterfaces.class);
        PowerMockito.doReturn(networkInterfaces).when(AzureNetworkSDK.class, "getNetworkInterfacesSDK", Mockito.eq(azure));

        Completable completable = Mockito.mock(Completable.class);
        Mockito.when(networkInterfaces.deleteByIdAsync(Mockito.eq(resourceId))).thenReturn(completable);

        // exercise
        AzureNetworkSDK.buildDeleteNetworkInterfaceCompletable(azure, resourceId);

        // verify
        PowerMockito.verifyStatic(AzureNetworkSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSDK.getNetworkInterfacesSDK(Mockito.eq(azure));

        Mockito.verify(networkInterfaces, Mockito.times(TestUtils.RUN_ONCE)).deleteByIdAsync(Mockito.eq(resourceId));
    }

    // test case: When calling the buildDeleteNetworkInterfaceCompletable method and
    // an unexpected error occurs, it must verify than an UnexpectedException has
    // been thrown.
    @Test
    public void testBuildDeleteDiskCompletableFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.networkInterfaceId().build();
        PowerMockito.doCallRealMethod().when(AzureNetworkSDK.class, "buildDeleteNetworkInterfaceCompletable",
                Mockito.eq(azure), Mockito.eq(resourceId));

        NetworkInterfaces networkInterfaces = Mockito.mock(NetworkInterfaces.class);
        PowerMockito.doReturn(networkInterfaces).when(AzureNetworkSDK.class, "getNetworkInterfacesSDK",
                Mockito.eq(azure));
        Mockito.when(networkInterfaces.deleteByIdAsync(resourceId)).thenThrow(new RuntimeException());

        // verify
        this.expectedException.expect(UnexpectedException.class);

        // exercise
        AzureNetworkSDK.buildDeleteNetworkInterfaceCompletable(azure, resourceId);
    }

    // test case: When calling the getNetwork method with a valid resource ID, it
    // must verify that is call was successful.
    @Test
    public void testGetNetworkSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.networkId().build();
        PowerMockito.doCallRealMethod().when(AzureNetworkSDK.class, "getNetwork", Mockito.eq(azure),
                Mockito.eq(resourceId));

        Networks networks = Mockito.mock(Networks.class);
        PowerMockito.doReturn(networks).when(AzureNetworkSDK.class, "getNetworksSDK", Mockito.eq(azure));

        Network network = Mockito.mock(Network.class);
        Mockito.when(networks.getById(Mockito.eq(resourceId))).thenReturn(network);

        // exercise
        AzureNetworkSDK.getNetwork(azure, resourceId);

        // verify
        PowerMockito.verifyStatic(AzureNetworkSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSDK.getNetworksSDK(Mockito.eq(azure));

        Mockito.verify(networks, Mockito.times(TestUtils.RUN_ONCE)).getById(Mockito.eq(resourceId));
    }

    // test case: When calling the getNetwork method and an unexpected error occurs,
    // it must verify than an UnexpectedException has been thrown.
    @Test
    public void testGetNetworkFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.networkId().build();
        PowerMockito.doCallRealMethod().when(AzureNetworkSDK.class, "getNetwork", Mockito.eq(azure),
                Mockito.eq(resourceId));

        Networks networks = Mockito.mock(Networks.class);
        PowerMockito.doReturn(networks).when(AzureNetworkSDK.class, "getNetworksSDK", Mockito.eq(azure));
        Mockito.when(networks.getById(Mockito.eq(resourceId))).thenThrow(new RuntimeException());

        // verify
        this.expectedException.expect(UnexpectedException.class);

        // exercise
        AzureNetworkSDK.getNetwork(azure, resourceId);
    }

}
