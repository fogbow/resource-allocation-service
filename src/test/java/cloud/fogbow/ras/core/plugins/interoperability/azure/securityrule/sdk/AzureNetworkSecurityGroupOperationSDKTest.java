package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk;

import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.connectivity.cloud.azure.AzureClientCacheManager;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.model.AzureUpdateNetworkSecurityGroupRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util.AzureSecurityRuleUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util.SecurityRuleIdContext;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.network.NetworkSecurityGroup;
import com.microsoft.azure.management.network.NetworkSecurityRule;
import com.microsoft.azure.management.network.SecurityRuleDirection;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
    Azure.class,
    AzureClientCacheManager.class,
    AzureGeneralUtil.class,
    AzureNetworkSecurityGroupSDK.class,
    AzureSecurityRuleUtil.class,
    SecurityRuleIdContext.class
})
public class AzureNetworkSecurityGroupOperationSDKTest extends TestUtils {

    private static final int DEFAULT_PORT_TO = 22;
    private static final int DEFAULT_PORT_FROM = 22;
    private static final int DEFAULT_PRIORITY = 100;

    private AzureUser azureUser;
    private AzureNetworkSecurityGroupOperationSDK operation;
    private String defaultResourceGroupName;

    @Rule
    private ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        this.azureUser = AzureTestUtils.createAzureUser();
        this.defaultResourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        this.operation = Mockito.spy(new AzureNetworkSecurityGroupOperationSDK(this.defaultResourceGroupName));
    }

    // test case: When calling the doCreateInstance method with mocked methods,
    // it must verify if it creates all variable correct.
    @Test
    public void testDoCreateInstanceSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser)))
                .thenReturn(azure);

        String networkSecurityGroupName = AzureTestUtils.RESOURCE_NAME;
        AzureUpdateNetworkSecurityGroupRef networkSecurityGroupRef = Mockito.mock(AzureUpdateNetworkSecurityGroupRef.class);
        Mockito.when(networkSecurityGroupRef.getSecurityGroupResourceName()).thenReturn(networkSecurityGroupName);

        String networkSecurityGroupId = createNetworkSecurityGroupId();
        Mockito.doReturn(networkSecurityGroupId).when(this.operation).buildNetworkSecurityGroupId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.eq(networkSecurityGroupName));

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Optional<NetworkSecurityGroup> nsgOptional = Optional.ofNullable(networkSecurityGroup);
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        PowerMockito.doReturn(nsgOptional).when(AzureNetworkSecurityGroupSDK.class, "getNetworkSecurityGroup",
                Mockito.eq(azure), Mockito.anyString());

        SecurityRule.Protocol protocol = SecurityRule.Protocol.TCP;
        Mockito.when(networkSecurityGroupRef.getProtocol()).thenReturn(protocol);

        SecurityRuleProtocol securityRuleProtocol = SecurityRuleProtocol.TCP;
        PowerMockito.mockStatic(AzureSecurityRuleUtil.class);
        Mockito.when(AzureSecurityRuleUtil.getFogbowProtocol(Mockito.eq(protocol))).thenReturn(securityRuleProtocol);

        SecurityRule.Direction direction = SecurityRule.Direction.IN;
        Mockito.when(networkSecurityGroupRef.getDirection()).thenReturn(direction);

        AzureNetworkSecurityGroupSDK.Direction nsgDirection = AzureNetworkSecurityGroupSDK.Direction.IN_BOUND;
        Mockito.when(AzureSecurityRuleUtil.getFogbowDirection(Mockito.eq(direction))).thenReturn(nsgDirection);

        PowerMockito.doNothing().when(AzureNetworkSecurityGroupSDK.class, "updateNetworkSecurityGroup",
                Mockito.eq(networkSecurityGroup), Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyString(), Mockito.eq(securityRuleProtocol), Mockito.eq(nsgDirection), Mockito.anyInt());

        // exercise
        this.operation.doCreateInstance(networkSecurityGroupRef, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).buildNetworkSecurityGroupId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.eq(networkSecurityGroupName));

        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure), Mockito.anyString());

        PowerMockito.verifyStatic(AzureSecurityRuleUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureSecurityRuleUtil.getFogbowProtocol(Mockito.eq(protocol));

        PowerMockito.verifyStatic(AzureSecurityRuleUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureSecurityRuleUtil.getFogbowDirection(Mockito.eq(direction));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).getPriority(Mockito.eq(networkSecurityGroup));

        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.updateNetworkSecurityGroup(Mockito.eq(networkSecurityGroup), Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyInt(), Mockito.anyString(), Mockito.eq(securityRuleProtocol),
                Mockito.eq(nsgDirection), Mockito.anyInt());
    }

    // test case: When calling the doCreateInstance method and the network
    // security group is not found, an InstanceNotFoundException should be
    // thrown.
    @Test
    public void testDoCreateInstanceFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser)))
                .thenReturn(azure);

        String networkSecurityGroupName = AzureTestUtils.RESOURCE_NAME;
        AzureUpdateNetworkSecurityGroupRef networkSecurityGroupRef = Mockito.mock(AzureUpdateNetworkSecurityGroupRef.class);
        Mockito.when(networkSecurityGroupRef.getSecurityGroupResourceName()).thenReturn(networkSecurityGroupName);

        String networkSecurityGroupId = createNetworkSecurityGroupId();
        Mockito.doReturn(networkSecurityGroupId).when(this.operation).buildNetworkSecurityGroupId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.eq(networkSecurityGroupName));

        NetworkSecurityGroup networkSecurityGroup = null;
        Optional<NetworkSecurityGroup> nsgOptional = Optional.ofNullable(networkSecurityGroup);
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        PowerMockito.doReturn(nsgOptional).when(AzureNetworkSecurityGroupSDK.class, "getNetworkSecurityGroup",
                Mockito.eq(azure), Mockito.anyString());

        // verify
        this.expectedException.expect(InstanceNotFoundException.class);

        // exercise
        this.operation.doCreateInstance(networkSecurityGroupRef, this.azureUser);
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
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser)))
                .thenReturn(azure);

        String networkSecurityGroupName = AzureTestUtils.RESOURCE_NAME;
        String networkSecurityGroupId = createNetworkSecurityGroupId();
        Mockito.doReturn(networkSecurityGroupId).when(this.operation).buildNetworkSecurityGroupId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.eq(networkSecurityGroupName));

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Optional<NetworkSecurityGroup> nsgOptional = Optional.ofNullable(networkSecurityGroup);
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        PowerMockito.doReturn(nsgOptional).when(AzureNetworkSecurityGroupSDK.class, "getNetworkSecurityGroup",
                Mockito.eq(azure), Mockito.anyString());

        String securityRuleName = AzureTestUtils.RESOURCE_NAME;
        PowerMockito.doNothing().when(AzureNetworkSecurityGroupSDK.class, "deleteNetworkSecurityRule",
                Mockito.eq(networkSecurityGroup), Mockito.eq(securityRuleName ));

        // exercise
        this.operation.deleteNetworkSecurityRule(networkSecurityGroupName, securityRuleName, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).buildNetworkSecurityGroupId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.eq(networkSecurityGroupName));

        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure), Mockito.anyString());

        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.deleteNetworkSecurityRule(Mockito.eq(networkSecurityGroup),
                Mockito.eq(securityRuleName));
    }

    // test case: When calling the deleteNetworkSecurityRule method and the network
    // security group is not found, an InstanceNotFoundException should be thrown.
    @Test
    public void testDeleteNetworkSecurityRuleFail() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser)))
                .thenReturn(azure);

        String networkSecurityGroupId = createNetworkSecurityGroupId();
        Mockito.doReturn(networkSecurityGroupId).when(this.operation).buildNetworkSecurityGroupId(Mockito.any(Azure.class),
                Mockito.anyString(), Mockito.anyString());

        NetworkSecurityGroup networkSecurityGroup = null;
        Optional<NetworkSecurityGroup> nsgOptional = Optional.ofNullable(networkSecurityGroup);
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        PowerMockito.doReturn(nsgOptional).when(AzureNetworkSecurityGroupSDK.class, "getNetworkSecurityGroup",
                Mockito.eq(azure), Mockito.anyString());

        String securityRuleName = AzureTestUtils.RESOURCE_NAME;

        // verify
        this.expectedException.expect(InstanceNotFoundException.class);

        // exercise
        this.operation.deleteNetworkSecurityRule(networkSecurityGroupId, securityRuleName, this.azureUser);
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

    // test case: When calling the getNetworkSecurityRules method with simulated
    // methods, it must verify that returned the correct list of instances.
    @Test
    public void testGetNetworkSecurityRulesSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser)))
                .thenReturn(azure);

        String networkSecurityGroupName = AzureTestUtils.RESOURCE_NAME;
        String networkSecurityGroupId = createNetworkSecurityGroupId();
        Mockito.doReturn(networkSecurityGroupId).when(this.operation).buildNetworkSecurityGroupId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.eq(networkSecurityGroupName));

        NetworkSecurityGroup networkSecurityGroup = Mockito.mock(NetworkSecurityGroup.class);
        Optional<NetworkSecurityGroup> nsgOptional = Optional.ofNullable(networkSecurityGroup);
        PowerMockito.mockStatic(AzureNetworkSecurityGroupSDK.class);
        PowerMockito.doReturn(nsgOptional).when(AzureNetworkSecurityGroupSDK.class, "getNetworkSecurityGroup",
                Mockito.eq(azure), Mockito.anyString());

        String securityRuleName = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + AzureTestUtils.RESOURCE_NAME;

        NetworkSecurityRule networkSecurityRule = Mockito.mock(NetworkSecurityRule.class);
        Mockito.when(networkSecurityRule.name()).thenReturn(securityRuleName);

        Map<String, NetworkSecurityRule> instanceList = new HashMap<>();
        instanceList.put(securityRuleName, networkSecurityRule);
        Mockito.when(networkSecurityGroup.securityRules()).thenReturn(instanceList);

        Optional<NetworkSecurityGroup> optNetworkSecurityGroup = Optional.ofNullable(networkSecurityGroup);
        Mockito.when(AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure),
                Mockito.eq(networkSecurityGroupId))).thenReturn(optNetworkSecurityGroup);

        SecurityRuleInstance securityRuleInstance = createSecurityRuleInstance(networkSecurityGroupName, securityRuleName);
        Mockito.doReturn(securityRuleInstance).when(this.operation).buildSecurityRuleInstance(
                Mockito.eq(networkSecurityRule), Mockito.eq(networkSecurityGroupName));

        // exercise
        List<SecurityRuleInstance> networkSecurityRules = this.operation
                .getNetworkSecurityRules(networkSecurityGroupName, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureClientCacheManager.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE)).buildNetworkSecurityGroupId(Mockito.eq(azure),
                Mockito.anyString(), Mockito.eq(networkSecurityGroupName));

        PowerMockito.verifyStatic(AzureNetworkSecurityGroupSDK.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureNetworkSecurityGroupSDK.getNetworkSecurityGroup(Mockito.eq(azure), Mockito.anyString());

        Assert.assertTrue(networkSecurityRules.contains(securityRuleInstance));
    }

    // test case: When calling the buildNetworkSecurityGroupId method, it must
    // verify that the resource ID was assembled correctly.
    @Test
    public void testBuildNetworkSecurityGroupIdSuccessfully() throws Exception {
        // set up
        Azure azure = PowerMockito.mock(Azure.class);
        String subscriptionId = AzureTestUtils.DEFAULT_SUBSCRIPTION_ID;
        String networkSecurityGroupName = AzureTestUtils.RESOURCE_NAME;

        String resourceGroupName = AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME;
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(resourceGroupName).when(AzureGeneralUtil.class, "selectResourceGroupName",
                Mockito.eq(azure), Mockito.eq(networkSecurityGroupName), Mockito.eq(this.defaultResourceGroupName));

        String expected = createNetworkSecurityGroupId();

        // exercise
        String resourceId = this.operation.buildNetworkSecurityGroupId(azure, subscriptionId, networkSecurityGroupName);

        // verify
        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.selectResourceGroupName(Mockito.eq(azure), Mockito.eq(networkSecurityGroupName),
                Mockito.eq(this.defaultResourceGroupName));

        Assert.assertEquals(expected, resourceId);
    }

    private SecurityRuleInstance createSecurityRuleInstance(String networkSecurityGroupName, String securityRuleName) {
        String instanceId = SecurityRuleIdContext.buildInstanceId(networkSecurityGroupName, securityRuleName);
        String cidr = TestUtils.DEFAULT_CIDR;
        int portFrom = DEFAULT_PORT_FROM;
        int portTo = DEFAULT_PORT_TO;
        SecurityRule.Direction direction = SecurityRule.Direction.IN;
        SecurityRule.EtherType etherType = SecurityRule.EtherType.IPv4;
        SecurityRule.Protocol protocol = SecurityRule.Protocol.ANY;
        return new SecurityRuleInstance(instanceId, direction, portFrom, portTo, cidr, etherType, protocol);
    }

    private NetworkSecurityRule createSecurityRule(String name, int priority) {
        NetworkSecurityRule networkSecurityRule = Mockito.mock(NetworkSecurityRule.class);
        Mockito.when(networkSecurityRule.priority()).thenReturn(priority);
        Mockito.when(networkSecurityRule.name()).thenReturn(name);
        return networkSecurityRule;
    }

    private String createNetworkSecurityGroupId() {
        String networkSecurityGroupIdFormat = AzureResourceIdBuilder.NETWORK_SECURITY_GROUP_STRUCTURE;
        return String.format(networkSecurityGroupIdFormat,
                AzureTestUtils.DEFAULT_SUBSCRIPTION_ID, this.defaultResourceGroupName,
                AzureTestUtils.RESOURCE_NAME);
    }

}
