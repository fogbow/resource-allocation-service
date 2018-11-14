package org.fogbowcloud.ras.requests.api.local.http;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.fogbowcloud.ras.api.http.Network;
import org.fogbowcloud.ras.api.http.PublicIp;
import org.fogbowcloud.ras.api.http.SecurityGroupRuleAPI;
import org.fogbowcloud.ras.core.ApplicationFacade;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.securitygroups.SecurityGroupRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = SecurityGroupRuleAPI.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class SecurityGroupRuleAPITest {

    private static final String CORRECT_BODY =
            "{\n" +
                "  \"direction\": \"IN\",\n" +
                "  \"portFrom\": 10000,\n" +
                "  \"portTo\": 11000,\n" +
                "  \"cidr\": \"10.150.10.0/24\",\n" +
                "  \"etherType\": \"IPv4\",\n" +
                "  \"protocol\": \"TCP\"\n" +
            "}";

    private static final String WRONG_BODY = "";
    private static final String FAKE_SG_RULE_ID = "fake-sg-rule-id";
    private static final String FAKE_ORDER_ID = "fakeOrderId";

    @Autowired
    private MockMvc mockMvc;

    private ApplicationFacade facade;

    private static final String NETWORK_ENDPOINT = "/" + Network.NETWORK_ENDPOINT + "/" + FAKE_ORDER_ID + "/" +
            SecurityGroupRuleAPI.SECURITY_GROUP_RULES_ENDPOINT;
    private static final String PUBLIC_IP_ENDPOINT = "/" + PublicIp.PUBLIC_IP_ENDPOINT + "/" + FAKE_ORDER_ID + "/" +
            SecurityGroupRuleAPI.SECURITY_GROUP_RULES_ENDPOINT;

    @Before
    public void setUp() {
        this.facade = Mockito.spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
    }

    //test case: Request a security group rule creation and test successful return. Also check if the request has
    // same behaviour in both endpoints (/networks and /publicIps)
    @Test
    public void testPost() throws Exception {
        // set up
        Mockito.doReturn(FAKE_SG_RULE_ID).when(this.facade).createSecurityGroupRules(Mockito.anyString(),
                Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));

        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, NETWORK_ENDPOINT, getHttpHeaders(),
                CORRECT_BODY);

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.CREATED.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Assert.assertEquals(FAKE_SG_RULE_ID, result.getResponse().getContentAsString());

        //set up
        requestBuilder = createRequestBuilder(HttpMethod.POST, PUBLIC_IP_ENDPOINT, getHttpHeaders(), CORRECT_BODY);
        Mockito.doReturn(FAKE_SG_RULE_ID).when(this.facade).createSecurityGroupRules(Mockito.anyString(),
                Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise: Make the request
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Assert.assertEquals(FAKE_SG_RULE_ID, result.getResponse().getContentAsString());
        Mockito.verify(this.facade, Mockito.times(2)).createSecurityGroupRules(
                Mockito.anyString(), Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.any(ResourceType.class));
    }

    // test case: Request a security group rule creation and test bad request return. Also check if the request has
    // same behaviour in both endpoints (/networks and /publicIps)
    @Test
    public void testWrongPost() throws Exception {
        // set up
        Mockito.doReturn(FAKE_SG_RULE_ID).when(this.facade).createSecurityGroupRules(Mockito.anyString(),
                Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));

        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, NETWORK_ENDPOINT, getHttpHeaders(),
                WRONG_BODY);

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.BAD_REQUEST.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        // set up
        requestBuilder = createRequestBuilder(HttpMethod.POST, PUBLIC_IP_ENDPOINT, getHttpHeaders(),
                WRONG_BODY);
        Mockito.doReturn(FAKE_SG_RULE_ID).when(this.facade).createSecurityGroupRules(Mockito.anyString(),
                Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise: Make the request
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(0)).createSecurityGroupRules(
                Mockito.anyString(), Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.any(ResourceType.class));
    }

    // test case: Request a security group rule creation with an unauthorized user. Also check if the request has
    // same behaviour in both endpoints (/networks and /publicIps)
    @Test
    public void testCreateSecurityGroupRuleUnauthorizedException() throws Exception {
        // set up
        Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).createSecurityGroupRules(Mockito.anyString(),
                Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));

        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, NETWORK_ENDPOINT, getHttpHeaders(),
                CORRECT_BODY);

        // exercise: Make the request
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.FORBIDDEN.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        // set up
        requestBuilder = createRequestBuilder(HttpMethod.POST, PUBLIC_IP_ENDPOINT, getHttpHeaders(),
                CORRECT_BODY);
        Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).createSecurityGroupRules(Mockito.anyString(),
                Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise: Make the request
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(2)).createSecurityGroupRules(
                Mockito.anyString(), Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.any(ResourceType.class));
    }

    // test case: Request a security group rule creation with an unauthenticated user. Also check if the request has
    // same behaviour in both endpoints (/networks and /publicIps)
    @Test
    public void testCreateSecurityGroupRuleUnauthenticatedException() throws Exception {
        // set up
        Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).createSecurityGroupRules(
                Mockito.anyString(), Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.POST, NETWORK_ENDPOINT, getHttpHeaders(),
                CORRECT_BODY);

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.UNAUTHORIZED.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        // set up
        requestBuilder = createRequestBuilder(HttpMethod.POST, PUBLIC_IP_ENDPOINT, getHttpHeaders(),
                CORRECT_BODY);
        Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).createSecurityGroupRules(
                Mockito.anyString(), Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Mockito.verify(this.facade, Mockito.times(2)).createSecurityGroupRules(
                Mockito.anyString(), Mockito.any(SecurityGroupRule.class), Mockito.anyString(), Mockito.any(ResourceType.class));
    }


    // test case: Request the list of all security group rules when the facade returns an empty list. Also check if
    // the request has same behaviour in both endpoints (/networks and /publicIps)
    @Test
    public void testGetAllSecurityGroupRulesEmptyList() throws Exception {
        // set up
        Mockito.doReturn(new ArrayList<SecurityGroupRule>()).when(this.facade).getAllSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, NETWORK_ENDPOINT, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        String expectedResult = "[]";
        Assert.assertEquals(expectedResult, result.getResponse().getContentAsString());

        // set up
        requestBuilder = createRequestBuilder(HttpMethod.GET, PUBLIC_IP_ENDPOINT, getHttpHeaders(), "");
        Mockito.doReturn(new ArrayList<SecurityGroupRule>()).when(this.facade).getAllSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Assert.assertEquals(expectedResult, result.getResponse().getContentAsString());
        Mockito.verify(this.facade, Mockito.times(2)).getAllSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.any(ResourceType.class));
    }


    // test case: Request the list of all security group rules status when the facade returns a non-empty list.
    // Also check if the request has same behaviour in both endpoints (/networks and /publicIps)
    @Test
    public void testGetAllSecurityGroupRulesWhenHasData() throws Exception {
        // set up
        final String FAKE_ID_1 = "fake-Id-1";
        final String FAKE_ID_2 = "fake-Id-2";
        final String FAKE_ID_3 = "fake-Id-3";

        SecurityGroupRule rule1 = new SecurityGroupRule();
        rule1.setInstanceId(FAKE_ID_1);
        SecurityGroupRule rule2 = new SecurityGroupRule();
        rule2.setInstanceId(FAKE_ID_2);
        SecurityGroupRule rule3 = new SecurityGroupRule();
        rule3.setInstanceId(FAKE_ID_3);

        List<SecurityGroupRule> securityGroupRuleList = Arrays.asList(new SecurityGroupRule[]{rule1, rule2, rule3});
        Mockito.doReturn(securityGroupRuleList).when(this.facade).getAllSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));

        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, NETWORK_ENDPOINT, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        TypeToken<List<SecurityGroupRule>> token = new TypeToken<List<SecurityGroupRule>>() {};

        List<SecurityGroupRule> resultList = new Gson().fromJson(result.getResponse().getContentAsString(),
                token.getType());
        Assert.assertEquals(3, resultList.size());
        Assert.assertEquals(FAKE_ID_1, resultList.get(0).getInstanceId());
        Assert.assertEquals(FAKE_ID_2, resultList.get(1).getInstanceId());
        Assert.assertEquals(FAKE_ID_3, resultList.get(2).getInstanceId());

        // set up
        requestBuilder = createRequestBuilder(HttpMethod.GET, NETWORK_ENDPOINT, getHttpHeaders(), "");
        Mockito.doReturn(securityGroupRuleList).when(this.facade).getAllSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        resultList = new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());
        Assert.assertEquals(3, resultList.size());
        Assert.assertEquals(FAKE_ID_1, resultList.get(0).getInstanceId());
        Assert.assertEquals(FAKE_ID_2, resultList.get(1).getInstanceId());
        Assert.assertEquals(FAKE_ID_3, resultList.get(2).getInstanceId());

        Mockito.verify(this.facade, Mockito.times(2)).getAllSecurityGroupRules(Mockito.anyString(), Mockito.anyString(), Mockito.any(ResourceType.class));
    }

    // test case: Delete a security group rule by its id and test successfully return. Also check if the request has
    // same behaviour in both endpoints (/networks and /publicIps)
    @Test
    public void testDeleteExistingSecurityGroupRule() throws Exception {
        // set up
        final String FAKE_ID = "fake-Id-1";
        Mockito.doNothing().when(this.facade).deleteSecurityGroupRules(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));
        String deleteEndpoint = NETWORK_ENDPOINT + "/" + FAKE_ID;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.DELETE, deleteEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        // set up
        deleteEndpoint = PUBLIC_IP_ENDPOINT + "/" + FAKE_ID;
        requestBuilder = createRequestBuilder(HttpMethod.DELETE, deleteEndpoint, getHttpHeaders(), "");
        Mockito.doNothing().when(this.facade).deleteSecurityGroupRules(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Mockito.verify(this.facade, Mockito.times(2)).deleteSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(ResourceType.class));
    }

    // test case: Delete a security group rule not found. Also check if the request has same behaviour in both
    // endpoints (/networks and /publicIps)
    @Test
    public void testDeleteNotFoundSecurityGroupRule() throws Exception {
        // set up
        final String FAKE_ID = "fake-Id-1";
        String deleteEndpoint = NETWORK_ENDPOINT + "/" + FAKE_ID;

        Mockito.doThrow(new InstanceNotFoundException()).when(this.facade).deleteSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.DELETE, deleteEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.NOT_FOUND.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());


        // set up
        deleteEndpoint = PUBLIC_IP_ENDPOINT + "/" + FAKE_ID;
        requestBuilder = createRequestBuilder(HttpMethod.DELETE, deleteEndpoint, getHttpHeaders(), "");
        Mockito.doThrow(new InstanceNotFoundException()).when(this.facade).deleteSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Mockito.verify(this.facade, Mockito.times(2)).deleteSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(ResourceType.class));
    }

    // test case: Delete a security group rule with unauthenticated user. Also check if the request has same behaviour
    // in both endpoints (/networks and /publicIps)
    @Test
    public void testDeleteUnauthenticatedException() throws Exception {
        // set up
        final String FAKE_ID = "fake-Id-1";
        String deleteEndpoint = NETWORK_ENDPOINT + "/" + FAKE_ID;

        Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).deleteSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.DELETE, deleteEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.UNAUTHORIZED.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        // set up
        deleteEndpoint = PUBLIC_IP_ENDPOINT + "/" + FAKE_ID;
        requestBuilder = createRequestBuilder(HttpMethod.DELETE, deleteEndpoint, getHttpHeaders(), "");
        Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).deleteSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Mockito.verify(this.facade, Mockito.times(2)).deleteSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(ResourceType.class));
    }

    // test case: Delete a security group rule with unauthorized user. Also check if the request has same behaviour
    // in both endpoints (/networks and /publicIps)
    @Test
    public void testDeleteUnauthorizedException() throws Exception {
        // set up
        final String FAKE_ID = "fake-Id-1";
        String deleteEndpoint = NETWORK_ENDPOINT + "/" + FAKE_ID;
        Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).deleteSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.DELETE, deleteEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.FORBIDDEN.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        // set up
        requestBuilder = createRequestBuilder(HttpMethod.DELETE, deleteEndpoint, getHttpHeaders(), "");
        Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).deleteSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

        // exercise
        result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        Mockito.verify(this.facade, Mockito.times(2)).deleteSecurityGroupRules(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(ResourceType.class));
    }

    private RequestBuilder createRequestBuilder(HttpMethod method, String urlTemplate, HttpHeaders headers, String body) {
        switch (method) {
            case POST:
                return MockMvcRequestBuilders.post(urlTemplate)
                        .headers(headers)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON);
            case GET:
                return MockMvcRequestBuilders.get(urlTemplate)
                        .headers(headers)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON);
            case DELETE:
                return MockMvcRequestBuilders.delete(urlTemplate)
                        .headers(headers)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON);
            default:
                return null;
        }
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String fakeFederationTokenValue = "fake-access-id";
        headers.set(SecurityGroupRuleAPI.FEDERATION_TOKEN_VALUE_HEADER_KEY, fakeFederationTokenValue);
        return headers;
    }

}
