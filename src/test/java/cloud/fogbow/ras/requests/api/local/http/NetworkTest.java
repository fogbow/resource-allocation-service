package cloud.fogbow.ras.requests.api.local.http;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.ras.api.http.response.quotas.allocation.NetworkAllocation;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.core.TestUtils;
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

import com.google.gson.Gson;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.request.Network;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = Network.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class NetworkTest {

    private static final String ADDRESS_SEPARATOR = "/";
    private static final String FAKE_ADDRESS = "fake-address";
    private static final String FAKE_CLOUD_STATE = "fake-cloud-state";
    private static final String FAKE_GATEWAY = "fake-gateway";
    private static final String FAKE_IDENTITY_PROVIDER_ID = "fake-identity-provider-id";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_LABEL = "fake-label";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_USER_TOKEN = "fake-user-token";
    private static final String FAKE_VLAN = "fake-vlan";
    private static final String NETWORK_ENDPOINT = ADDRESS_SEPARATOR + Network.NETWORK_ENDPOINT;
    private static final String NETWORK_ENDPOINT_BAR_STATUS = NETWORK_ENDPOINT + "/status";
    private static final String RULE_ID_EXAMPLE = "ANY@@192.168.0.1@@4@@8080:8081@@inbound@@anything@@1";
    private static final String SECURITY_RULES_ENDPOINT = "/" + Network.SECURITY_RULES_SUFFIX_ENDPOINT;
    private static final String ENDPOINT_SUFFIX = "/cloudName";

    private ApplicationFacade facade;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.facade = Mockito.spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
    }

    // test case: Create a network instance
    @Test
    public void testCreateNetwork() throws Exception {
        // set up
        NetworkOrder networkOrder = createNetworkOrder();

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
        Mockito.doReturn(networkOrder.getId()).when(this.facade).createNetwork(Mockito.any(NetworkOrder.class),
                Mockito.anyString());

        String body = createNetworkOrderBody();
        HttpHeaders headers = getHttpHeaders();
        int expectedStatus = HttpStatus.CREATED.value();

        // exercise
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.post(NETWORK_ENDPOINT)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .content(body)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).createNetwork(Mockito.any(NetworkOrder.class),
                Mockito.anyString());
    }

    // test case: Fail to create a network instance
    @Test
    public void testCreateNetworkFailed() throws Exception {
        // set up
        String body = createNetworkOrderBody();
        HttpHeaders headers = getHttpHeaders();

        try {
            // exercise
            this.mockMvc.perform(MockMvcRequestBuilders.post(NETWORK_ENDPOINT)
                    .headers(headers)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

        } catch (Exception e) {
            // verify
            fail();
        }
    }

    // test case: Get a list of networks instance status
    @Test
    public void testGetAllNetworksStatus() throws Exception {
        // set up
        List<InstanceStatus> networkInstanceStatus = Mockito.spy(new ArrayList<InstanceStatus>());

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
        Mockito.doReturn(networkInstanceStatus).when(this.facade).getAllInstancesStatus(Mockito.anyString(),
                Mockito.eq(ResourceType.NETWORK));

        HttpHeaders headers = getHttpHeaders();
        int expectedStatus = HttpStatus.OK.value();

        // exercise
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(NETWORK_ENDPOINT_BAR_STATUS)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // verify
        List<InstanceStatus> resultList = new Gson().fromJson(result.getResponse().getContentAsString(), List.class);
        Assert.assertNotNull(resultList);
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).getAllInstancesStatus(Mockito.anyString(),
                Mockito.eq(ResourceType.NETWORK));
    }

    // test case: Fail to get a list of networks instance status
    @Test
    public void testGetAllNetworksStatusFailed() throws Exception {
        // set up
        HttpHeaders headers = getHttpHeaders();

        try {
            // exercise
            this.mockMvc.perform(MockMvcRequestBuilders.get(NETWORK_ENDPOINT_BAR_STATUS)
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

        } catch (Exception e) {
            // verify
            fail();
        }
    }

    // test case: Get a network instance
    @Test
    public void testGetNetwork() throws Exception {
        // set up
        NetworkInstance instance = createNetworkInstance();
        String networkId = instance.getId();

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
        Mockito.doReturn(instance).when(this.facade).getNetwork(Mockito.anyString(), Mockito.anyString());

        HttpHeaders headers = getHttpHeaders();
        int expectedStatus = HttpStatus.OK.value();

        // exercise
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(NETWORK_ENDPOINT
                + ADDRESS_SEPARATOR
                + networkId)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // verify
        NetworkInstance resultInstance = new Gson().fromJson(result.getResponse().getContentAsString(),
                NetworkInstance.class);
        Assert.assertEquals(instance.getId(), resultInstance.getId());
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).getNetwork(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Fail to get a network instance
    @Test
    public void testGetNetworkFailed() throws Exception {
        // set up
        String networkId = FAKE_INSTANCE_ID;
        HttpHeaders headers = getHttpHeaders();

        try {
            // exercise
            this.mockMvc.perform(MockMvcRequestBuilders.get(NETWORK_ENDPOINT + ADDRESS_SEPARATOR + networkId)
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

        } catch (Exception e) {
            // verify
            fail();
        }
    }

    // test case: Delete an existing network instance
    @Test
    public void testDeleteNetwork() throws Exception {
        // set up
        NetworkOrder networkOrder = createNetworkOrder();
        String networkId = networkOrder.getId();

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
        Mockito.doNothing().when(this.facade).deleteNetwork(Mockito.anyString(), Mockito.anyString());

        HttpHeaders headers = getHttpHeaders();
        int expectedStatus = HttpStatus.OK.value();

        // exercise
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.delete(NETWORK_ENDPOINT
                + ADDRESS_SEPARATOR
                + networkId)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).deleteNetwork(Mockito.anyString(), Mockito.anyString());
    }

    // test case: Fail to delete an existing network instance
    @Test
    public void testDeleteNetworkFailed() throws Exception {
        // set up
        NetworkOrder networkOrder = createNetworkOrder();
        String networkId = networkOrder.getId();
        HttpHeaders headers = getHttpHeaders();

        try {
            // exercise
            this.mockMvc.perform(MockMvcRequestBuilders.delete(NETWORK_ENDPOINT + ADDRESS_SEPARATOR + networkId)
                    .headers(headers).contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

        } catch (Exception e) {
            // Verify
            fail();
        }
    }

    // test case: Create a instance of security rule
    @Test
    public void testCreateSecurityRule() throws Exception {
        // set up
        String ruleId = RULE_ID_EXAMPLE;

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
        Mockito.doReturn(ruleId).when(this.facade).createSecurityRule(Mockito.anyString(),
                Mockito.any(SecurityRule.class), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));

        NetworkOrder networkOrder = createNetworkOrder();
        HttpHeaders headers = getHttpHeaders();
        String body = createSecurityRuleBody();

        int expectedStatus = HttpStatus.CREATED.value();

        // exercise
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders
                .post(NETWORK_ENDPOINT + ADDRESS_SEPARATOR + networkOrder.getId() + SECURITY_RULES_ENDPOINT)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .content(body)
                .contentType(MediaType.APPLICATION_JSON)).andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).createSecurityRule(Mockito.eq(networkOrder.getId()),
                Mockito.any(SecurityRule.class), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));
    }

    // test case: Fail to create a instance of security rule
    @Test
    public void testCreateSecurityRuleFailed() throws Exception {
        // set up
        NetworkOrder networkOrder = createNetworkOrder();
        HttpHeaders headers = getHttpHeaders();
        String body = createSecurityRuleBody();

        try {
            // exercise
            this.mockMvc.perform(MockMvcRequestBuilders
                    .post(NETWORK_ENDPOINT + ADDRESS_SEPARATOR + networkOrder.getId() + SECURITY_RULES_ENDPOINT)
                    .headers(headers)
                    .accept(MediaType.APPLICATION_JSON)
                    .content(body)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

        } catch (Exception e) {
            // verify
            fail();
        }
    }

    // test case: Get a list of security rules
    @Test
    public void testGetAllSecurityRules() throws Exception {
        // set up
        NetworkOrder networkOrder = createNetworkOrder();
        List<SecurityRuleInstance> securityRuleInstances = Mockito.spy(new ArrayList<SecurityRuleInstance>());

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
        Mockito.doReturn(securityRuleInstances).when(this.facade).getAllSecurityRules(Mockito.anyString(), Mockito.anyString(),
                Mockito.eq(ResourceType.NETWORK));

        HttpHeaders headers = getHttpHeaders();
        int expectedStatus = HttpStatus.OK.value();

        // exercise
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(NETWORK_ENDPOINT
                + ADDRESS_SEPARATOR
                + networkOrder.getId()
                + ADDRESS_SEPARATOR
                + SECURITY_RULES_ENDPOINT)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // verify
        List<SecurityRuleInstance> resultList = new Gson().fromJson(result.getResponse().getContentAsString(), List.class);
        Assert.assertNotNull(resultList);
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).getAllSecurityRules(Mockito.anyString(), Mockito.anyString(),
                Mockito.eq(ResourceType.NETWORK));
    }

    // test case: Fail to get a list of security rules
    @Test
    public void testGetAllSecurityRulesFailed() throws Exception {
        // set up
        NetworkOrder networkOrder = createNetworkOrder();
        HttpHeaders headers = getHttpHeaders();

        try {
            // exercise
            this.mockMvc.perform(MockMvcRequestBuilders.get(NETWORK_ENDPOINT
                    + ADDRESS_SEPARATOR
                    + networkOrder.getId()
                    + ADDRESS_SEPARATOR
                    + SECURITY_RULES_ENDPOINT)
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

        } catch (Exception e) {
            // verify
            fail();
        }
    }

    // test case: Delete an existing network security rule instance
    @Test
    public void testDeleteSecurityRule() throws Exception {
        // set up
        NetworkOrder networkOrder = createNetworkOrder();
        String orderId = networkOrder.getId();
        String ruleId = RULE_ID_EXAMPLE;

        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
        Mockito.doNothing().when(this.facade).deleteSecurityRule(Mockito.eq(orderId), Mockito.eq(ruleId),
                Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));

        HttpHeaders headers = getHttpHeaders();
        int expectedStatus = HttpStatus.OK.value();

        // exercise
        MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.delete(NETWORK_ENDPOINT
                + ADDRESS_SEPARATOR
                + networkOrder.getId()
                + SECURITY_RULES_ENDPOINT
                + ADDRESS_SEPARATOR
                + ruleId)
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // verify
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(1)).deleteSecurityRule(Mockito.eq(networkOrder.getId()),
                Mockito.eq(ruleId), Mockito.anyString(), Mockito.eq(ResourceType.NETWORK));
    }

    // test case: Fail to delete an existing network security rule instance
    @Test
    public void testDeleteSecurityRuleFailed() throws Exception {
        // set up
        NetworkOrder networkOrder = createNetworkOrder();
        String ruleId = RULE_ID_EXAMPLE;
        HttpHeaders headers = getHttpHeaders();

        try {
            // exercise
            this.mockMvc.perform(MockMvcRequestBuilders.delete(NETWORK_ENDPOINT
                    + ADDRESS_SEPARATOR
                    + networkOrder.getId()
                    + SECURITY_RULES_ENDPOINT
                    + ADDRESS_SEPARATOR
                    + ruleId)
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

        } catch (Exception e) {
            // verify
            fail();
        }
    }

    // test case: Request the user allocation with unauthenticated user. Check the response of request
    // and the call of facade for get the user allocation.
    @Test
    public void testGetUserAllocationUnauthenticatedException() throws Exception {
        // set up
        final String FAKE_PROVIDER_ID = "fake-provider-id";
        Mockito.doThrow(new UnauthenticatedUserException()).when(this.facade).getNetworkAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        final String ALLOCATION_ENDPOINT = NETWORK_ENDPOINT + "/" + Network.ALLOCATION_SUFFIX_ENDPOINT;
        final String providerIdEndpoint = ALLOCATION_ENDPOINT + "/" + FAKE_PROVIDER_ID + ENDPOINT_SUFFIX;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, providerIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.UNAUTHORIZED.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).getNetworkAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request the user allocation with unauthorized user. Check the response of request
    // and the call of facade for get the user allocation.
    @Test
    public void testGetUserAllocationUnauthorizedException() throws Exception {
        // set up
        final String FAKE_PROVIDER_ID = "fake-provider-id";
        Mockito.doThrow(new UnauthorizedRequestException()).when(this.facade).getNetworkAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        final String ALLOCATION_ENDPOINT = NETWORK_ENDPOINT + "/" + Network.ALLOCATION_SUFFIX_ENDPOINT;
        final String providerIdEndpoint = ALLOCATION_ENDPOINT + "/" + FAKE_PROVIDER_ID + ENDPOINT_SUFFIX;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, providerIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.FORBIDDEN.value();

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).getNetworkAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    // test case: Request the user allocation and test successfully return. Check the response of request
    // and the call of facade for get the user allocation.
    @Test
    public void testGetUserAllocation() throws Exception {
        // set up
        final String FAKE_PROVIDER_ID = "fake-provider-id";
        final int NETWORKS_TOTAL = 1;

        NetworkAllocation fakeNetworkAllocation = new NetworkAllocation(NETWORKS_TOTAL);

        Mockito.doReturn(fakeNetworkAllocation).when(this.facade).getNetworkAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        final String ALLOCATION_ENDPOINT = NETWORK_ENDPOINT + "/" + Network.ALLOCATION_SUFFIX_ENDPOINT;
        final String providerIdEndpoint = ALLOCATION_ENDPOINT + "/" + FAKE_PROVIDER_ID + ENDPOINT_SUFFIX;
        RequestBuilder requestBuilder = createRequestBuilder(HttpMethod.GET, providerIdEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        NetworkAllocation resultComputeAllocation = new Gson().fromJson(result.getResponse().getContentAsString(), NetworkAllocation.class);

        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
        Assert.assertEquals(fakeNetworkAllocation.getNetworks(), resultComputeAllocation.getNetworks());

        Mockito.verify(this.facade, Mockito.times(TestUtils.RUN_ONCE)).getNetworkAllocation(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
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
        String userToken = FAKE_USER_TOKEN;
        headers.set(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, userToken);
        return headers;
    }

    private NetworkOrder createNetworkOrder() throws UnexpectedException {
        String userId = FAKE_USER_ID;
        String userName = FAKE_USER_NAME;
        String identityProviderId = FAKE_IDENTITY_PROVIDER_ID;
        SystemUser systemUser = new SystemUser(userId, userName, identityProviderId);

        NetworkOrder networkOrder = Mockito.spy(new NetworkOrder());
        networkOrder.setSystemUser(systemUser);
        return networkOrder;
    }

    private NetworkInstance createNetworkInstance() {
        String id = FAKE_INSTANCE_ID;
        String name = FAKE_LABEL;
        String cidr = FAKE_ADDRESS;
        String gateway = FAKE_GATEWAY;
        String vLan = FAKE_VLAN;
        String cloudState = FAKE_CLOUD_STATE;
        NetworkAllocationMode networkAllocationMode = NetworkAllocationMode.STATIC;
        return new NetworkInstance(id, cloudState, name, cidr, gateway, vLan, networkAllocationMode, null, null, null);
    }

    private String createSecurityRuleBody() {
        String requestBody = "{\n"
                + "  \"cidr\": \"192.168.0.1/24\",\n"
                + "  \"direction\": \"IN\",\n"
                + "  \"etherType\": \"IPv4\",\n"
                + "  \"instanceId\": \"fake-instance-id\",\n"
                + "  \"portFrom\": 8080,\n"
                + "  \"portTo\": 8081,\n"
                + "  \"protocol\": \"TCP\"\n"
                + "}";

        return requestBody;
    }

    private String createNetworkOrderBody() {
        String requestBody = "{"
                + "\"requestingMember\":\"req-member\", "
                + "\"providingMember\":\"prov-member\", "
                + "\"gateway\":\"gateway\", "
                + "\"address\":\"address\", "
                + "\"allocation\":\"dynamic\""
                + "}";

        return requestBody;
    }

}
