package cloud.fogbow.ras.core.plugins.interoperability.azure.publicip.sdk;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.network.NetworkInterface;
import com.microsoft.azure.management.network.NetworkInterface.Update;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityGroups;
import com.microsoft.azure.management.network.NetworkSecurityRule;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.network.PublicIPAddresses;
import com.microsoft.azure.management.network.SecurityRuleProtocol;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;
import rx.Completable;
import rx.Observable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Azure.class, AzurePublicIPAddressSDK.class })
public class AzurePublicIPAddressSDKTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        PowerMockito.mockStatic(AzurePublicIPAddressSDK.class);
    }
    
    // test case: When calling the associatePublicIPAddressAsync method, it must
    // verify that is call was successful.
    @Test
    public void testAssociatePublicIPAddressAsyncSuccessfully() throws Exception {
        // set up
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        Creatable<PublicIPAddress> creatable = Mockito.mock(Creatable.class);
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "associatePublicIPAddressAsync",
                Mockito.eq(networkInterface), Mockito.eq(creatable));

        Update update = Mockito.mock(Update.class);
        Mockito.when(networkInterface.update()).thenReturn(update);

        Observable<NetworkInterface> observable = Mockito.mock(Observable.class);
        Mockito.when(update.withNewPrimaryPublicIPAddress(Mockito.eq(creatable))).thenReturn(update);
        Mockito.when(update.applyAsync()).thenReturn(observable);

        // exercise
        AzurePublicIPAddressSDK.associatePublicIPAddressAsync(networkInterface, creatable);

        // verify
        Mockito.verify(networkInterface, Mockito.times(TestUtils.RUN_ONCE)).update();
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).withNewPrimaryPublicIPAddress(Mockito.eq(creatable));
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).applyAsync();
    }

    // test case: When calling the buildNetworkSecurityGroupCreatable method, it
    // must verify that is call was successful.
    @Test
    public void testBuildNetworkSecurityGroupCreatableSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PublicIPAddress publicIPAddress = mockPublicIPAddress();
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "buildNetworkSecurityGroupCreatable",
                Mockito.eq(azure), Mockito.eq(publicIPAddress));

        String resourceName = AzureTestUtils.RESOURCE_NAME;
        NetworkSecurityGroups networkSecurityGroups = Mockito.mock(NetworkSecurityGroups.class);
        Mockito.when(azure.networkSecurityGroups()).thenReturn(networkSecurityGroups);

        NetworkSecurityGroup.DefinitionStages.Blank defineSecurityGroup = Mockito
                .mock(NetworkSecurityGroup.DefinitionStages.Blank.class);
        Mockito.when(networkSecurityGroups.define(Mockito.eq(resourceName))).thenReturn(defineSecurityGroup);

        String regionName = "eastus";
        NetworkSecurityGroup.DefinitionStages.WithGroup withGroup = Mockito
                .mock(NetworkSecurityGroup.DefinitionStages.WithGroup.class);
        Mockito.when(defineSecurityGroup.withRegion(Mockito.eq(regionName))).thenReturn(withGroup);

        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        NetworkSecurityGroup.DefinitionStages.WithCreate withCreate = Mockito
                .mock(NetworkSecurityGroup.DefinitionStages.WithCreate.class);
        Mockito.when(withGroup.withExistingResourceGroup(Mockito.eq(resourceGroupName))).thenReturn(withCreate);

        String securityRuleName = resourceName + AzurePublicIPAddressSDK.SECURITY_RULE_NAME_SUFIX;
        NetworkSecurityRule.DefinitionStages.Blank defineSecurityRule = Mockito
                .mock(NetworkSecurityRule.DefinitionStages.Blank.class);
        Mockito.when(withCreate.defineRule(Mockito.eq(securityRuleName))).thenReturn(defineSecurityRule);

        NetworkSecurityRule.DefinitionStages.WithSourceAddress withSourceAddress = Mockito
                .mock(NetworkSecurityRule.DefinitionStages.WithSourceAddress.class);
        Mockito.when(defineSecurityRule.allowInbound()).thenReturn(withSourceAddress);

        NetworkSecurityRule.DefinitionStages.WithSourcePort withSourcePort = Mockito
                .mock(NetworkSecurityRule.DefinitionStages.WithSourcePort.class);
        Mockito.when(withSourceAddress.fromAnyAddress()).thenReturn(withSourcePort);

        NetworkSecurityRule.DefinitionStages.WithDestinationAddress withDestinationAddress = Mockito
                .mock(NetworkSecurityRule.DefinitionStages.WithDestinationAddress.class);
        Mockito.when(withSourcePort.fromAnyPort()).thenReturn(withDestinationAddress);

        String ipAddress = "0.0.0.0";
        NetworkSecurityRule.DefinitionStages.WithDestinationPort withDestinationPort = Mockito
                .mock(NetworkSecurityRule.DefinitionStages.WithDestinationPort.class);
        Mockito.when(withDestinationAddress.toAddress(Mockito.eq(ipAddress))).thenReturn(withDestinationPort);

        int port = AzurePublicIPAddressSDK.SSH_ACCESS_PORT;
        NetworkSecurityRule.DefinitionStages.WithProtocol withProtocol = Mockito
                .mock(NetworkSecurityRule.DefinitionStages.WithProtocol.class);
        Mockito.when(withDestinationPort.toPort(port)).thenReturn(withProtocol);

        SecurityRuleProtocol protocol = SecurityRuleProtocol.ASTERISK;
        NetworkSecurityRule.DefinitionStages.WithAttach withAttach = Mockito
                .mock(NetworkSecurityRule.DefinitionStages.WithAttach.class);
        Mockito.when(withProtocol.withProtocol(protocol)).thenReturn(withAttach);
        Mockito.when(withAttach.attach()).thenReturn(withCreate);

        // exercise
        AzurePublicIPAddressSDK.buildNetworkSecurityGroupCreatable(azure, publicIPAddress);

        // verify
        Mockito.verify(publicIPAddress, Mockito.times(TestUtils.RUN_ONCE)).regionName();
        Mockito.verify(publicIPAddress, Mockito.times(TestUtils.RUN_ONCE)).resourceGroupName();
        Mockito.verify(publicIPAddress, Mockito.times(TestUtils.RUN_ONCE)).name();
        Mockito.verify(publicIPAddress, Mockito.times(TestUtils.RUN_ONCE)).ipAddress();

        Mockito.verify(azure, Mockito.times(TestUtils.RUN_ONCE)).networkSecurityGroups();
        Mockito.verify(networkSecurityGroups, Mockito.times(TestUtils.RUN_ONCE)).define(Mockito.eq(resourceName));
        Mockito.verify(defineSecurityGroup, Mockito.times(TestUtils.RUN_ONCE)).withRegion(Mockito.eq(regionName));
        Mockito.verify(withGroup, Mockito.times(TestUtils.RUN_ONCE))
                .withExistingResourceGroup(Mockito.eq(resourceGroupName));
        Mockito.verify(withCreate, Mockito.times(TestUtils.RUN_ONCE)).defineRule(Mockito.eq(securityRuleName));
        Mockito.verify(defineSecurityRule, Mockito.times(TestUtils.RUN_ONCE)).allowInbound();
        Mockito.verify(withSourceAddress, Mockito.times(TestUtils.RUN_ONCE)).fromAnyAddress();
        Mockito.verify(withSourcePort, Mockito.times(TestUtils.RUN_ONCE)).fromAnyPort();
        Mockito.verify(withDestinationAddress, Mockito.times(TestUtils.RUN_ONCE)).toAddress(Mockito.eq(ipAddress));
        Mockito.verify(withDestinationPort, Mockito.times(TestUtils.RUN_ONCE)).toPort(port);
        Mockito.verify(withProtocol, Mockito.times(TestUtils.RUN_ONCE)).withProtocol(protocol);
        Mockito.verify(withAttach, Mockito.times(TestUtils.RUN_ONCE)).attach();
    }

    // test case: When calling the associateNetworkSecurityGroupAsync method, it
    // must verify that is call was successful.
    @Test
    public void testAssociateNetworkSecurityGroupAsyncSuccessfully() throws Exception {
        // set up
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        Creatable<NetworkSecurityGroup> creatable = Mockito.mock(Creatable.class);
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "associateNetworkSecurityGroupAsync",
                Mockito.eq(networkInterface), Mockito.eq(creatable));

        Update update = Mockito.mock(Update.class);
        Mockito.when(networkInterface.update()).thenReturn(update);

        Observable<NetworkInterface> observable = Mockito.mock(Observable.class);
        Mockito.when(update.withNewNetworkSecurityGroup(Mockito.eq(creatable))).thenReturn(update);
        Mockito.when(update.applyAsync()).thenReturn(observable);

        // exercise
        AzurePublicIPAddressSDK.associateNetworkSecurityGroupAsync(networkInterface, creatable);

        // verify
        Mockito.verify(networkInterface, Mockito.times(TestUtils.RUN_ONCE)).update();
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).withNewNetworkSecurityGroup(Mockito.eq(creatable));
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).applyAsync();
    }

    // test case: When calling the disassociateNetworkSecurityGroupAsync method, it
    // must verify that is call was successful.
    @Test
    public void testDisassociateNetworkSecurityGroupAsyncSuccessfully() throws Exception {
        // set up
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "disassociateNetworkSecurityGroupAsync",
                Mockito.eq(networkInterface));

        Update update = Mockito.mock(Update.class);
        Mockito.when(networkInterface.update()).thenReturn(update);

        Observable<NetworkInterface> observable = Mockito.mock(Observable.class);
        Mockito.when(update.withoutNetworkSecurityGroup()).thenReturn(update);
        Mockito.when(update.applyAsync()).thenReturn(observable);

        // exercise
        AzurePublicIPAddressSDK.disassociateNetworkSecurityGroupAsync(networkInterface);

        // verify
        Mockito.verify(networkInterface, Mockito.times(TestUtils.RUN_ONCE)).update();
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).withoutNetworkSecurityGroup();
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).applyAsync();
    }

    // test case: When calling the disassociatePublicIPAddressAsync method, it
    // must verify that is call was successful.
    @Test
    public void testDisassociatePublicIPAddressAsyncSuccessfully() throws Exception {
        // set up
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "disassociatePublicIPAddressAsync",
                Mockito.eq(networkInterface));

        Update update = Mockito.mock(Update.class);
        Mockito.when(networkInterface.update()).thenReturn(update);

        Observable<NetworkInterface> observable = Mockito.mock(Observable.class);
        Mockito.when(update.withoutPrimaryPublicIPAddress()).thenReturn(update);
        Mockito.when(update.applyAsync()).thenReturn(observable);
        
        // exercise
        AzurePublicIPAddressSDK.disassociatePublicIPAddressAsync(networkInterface);

        // verify
        Mockito.verify(networkInterface, Mockito.times(TestUtils.RUN_ONCE)).update();
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).withoutPrimaryPublicIPAddress();
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).applyAsync();
    }

    // test case: When calling the deleteNetworkSecurityGroupAsync method, it
    // must verify that is call was successful.
    @Test
    public void testDeleteNetworkSecurityGroupAsyncSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.networkSecurityGroupId().build();
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "deleteNetworkSecurityGroupAsync",
                Mockito.eq(azure), Mockito.eq(resourceId));

        NetworkSecurityGroups networkSecurityGroups = Mockito.mock(NetworkSecurityGroups.class);
        PowerMockito.doReturn(networkSecurityGroups).when(AzurePublicIPAddressSDK.class, "getNetworkSecurityGroupsSDK",
                Mockito.eq(azure));

        Completable completable = Mockito.mock(Completable.class);
        Mockito.when(networkSecurityGroups.deleteByIdAsync(Mockito.eq(resourceId))).thenReturn(completable);

        // exercise
        AzurePublicIPAddressSDK.deleteNetworkSecurityGroupAsync(azure, resourceId);

        // verify
        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.getNetworkSecurityGroupsSDK(Mockito.eq(azure));

        Mockito.verify(networkSecurityGroups, Mockito.times(TestUtils.RUN_ONCE))
                .deleteByIdAsync(Mockito.eq(resourceId));
    }

    // test case: When calling the getNetworkSecurityGroupsSDK method, it
    // must verify that is call was successful.
    @Test
    public void testGetNetworkSecurityGroupsSDKSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "getNetworkSecurityGroupsSDK",
                Mockito.eq(azure));

        NetworkSecurityGroups networkSecurityGroups = Mockito.mock(NetworkSecurityGroups.class);
        Mockito.when(azure.networkSecurityGroups()).thenReturn(networkSecurityGroups);

        // exercise
        AzurePublicIPAddressSDK.getNetworkSecurityGroupsSDK(azure);

        // verify
        Mockito.verify(azure, Mockito.times(TestUtils.RUN_ONCE)).networkSecurityGroups();
    }

    // test case: When calling the deletePublicIpAddressAsync method, it
    // must verify that is call was successful.
    @Test
    public void testDeletePublicIpAddressAsyncSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.networkSecurityGroupId().build();
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "deletePublicIpAddressAsync",
                Mockito.eq(azure), Mockito.eq(resourceId));

        PublicIPAddresses publicIPAddresses = Mockito.mock(PublicIPAddresses.class);
        PowerMockito.doReturn(publicIPAddresses).when(AzurePublicIPAddressSDK.class, "getPublicIPAddressesSDK",
                Mockito.eq(azure));

        Completable completable = Mockito.mock(Completable.class);
        Mockito.when(publicIPAddresses.deleteByIdAsync(Mockito.eq(resourceId))).thenReturn(completable);

        // exercise
        AzurePublicIPAddressSDK.deletePublicIpAddressAsync(azure, resourceId);

        // verify
        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.getPublicIPAddressesSDK(Mockito.eq(azure));

        Mockito.verify(publicIPAddresses, Mockito.times(TestUtils.RUN_ONCE)).deleteByIdAsync(Mockito.eq(resourceId));
    }

    // test case: When calling the getNetworkSecurityGroupFrom method with a valid
    // network interface, it must verify that is call was successful.
    @Test
    public void testGetNetworkSecurityGroupFromNetworkInterfaceSuccessfully() throws Exception {
        // set up
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "getNetworkSecurityGroupFrom",
                Mockito.eq(networkInterface));

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Mockito.when(networkInterface.getNetworkSecurityGroup()).thenReturn(networkSecurityGroup);

        // exercise
        AzurePublicIPAddressSDK.getNetworkSecurityGroupFrom(networkInterface);

        // verify
        Mockito.verify(networkInterface, Mockito.times(TestUtils.RUN_ONCE)).getNetworkSecurityGroup();
    }

    // test case: When calling the getNetworkSecurityGroupFrom method and an
    // unexpected error occurs, it must verify than an UnexpectedException has been
    // thrown.
    @Test
    public void testGetNetworkSecurityGroupFromNetworkInterfaceFail() throws Exception {
        // set up
        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "getNetworkSecurityGroupFrom",
                Mockito.eq(networkInterface));

        Mockito.when(networkInterface.getNetworkSecurityGroup()).thenThrow(new RuntimeException());

        // verify
        this.expectedException.expect(UnexpectedException.class);

        // exercise
        AzurePublicIPAddressSDK.getNetworkSecurityGroupFrom(networkInterface);
    }

    // test case: When calling the getPrimaryNetworkInterfaceFrom method with a
    // valid virtual machine, it must verify that is call was successful.
    @Test
    public void testGetPrimaryNetworkInterfaceFromVirtualMachineSuccessfully() throws Exception {
        // set up
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "getPrimaryNetworkInterfaceFrom",
                Mockito.eq(virtualMachine));

        NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
        Mockito.when(virtualMachine.getPrimaryNetworkInterface()).thenReturn(networkInterface);

        // exercise
        AzurePublicIPAddressSDK.getPrimaryNetworkInterfaceFrom(virtualMachine);

        // verify
        Mockito.verify(virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).getPrimaryNetworkInterface();
    }

    // test case: When calling the getPrimaryNetworkInterfaceFrom method and an
    // unexpected error occurs, it must verify than an UnexpectedException has been
    // thrown.
    @Test
    public void testGetPrimaryNetworkInterfaceFromVirtualMachineFail() throws Exception {
        // set up
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "getPrimaryNetworkInterfaceFrom",
                Mockito.eq(virtualMachine));

        Mockito.when(virtualMachine.getPrimaryNetworkInterface()).thenThrow(new RuntimeException());

        // verify
        this.expectedException.expect(UnexpectedException.class);

        // exercise
        AzurePublicIPAddressSDK.getPrimaryNetworkInterfaceFrom(virtualMachine);
    }

    // test case: When calling the getPublicIpAddress method with a valid resource
    // ID, it must verify that is call was successful.
    @Test
    public void testGetPublicIpAddressSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.publicIpAddressId().build();
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "getPublicIpAddress", Mockito.eq(azure),
                Mockito.eq(resourceId));

        PublicIPAddresses publicIPAddresses = Mockito.mock(PublicIPAddresses.class);
        PowerMockito.doReturn(publicIPAddresses).when(AzurePublicIPAddressSDK.class, "getPublicIPAddressesSDK",
                Mockito.eq(azure));

        PublicIPAddress publicIPAddress = Mockito.mock(PublicIPAddress.class);
        Mockito.when(publicIPAddresses.getById(Mockito.eq(resourceId))).thenReturn(publicIPAddress);

        // exercise
        AzurePublicIPAddressSDK.getPublicIpAddress(azure, resourceId);

        // verify
        PowerMockito.verifyStatic(AzurePublicIPAddressSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzurePublicIPAddressSDK.getPublicIPAddressesSDK(Mockito.eq(azure));

        Mockito.verify(publicIPAddresses, Mockito.times(TestUtils.RUN_ONCE)).getById(Mockito.eq(resourceId));
    }

    // test case: When calling the getPublicIpAddress method and an unexpected error
    // occurs, it must verify than an UnexpectedException has been thrown.
    @Test
    public void testGetPublicIpAddressFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String resourceId = AzureResourceIdBuilder.publicIpAddressId().build();
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "getPublicIpAddress", Mockito.eq(azure),
                Mockito.eq(resourceId));

        PublicIPAddresses publicIPAddresses = Mockito.mock(PublicIPAddresses.class);
        PowerMockito.doReturn(publicIPAddresses).when(AzurePublicIPAddressSDK.class, "getPublicIPAddressesSDK",
                Mockito.eq(azure));

        Mockito.when(publicIPAddresses.getById(Mockito.eq(resourceId))).thenThrow(new RuntimeException());

        // verify
        this.expectedException.expect(UnexpectedException.class);

        // exercise
        AzurePublicIPAddressSDK.getPublicIpAddress(azure, resourceId);
    }

    // test case: When calling the getPublicIPAddressesSDK method, it
    // must verify that is call was successful.
    @Test
    public void testGetPublicIPAddressesSDKSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.doCallRealMethod().when(AzurePublicIPAddressSDK.class, "getPublicIPAddressesSDK",
                Mockito.eq(azure));

        PublicIPAddresses publicIPAddresses = Mockito.mock(PublicIPAddresses.class);
        Mockito.when(azure.publicIPAddresses()).thenReturn(publicIPAddresses);

        // exercise
        AzurePublicIPAddressSDK.getPublicIPAddressesSDK(azure);

        // verify
        Mockito.verify(azure, Mockito.times(TestUtils.RUN_ONCE)).publicIPAddresses();
    }

    private PublicIPAddress mockPublicIPAddress() {
        String regionName = "eastus";
        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        String resourceName = AzureTestUtils.RESOURCE_NAME;
        String ipAddress = "0.0.0.0";

        PublicIPAddress publicIPAddress = Mockito.mock(PublicIPAddress.class);
        Mockito.when(publicIPAddress.regionName()).thenReturn(regionName);
        Mockito.when(publicIPAddress.resourceGroupName()).thenReturn(resourceGroupName);
        Mockito.when(publicIPAddress.name()).thenReturn(resourceName);
        Mockito.when(publicIPAddress.ipAddress()).thenReturn(ipAddress);

        return publicIPAddress;
    }

}
