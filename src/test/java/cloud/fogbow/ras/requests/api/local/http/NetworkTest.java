package cloud.fogbow.ras.requests.api.local.http;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.google.gson.Gson;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.request.Network;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
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
	private static final String RULE_ID_EXAMPLE = "ANY@@192.168.0.1@@4@@8080:8081@@inbound@@anything@@1";
	private static final String SECURITY_RULES_ENDPOINT = "/" + Network.SECURITY_RULES_ENDPOINT;

    private ApplicationFacade facade;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        this.facade = Mockito.spy(ApplicationFacade.class);
    }

    // test case: Create a network instance
    @Test
    public void testCreateNetwork() throws Exception {
        // set up
    	NetworkOrder networkOrder = createNetworkOrder();
    	
    	PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
        Mockito.doReturn(networkOrder.getId()).when(this.facade).createNetwork(Mockito.any(NetworkOrder.class), Mockito.anyString());

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
        Mockito.verify(this.facade, Mockito.times(1)).createNetwork(Mockito.any(NetworkOrder.class), Mockito.anyString());
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
		MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(NETWORK_ENDPOINT + ADDRESS_SEPARATOR + networkId)
				.headers(headers).contentType(MediaType.APPLICATION_JSON)).andReturn();

		// verify
		NetworkInstance resultInstance = new Gson().fromJson(result.getResponse().getContentAsString(),
				NetworkInstance.class);
		Assert.assertEquals(instance.getId(), resultInstance.getId());
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
		Mockito.verify(this.facade, Mockito.times(1)).getNetwork(Mockito.anyString(), Mockito.anyString());
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
		MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.delete(NETWORK_ENDPOINT + ADDRESS_SEPARATOR + networkId)
				.headers(headers).contentType(MediaType.APPLICATION_JSON)).andReturn();

		// verify
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
		Mockito.verify(this.facade, Mockito.times(1)).deleteNetwork(Mockito.anyString(), Mockito.anyString());
	}
    
	// test case: Delete an existing network security rule instance
	@Test
	public void testDeleteSecurityRule() throws Exception {
		// set up
		NetworkOrder networkOrder = createNetworkOrder();
		String ruleId = RULE_ID_EXAMPLE;

		PowerMockito.mockStatic(ApplicationFacade.class);
		BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
		Mockito.doNothing().when(this.facade).deleteSecurityRule(Mockito.anyString(), Mockito.anyString(),
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
