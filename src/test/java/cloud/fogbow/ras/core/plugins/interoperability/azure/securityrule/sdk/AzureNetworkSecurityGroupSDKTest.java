package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.core.TestUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityGroups;
import com.microsoft.azure.management.network.NetworkSecurityRule.UpdateDefinitionStages.*;
import com.microsoft.azure.management.network.SecurityRuleProtocol;
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
@PrepareForTest({
        AzureNetworkSecurityGroupSDK.class
})
public class AzureNetworkSecurityGroupSDKTest extends TestUtils {

    private Azure azure;

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        this.azure = null;
    }

    // test case: When calling getNetworkSecurityGroup with mocked methods,
    // it must verify if all variables are created and the result is as expected.
    @Test
    public void testGetNetworkSecurityGroupSuccessfully() throws InternalServerErrorException {
        // set up
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        String networkSecurityGroupId = "network-security-group-id";

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);

        NetworkSecurityGroups networkSecurityGroups = Mockito.mock(NetworkSecurityGroups.class);
        Mockito.when(networkSecurityGroups.getById(Mockito.eq(networkSecurityGroupId))).thenReturn(networkSecurityGroup);

        Mockito.when(AzureNetworkSecurityGroupSDK.getNetworkSecurityGroupsSDK(Mockito.eq(azure)))
                .thenReturn(networkSecurityGroups);

        PowerMockito.when(AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure),
                Mockito.eq(networkSecurityGroupId))).thenCallRealMethod();

        // exercise
        Optional<NetworkSecurityGroup> securityGroupOptional =
                AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(azure, networkSecurityGroupId);

        // verify
        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.getNetworkSecurityGroupsSDK(Mockito.eq(azure));

        Mockito.verify(networkSecurityGroups, Mockito.times(TestUtils.RUN_ONCE)).getById(networkSecurityGroupId);

        Assert.assertTrue(securityGroupOptional.isPresent());
        Assert.assertEquals(networkSecurityGroup, securityGroupOptional.get());
    }

    // test case: When calling getNetworkSecurityGroup and a RuntimeException happens
    // it must verify if a InternalServerErrorException is rethrow.
    @Test
    public void testGetNetworkSecurityGroupFail() throws InternalServerErrorException {
        // set up
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        String networkSecurityGroupId = "network-security-group-id";

        NetworkSecurityGroups networkSecurityGroups = Mockito.mock(NetworkSecurityGroups.class);
        Mockito.when(networkSecurityGroups.getById(Mockito.eq(networkSecurityGroupId)))
                .thenThrow(RuntimeException.class);

        Mockito.when(AzureNetworkSecurityGroupSDK.getNetworkSecurityGroupsSDK(Mockito.eq(azure)))
                .thenReturn(networkSecurityGroups);

        PowerMockito.when(AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure),
                Mockito.eq(networkSecurityGroupId))).thenCallRealMethod();

        // verify
        this.expectedException.expect(InternalServerErrorException.class);

        // exercise
        AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(azure, networkSecurityGroupId);
    }

    // test case: When calling updateNetworkSecurityGroup with mocked methods and the direction is in bound,
    // it must verify if it creates all variable correct.
    @Test
    public void testUpdateNetworkSecurityGroupSuccessfullyDirectionInBound() throws Exception {
        // set up
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);

        String cidr = DEFAULT_CIDR;
        int portFrom = 22;
        int portTo = 22;
        String ruleName = "rule-name";
        SecurityRuleProtocol securityRuleProtocol = SecurityRuleProtocol.TCP;
        AzureNetworkSecurityGroupSDK.Direction direction = AzureNetworkSecurityGroupSDK.Direction.IN_BOUND;
        int priority = 101;

        NetworkSecurityGroup securityGroup = Mockito.mock(NetworkSecurityGroup.class);

        NetworkSecurityGroup.Update applyUpdate = Mockito.mock(NetworkSecurityGroup.Update.class);
        Mockito.when(applyUpdate.apply()).thenReturn(securityGroup);

        WithAttach<NetworkSecurityGroup.Update> withUpdate = (WithAttach<NetworkSecurityGroup.Update>)
                Mockito.mock(WithAttach.class);
        Mockito.when(withUpdate.attach()).thenReturn(applyUpdate);

        WithAttach<NetworkSecurityGroup.Update> withAttach = (WithAttach<NetworkSecurityGroup.Update>)
                Mockito.mock(WithAttach.class);
        Mockito.when(withAttach.withPriority(Mockito.eq(priority))).thenReturn(withUpdate);

        WithProtocol<NetworkSecurityGroup.Update> withProtocol = (WithProtocol<NetworkSecurityGroup.Update>)
                Mockito.mock(WithProtocol.class);
        Mockito.when(withProtocol.withProtocol(Mockito.eq(securityRuleProtocol))).thenReturn(withAttach);

        WithDestinationPort<NetworkSecurityGroup.Update> withDestinationPort = (WithDestinationPort<NetworkSecurityGroup.Update>)
                Mockito.mock(WithDestinationPort.class);
        Mockito.when(withDestinationPort.toPortRange(Mockito.eq(portFrom), Mockito.eq(portTo))).thenReturn(withProtocol);

        WithDestinationAddress<NetworkSecurityGroup.Update> withDestinationAddress = (WithDestinationAddress<NetworkSecurityGroup.Update>)
                Mockito.mock(WithDestinationAddress.class);
        Mockito.when(withDestinationAddress.toAnyAddress()).thenReturn(withDestinationPort);

        WithSourcePort<NetworkSecurityGroup.Update> withSourcePort = (WithSourcePort<NetworkSecurityGroup.Update>)
                Mockito.mock(WithSourcePort.class);
        Mockito.when(withSourcePort.fromAnyPort()).thenReturn(withDestinationAddress);

        WithSourceAddress<NetworkSecurityGroup.Update> withSourceAddress;
        withSourceAddress = (WithSourceAddress<NetworkSecurityGroup.Update>) Mockito.mock(WithSourceAddress.class);
        Mockito.when(withSourceAddress.fromAddress(Mockito.eq(cidr))).thenReturn(withSourcePort);

        Blank<NetworkSecurityGroup.Update> updateBlank = (Blank<NetworkSecurityGroup.Update>) Mockito.mock(Blank.class);
        Mockito.when(updateBlank.allowInbound()).thenReturn(withSourceAddress);

        NetworkSecurityGroup.Update update = Mockito.mock(NetworkSecurityGroup.Update.class);
        Mockito.when(update.defineRule(Mockito.eq(ruleName))).thenReturn(updateBlank);

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Mockito.when(networkSecurityGroup.update()).thenReturn(update);

        PowerMockito.doCallRealMethod().when(AzureNetworkSecurityGroupSDK.class, "updateNetworkSecurityGroup",
                Mockito.eq(networkSecurityGroup), Mockito.eq(cidr), Mockito.eq(portFrom), Mockito.eq(portTo),
                Mockito.eq(ruleName), Mockito.eq(securityRuleProtocol), Mockito.eq(direction), Mockito.eq(priority));

        // exercise
        AzureNetworkSecurityGroupSDK.updateNetworkSecurityGroup(networkSecurityGroup, cidr, portFrom, portTo,
                ruleName, securityRuleProtocol, direction, priority);

        // verify
        Mockito.verify(updateBlank, Mockito.times(TestUtils.RUN_ONCE)).allowInbound();
        Mockito.verify(updateBlank, Mockito.times(TestUtils.NEVER_RUN)).allowOutbound();
        Mockito.verify(networkSecurityGroup, Mockito.times(TestUtils.RUN_ONCE)).update();
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).defineRule(Mockito.eq(ruleName));

        Mockito.verify(withSourceAddress, Mockito.times(TestUtils.RUN_ONCE)).fromAddress(Mockito.eq(cidr));
        Mockito.verify(applyUpdate, Mockito.times(TestUtils.RUN_ONCE)).apply();
        Mockito.verify(withUpdate, Mockito.times(TestUtils.RUN_ONCE)).attach();
        Mockito.verify(withAttach, Mockito.times(TestUtils.RUN_ONCE)).withPriority(Mockito.eq(priority));
        Mockito.verify(withProtocol, Mockito.times(TestUtils.RUN_ONCE)).withProtocol(Mockito.eq(securityRuleProtocol));
        Mockito.verify(withDestinationPort, Mockito.times(TestUtils.RUN_ONCE)).toPortRange(Mockito.eq(portFrom), Mockito.eq(portTo));
        Mockito.verify(withDestinationAddress, Mockito.times(TestUtils.RUN_ONCE)).toAnyAddress();
        Mockito.verify(withSourcePort, Mockito.times(TestUtils.RUN_ONCE)).fromAnyPort();
    }

    // test case: When calling updateNetworkSecurityGroup with mocked methods and the direction is out bound,
    // it must verify if it creates all variable correct.
    @Test
    public void testUpdateNetworkSecurityGroupSuccessfullyDirectionOutBound() throws Exception {
        // set up
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);

        String cidr = DEFAULT_CIDR;
        int portFrom = 22;
        int portTo = 22;
        String ruleName = "rule-name";
        SecurityRuleProtocol securityRuleProtocol = SecurityRuleProtocol.TCP;
        AzureNetworkSecurityGroupSDK.Direction direction = AzureNetworkSecurityGroupSDK.Direction.OUT_BOUND;
        int priority = 101;

        NetworkSecurityGroup securityGroup = Mockito.mock(NetworkSecurityGroup.class);

        NetworkSecurityGroup.Update applyUpdate = Mockito.mock(NetworkSecurityGroup.Update.class);
        Mockito.when(applyUpdate.apply()).thenReturn(securityGroup);

        WithAttach<NetworkSecurityGroup.Update> withUpdate = (WithAttach<NetworkSecurityGroup.Update>)
                Mockito.mock(WithAttach.class);
        Mockito.when(withUpdate.attach()).thenReturn(applyUpdate);

        WithAttach<NetworkSecurityGroup.Update> withAttach = (WithAttach<NetworkSecurityGroup.Update>)
                Mockito.mock(WithAttach.class);
        Mockito.when(withAttach.withPriority(Mockito.eq(priority))).thenReturn(withUpdate);

        WithProtocol<NetworkSecurityGroup.Update> withProtocol = (WithProtocol<NetworkSecurityGroup.Update>)
                Mockito.mock(WithProtocol.class);
        Mockito.when(withProtocol.withProtocol(Mockito.eq(securityRuleProtocol))).thenReturn(withAttach);

        WithDestinationPort<NetworkSecurityGroup.Update> withDestinationPort = (WithDestinationPort<NetworkSecurityGroup.Update>)
                Mockito.mock(WithDestinationPort.class);
        Mockito.when(withDestinationPort.toPortRange(Mockito.eq(portFrom), Mockito.eq(portTo))).thenReturn(withProtocol);

        WithDestinationAddress<NetworkSecurityGroup.Update> withDestinationAddress = (WithDestinationAddress<NetworkSecurityGroup.Update>)
                Mockito.mock(WithDestinationAddress.class);
        Mockito.when(withDestinationAddress.toAnyAddress()).thenReturn(withDestinationPort);

        WithSourcePort<NetworkSecurityGroup.Update> withSourcePort = (WithSourcePort<NetworkSecurityGroup.Update>)
                Mockito.mock(WithSourcePort.class);
        Mockito.when(withSourcePort.fromAnyPort()).thenReturn(withDestinationAddress);

        WithSourceAddress<NetworkSecurityGroup.Update> withSourceAddress;
        withSourceAddress = (WithSourceAddress<NetworkSecurityGroup.Update>) Mockito.mock(WithSourceAddress.class);
        Mockito.when(withSourceAddress.fromAddress(Mockito.eq(cidr))).thenReturn(withSourcePort);

        Blank<NetworkSecurityGroup.Update> updateBlank = (Blank<NetworkSecurityGroup.Update>) Mockito.mock(Blank.class);
        Mockito.when(updateBlank.allowOutbound()).thenReturn(withSourceAddress);

        NetworkSecurityGroup.Update update = Mockito.mock(NetworkSecurityGroup.Update.class);
        Mockito.when(update.defineRule(Mockito.eq(ruleName))).thenReturn(updateBlank);

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Mockito.when(networkSecurityGroup.update()).thenReturn(update);

        PowerMockito.doCallRealMethod().when(AzureNetworkSecurityGroupSDK.class, "updateNetworkSecurityGroup",
                Mockito.eq(networkSecurityGroup), Mockito.eq(cidr), Mockito.eq(portFrom), Mockito.eq(portTo),
                Mockito.eq(ruleName), Mockito.eq(securityRuleProtocol), Mockito.eq(direction), Mockito.eq(priority));

        // exercise
        AzureNetworkSecurityGroupSDK.updateNetworkSecurityGroup(networkSecurityGroup, cidr, portFrom, portTo,
                ruleName, securityRuleProtocol, direction, priority);

        // verify
        Mockito.verify(updateBlank, Mockito.times(TestUtils.NEVER_RUN)).allowInbound();
        Mockito.verify(updateBlank, Mockito.times(TestUtils.RUN_ONCE)).allowOutbound();
        Mockito.verify(networkSecurityGroup, Mockito.times(TestUtils.RUN_ONCE)).update();
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).defineRule(Mockito.eq(ruleName));

        Mockito.verify(withSourceAddress, Mockito.times(TestUtils.RUN_ONCE)).fromAddress(Mockito.eq(cidr));
        Mockito.verify(applyUpdate, Mockito.times(TestUtils.RUN_ONCE)).apply();
        Mockito.verify(withUpdate, Mockito.times(TestUtils.RUN_ONCE)).attach();
        Mockito.verify(withAttach, Mockito.times(TestUtils.RUN_ONCE)).withPriority(Mockito.eq(priority));
        Mockito.verify(withProtocol, Mockito.times(TestUtils.RUN_ONCE)).withProtocol(Mockito.eq(securityRuleProtocol));
        Mockito.verify(withDestinationPort, Mockito.times(TestUtils.RUN_ONCE)).toPortRange(Mockito.eq(portFrom), Mockito.eq(portTo));
        Mockito.verify(withDestinationAddress, Mockito.times(TestUtils.RUN_ONCE)).toAnyAddress();
        Mockito.verify(withSourcePort, Mockito.times(TestUtils.RUN_ONCE)).fromAnyPort();
    }

    // test case: When calling deleteNetworkSecurityRule with mocked methods,
    // it must verify if it occurs alright
    @Test
    public void testDeleteNetworkSecurityRuleSuccessfully() throws Exception {
        // set up
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        String securityRuleName = "security-rule-name";

        NetworkSecurityGroup.Update applyUpdate = Mockito.mock(NetworkSecurityGroup.Update.class);
        Mockito.when(applyUpdate.apply()).thenReturn(null);

        NetworkSecurityGroup.Update update = Mockito.mock(NetworkSecurityGroup.Update.class);
        Mockito.when(update.withoutRule(Mockito.eq(securityRuleName))).thenReturn(applyUpdate);

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Mockito.when(networkSecurityGroup.update()).thenReturn(update);


        PowerMockito.when(AzureNetworkSecurityGroupSDK.class, "deleteNetworkSecurityRule", Mockito.eq(networkSecurityGroup),
                Mockito.eq(securityRuleName)).thenCallRealMethod();

        // exercise
        AzureNetworkSecurityGroupSDK.deleteNetworkSecurityRule(networkSecurityGroup, securityRuleName);

        // verify
        Mockito.verify(networkSecurityGroup, Mockito.times(TestUtils.RUN_ONCE)).update();
        Mockito.verify(update, Mockito.times(TestUtils.RUN_ONCE)).withoutRule(Mockito.eq(securityRuleName));
        Mockito.verify(applyUpdate, Mockito.times(TestUtils.RUN_ONCE)).apply();
    }

    // test case: When calling deleteNetworkSecurityRule and a RuntimeException is thrown,
    // it must verify if a InternalServerErrorException is rethrow
    @Test
    public void testDeleteNetworkSecurityRuleFail() throws Exception {
        // set up
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        String securityRuleName = "security-rule-name";

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Mockito.when(networkSecurityGroup.update()).thenThrow(RuntimeException.class);

        PowerMockito.when(AzureNetworkSecurityGroupSDK.class, "deleteNetworkSecurityRule", Mockito.eq(networkSecurityGroup),
                Mockito.eq(securityRuleName)).thenCallRealMethod();

        // verify
        this.expectedException.expect(InternalServerErrorException.class);

        // exercise
        AzureNetworkSecurityGroupSDK.deleteNetworkSecurityRule(networkSecurityGroup, securityRuleName);

    }
}
