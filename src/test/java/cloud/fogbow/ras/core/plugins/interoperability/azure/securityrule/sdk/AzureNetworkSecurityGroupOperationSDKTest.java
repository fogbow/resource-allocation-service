package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.model.AzureUpdateNetworkSecurityGroupRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util.AzureSecurityRuleUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util.SecurityRuleIdContext;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityRule;
import com.microsoft.azure.management.network.SecurityRuleDirection;
import com.microsoft.azure.management.network.SecurityRuleProtocol;
import com.microsoft.azure.management.network.implementation.SecurityRuleInner;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AzureClientCacheManager.class,
        AzureNetworkSecurityGroupSDK.class,
        AzureSecurityRuleUtil.class,
        SecurityRuleIdContext.class
})
public class AzureNetworkSecurityGroupOperationSDKTest extends TestUtils {

    private static final int DEFAULT_PORT_TO = 22;
    private static final int DEFAULT_PORT_FROM = 22;
    private static final int DEFAULT_PRIORITY = 100;
    private Azure azure;
    private AzureUser azureUser;
    private AzureNetworkSecurityGroupOperationSDK operation;

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        this.azure = null;
        this.azureUser = AzureTestUtils.createAzureUser();
        this.operation = Mockito.spy(new AzureNetworkSecurityGroupOperationSDK());
    }

    // test case: When calling the doCreateInstance method with mocked methods,
    // it must verify if it creates all variable correct.
    @Test
    public void testDoCreateInstanceSuccessfully() throws Exception {
        // set up
        mockAzureClient();
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        PowerMockito.mockStatic(AzureSecurityRuleUtil.class);

        String networkSecurityGroupId = "network-security-group-id";
        String cidr = TestUtils.DEFAULT_CIDR;
        int portFrom = DEFAULT_PORT_FROM;
        int portTo = DEFAULT_PORT_TO;
        String ruleName = "rule-name";
        SecurityRule.Protocol protocol = SecurityRule.Protocol.ANY;
        SecurityRule.Direction direction = SecurityRule.Direction.IN;
        int priority = DEFAULT_PRIORITY;

        AzureUpdateNetworkSecurityGroupRef ref = AzureUpdateNetworkSecurityGroupRef.builder()
                .networkSecurityGroupId(networkSecurityGroupId)
                .cidr(cidr)
                .portFrom(portFrom)
                .portTo(portTo)
                .ruleResourceName(ruleName)
                .protocol(protocol)
                .direction(direction)
                .build();

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Optional<NetworkSecurityGroup> optNetworkSecurityGroup = Optional.ofNullable(networkSecurityGroup);
        Mockito.when(AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure),
                Mockito.eq(networkSecurityGroupId))).thenReturn(optNetworkSecurityGroup);

        SecurityRuleProtocol securityRuleProtocol = SecurityRuleProtocol.TCP;
        Mockito.when(AzureSecurityRuleUtil.getFogbowProtocol(Mockito.eq(protocol))).thenReturn(securityRuleProtocol);

        AzureNetworkSecurityGroupSDK.Direction securityRuleDirection = AzureNetworkSecurityGroupSDK.Direction.IN_BOUND;
        Mockito.when(AzureSecurityRuleUtil.getFogbowDirection(Mockito.eq(direction))).thenReturn(securityRuleDirection);

        Mockito.doReturn(priority).when(this.operation).getPriority(Mockito.eq(networkSecurityGroup));

        PowerMockito.doNothing().when(AzureNetworkSecurityGroupSDK.class, "updateNetworkSecurityGroup", Mockito.eq(networkSecurityGroup),
                Mockito.eq(cidr), Mockito.eq(portFrom), Mockito.eq(portTo), Mockito.eq(ruleName),
                Mockito.eq(securityRuleProtocol), Mockito.eq(securityRuleDirection), Mockito.eq(priority));

        // exercise
        this.operation.doCreateInstance(ref, azureUser);

        // verify
        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure),
                Mockito.eq(networkSecurityGroupId));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).getPriority(Mockito.eq(networkSecurityGroup));

        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.updateNetworkSecurityGroup(Mockito.eq(networkSecurityGroup),
                Mockito.eq(cidr), Mockito.eq(portFrom), Mockito.eq(portTo), Mockito.eq(ruleName),
                Mockito.eq(securityRuleProtocol), Mockito.eq(securityRuleDirection), Mockito.eq(priority));
    }

    // test case: When calling the doCreateInstance method with mocked methods,
    // if the network security group instance is not found, it must throw a InstanceNotFoundException
    @Test
    public void testDoCreateInstanceNotFound() throws FogbowException {
        // set up
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        mockAzureClient();
        AzureUpdateNetworkSecurityGroupRef ref = Mockito.mock(AzureUpdateNetworkSecurityGroupRef.class);

        String networkSecurityGroupId = "network-security-group-id";
        Mockito.when(ref.getNetworkSecurityGroupId()).thenReturn(networkSecurityGroupId);

        Optional<NetworkSecurityGroup> optNetworkSecurityGroup = Optional.empty();
        Mockito.when(AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure),
                Mockito.eq(networkSecurityGroupId))).thenReturn(optNetworkSecurityGroup);

        // verify
        this.expectedException.expect(InstanceNotFoundException.class);

        // exercise
        this.operation.doCreateInstance(ref, azureUser);
    }

    // Test case: When calling getPriority method with mocked methods,
    // if the current priority is not an unknown value, it must return the current priority plus one
    @Test
    public void testGetPrioritySuccessfully() {
        // set up
        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);

        int currentPriority = 111;
        int expectedPriority = currentPriority + 1;

        Mockito.doReturn(currentPriority).when(this.operation).getCurrentPriority();

        // exercise
        int actualPriority = this.operation.getPriority(networkSecurityGroup);

        // verify
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).getCurrentPriority();
        Mockito.verify(this.operation, Mockito.times(TestUtils.NEVER_RUN)).getLastPriority(Mockito.eq(networkSecurityGroup));

        Assert.assertEquals(expectedPriority, actualPriority);
    }

    // Test case: When calling getPriority method, if the current value is unknown
    // it must call getLastPriority method and the priority should be the last plus one
    @Test
    public void testGetPriorityUnknownCurrentPriority() {
        // set up
        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);

        int currentPriority = AzureNetworkSecurityGroupOperationSDK.UNKNOWN_PRIORITY_VALUE;
        int lastPriority = 521;
        int expectedPriority = lastPriority + 1;

        Mockito.doReturn(currentPriority).when(this.operation).getCurrentPriority();
        Mockito.doReturn(lastPriority).when(this.operation).getLastPriority(Mockito.eq(networkSecurityGroup));

        // exercise
        int actualPriority = this.operation.getPriority(networkSecurityGroup);

        // verify
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).getCurrentPriority();
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).getLastPriority(Mockito.eq(networkSecurityGroup));

        Assert.assertEquals(expectedPriority, actualPriority);
    }

    // test case: When calling getLastPriority method with mocked methods,
    // it must get the last priority of a list of security rules looking for rules starting
    // with fogbow instance name prefix
    @Test
    public void testGetLastPrioritySuccessfully() {
        // set up
        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Map<String, NetworkSecurityRule> securityRules = new HashMap<>();

        int numberOfPriorities = 5;
        int expectedPriority = DEFAULT_PRIORITY + numberOfPriorities;

        int lastPriority = DEFAULT_PRIORITY;
        String name;
        for (int i = 0; i < numberOfPriorities; i++) {
            name = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + "test-security-rule-" + i;
            lastPriority++;
            securityRules.put(name, createSecurityRule(name, lastPriority));
        }

        Mockito.when(networkSecurityGroup.securityRules()).thenReturn(securityRules);

        // exercise
        int actualPriority = this.operation.getLastPriority(networkSecurityGroup);

        // verify
        Assert.assertEquals(expectedPriority, actualPriority);
    }

    // test case: When calling getLastPriority method with mocked methods,
    // if no security rule is found, it must return the first priority value (100)
    @Test
    public void testGetLastPriorityNoSecurityRules() {
        // set up
        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Map<String, NetworkSecurityRule> securityRules = new HashMap<>();
        Mockito.when(networkSecurityGroup.securityRules()).thenReturn(securityRules);
        int expectedPriority = DEFAULT_PRIORITY;

        // exercise
        int actualPriority = this.operation.getLastPriority(networkSecurityGroup);

        // verify
        Assert.assertEquals(expectedPriority, actualPriority);
    }

    // test case: When calling deleteNetworkSecurityRule method with mocked methods,
    // it must verify if all occurs in the right way
    @Test
    public void testDeleteNetworkSecurityRuleSuccessfully() throws Exception {
        // set up
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        mockAzureClient();

        String networkSecurityGroupId = "network-security-group-id";
        String securityRuleName = "security-rule-name";

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);

        Mockito.when(AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure),
                Mockito.eq(networkSecurityGroupId))).thenReturn(Optional.ofNullable(networkSecurityGroup));

        PowerMockito.doNothing().when(AzureNetworkSecurityGroupSDK.class, "deleteNetworkSecurityRule",
                Mockito.eq(networkSecurityGroup),
                Mockito.eq(securityRuleName));

        // exercise
        this.operation.deleteNetworkSecurityRule(networkSecurityGroupId, securityRuleName, azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(azureUser));

        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure), Mockito.eq(networkSecurityGroupId));

        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.deleteNetworkSecurityRule(Mockito.eq(networkSecurityGroup),
                Mockito.eq(securityRuleName));
    }

    // test case: When calling deleteNetworkSecurityRule method and the security rule
    // is not found, it must throw a InstanceNotFoundException
    @Test
    public void testDeleteNetworkSecurityRuleNotFound() throws Exception {
        // set up
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        mockAzureClient();

        String networkSecurityGroupId = "network-security-group-id";
        String securityRuleName = "security-rule-name";

        Mockito.when(AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure),
                Mockito.eq(networkSecurityGroupId))).thenReturn(Optional.empty());

        this.expectedException.expect(InstanceNotFoundException.class);

        // exercise
        this.operation.deleteNetworkSecurityRule(networkSecurityGroupId, securityRuleName, azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(azureUser));

        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure), Mockito.eq(networkSecurityGroupId));

        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.NEVER_RUN));
        AzureNetworkSecurityGroupSDK.deleteNetworkSecurityRule(Mockito.any(), Mockito.any());
    }

    // test case: When buildSecurityRuleInstance is called with mocked methods,
    // it must verify if it returns the right instance object
    @Test
    public void testBuildSecurityRuleInstanceSuccessfully() {
        PowerMockito.mockStatic(SecurityRuleIdContext.class);
        PowerMockito.mockStatic(AzureSecurityRuleUtil.class);

        String ruleName = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + "test-security-rule";
        String networkSecurityGroupName = "network-security-group-name";
        String cidr = TestUtils.DEFAULT_CIDR;
        String instanceId = "instance-id";
        int portFrom = DEFAULT_PORT_FROM;
        int portTo = DEFAULT_PORT_TO;

        SecurityRule.Direction direction = SecurityRule.Direction.IN;
        SecurityRuleDirection securityRuleDirection = SecurityRuleDirection.INBOUND;

        String portRange = "port-range";
        AzureSecurityRuleUtil.Ports ports = Mockito.mock(AzureSecurityRuleUtil.Ports.class);
        Mockito.when(ports.getFrom()).thenReturn(portFrom);
        Mockito.when(ports.getTo()).thenReturn(portTo);

        String ipAddress = "10.0.0.2";
        SecurityRule.EtherType etherType = SecurityRule.EtherType.IPv4;

        SecurityRuleProtocol securityRuleProtocol = SecurityRuleProtocol.TCP;
        SecurityRule.Protocol protocol = SecurityRule.Protocol.ANY;

        NetworkSecurityRule networkSecurityRule = Mockito.mock(NetworkSecurityRule.class);
        Mockito.when(networkSecurityRule.name()).thenReturn(ruleName);
        Mockito.when(networkSecurityRule.sourceAddressPrefix()).thenReturn(cidr);
        Mockito.when(networkSecurityRule.direction()).thenReturn(securityRuleDirection);
        Mockito.when(networkSecurityRule.destinationPortRange()).thenReturn(portRange);
        Mockito.when(networkSecurityRule.protocol()).thenReturn(securityRuleProtocol);

        Mockito.when(AzureSecurityRuleUtil.getFogbowDirection(Mockito.eq(securityRuleDirection))).thenReturn(direction);
        Mockito.when(AzureSecurityRuleUtil.getPorts(Mockito.eq(portRange))).thenReturn(ports);
        Mockito.when(AzureSecurityRuleUtil.getIpAddress(Mockito.eq(cidr))).thenReturn(ipAddress);
        Mockito.when(AzureSecurityRuleUtil.inferEtherType(Mockito.eq(ipAddress))).thenReturn(etherType);
        Mockito.when(AzureSecurityRuleUtil.getFogbowProtocol(Mockito.eq(securityRuleProtocol.toString()))).thenReturn(protocol);

        Mockito.when(SecurityRuleIdContext.buildInstanceId(Mockito.eq(networkSecurityGroupName),
                Mockito.eq(ruleName))).thenReturn(instanceId);

        SecurityRuleInstance expectedInstance = new SecurityRuleInstance(instanceId, direction, portFrom, portTo,
                cidr, etherType, protocol);

        // exercise
        SecurityRuleInstance actualInstance = this.operation.buildSecurityRuleInstance(networkSecurityRule,
                networkSecurityGroupName);

        // verify
        Assert.assertEquals(expectedInstance, actualInstance);
    }

    // test case: When calling getNetworkSecurityRules method with mocked methods,
    // it must verify if it returns the right list of instances
    @Test
    public void testGetNetworkSecurityRules() throws FogbowException {
        // set up
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        PowerMockito.mockStatic(AzureSecurityRuleUtil.class);

        mockAzureClient();

        String ruleName = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + "test-security-rule";
        String networkSecurityGroupName = "network-security-group-name";
        String networkSecurityGroupId = "network-security-group-id";
        String cidr = TestUtils.DEFAULT_CIDR;
        int portFrom = DEFAULT_PORT_FROM;
        int portTo = DEFAULT_PORT_TO;

        SecurityRule.Direction direction = SecurityRule.Direction.IN;
        SecurityRuleDirection securityRuleDirection = SecurityRuleDirection.INBOUND;

        String portRange = "port-range";
        AzureSecurityRuleUtil.Ports ports = Mockito.mock(AzureSecurityRuleUtil.Ports.class);
        Mockito.when(ports.getFrom()).thenReturn(portFrom);
        Mockito.when(ports.getTo()).thenReturn(portTo);

        String ipAddress = "10.0.0.2";
        SecurityRule.EtherType etherType = SecurityRule.EtherType.IPv4;

        SecurityRuleProtocol securityRuleProtocol = SecurityRuleProtocol.TCP;
        SecurityRule.Protocol protocol = SecurityRule.Protocol.ANY;

        String instanceId = "security-rule-id";
        SecurityRuleInner securityRuleInner = Mockito.mock(SecurityRuleInner.class);
        Mockito.when(securityRuleInner.id()).thenReturn(instanceId);

        NetworkSecurityRule networkSecurityRule = Mockito.mock(NetworkSecurityRule.class);
        Mockito.when(networkSecurityRule.name()).thenReturn(ruleName);
//        Mockito.when(networkSecurityRule.sourceAddressPrefix()).thenReturn(cidr);
//        Mockito.when(networkSecurityRule.direction()).thenReturn(securityRuleDirection);
//        Mockito.when(networkSecurityRule.destinationPortRange()).thenReturn(portRange);
//        Mockito.when(networkSecurityRule.protocol()).thenReturn(securityRuleProtocol);
//        Mockito.when(networkSecurityRule.inner()).thenReturn(securityRuleInner);
//
//        Mockito.when(AzureSecurityRuleUtil.getFogbowDirection(Mockito.eq(securityRuleDirection))).thenReturn(direction);
//        Mockito.when(AzureSecurityRuleUtil.getPorts(Mockito.eq(portRange))).thenReturn(ports);
//        Mockito.when(AzureSecurityRuleUtil.getIpAddress(Mockito.eq(cidr))).thenReturn(ipAddress);
//        Mockito.when(AzureSecurityRuleUtil.inferEtherType(Mockito.eq(ipAddress))).thenReturn(etherType);
//        Mockito.when(AzureSecurityRuleUtil.getFogbowProtocol(Mockito.eq(securityRuleProtocol.toString()))).thenReturn(protocol);

        SecurityRuleInstance securityRuleInstance = new SecurityRuleInstance(instanceId, direction, portFrom, portTo, cidr, etherType, protocol);

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Map<String, NetworkSecurityRule> instanceList = new HashMap<>();
        instanceList.put(ruleName, networkSecurityRule);
        Mockito.when(networkSecurityGroup.securityRules()).thenReturn(instanceList);

        Optional<NetworkSecurityGroup> optNetworkSecurityGroup = Optional.ofNullable(networkSecurityGroup);
        Mockito.when(AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure),
                Mockito.eq(networkSecurityGroupId))).thenReturn(optNetworkSecurityGroup);

        Mockito.doReturn(securityRuleInstance).when(this.operation).buildSecurityRuleInstance(Mockito.eq(networkSecurityRule),
                Mockito.eq(networkSecurityGroupName));

        // exercise
        List<SecurityRuleInstance> networkSecurityRules = this.operation.getNetworkSecurityRules(networkSecurityGroupId,
                networkSecurityGroupName, azureUser);

        // verify
        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure), Mockito.eq(networkSecurityGroupId));

        Assert.assertTrue(networkSecurityRules.contains(securityRuleInstance));
    }

    private NetworkSecurityRule createSecurityRule(String name, int priority) {
        NetworkSecurityRule networkSecurityRule = Mockito.mock(NetworkSecurityRule.class);
        Mockito.when(networkSecurityRule.priority()).thenReturn(priority);
        Mockito.when(networkSecurityRule.name()).thenReturn(name);
        return networkSecurityRule;
    }

    private void mockAzureClient() throws UnauthenticatedUserException {
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser)))
                .thenReturn(azure);
    }
}
