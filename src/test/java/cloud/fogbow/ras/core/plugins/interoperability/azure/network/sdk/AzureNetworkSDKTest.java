package cloud.fogbow.ras.core.plugins.interoperability.azure.network.sdk;

import cloud.fogbow.common.exceptions.UnexpectedException;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterfaces;
import org.junit.Assert;
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
@PrepareForTest({AzureNetworkSDK.class})
public class AzureNetworkSDKTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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

}
