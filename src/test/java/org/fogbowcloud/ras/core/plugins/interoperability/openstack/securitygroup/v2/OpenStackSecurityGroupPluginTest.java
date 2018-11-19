package org.fogbowcloud.ras.core.plugins.interoperability.openstack.securitygroup.v2;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicStatusLine;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.NetworkInstance;
import org.fogbowcloud.ras.core.models.orders.NetworkAllocationMode;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.securitygroups.*;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenstackRestApiConstants;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

public class OpenStackSecurityGroupPluginTest {

    private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";
    private static final String DEFAULT_NETWORK_URL = "http://localhost:0000";
    private static final String SECURITY_GROUP_RULE_ID = "securityGroupRuleId";
    private static final String SECURITY_GROUP_ID = "securityGroupId";

    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_FEDERATION_TOKEN_VALUE = "federation-token-value";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_MEMBER_ID = "fake-member-id";
    private static final String FAKE_GATEWAY = "fake-gateway";
    private static final String FAKE_ADDRESS = "fake-address";

    private static final String SUFFIX_ENDPOINT_SECURITY_GROUP_RULES = OpenStackSecurityGroupPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES  +
            OpenStackSecurityGroupPlugin.QUERY_PREFIX;

    private OpenStackSecurityGroupPlugin openStackSecurityGroupPlugin;
    private OpenStackV3Token defaultLocalUserAttributes;
    private HttpClient client;
    private Properties properties;
    private HttpRequestClientUtil httpRequestClientUtil;

    @Before
    public void setUp() throws InvalidParameterException {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.put(NETWORK_NEUTRONV2_URL_KEY, DEFAULT_NETWORK_URL);
        this.openStackSecurityGroupPlugin = Mockito.spy(new OpenStackSecurityGroupPlugin());

        this.client = Mockito.mock(HttpClient.class);
        this.httpRequestClientUtil = Mockito.spy(new HttpRequestClientUtil(this.client));
        this.openStackSecurityGroupPlugin.setClient(this.httpRequestClientUtil);

        this.defaultLocalUserAttributes = new OpenStackV3Token(FAKE_TOKEN_PROVIDER, FAKE_TOKEN_VALUE, FAKE_USER_ID,
                FAKE_NAME, FAKE_PROJECT_ID, null);
    }

    @After
    public void validate() {
        Mockito.validateMockitoUsage();
    }

    //test case: The http client must make only 1 request
    @Test
    public void testRequestSecurityGroupRule() throws Exception {
        //set up
        // post network
        String createSecurityGroupRuleResponse = new CreateSecurityGroupRuleResponse(
                new CreateSecurityGroupRuleResponse.SecurityGroupRule(SECURITY_GROUP_RULE_ID)).toJson();
        Mockito.doReturn(createSecurityGroupRuleResponse).when(this.httpRequestClientUtil)
                .doPostRequest(Mockito.endsWith(OpenStackSecurityGroupPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES),
                        Mockito.eq(this.defaultLocalUserAttributes), Mockito.anyString());

        Mockito.doReturn(null).when(this.openStackSecurityGroupPlugin).
                getSecurityGroupRulesFromJson(Mockito.anyString());
        Mockito.doReturn(SECURITY_GROUP_ID).when(this.openStackSecurityGroupPlugin).
                retrieveSecurityGroupId(Mockito.anyString(), Mockito.any(OpenStackV3Token.class));
        SecurityGroupRule securityGroupRule = createEmptySecurityGroupRule();
        NetworkOrder order = createNetworkOrder();

        //exercise
        this.openStackSecurityGroupPlugin.requestSecurityGroupRule(securityGroupRule, order,
                this.defaultLocalUserAttributes);

        //verify
        Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(
                Mockito.endsWith(OpenStackSecurityGroupPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES),
                Mockito.eq(this.defaultLocalUserAttributes), Mockito.anyString());
    }

    //test case: Tests if an exception will be thrown in case that openstack raise an error in security group rule request.
    @Test
    public void testRequestSecurityGroupRuleNetworkError() throws Exception {
        //set up
        HttpResponse httpResponsePostNetwork = createHttpResponse("", HttpStatus.SC_BAD_REQUEST);
        Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponsePostNetwork);
        SecurityGroupRule securityGroupRule = createEmptySecurityGroupRule();
        NetworkOrder order = createNetworkOrder();

        //exercise
        try {
            this.openStackSecurityGroupPlugin.requestSecurityGroupRule(securityGroupRule, order,
                    this.defaultLocalUserAttributes);
            Assert.fail();
        } catch (FogbowRasException e) {
            // Throws an exception, as expected
        } catch (Exception e) {
            Assert.fail();
        }

        //verify
        Mockito.verify(this.client, Mockito.times(1)).execute(Mockito.any(HttpUriRequest.class));
    }

    //test case: Tests get security group rule from json response
    @Test
    public void testGetSecurityGroupRuleFromJson() throws Exception {
        //set up
        String id = "securityGroupRuleId";
        String cidr = "0.0.0.0";
        int portFrom = 0;
        int portTo = 0;
        String direction = "egress";
        String etherType = "IPv4";
        String protocol = "tcp";
        NetworkOrder order = createNetworkOrder();

        // Generating security group rule response string
        JSONObject securityGroupRuleContentJsonObject = generateJsonResponseForSecurityGroupRules(id, cidr, portFrom, portTo,
                direction, etherType, protocol);

        HttpResponse httpResponseGetSecurityGroups = createHttpResponse(securityGroupRuleContentJsonObject.toString(), HttpStatus.SC_OK);
        Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(httpResponseGetSecurityGroups);
        Mockito.doReturn(SECURITY_GROUP_ID).when(this.openStackSecurityGroupPlugin).
                retrieveSecurityGroupId(Mockito.anyString(), Mockito.any(OpenStackV3Token.class));

        //exercise
        List<SecurityGroupRule> securityGroups = this.openStackSecurityGroupPlugin.getSecurityGroupRules(order,
                this.defaultLocalUserAttributes);
        SecurityGroupRule securityGroupRule = securityGroups.get(0);

        //verify
        Assert.assertEquals(id, securityGroupRule.getInstanceId());
        Assert.assertEquals(cidr, securityGroupRule.getCidr());
        Assert.assertEquals(portFrom, securityGroupRule.getPortFrom());
        Assert.assertEquals(portTo, securityGroupRule.getPortTo());
        Assert.assertEquals(direction, securityGroupRule.getDirection().toString());
        Assert.assertEquals(etherType, securityGroupRule.getEtherType().toString());
        Assert.assertEquals(protocol, securityGroupRule.getProtocol().toString());
    }

    //test case: Tests remove security group rule
    @Test
    public void testRemoveInstance() throws IOException, JSONException, FogbowRasException, UnexpectedException {
        //set up
        String suffixEndpointSecurityGroup = OpenStackSecurityGroupPlugin.SUFFIX_ENDPOINT_SECURITY_GROUP_RULES + "/" +
                SECURITY_GROUP_RULE_ID;

        Mockito.doNothing().when(this.httpRequestClientUtil).doDeleteRequest(
                Mockito.endsWith(suffixEndpointSecurityGroup), Mockito.eq(this.defaultLocalUserAttributes));

        //exercise
        this.openStackSecurityGroupPlugin.deleteSecurityGroupRule(SECURITY_GROUP_RULE_ID, this.defaultLocalUserAttributes);

        //verify
        Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doDeleteRequest(
                Mockito.endsWith(suffixEndpointSecurityGroup), Mockito.eq(this.defaultLocalUserAttributes));
    }

    @Test
    public void testRetrieveSecurityGroupId() {
        
    }

    private SecurityGroupRule createEmptySecurityGroupRule() {
        return new SecurityGroupRule(Direction.OUT, 0, 0, "0.0.0.0/0 ", EtherType.IPv4, Protocol.TCP);
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
        FederationUserToken federationUserToken = new FederationUserToken(FAKE_TOKEN_PROVIDER,
                FAKE_FEDERATION_TOKEN_VALUE,
                FAKE_USER_ID, FAKE_USER_NAME);
        NetworkOrder order = new NetworkOrder(federationUserToken, FAKE_MEMBER_ID, FAKE_MEMBER_ID,
                FAKE_NAME, FAKE_GATEWAY, FAKE_ADDRESS, NetworkAllocationMode.STATIC);

        NetworkInstance networtkInstanceExcepted = new NetworkInstance(order.getId());
        order.setInstanceId(networtkInstanceExcepted.getId());
        return order;
    }

    private JSONObject generateJsonResponseForSecurityGroupRules(String securityGroupId, String cidr, int portFrom, int portTo,
                                                                 String direction, String etherType, String protocol) {
        JSONObject securityGroupRuleContentJsonObject = new JSONObject();

        securityGroupRuleContentJsonObject.put(OpenstackRestApiConstants.Network.ID_KEY_JSON, securityGroupId);
        securityGroupRuleContentJsonObject.put(OpenstackRestApiConstants.Network.REMOTE_IP_PREFIX_KEY_JSON, cidr);
        securityGroupRuleContentJsonObject.put(OpenstackRestApiConstants.Network.MAX_PORT_KEY_JSON, portTo);
        securityGroupRuleContentJsonObject.put(OpenstackRestApiConstants.Network.MIN_PORT_KEY_JSON, portFrom);
        securityGroupRuleContentJsonObject.put(OpenstackRestApiConstants.Network.DIRECTION_KEY_JSON, direction);
        securityGroupRuleContentJsonObject.put(OpenstackRestApiConstants.Network.ETHER_TYPE_KEY_JSON, etherType);
        securityGroupRuleContentJsonObject.put(OpenstackRestApiConstants.Network.PROTOCOL_KEY_JSON, protocol);

        JSONArray securityGroupRulesJsonArray = new JSONArray();
        securityGroupRulesJsonArray.add(securityGroupRuleContentJsonObject);

        JSONObject securityGroupRulesContentJsonObject = new JSONObject();
        securityGroupRulesContentJsonObject.put(OpenstackRestApiConstants.Network.SECURITY_GROUP_RULES_KEY_JSON,
                securityGroupRulesJsonArray);

        return securityGroupRulesContentJsonObject;
    }
}
