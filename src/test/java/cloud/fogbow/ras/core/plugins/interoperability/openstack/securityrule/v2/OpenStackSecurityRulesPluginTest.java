package cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.CidrUtils;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.api.parameters.SecurityRule.Direction;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;
import cloud.fogbow.ras.api.parameters.SecurityRule.Protocol;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2.GetSecurityGroupsResponse.SecurityGroup;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2.GetSecurityRulesResponse.SecurityGroupRule;

@PrepareForTest({
        CreateSecurityRuleResponse.class,
        DatabaseManager.class,
        GetSecurityGroupsResponse.class,
        GetSecurityRulesResponse.class,
        OpenStackHttpToFogbowExceptionMapper.class,
        PropertiesUtil.class,
})
public class OpenStackSecurityRulesPluginTest extends BaseUnitTests {

    private static final String NETWORK_PREFIX_ENDPOINT = "https://mycloud.domain:9696";
    private static final String QUERY_SECURITY_GROUP_ID = "?security_group_id=";
    private static final String QUERY_SECURITY_GROUP_NAME = "?name=";
    private static final String SECURITY_GROUPS_JSON_FORMAT = "{\"security_groups\":[{\"id\":\"%s\"}]}";
    private static final String SECURITY_GROUP_NAME = "securityGroupName";

    private NetworkOrder majorOrder;
    private OpenStackHttpClient client;
    private OpenStackSecurityRulePlugin plugin;
    private OpenStackV3User cloudUser;

    @Before
    public void setUp() throws UnexpectedException {
        String confFilePath = HomeDir.getPath()
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + TestUtils.DEFAULT_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.testUtils.mockReadOrdersFromDataBase();
        this.client = Mockito.mock(OpenStackHttpClient.class);
        this.plugin = Mockito.spy(new OpenStackSecurityRulePlugin(confFilePath));
        this.plugin.setClient(this.client);

        this.majorOrder = this.testUtils.createLocalNetworkOrder();
        this.cloudUser = this.testUtils.createOpenStackUser();
    }

    // test case: When calling the requestSecurityRule method, it must verify if the
    // call was successful.
    @Test
    public void testRequestSecurityRuleSuccessfully() throws Exception {
        // set up
        String securityGroupName = SECURITY_GROUP_NAME;
        Mockito.doReturn(securityGroupName).when(this.plugin)
                .retrieveSecurityGroupName(Mockito.eq(this.majorOrder));

        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        Mockito.doReturn(securityGroupId).when(this.plugin)
                .retrieveSecurityGroupId(Mockito.eq(securityGroupName), Mockito.eq(this.cloudUser));

        String securityRuleId = TestUtils.FAKE_SECURITY_RULE_ID;
        Mockito.doReturn(securityRuleId).when(this.plugin)
                .doRequestSecurityRule(Mockito.any(CreateSecurityRuleRequest.class),
                Mockito.eq(this.cloudUser));

        SecurityRule securityRule = createSecurityRule();

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
                .doRequestSecurityRule(Mockito.any(CreateSecurityRuleRequest.class),
                Mockito.eq(this.cloudUser));
    }

    // test case: When calling the getSecurityRules method, it must verify if the
    // call was successful.
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

    // test case: When calling the deleteSecurityRule method, it must verify if the
    // call was successful.
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

    // test case: When calling the doDeleteRequest method, it must verify if the
    // call was successful.
    @Test
    public void testDoDeleteRequestSuccessfully() throws Exception {
        // set up
        String securityRuleId = TestUtils.FAKE_SECURITY_RULE_ID;
        String endpoint = NETWORK_PREFIX_ENDPOINT
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
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
    // rule ID endpoint, it must verify if an InstanceNotFoundException has been
    // thrown.
    @Test
    public void testDoDeleteRequestFail() throws Exception {
        // set up
        String securityRuleId = TestUtils.ANY_VALUE;
        String endpoint = NETWORK_PREFIX_ENDPOINT
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
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

    // test case: When calling doPostRequest method, it must verify if the
    // call was successful.
    @Test
    public void testDoPostRequestSuccessfully() throws Exception {
        // set up
        String endpoint = NETWORK_PREFIX_ENDPOINT
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT;

        String bodyContent = TestUtils.ANY_VALUE;

        // exercise
        this.plugin.doPostRequest(endpoint, bodyContent, this.cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE))
                .doPostRequest(Mockito.eq(endpoint), Mockito.eq(bodyContent), Mockito.eq(this.cloudUser));
    }

    // test case: When calling the doPostRequest method and an error occurs, it must
    // verify if an UnexpectedException has been thrown.
    @Test
    public void testDoPostRequestFail() throws Exception {
        // set up
        String endpoint = NETWORK_PREFIX_ENDPOINT
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT;

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

    // test case: When calling doGetRequest method, it must verify if the
    // call was successful.
    @Test
    public void testDoGetRequestSuccessfully() throws Exception {
        // set up
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        String endpoint = NETWORK_PREFIX_ENDPOINT
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
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
    // group ID endpoint, it must verify if an InstanceNotFoundException has been
    // thrown.
    @Test
    public void testDoGetRequestFail() throws Exception {
        // set up
        String securityGroupId = TestUtils.ANY_VALUE;
        String endpoint = NETWORK_PREFIX_ENDPOINT
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
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

    // test case: When calling doGetRequest method, it must verify if returned the
    // expected instance.
    @Test
    public void testGetSecurityRuleInstancesSuccessfully() throws FogbowException {
        // set up
        GetSecurityRulesResponse response = generateGetSecurityRulesResponse();
        SecurityRuleInstance expectedInstance = createSecurityRuleInstance();

        // exercise
        List<SecurityRuleInstance> instances = this.plugin.getSecurityRuleInstances(response);

        // verify
        Assert.assertEquals(expectedInstance, instances.listIterator().next());
    }

    // test case: When calling doGetSecurityRules method, it must verify if the
    // call was successful.
    @Test
    public void testDoGetSecurityRulesSuccessfully() throws Exception {
        // set up
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;
        String endpoint = NETWORK_PREFIX_ENDPOINT
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT
                + QUERY_SECURITY_GROUP_ID
                + securityGroupId;

        Mockito.doReturn(endpoint).when(this.plugin)
                .buildQueryEndpointBySecurityGroupId(Mockito.eq(securityGroupId));

        String responseJson = TestUtils.ANY_VALUE;
        Mockito.doReturn(responseJson).when(this.plugin)
                .doGetRequest(Mockito.eq(endpoint), Mockito.eq(this.cloudUser));

        GetSecurityRulesResponse response = Mockito.mock(GetSecurityRulesResponse.class);
        PowerMockito.mockStatic(GetSecurityRulesResponse.class);
        PowerMockito.doReturn(response).when(GetSecurityRulesResponse.class, "fromJson",
                Mockito.eq(responseJson));

        // exercise
        this.plugin.doGetSecurityRules(securityGroupId, this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .buildQueryEndpointBySecurityGroupId(Mockito.eq(securityGroupId));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetRequest(Mockito.eq(endpoint), Mockito.eq(this.cloudUser));

        PowerMockito.verifyStatic(GetSecurityRulesResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetSecurityRulesResponse.fromJson(Mockito.eq(responseJson));
    }

    // test case: When calling buildQueryEndpointBySecurityGroupId method, it must
    // verify if returned the expected endpoint.
    @Test
    public void testBuildQueryEndpointBySecurityGroupIdSuccessfully() {
        // set up
        String securityGroupId = TestUtils.FAKE_SECURITY_GROUP_ID;

        String expected = NETWORK_PREFIX_ENDPOINT
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT
                + QUERY_SECURITY_GROUP_ID
                + securityGroupId;

        // exercise
        String endpoint = this.plugin.buildQueryEndpointBySecurityGroupId(securityGroupId);

        // verify
        Assert.assertEquals(expected, endpoint);
    }

    // test case: When calling doRequestSecurityRule method, it must verify if the
    // call was successful.
    @Test
    public void testDoRequestSecurityRule() throws Exception {
        // set up
        String endpoint = NETWORK_PREFIX_ENDPOINT
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.SECURITY_GROUP_RULES_ENDPOINT;

        String requestJson = TestUtils.ANY_VALUE;
        CreateSecurityRuleRequest request = Mockito.mock(CreateSecurityRuleRequest.class);
        Mockito.when(request.toJson()).thenReturn(requestJson);

        String responseJson = TestUtils.ANY_VALUE;
        Mockito.doReturn(responseJson).when(this.plugin).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(requestJson), Mockito.eq(this.cloudUser));

        String securityRuleId = TestUtils.FAKE_SECURITY_RULE_ID;
        CreateSecurityRuleResponse response = Mockito.mock(CreateSecurityRuleResponse.class);
        Mockito.when(response.getId()).thenReturn(securityRuleId);

        PowerMockito.mockStatic(CreateSecurityRuleResponse.class);
        PowerMockito.doReturn(response).when(CreateSecurityRuleResponse.class, "fromJson",
                Mockito.eq(responseJson));

        // exercise
        this.plugin.doRequestSecurityRule(request, this.cloudUser);

        // verify
        Mockito.verify(request, Mockito.times(TestUtils.RUN_ONCE)).toJson();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doPostRequest(Mockito.eq(endpoint), Mockito.eq(requestJson),
                Mockito.eq(this.cloudUser));

        PowerMockito.verifyStatic(CreateSecurityRuleResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        CreateSecurityRuleResponse.fromJson(Mockito.eq(responseJson));

        Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).getId();
    }

    // test case: When calling retrieveSecurityGroupName method with a network
    // order, it must verify if the appropriate security group name was returned.
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
    // order, it must verify if the appropriate security group name was returned.
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
    // it must verify if an InvalidParameterException has been thrown.
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

    // test case: When calling retrieveSecurityGroupId method, it must verify if the
    // call was successful.
    @Test
    public void testRetrieveSecurityGroupIdSuccessfully() throws FogbowException {
        // setup
        String securityGroupName = SECURITY_GROUP_NAME;
        String endpoint = NETWORK_PREFIX_ENDPOINT
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
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

    // test case: When calling getSecurityGroupId method, it must verify if returned
    // the expected ID.
    @Test
    public void testgetSecurityGroupIdSuccessfully() throws FogbowException {
        // set up
        String securityGroupName = SECURITY_GROUP_NAME;
        String expectedId = TestUtils.FAKE_SECURITY_GROUP_ID;
        String rersponseJson = String.format(SECURITY_GROUPS_JSON_FORMAT, expectedId);
        GetSecurityGroupsResponse response = GetSecurityGroupsResponse.fromJson(rersponseJson);

        Mockito.doNothing().when(this.plugin).checkSecurityGroupsListIntegrity(Mockito.anyList(),
                Mockito.eq(securityGroupName));

        // exercise
        String securityGroupId = this.plugin.getSecurityGroupId(response, securityGroupName);

        // verify
        Assert.assertEquals(expectedId, securityGroupId);
    }

    // test case: When calling checkSecurityGroupsListIntegrity method with an empty
    // security group list, it must verify if an InvalidParameterException has been
    // thrown.
    @Test
    public void testCheckSecurityGroupsListIntegrityThrowsInstanceNotFoundException()
            throws FogbowException {
        // set up
        String securityGroupName = SECURITY_GROUP_NAME;
        List<SecurityGroup> securityGroupsList = new ArrayList<SecurityGroup>();

        String expectedMessage = String
                .format(Messages.Exception.SECURITY_GROUP_EQUALLY_NAMED_S_NOT_FOUND, securityGroupName);
        try {
            // exercise
            this.plugin.checkSecurityGroupsListIntegrity(securityGroupsList, securityGroupName);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    // test case: When calling checkSecurityGroupsListIntegrity method with a
    // security group list containing more than one element, it must verify if an
    // UnexpectedException has been thrown.
    @Test
    public void testCheckSecurityGroupsListIntegrityThrowsUnexpectedException()
            throws FogbowException {
        // set up
        String securityGroupName = SECURITY_GROUP_NAME;
        SecurityGroup securityGroup1 = Mockito.mock(SecurityGroup.class);
        SecurityGroup securityGroup2 = Mockito.mock(SecurityGroup.class);
        SecurityGroup[] securityGroups = { securityGroup1, securityGroup2 };
        List<SecurityGroup> securityGroupList = Arrays.asList(securityGroups);

        String expectedMessage = String
                .format(Messages.Exception.MULTIPLE_SECURITY_GROUPS_EQUALLY_NAMED, securityGroupName);
        try {
            // exercise
            this.plugin.checkSecurityGroupsListIntegrity(securityGroupList, securityGroupName);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }

    // test case: When calling buildQueryEndpointBySecurityGroupName method, it must
    // verify if returned the expected endpoint.
    @Test
    public void testbuildQueryEndpointBySecurityGroupNameSuccessfully() {
        // set up
        String securityGroupName = SECURITY_GROUP_NAME;

        String expected = NETWORK_PREFIX_ENDPOINT
                + OpenStackConstants.NEUTRON_V2_API_ENDPOINT
                + OpenStackConstants.SECURITY_GROUPS_ENDPOINT
                + QUERY_SECURITY_GROUP_NAME
                + securityGroupName;

        // exercise
        String endpoint = this.plugin.buildQueryEndpointBySecurityGroupName(securityGroupName);

        // verify
        Assert.assertEquals(expected, endpoint);
    }

    // test case: When calling the defineProtocol method with a valid parameter, it
    // must verify if returned the corresponding protocol.
    @Test
    public void testDefineProtocolWithAValidParameter() throws FogbowException {
        // set up
        Protocol expected = Protocol.UDP;
        String protocolStr = expected.toString();
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getProtocol()).thenReturn(protocolStr);

        // exercise
        Protocol protocol = this.plugin.defineProtocol(securityGroupRule);

        // verify
        Assert.assertEquals(expected, protocol);
    }

    // test case: When calling the defineProtocol method with an invalid parameter,
    // it must verify if returned the protocol ANY.
    @Test
    public void testDefineProtocolWithAnInvalidParameter() throws FogbowException {
        // set up
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Protocol expected = Protocol.ANY;

        // exercise
        Protocol protocol = this.plugin.defineProtocol(securityGroupRule);

        // verify
        Assert.assertEquals(expected, protocol);
    }

    // test case: When calling the defineCIDR method with a valid IPV4 address, it
    // must verify if returned the corresponding CIDR.
    @Test
    public void testDefineCIDRWithAValidIPV4Address() {
        // set up
        String expected = "192.168.0.1/24";
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getEtherType()).thenReturn(EtherType.IPv4.toString());
        Mockito.when(securityGroupRule.getCidr()).thenReturn(expected);

        // exercise
        String cidr = this.plugin.defineCIDR(securityGroupRule);

        // verify
        Assert.assertEquals(expected, cidr);
    }

    // test case: When calling the defineCIDR method with a valid IPV6 address, it
    // must verify if returned the corresponding CIDR.
    @Test
    public void testDefineCIDRWithAValidIPV6Address() {
        // set up
        String expected = "2002::1234:abcd:ffff:c0a8:101/64";
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getEtherType()).thenReturn(EtherType.IPv6.toString());
        Mockito.when(securityGroupRule.getCidr()).thenReturn(expected);

        // exercise
        String cidr = this.plugin.defineCIDR(securityGroupRule);

        // verify
        Assert.assertEquals(expected, cidr);
    }

    // test case: When calling the defineCIDR method with an invalid IPV4 parameter,
    // it must verify if returned the default IPV4 CIDR.
    @Test
    public void testDefineCIDRWithAnIvalidIPV4Parameter() {
        // set up
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getEtherType()).thenReturn(EtherType.IPv4.toString());

        String expected = CidrUtils.DEFAULT_IPV4_CIDR;

        // exercise
        String cidr = this.plugin.defineCIDR(securityGroupRule);

        // verify
        Assert.assertEquals(expected, cidr);
    }

    // test case: When calling the defineCIDR method with an invalid IPV6 parameter,
    // it must verify if returned the default IPV6 CIDR.
    @Test
    public void testDefineCIDRWithAnIvalidIPV6Parameter() {
        // set up
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getEtherType()).thenReturn(EtherType.IPv6.toString());

        String expected = CidrUtils.DEFAULT_IPV6_CIDR;

        // exercise
        String cidr = this.plugin.defineCIDR(securityGroupRule);

        // verify
        Assert.assertEquals(expected, cidr);
    }

    // test case: When calling the defineEtherType method with a valid IPV4 ether
    // type, it must verify if returned the expected value.
    @Test
    public void testDefineEtherTypeWithIPV4Value() {
        // set up
        EtherType expected = EtherType.IPv4;
        String etherTypeStr = expected.toString();
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getEtherType()).thenReturn(etherTypeStr);

        // exercise
        EtherType etherType = this.plugin.defineEtherType(securityGroupRule);

        // verify
        Assert.assertEquals(expected, etherType);
    }

    // test case: When calling the defineEtherType method with a valid IPV6 ether
    // type, it must verify if returned the expected value.
    @Test
    public void testDefineEtherTypeWithIPV6Value() {
        // set up
        EtherType expected = EtherType.IPv6;
        String etherTypeStr = expected.toString();
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getEtherType()).thenReturn(etherTypeStr);

        // exercise
        EtherType etherType = this.plugin.defineEtherType(securityGroupRule);

        // verify
        Assert.assertEquals(expected, etherType);
    }

    // test case: When calling the definePortTo method with a valid parameter, it
    // must verify if returned the corresponding value.
    @Test
    public void testDefinePortToWithAValidValue() {
        // set up
        Integer expected = 8080;
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getPortTo()).thenReturn(expected);

        // exercise
        Integer port = this.plugin.definePortTo(securityGroupRule);

        // verify
        Assert.assertEquals(expected, port);
    }

    // test case: When calling the definePortFrom method with an invalid parameter, it
    // must verify if returned the maximum port range.
    @Test
    public void testDefinePortToWithAInvalidValue() {
        // set up
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getPortTo()).thenReturn(null);

        Integer expected = OpenStackSecurityRulePlugin.MAXIMUM_PORT_RANGE;

        // exercise
        Integer port = this.plugin.definePortTo(securityGroupRule);

        // verify
        Assert.assertEquals(expected, port);
    }

    // test case: When calling the definePortFrom method with a valid parameter, it
    // must verify if returned the corresponding value.
    @Test
    public void testDefinePortFromWithAValidValue() {
        // set up
        Integer expected = 8080;
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getPortFrom()).thenReturn(expected);

        // exercise
        Integer port = this.plugin.definePortFrom(securityGroupRule);

        // verify
        Assert.assertEquals(expected, port);
    }

    // test case: When calling the definePortFrom method with an invalid parameter, it
    // must verify if returned the minimum port range.
    @Test
    public void testDefinePortFromWithAInalidValue() {
        // set up
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getPortFrom()).thenReturn(null);

        Integer expected = OpenStackSecurityRulePlugin.MINIMUM_PORT_RANGE;

        // exercise
        Integer port = this.plugin.definePortFrom(securityGroupRule);

        // verify
        Assert.assertEquals(expected, port);
    }

    // test case: When calling the defineDirection method with an ingress parameter,
    // it must verify if returned the direction IN.
    @Test
    public void testDefineDirectionWithIngressParameter() {
        // set up
        String directionStr = "ingress";
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getDirection()).thenReturn(directionStr);

        Direction expected = Direction.IN;

        // exercise
        Direction direction = this.plugin.defineDirection(securityGroupRule);

        // verify
        Assert.assertEquals(expected, direction);
    }

    // test case: When calling the defineDirection method with an egress parameter,
    // it must verify if returned the direction OUT.
    @Test
    public void testDefineDirectionWithEgressParameter() {
        // set up
        String directionStr = "egress";
        SecurityGroupRule securityGroupRule = Mockito.mock(SecurityGroupRule.class);
        Mockito.when(securityGroupRule.getDirection()).thenReturn(directionStr);

        Direction expected = Direction.OUT;

        // exercise
        Direction direction = this.plugin.defineDirection(securityGroupRule);

        // verify
        Assert.assertEquals(expected, direction);
    }

    private SecurityRuleInstance createSecurityRuleInstance() {
        String id = TestUtils.FAKE_SECURITY_RULE_ID;
        Direction direction = Direction.IN;
        int portFrom = OpenStackSecurityRulePlugin.MINIMUM_PORT_RANGE;
        int portTo = OpenStackSecurityRulePlugin.MAXIMUM_PORT_RANGE;
        String cidr = TestUtils.DEFAULT_CIDR;
        EtherType etherType = EtherType.IPv4;
        Protocol protocol = Protocol.TCP;
        return new SecurityRuleInstance(id, direction, portFrom, portTo, cidr, etherType, protocol);
    }
    private SecurityRule createSecurityRule() {
        Direction direction = Direction.IN;
        String cidr = TestUtils.DEFAULT_CIDR;
        int portFrom = OpenStackSecurityRulePlugin.MINIMUM_PORT_RANGE;
        int portTo = OpenStackSecurityRulePlugin.MAXIMUM_PORT_RANGE;
        EtherType etherType = EtherType.IPv4;
        Protocol protocol = Protocol.TCP;
        return new SecurityRule(direction, portFrom, portTo, cidr, etherType, protocol);
    }

    private GetSecurityRulesResponse generateGetSecurityRulesResponse() {
        String json = "{\"security_group_rules\": [{"
                + " \"id\": \"fake-security-rule-id\", "
                + " \"remote_ip_prefix\": null,"
                + " \"port_range_min\": 0,"
                + " \"port_range_max\": 65535,"
                + " \"direction\": \"ingress\","
                + " \"ethertype\": \"IPv4\","
                + " \"protocol\": \"tcp\""
                + "}]}";

        return GetSecurityRulesResponse.fromJson(json);
    }

}
