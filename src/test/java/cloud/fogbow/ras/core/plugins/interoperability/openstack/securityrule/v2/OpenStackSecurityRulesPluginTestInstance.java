package cloud.fogbow.ras.core.plugins.interoperability.openstack.securityrule.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.http.*;
import org.apache.http.client.HttpResponseException;
import org.apache.http.message.BasicStatusLine;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OpenStackSecurityRulesPluginTestInstance {

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

    private OpenStackSecurityRulePlugin openStackSecurityRulePlugin;
    private OpenStackV3User openStackV3Token;
    private HttpRequestClient clientUtil;
    private Properties properties;
    private OpenStackHttpClient openStackHttpClient;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.put(NETWORK_NEUTRONV2_URL_KEY, DEFAULT_NETWORK_URL);

        String confFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.openStackSecurityRulePlugin = Mockito.spy(new OpenStackSecurityRulePlugin(confFilePath));

        this.clientUtil = Mockito.mock(HttpRequestClient.class);
        this.openStackHttpClient = Mockito.spy(new OpenStackHttpClient());
        this.openStackSecurityRulePlugin.setClient(this.openStackHttpClient);
        this.openStackV3Token = new OpenStackV3User(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);
    }

    @After
    public void validate() {
        Mockito.validateMockitoUsage();
    }

    // test case: The http client must make only 1 request
    @Test
    public void testRequestSecurityRule() throws Exception {
        // set up
        // post network
        String createSecurityRuleResponse = new CreateSecurityRuleResponse(
                new CreateSecurityRuleResponse.SecurityRule(SECURITY_RULE_ID)).toJson();
        Mockito.doReturn(createSecurityRuleResponse).when(this.openStackHttpClient)
                .doPostRequest(Mockito.endsWith(OpenStackSecurityRulePlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES),
                        Mockito.anyString(), Mockito.eq(this.openStackV3Token));

        Mockito.doReturn(null).when(this.openStackSecurityRulePlugin).
                getSecurityRulesFromJson(Mockito.anyString());
        Mockito.doReturn(SECURITY_GROUP_ID).when(this.openStackSecurityRulePlugin).
                retrieveSecurityGroupId(Mockito.anyString(), Mockito.any(OpenStackV3User.class));
        SecurityRule securityRule = createEmptySecurityRule();
        NetworkOrder order = createNetworkOrder();

        // exercise
        this.openStackSecurityRulePlugin.requestSecurityRule(securityRule, order, this.openStackV3Token);

        // verify
        Mockito.verify(this.openStackHttpClient, Mockito.times(1)).doPostRequest(
                Mockito.endsWith(OpenStackSecurityRulePlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES),
                Mockito.anyString(), Mockito.eq(this.openStackV3Token));
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

        Mockito.doReturn(securityGroupsResponse).when(this.openStackHttpClient).
                doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        Mockito.doThrow(toBeThrown).when(this.openStackHttpClient).
                doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        SecurityRule securityRule = createEmptySecurityRule();
        NetworkOrder order = createNetworkOrder();

        // exercise
        try {
            this.openStackSecurityRulePlugin.requestSecurityRule(securityRule, order,
                    this.openStackV3Token);
            Assert.fail();
        } catch (FogbowException e) {
            // throws a FogbowException, as expected
        }

        // verify
        Mockito.verify(this.openStackHttpClient, Mockito.times(1)).
                doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        Mockito.verify(this.openStackHttpClient, Mockito.times(1)).
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
        NetworkOrder order = createNetworkOrder();

        // Generating security rule response string
        JSONObject securityRuleContentJsonObject = generateJsonResponseForSecurityRules(id, cidr, portFrom, portTo,
                direction, etherType, protocol);

        Mockito.doReturn(securityRuleContentJsonObject.toString()).when(this.openStackHttpClient).
                doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));
        Mockito.doReturn(SECURITY_GROUP_ID).when(this.openStackSecurityRulePlugin).
                retrieveSecurityGroupId(Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        //exercise
        List<SecurityRuleInstance> securityRuleInstances = this.openStackSecurityRulePlugin.getSecurityRules(order,
                this.openStackV3Token);
        SecurityRuleInstance securityRuleInstance = securityRuleInstances.get(0);

        //verify
        Assert.assertEquals(id, securityRuleInstance.getId());
        Assert.assertEquals(cidr, securityRuleInstance.getRule().getCidr());
        Assert.assertEquals(portFrom, securityRuleInstance.getRule().getPortFrom());
        Assert.assertEquals(portTo, securityRuleInstance.getRule().getPortTo());
        Assert.assertEquals(direction, securityRuleInstance.getRule().getDirection().toString());
        Assert.assertEquals(etherType, securityRuleInstance.getRule().getEtherType().toString());
        Assert.assertEquals(protocol, securityRuleInstance.getRule().getProtocol().toString());
    }

    //test case: Tests remove security rule
    @Test
    public void testRemoveInstance() throws IOException, JSONException, FogbowException {
        //set up
        String suffixEndpointSecurityRules = OpenStackSecurityRulePlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + "/" +
                SECURITY_RULE_ID;

        Mockito.doNothing().when(this.openStackHttpClient).doDeleteRequest(
                Mockito.endsWith(suffixEndpointSecurityRules), Mockito.eq(this.openStackV3Token));

        //exercise
        this.openStackSecurityRulePlugin.deleteSecurityRule(SECURITY_RULE_ID, this.openStackV3Token);

        //verify
        Mockito.verify(this.openStackHttpClient, Mockito.times(1)).doDeleteRequest(
                Mockito.endsWith(suffixEndpointSecurityRules), Mockito.eq(this.openStackV3Token));
    }

    private SecurityRule createEmptySecurityRule() {
        return new SecurityRule(SecurityRule.Direction.OUT, 0, 0, "0.0.0.0/0 ", SecurityRule.EtherType.IPv4, SecurityRule.Protocol.TCP);
    }

    private HttpResponse createHttpResponse(String content, int httpStatus) throws IOException {
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        InputStream inputStrem = new ByteArrayInputStream(content.getBytes(UTF_8));

        Mockito.when(httpEntity.getContent()).thenReturn(inputStrem);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);

        StatusLine statusLine = new BasicStatusLine(new ProtocolVersion("", 0, 0), httpStatus, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);

        return httpResponse;
    }

    private NetworkOrder createNetworkOrder() throws Exception {
        SystemUser systemUser = new SystemUser(FAKE_USER_ID, FAKE_USER_NAME, FAKE_TOKEN_PROVIDER);

        NetworkOrder order = new NetworkOrder(systemUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID, FAKE_CLOUD_NAME,
                FAKE_NAME, FAKE_GATEWAY, FAKE_ADDRESS, NetworkAllocationMode.STATIC);

        NetworkInstance networtkInstanceExcepted = new NetworkInstance(order.getId());
        order.setInstanceId(networtkInstanceExcepted.getId());
        return order;
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
