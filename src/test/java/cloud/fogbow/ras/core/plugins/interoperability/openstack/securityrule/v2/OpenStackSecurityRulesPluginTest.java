package cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.util.List;
import java.util.Properties;

@PrepareForTest({
        CreateSecurityRuleResponse.class,
        DatabaseManager.class,
        GetSecurityGroupsResponse.class,
        GetSecurityRulesResponse.class,
        OpenStackHttpToFogbowExceptionMapper.class,
        PropertiesUtil.class,
})
public class OpenStackSecurityRulesPluginTest extends BaseUnitTests {

    private static final String DEFAULT_PREFIX_ENDPOINT = "http://localhost:8080";
    private static final String QUERY_SECURITY_GROUP_ID = "?security_group_id=";
    private static final String SECURITY_GROUPS_JSON_FORMAT = "{\"security_groups\":[{\"id\":\"%s\"}]}";
    private static final String SECURITY_GROUP_NAME = "securityGroupName";

    private NetworkOrder majorOrder;
    private OpenStackHttpClient client;
    private OpenStackSecurityRulePlugin plugin;
    private OpenStackV3User cloudUser;
    private Properties properties;

    @Before
    public void setUp() throws UnexpectedException {
        this.properties = Mockito.mock(Properties.class);
        Mockito.when(this.properties.get(OpenStackCloudUtils.NETWORK_NEUTRON_URL_KEY)).thenReturn(DEFAULT_PREFIX_ENDPOINT);

        String confFilePath = HomeDir.getPath()
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + TestUtils.DEFAULT_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        PowerMockito.mockStatic(PropertiesUtil.class);
        BDDMockito.when(PropertiesUtil.readProperties(Mockito.eq(confFilePath))).thenReturn(this.properties);

        this.testUtils.mockReadOrdersFromDataBase();
        this.client = Mockito.mock(OpenStackHttpClient.class);
        this.plugin = Mockito.spy(new OpenStackSecurityRulePlugin(confFilePath));
        this.plugin.setClient(this.client);

        String requestingMember = this.testUtils.getLocalMemberId();
        String providingMember = TestUtils.FAKE_REMOTE_MEMBER_ID;
        this.majorOrder = this.testUtils.createNetworkOrder(requestingMember, providingMember);
        this.cloudUser = this.testUtils.createOpenStackUser();
    }

    // test case: When calling the requestSecurityRule method, it must verify that
    // is call was successful.
    @Test
    public void testRequestSecurityRuleSuccessfully() throws Exception {
        // set up
        String securityGroupName = SECURITY_GROUP_NAME;
        Mockito.doReturn(securityGroupName).when(this.plugin)
                .retrieveSecurityGroupName(Mockito.eq(this.majorOrder));

        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        Mockito.doReturn(securityGroupId).when(this.plugin)
                .retrieveSecurityGroupId(Mockito.eq(securityGroupName), Mockito.eq(this.cloudUser));

        SecurityRule securityRule = Mockito.mock(SecurityRule.class);
        CreateSecurityRuleRequest request = Mockito.mock(CreateSecurityRuleRequest.class);
        Mockito.doReturn(request).when(this.plugin)
                .buildCreateSecurityRuleRequest(Mockito.eq(securityGroupId), Mockito.eq(securityRule));

        String securityRuleId = TestUtils.FAKE_SECURITY_RULE_ID;
        Mockito.doReturn(securityRuleId).when(this.plugin)
                .doRequestSecurityRule(Mockito.eq(request), Mockito.eq(this.cloudUser));

        // exercise
        this.plugin.requestSecurityRule(securityRule, this.majorOrder, this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .retrieveSecurityGroupName(Mockito.eq(this.majorOrder));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .retrieveSecurityGroupId(Mockito.eq(securityGroupName), Mockito.eq(this.cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .buildCreateSecurityRuleRequest(Mockito.eq(securityGroupId), Mockito.eq(securityRule));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doRequestSecurityRule(Mockito.eq(request), Mockito.eq(this.cloudUser));
    }

    // test case: When calling the getSecurityRules method, it must verify that
    // is call was successful.
    @Test
    public void testGetSecurityRulesSuccessfully() throws FogbowException {
        // set up
        String securityGroupName = SECURITY_GROUP_NAME;
        Mockito.doReturn(securityGroupName).when(this.plugin)
                .retrieveSecurityGroupName(Mockito.eq(this.majorOrder));

        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        Mockito.doReturn(securityGroupId).when(this.plugin)
                .retrieveSecurityGroupId(Mockito.eq(securityGroupName), Mockito.eq(this.cloudUser));

        GetSecurityRulesResponse response = Mockito.mock(GetSecurityRulesResponse.class);
        Mockito.doReturn(response).when(this.plugin)
                .doGetSecurityRules(Mockito.eq(securityGroupId), Mockito.eq(this.cloudUser));

        List<SecurityRuleInstance> securityRuleInstances = Mockito.mock(List.class);
        Mockito.doReturn(securityRuleInstances).when(this.plugin).getSecurityRuleInstances(Mockito.any());

        // exercise
        this.plugin.getSecurityRules(this.majorOrder, this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .retrieveSecurityGroupName(Mockito.eq(this.majorOrder));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .retrieveSecurityGroupId(Mockito.eq(securityGroupName), Mockito.eq(this.cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetSecurityRules(Mockito.eq(securityGroupId), Mockito.eq(this.cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getSecurityRuleInstances(Mockito.eq(response));
    }

    // test case: When calling the deleteSecurityRule method, it must verify that
    // is call was successful.
    @Test
    public void testDeleteSecurityRuleSuccessfully() throws FogbowException {
        // set up
        Mockito.doNothing().when(this.plugin)
                .doDeleteRequest(Mockito.anyString(), Mockito.eq(this.cloudUser));

        String securityRuleId = TestUtils.FAKE_SECURITY_RULE_ID;

        // exercise
        this.plugin.deleteSecurityRule(securityRuleId, this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteRequest(Mockito.anyString(), Mockito.eq(this.cloudUser));
    }

    // test case: When calling doDeleteRequest method, it must verify that the
    // call was successful.
    @Test
    public void testDoDeleteRequestSuccessfully() throws Exception {
        // set up
        String securityRuleId = TestUtils.FAKE_SECURITY_RULE_ID;
        String endpoint = DEFAULT_PREFIX_ENDPOINT
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + securityRuleId;

        // exercise
        this.plugin.doDeleteRequest(endpoint, this.cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteRequest(Mockito.eq(endpoint), Mockito.eq(this.cloudUser));
    }

    // test case: When calling the doDeleteRequest method with an invalid security
    // rule ID endpoint, it must verify than an InstanceNotFoundException has been
    // thrown.
    @Test
    public void testDoDeleteRequestFail() throws Exception {
        // set up
        String securityRuleId = TestUtils.ANY_VALUE;
        String endpoint = DEFAULT_PREFIX_ENDPOINT
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT
                + OpenStackConstants.ENDPOINT_SEPARATOR
                + securityRuleId;

        String message = Messages.Exception.NULL_VALUE_RETURNED;
        HttpResponseException responseException = new HttpResponseException(HttpStatus.SC_NOT_FOUND, message);
        Mockito.doThrow(responseException).when(this.client)
                .doDeleteRequest(Mockito.eq(endpoint), Mockito.eq(this.cloudUser));

        InstanceNotFoundException expectedException = new InstanceNotFoundException(message);
        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doThrow(expectedException).when(OpenStackHttpToFogbowExceptionMapper.class, "map",
                Mockito.eq(responseException));

        try {
            // exercise
            this.plugin.doDeleteRequest(endpoint, this.cloudUser);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(responseException));
        }
    }

    // test case: When calling doPostRequest method, it must verify that the
    // call was successful.
    @Test
    public void testDoPostRequestSuccessfully() throws Exception {
        // set up
        String endpoint = DEFAULT_PREFIX_ENDPOINT + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT;
        String bodyContent = TestUtils.ANY_VALUE;

        // exercise
        this.plugin.doPostRequest(endpoint, bodyContent, this.cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE))
                .doPostRequest(Mockito.eq(endpoint), Mockito.eq(bodyContent), Mockito.eq(this.cloudUser));
    }

    // test case: When calling the doPostRequest method and an error occurs, it must
    // verify than an UnexpectedException has been thrown.
    @Test
    public void testDoPostRequestFail() throws Exception {
        // set up
        String endpoint = DEFAULT_PREFIX_ENDPOINT + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT;
        String bodyContent = TestUtils.ANY_VALUE;

        String message = Messages.Exception.UNEXPECTED_ERROR;
        HttpResponseException responseException = new HttpResponseException(HttpStatus.SC_INTERNAL_SERVER_ERROR, message);
        Mockito.doThrow(responseException).when(this.client)
                .doPostRequest(Mockito.eq(endpoint), Mockito.eq(bodyContent), Mockito.eq(this.cloudUser));

        UnexpectedException expectedException = new UnexpectedException(message);
        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doThrow(expectedException).when(OpenStackHttpToFogbowExceptionMapper.class, "map",
                Mockito.eq(responseException));

        // exercise
        try {
            this.plugin.doPostRequest(endpoint, bodyContent, cloudUser);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(responseException));
        }
    }

    // test case: When calling doGetRequest method, it must verify that the
    // call was successful.
    @Test
    public void testDoGetRequestSuccessfully() throws Exception {
        // set up
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        String endpoint = DEFAULT_PREFIX_ENDPOINT
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT
                + QUERY_SECURITY_GROUP_ID
                + securityGroupId;

        // exercise
        this.plugin.doGetRequest(endpoint, this.cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE))
                .doGetRequest(Mockito.eq(endpoint), Mockito.eq(this.cloudUser));
    }

    // test case: When calling the doDeleteRequest method with an invalid security
    // group ID endpoint, it must verify than an InstanceNotFoundException has been
    // thrown.
    @Test
    public void testDoGetRequestFail() throws Exception {
        // set up
        String securityGroupId = TestUtils.ANY_VALUE;
        String endpoint = DEFAULT_PREFIX_ENDPOINT
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT
                + QUERY_SECURITY_GROUP_ID
                + securityGroupId;

        String message = Messages.Exception.NULL_VALUE_RETURNED;
        HttpResponseException responseException = new HttpResponseException(HttpStatus.SC_NOT_FOUND, message);
        Mockito.doThrow(responseException).when(this.client)
                .doGetRequest(Mockito.eq(endpoint), Mockito.eq(this.cloudUser));

        InstanceNotFoundException expectedException = new InstanceNotFoundException(message);
        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doThrow(expectedException).when(OpenStackHttpToFogbowExceptionMapper.class, "map",
                Mockito.eq(responseException));

        // exercise
        try {
            this.plugin.doGetRequest(endpoint, this.cloudUser);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(responseException));
        }
    }

    // test case: When calling retrieveSecurityGroupName method with a network
    // order, it must verify that the appropriate security group name was returned.
    @Test
    public void testRetrieveSecurityGroupNameWithNetworkOrder() throws FogbowException {
        // setup
        NetworkOrder networkOrder = this.testUtils.createLocalNetworkOrder();
        NetworkOrder spyNetworkOrder = Mockito.spy(networkOrder);
        String expectedGroupName = SystemConstants.PN_SECURITY_GROUP_PREFIX + networkOrder.getInstanceId();

        // exercise
        String actualGroupName = this.plugin.retrieveSecurityGroupName(spyNetworkOrder);

        // verify
        Mockito.verify(spyNetworkOrder, Mockito.times((TestUtils.RUN_ONCE))).getType();
        Mockito.verify(spyNetworkOrder, Mockito.times((TestUtils.RUN_ONCE))).getInstanceId();
        Assert.assertEquals(expectedGroupName, actualGroupName);
    }

    // test case: When calling retrieveSecurityGroupName method with a public IP
    // order, it must verify that the appropriate security group name was returned.
    @Test
    public void testRetrieveSecurityGroupNameWithPublicIpOrder() throws FogbowException {
        // setup
        String computeOrderId = TestUtils.FAKE_COMPUTE_ID;
        PublicIpOrder publicIpOrder = this.testUtils.createLocalPublicIpOrder(computeOrderId);
        String expectedGroupName = SystemConstants.PIP_SECURITY_GROUP_PREFIX + publicIpOrder.getInstanceId();
        PublicIpOrder spyPublicIpOrder = Mockito.spy(publicIpOrder);

        // exercise
        String actualGroupName = this.plugin.retrieveSecurityGroupName(spyPublicIpOrder);

        // verify
        Mockito.verify(spyPublicIpOrder, Mockito.times((TestUtils.RUN_ONCE))).getType();
        Mockito.verify(spyPublicIpOrder, Mockito.times((TestUtils.RUN_ONCE))).getInstanceId();
        Assert.assertEquals(expectedGroupName, actualGroupName);
    }

    // test case: When calling the doPostRequest method with an unsupported order,
    // it must verify than an InvalidParameterException has been thrown.
    @Test
    public void testRetrieveSecurityGroupNameWithInvalidOrder() throws FogbowException {
        // setup
        ComputeOrder order = testUtils.createLocalComputeOrder();
        String expected = String.format(Messages.Exception.INVALID_PARAMETER_S, order.getType());

        try {
            // exercise
            this.plugin.retrieveSecurityGroupName(order);
            Assert.fail();
        } catch (InvalidParameterException e) {
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    // test case: When calling retrieveSecurityGroupId method, it must verify that
    // the call was successful.
    @Test
    public void testRetrieveSecurityGroupIdSuccessfully() throws FogbowException {
        // setup
        String securityGroupName = SECURITY_GROUP_NAME;
        String endpoint = DEFAULT_PREFIX_ENDPOINT
                + OpenStackConstants.SECURITY_GROUPS_ENDPOINT
                + OpenStackConstants.QUERY_NAME
                + securityGroupName;

        Mockito.doReturn(endpoint).when(this.plugin)
                .buildQueryEndpointBySecurityGroupName(Mockito.eq(securityGroupName));

        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        String responseJson = String.format(SECURITY_GROUPS_JSON_FORMAT , securityGroupId);
        Mockito.doReturn(responseJson).when(this.plugin)
                .doGetRequest(Mockito.eq(endpoint), Mockito.eq(this.cloudUser));

        Mockito.doReturn(securityGroupId).when(this.plugin)
                .getSecurityGroupId(Mockito.any(), Mockito.eq(securityGroupName));

        // exercise
        this.plugin.retrieveSecurityGroupId(securityGroupName, this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .buildQueryEndpointBySecurityGroupName(Mockito.eq(securityGroupName));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetRequest(Mockito.anyString(), Mockito.eq(this.cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getSecurityGroupId(Mockito.any(), Mockito.eq(securityGroupName));
    }

}
