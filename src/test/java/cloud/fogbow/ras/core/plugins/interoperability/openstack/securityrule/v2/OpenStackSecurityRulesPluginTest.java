package cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.http.*;
import org.apache.http.client.HttpResponseException;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Properties;

@PrepareForTest({
        DatabaseManager.class,
        PropertiesUtil.class,
        CreateSecurityRuleResponse.class,
        OpenStackHttpToFogbowExceptionMapper.class
})
public class OpenStackSecurityRulesPluginTest extends BaseUnitTests {

    private static final String ANY_STRING = "any-string";
    private static final String ANY_URL = "http://localhost:8007";
    private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";
    private static final String DEFAULT_NETWORK_URL = "http://localhost:0000";
    private static final String SECURITY_RULE_ID = "securityRuleId";
    private static final String SECURITY_GROUP_ID = "securityGroupId";

    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_CLOUD_NAME = "fake-cloud-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_MEMBER_ID = "fake-member-id";
    private static final String FAKE_GATEWAY = "fake-gateway";
    private static final String FAKE_ADDRESS = "fake-address";
    private static final String MAP_METHOD = "map";


    private OpenStackSecurityRulePlugin plugin;
    private OpenStackV3User cloudUser;
    private Properties properties;
    private OpenStackHttpClient clientMock;
    private NetworkOrder majorOrder;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        this.testUtils.mockReadOrdersFromDataBase();
        String confFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.properties = Mockito.mock(Properties.class);
        Mockito.when(this.properties.get(NETWORK_NEUTRONV2_URL_KEY)).thenReturn(DEFAULT_NETWORK_URL);

        PowerMockito.mockStatic(PropertiesUtil.class);
        BDDMockito.when(PropertiesUtil.readProperties(Mockito.eq(confFilePath))).thenReturn(properties);

        this.plugin = Mockito.spy(new OpenStackSecurityRulePlugin(confFilePath));

        this.clientMock = Mockito.mock(OpenStackHttpClient.class);
        this.plugin.setClient(this.clientMock);
        this.cloudUser = new OpenStackV3User(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);

        this.majorOrder = testUtils.createNetworkOrder(testUtils.getLocalMemberId(), testUtils.FAKE_REMOTE_MEMBER_ID);
    }

    // test case: The http client must make only 1 request
    @Test
    public void testRequestSecurityRule() throws Exception {
        // set up
        Mockito.doReturn(ANY_STRING).when(this.plugin)
                .retrieveSecurityGroupName(Mockito.eq(majorOrder));

        Mockito.doReturn(SECURITY_GROUP_ID).when(this.plugin).
                retrieveSecurityGroupId(Mockito.anyString(), Mockito.eq(cloudUser));

        Mockito.doReturn(ANY_STRING).when(this.plugin)
                .doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.eq(this.cloudUser));

        CreateSecurityRuleResponse createSecurityRuleResponseMock = Mockito.mock(CreateSecurityRuleResponse.class);
        Mockito.when(createSecurityRuleResponseMock.getId()).thenReturn(ANY_STRING);

        PowerMockito.mockStatic(CreateSecurityRuleResponse.class);
        BDDMockito.when(CreateSecurityRuleResponse.fromJson(Mockito.eq(ANY_STRING)))
                .thenReturn(createSecurityRuleResponseMock);

        SecurityRule securityRule = createEmptySecurityRule();

        // exercise
        this.plugin.requestSecurityRule(securityRule, majorOrder, this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(1))
                .retrieveSecurityGroupName(Mockito.eq(majorOrder));

        Mockito.verify(this.plugin, Mockito.times(1))
                .retrieveSecurityGroupId(Mockito.anyString(), Mockito.eq(cloudUser));

        Mockito.verify(this.plugin, Mockito.times(1))
                .doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.verifyStatic(CreateSecurityRuleResponse.class);
        CreateSecurityRuleResponse.fromJson(Mockito.eq(ANY_STRING));
    }

    // test case: when calling doDeleteRequest() method, it must verify that the
    // call was successful.
    @Test
    public void testDoDeleteSuccessfully() throws Exception {
        // exercise
        this.plugin.doDeleteRequest(ANY_URL, cloudUser);

        // verify
        Mockito.verify(this.clientMock, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteRequest(Mockito.anyString(), Mockito.any());
    }

    // test case: when doDeleteRequest() request fails, it must
    // call OpenStackHttpToFogbowExceptionMapper.map(e)
    @Test
    public void testDoDeleteUnsuccessfully() throws Exception {
        // set up
        HttpResponseException exception = testUtils.getHttpInternalServerErrorResponseException();
        Mockito.doThrow(exception).when(this.clientMock)
                .doDeleteRequest(Mockito.anyString(), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, MAP_METHOD, Mockito.any());

        // exercise
        try {
            this.plugin.doDeleteRequest(ANY_URL, cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            // verify
            Assert.assertEquals(exception.getMessage(), e.getMessage());

            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(exception));
        }
    }

    // test case: Tests if an exception will be thrown in case that openstack raise an error in security rule request.
    @Test
    public void testRequestSecurityRuleNetworkError() throws Exception {
        // set up
        String securityGroupsResponse = "{\n" +
                "    \"security_groups\": [\n" +
                "        {\n" +
                "            \"id\": \"85cc3048-abc3-43cc-89b3-377341426ac5\"\n" +
                "        }\n" +
                "    ]\n" +
                "}";

        HttpResponseException toBeThrown = new HttpResponseException(HttpStatus.SC_BAD_REQUEST, "");

        Mockito.doReturn(securityGroupsResponse).when(this.clientMock).
                doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        Mockito.doThrow(toBeThrown).when(this.clientMock).
                doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        SecurityRule securityRule = createEmptySecurityRule();

        // exercise
        try {
            this.plugin.requestSecurityRule(securityRule, majorOrder,
                    this.cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            // throws a FogbowException, as expected
        }

        // verify
        Mockito.verify(this.clientMock, Mockito.times(1)).
                doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        Mockito.verify(this.clientMock, Mockito.times(1)).
                doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));
    }

    //test case: Tests get security rule from json response
    @Test
    public void testGetSecurityRuleFromJson() throws Exception {
        //set up
        String id = "securityRuleId";
        String cidr = "0.0.0.0";
        int portFrom = 0;
        int portTo = 0;
        String direction = "egress";
        String etherType = "IPv4";
        String protocol = "tcp";

        // Generating security rule response string
        JSONObject securityRuleContentJsonObject = generateJsonResponseForSecurityRules(id, cidr, portFrom, portTo,
                direction, etherType, protocol);

        Mockito.doReturn(securityRuleContentJsonObject.toString()).when(this.clientMock).
                doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));
        Mockito.doReturn(SECURITY_GROUP_ID).when(this.plugin).
                retrieveSecurityGroupId(Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        //exercise
        List<SecurityRuleInstance> securityRuleInstances = this.plugin.getSecurityRules(majorOrder,
                this.cloudUser);
        SecurityRuleInstance securityRuleInstance = securityRuleInstances.get(0);

        //verify
        Assert.assertEquals(id, securityRuleInstance.getId());
        Assert.assertEquals(cidr, securityRuleInstance.getCidr());
        Assert.assertEquals(portFrom, securityRuleInstance.getPortFrom());
        Assert.assertEquals(portTo, securityRuleInstance.getPortTo());
        Assert.assertEquals(direction, securityRuleInstance.getDirection().toString());
        Assert.assertEquals(etherType, securityRuleInstance.getEtherType().toString());
        Assert.assertEquals(protocol, securityRuleInstance.getProtocol().toString());
    }

    //test case: Tests remove security rule
    @Test
    public void testRemoveInstance() throws IOException, JSONException, FogbowException {
        //set up
        String suffixEndpointSecurityRules = OpenStackSecurityRulePlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + "/" +
                SECURITY_RULE_ID;

        Mockito.doNothing().when(this.clientMock).doDeleteRequest(
                Mockito.endsWith(suffixEndpointSecurityRules), Mockito.eq(this.cloudUser));

        //exercise
        this.plugin.deleteSecurityRule(SECURITY_RULE_ID, this.cloudUser);

        //verify
        Mockito.verify(this.clientMock, Mockito.times(1)).doDeleteRequest(
                Mockito.endsWith(suffixEndpointSecurityRules), Mockito.eq(this.cloudUser));
    }

    private SecurityRule createEmptySecurityRule() {
        return new SecurityRule(SecurityRule.Direction.OUT, 0, 0, "0.0.0.0/0 ", SecurityRule.EtherType.IPv4, SecurityRule.Protocol.TCP);
    }

    private JSONObject generateJsonResponseForSecurityRules(String securityGroupId, String cidr, int portFrom, int portTo,
                                                            String direction, String etherType, String protocol) {
        JSONObject securityRuleContentJsonObject = new JSONObject();

        securityRuleContentJsonObject.put(OpenStackConstants.Network.ID_KEY_JSON, securityGroupId);
        securityRuleContentJsonObject.put(OpenStackConstants.Network.REMOTE_IP_PREFIX_KEY_JSON, cidr);
        securityRuleContentJsonObject.put(OpenStackConstants.Network.MAX_PORT_KEY_JSON, portTo);
        securityRuleContentJsonObject.put(OpenStackConstants.Network.MIN_PORT_KEY_JSON, portFrom);
        securityRuleContentJsonObject.put(OpenStackConstants.Network.DIRECTION_KEY_JSON, direction);
        securityRuleContentJsonObject.put(OpenStackConstants.Network.ETHER_TYPE_KEY_JSON, etherType);
        securityRuleContentJsonObject.put(OpenStackConstants.Network.PROTOCOL_KEY_JSON, protocol);

        JSONArray securityRulesJsonArray = new JSONArray();
        securityRulesJsonArray.add(securityRuleContentJsonObject);

        JSONObject securityRulesContentJsonObject = new JSONObject();
        securityRulesContentJsonObject.put(OpenStackConstants.Network.SECURITY_GROUP_RULES_KEY_JSON,
                securityRulesJsonArray);

        return securityRulesContentJsonObject;
    }
}
