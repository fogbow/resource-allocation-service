package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.AzureNetworkSecurityGroupOperationSDK;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.sdk.model.AzureUpdateNetworkSecurityGroupRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util.SecurityRuleIdContext;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureGeneralUtil;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureResourceIdBuilder;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AzureGeneralUtil.class,
        SecurityRuleIdContext.class
})
public class AzureSecurityRulePluginTest extends TestUtils {
    private static final int DEFAULT_PORT_FROM = 22;
    private static final int DEFAULT_PORT_TO = 22;
    private AzureSecurityRulePlugin plugin;
    private AzureUser azureUser;
    private AzureNetworkSecurityGroupOperationSDK operation;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        String azureConfFilePath = HomeDir.getPath()
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.azureUser = AzureTestUtils.createAzureUser();
        this.operation = Mockito.mock(AzureNetworkSecurityGroupOperationSDK.class);
        this.plugin = Mockito.spy(new AzureSecurityRulePlugin(azureConfFilePath));
        this.plugin.setAzureNetworkSecurityGroupOperationSDK(operation);
    }

    // test case: When calling the requestSecurityRule method with mocked methods,
    // it must verify if it creates all variable correct.
    @Test
    public void testRequestSecurityRuleSuccessfully() throws FogbowException {
        // set up
        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.mockStatic(SecurityRuleIdContext.class);
        Order majorOrder = Mockito.mock(NetworkOrder.class);
        SecurityRule securityRule = mockSecurityRule();

        String majorOrderInstanceId = TestUtils.FAKE_INSTANCE_ID;
        String expectedSecurityGroupName = AzureTestUtils.RESOURCE_NAME;

        Mockito.when(majorOrder.getInstanceId()).thenReturn(majorOrderInstanceId);

        Mockito.when(AzureGeneralUtil.defineResourceName(Mockito.eq(majorOrderInstanceId)))
                .thenReturn(expectedSecurityGroupName);

        String expectedSecurityGroupId = AzureResourceIdBuilder.networkSecurityGroupId()
                .withSubscriptionId(this.azureUser.getSubscriptionId())
                .withResourceGroupName(AzureTestUtils.DEFAULT_RESOURCE_GROUP_NAME)
                .withResourceName(expectedSecurityGroupName)
                .build();

        String securityRuleName = AzureTestUtils.RESOURCE_NAME;
        Mockito.when(AzureGeneralUtil.generateResourceName()).thenReturn(securityRuleName);

        AzureUpdateNetworkSecurityGroupRef azureUpdateNetworkSecurityRef = AzureUpdateNetworkSecurityGroupRef.builder()
                .ruleResourceName(securityRuleName)
                .securityGroupResourceName(expectedSecurityGroupId)
                .protocol(securityRule.getProtocol())
                .cidr(securityRule.getCidr())
                .direction(securityRule.getDirection())
                .portFrom(securityRule.getPortFrom())
                .portTo(securityRule.getPortTo())
                .checkAndBuild();

        Mockito.doNothing().when(this.operation).doCreateInstance(Mockito.eq(azureUpdateNetworkSecurityRef),
                Mockito.eq(this.azureUser));

        String expectedInstanceId = TestUtils.FAKE_INSTANCE_ID;
        Mockito.when(SecurityRuleIdContext.buildInstanceId(Mockito.eq(expectedSecurityGroupName),
                Mockito.eq(securityRuleName))).thenReturn(expectedInstanceId);

        // exercise
        String instanceId = this.plugin.requestSecurityRule(securityRule, majorOrder, this.azureUser);

        // verify
        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.generateResourceName();

        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.defineResourceName(Mockito.eq(majorOrderInstanceId));

        Assert.assertEquals(expectedInstanceId, instanceId);
    }

    // test case: When calling the getSecurityRules method with mocked methods,
    // it must verify if it creates all variable correct.
    @Test
    public void testGetSecurityRulesSuccessfully() throws Exception {
        String networkSecurityGroupName = AzureTestUtils.RESOURCE_NAME;
        String instanceId = AzureGeneralUtil.defineInstanceId(networkSecurityGroupName);
        Order majorOrder = Mockito.mock(NetworkOrder.class);
        Mockito.when(majorOrder.getInstanceId()).thenReturn(instanceId);

        PowerMockito.mockStatic(AzureGeneralUtil.class);
        PowerMockito.doReturn(networkSecurityGroupName).when(AzureGeneralUtil.class, "defineResourceName",
                Mockito.eq(instanceId));

        List<SecurityRuleInstance> securityRuleInstances = new ArrayList<>();
        Mockito.when(this.operation.getNetworkSecurityRules(Mockito.eq(networkSecurityGroupName),
                Mockito.eq(this.azureUser))).thenReturn(securityRuleInstances);

        // exercise
        this.plugin.getSecurityRules(majorOrder, this.azureUser);

        // verify
        Mockito.verify(majorOrder, Mockito.times(TestUtils.RUN_TWICE)).getInstanceId();

        PowerMockito.verifyStatic(AzureGeneralUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AzureGeneralUtil.defineResourceName(Mockito.eq(instanceId));

        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .getNetworkSecurityRules(Mockito.eq(networkSecurityGroupName), Mockito.eq(this.azureUser));
    }

    // test case: When calling the deleteSecurityRule method with mocked methods,
    // it must verify if it creates all variable correct.
    @Test
    public void testDeleteSecurityRuleSuccessfully() throws FogbowException {
        // set up
        String securityRuleId = "networkSecurityGroupName_securityRuleName";
        String securityRuleName = "securityRuleName";
        String networkSecurityGroupName = "networkSecurityGroupName";

        SecurityRuleIdContext securityRuleIdContext = Mockito.mock(SecurityRuleIdContext.class);
        Mockito.when(securityRuleIdContext.getNetworkSecurityGroupName()).thenReturn(networkSecurityGroupName);
        Mockito.when(securityRuleIdContext.getSecurityRuleName()).thenReturn(securityRuleName);

        Mockito.when(this.plugin.getSecurityRuleIdContext(securityRuleId)).thenReturn(securityRuleIdContext);

        Mockito.doNothing().when(this.operation).deleteNetworkSecurityRule(Mockito.eq(networkSecurityGroupName),
                Mockito.eq(securityRuleName), Mockito.eq(this.azureUser));

        // exercise
        this.plugin.deleteSecurityRule(securityRuleId, this.azureUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getSecurityRuleIdContext(Mockito.eq(securityRuleId));

        Mockito.verify(securityRuleIdContext, Mockito.times(TestUtils.RUN_ONCE)).getNetworkSecurityGroupName();
        Mockito.verify(securityRuleIdContext, Mockito.times(TestUtils.RUN_ONCE)).getSecurityRuleName();
        Mockito.verify(this.operation, Mockito.times(TestUtils.RUN_ONCE))
                .deleteNetworkSecurityRule(Mockito.eq(networkSecurityGroupName), Mockito.eq(securityRuleName),
                Mockito.eq(this.azureUser));
    }

    private SecurityRule mockSecurityRule() {
        SecurityRule securityRule = Mockito.mock(SecurityRule.class);

        String cidr = DEFAULT_CIDR;
        int portFrom = DEFAULT_PORT_FROM;
        int portTo = DEFAULT_PORT_TO;
        SecurityRule.Direction direction = null;
        SecurityRule.Protocol protocol = null;

        Mockito.when(securityRule.getCidr()).thenReturn(cidr);
        Mockito.when(securityRule.getPortFrom()).thenReturn(portFrom);
        Mockito.when(securityRule.getPortTo()).thenReturn(portTo);
        Mockito.when(securityRule.getDirection()).thenReturn(direction);
        Mockito.when(securityRule.getProtocol()).thenReturn(protocol);

        return securityRule;
    }
}
