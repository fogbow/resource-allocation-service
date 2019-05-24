package cloud.fogbow.ras.requests.api.local.http;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import cloud.fogbow.ras.api.parameters.SecurityRule;
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

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.request.PublicIp;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = PublicIp.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class PublicIpTest {
    
    private static final String ADDRESS_SEPARATOR = "/";
	private static final String FAKE_CLOUD_STATE = "fake-cloud-state";
    private static final String FAKE_IDENTITY_PROVIDER_ID = "fake-identity-provider-id";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_USER_TOKEN = "fake-user-token";
    private static final String IP_ADDRESS_EXAMPLE = "192.168.0.1";
    private static final String PUBLIC_IP_ENDPOINT = ADDRESS_SEPARATOR + PublicIp.PUBLIC_IP_ENDPOINT;
    private static final String PUBLIC_IP_ENDPOINT_BAR_STATUS = PUBLIC_IP_ENDPOINT + "/status";
	private static final String RULE_ID_EXAMPLE = "ANY@@192.168.0.1@@4@@8080:8081@@inbound@@anything@@1";
	private static final String SECURITY_RULES_ENDPOINT = ADDRESS_SEPARATOR + PublicIp.SECURITY_RULES_ENDPOINT;

	@Autowired
    private MockMvc mockMvc;

    private ApplicationFacade facade;

    @Before
    public void setUp() throws FogbowException {
        this.facade = Mockito.spy(ApplicationFacade.class);
    }

	// test case: Create a public IP instance
	@Test
	public void testCreatePublicIp() throws Exception {
		// set up
		PublicIpOrder publicIpOrder = createPublicIpOrder();

		PowerMockito.mockStatic(ApplicationFacade.class);
		BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
		Mockito.doReturn(publicIpOrder.getId()).when(this.facade).createPublicIp(Mockito.any(PublicIpOrder.class),
				Mockito.anyString());

		String body = createPublicIpOrderBody();
		HttpHeaders headers = getHttpHeaders();
		int expectedStatus = HttpStatus.CREATED.value();

		// exercise
		MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.post(PUBLIC_IP_ENDPOINT)
						.headers(headers)
						.accept(MediaType.APPLICATION_JSON)
						.content(body)
						.contentType(MediaType.APPLICATION_JSON))
						.andReturn();

		// verify
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
		Mockito.verify(this.facade, Mockito.times(1)).createPublicIp(Mockito.any(PublicIpOrder.class),
				Mockito.anyString());
	}
	
	// test case: Fail to create a public IP instance
	@Test
	public void testCreatePublicIpFailed() throws Exception {
		// set up
		String body = createPublicIpOrderBody();
		HttpHeaders headers = getHttpHeaders();

		try {
			// exercise
			this.mockMvc.perform(MockMvcRequestBuilders.post(PUBLIC_IP_ENDPOINT)
					.headers(headers)
					.accept(MediaType.APPLICATION_JSON).content(body)
					.contentType(MediaType.APPLICATION_JSON))
					.andReturn();

		} catch (Exception e) {
			// verify
			fail();
		}
	}	
    
	// test case: Get a list of public IP instance status
	@Test
	public void testGetAllPublicIpsStatus() throws Exception {
		// set up
		List<InstanceStatus> publicIpInstanceStatus = Mockito.spy(new ArrayList<InstanceStatus>());

		PowerMockito.mockStatic(ApplicationFacade.class);
		BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
		Mockito.doReturn(publicIpInstanceStatus).when(this.facade).getAllInstancesStatus(Mockito.anyString(),
				Mockito.eq(ResourceType.PUBLIC_IP));

		HttpHeaders headers = getHttpHeaders();
		int expectedStatus = HttpStatus.OK.value();

		// exercise
		MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(PUBLIC_IP_ENDPOINT_BAR_STATUS)
				.headers(headers)
				.contentType(MediaType.APPLICATION_JSON))
				.andReturn();

		// verify
		List<InstanceStatus> resultList = new Gson().fromJson(result.getResponse().getContentAsString(), List.class);
		Assert.assertNotNull(resultList);
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
		Mockito.verify(this.facade, Mockito.times(1)).getAllInstancesStatus(Mockito.anyString(),
				Mockito.eq(ResourceType.PUBLIC_IP));
	}
	
	// test case: Fail to get a list of public IP instance status
	@Test
	public void testGetAllPublicIpStatusFailed() throws Exception {
		// set up
		HttpHeaders headers = getHttpHeaders();

		try {
			// exercise
			this.mockMvc.perform(MockMvcRequestBuilders.get(PUBLIC_IP_ENDPOINT_BAR_STATUS)
					.headers(headers)
					.contentType(MediaType.APPLICATION_JSON))
					.andReturn();

		} catch (Exception e) {
			// verify
			fail();
		}
	}
	
	// test case: Get a public ip instance
	@Test
	public void testGetPublicIp() throws Exception {
		// set up
		PublicIpInstance instance = createPublicIpInstance();
		String instanceId = instance.getId();

		PowerMockito.mockStatic(ApplicationFacade.class);
		BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
		Mockito.doReturn(instance).when(this.facade).getPublicIp(Mockito.anyString(), Mockito.anyString());

		HttpHeaders headers = getHttpHeaders();
		int expectedStatus = HttpStatus.OK.value();

		// exercise
		MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(PUBLIC_IP_ENDPOINT 
				+ ADDRESS_SEPARATOR 
				+ instanceId)
				.headers(headers)
				.contentType(MediaType.APPLICATION_JSON))
				.andReturn();

		// verify
		NetworkInstance resultInstance = new Gson().fromJson(result.getResponse().getContentAsString(),
				NetworkInstance.class);
		Assert.assertEquals(instance.getId(), resultInstance.getId());
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
		Mockito.verify(this.facade, Mockito.times(1)).getPublicIp(Mockito.anyString(), Mockito.anyString());
	}
	
	// test case: Fail to get a public IP instance
	@Test
	public void testGetPublicIpFailed() throws Exception {
		// set up
		String instanceId = FAKE_INSTANCE_ID;
		HttpHeaders headers = getHttpHeaders();

		try {
			// exercise
			this.mockMvc.perform(MockMvcRequestBuilders.get(PUBLIC_IP_ENDPOINT + ADDRESS_SEPARATOR + instanceId)
					.headers(headers)
					.contentType(MediaType.APPLICATION_JSON))
					.andReturn();

		} catch (Exception e) {
			// verify
			fail();
		}
	}
	
	// test case: Delete an existing public IP instance
	@Test
	public void testDeletePublicIp() throws Exception {
		// set up
		PublicIpOrder publicIpOrder = createPublicIpOrder();
		String instanceId = publicIpOrder.getId();

		PowerMockito.mockStatic(ApplicationFacade.class);
		BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
		Mockito.doNothing().when(this.facade).deletePublicIp(Mockito.anyString(), Mockito.anyString());

		HttpHeaders headers = getHttpHeaders();
		int expectedStatus = HttpStatus.OK.value();

		// exercise
		MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.delete(PUBLIC_IP_ENDPOINT 
				+ ADDRESS_SEPARATOR 
				+ instanceId)
				.headers(headers)
				.contentType(MediaType.APPLICATION_JSON))
				.andReturn();

		// verify
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
		Mockito.verify(this.facade, Mockito.times(1)).deletePublicIp(Mockito.anyString(), Mockito.anyString());
	}
	
	// test case: Fail to delete an existing public IP instance
	@Test
	public void testDeletePublicIpFailed() throws Exception {
		// set up
		PublicIpOrder publicIpOrder = createPublicIpOrder();
		String instanceId = publicIpOrder.getId();
		HttpHeaders headers = getHttpHeaders();

		try {
			// exercise
			this.mockMvc.perform(MockMvcRequestBuilders.delete(PUBLIC_IP_ENDPOINT + ADDRESS_SEPARATOR + instanceId)
					.headers(headers)
					.contentType(MediaType.APPLICATION_JSON))
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
				Mockito.any(SecurityRule.class), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

		PublicIpOrder publicIpOrder = createPublicIpOrder();
		HttpHeaders headers = getHttpHeaders();
		String body = createSecurityRuleBody();

		int expectedStatus = HttpStatus.CREATED.value();

		// exercise
		MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders
				.post(PUBLIC_IP_ENDPOINT + ADDRESS_SEPARATOR + publicIpOrder.getId() + SECURITY_RULES_ENDPOINT)
				.headers(headers)
				.accept(MediaType.APPLICATION_JSON)
				.content(body)
				.contentType(MediaType.APPLICATION_JSON))
				.andReturn();

		// verify
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
		Mockito.verify(this.facade, Mockito.times(1)).createSecurityRule(Mockito.eq(publicIpOrder.getId()),
				Mockito.any(SecurityRule.class), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));
	}
	
	// test case: Fail to create a instance of security rule
	@Test
	public void testCreateSecurityRuleFailed() throws Exception {
		// set up
		PublicIpOrder publicIpOrder = createPublicIpOrder();
		HttpHeaders headers = getHttpHeaders();
		String body = createSecurityRuleBody();

		try {
			// exercise
			this.mockMvc.perform(MockMvcRequestBuilders
					.post(PUBLIC_IP_ENDPOINT + ADDRESS_SEPARATOR + publicIpOrder.getId() + SECURITY_RULES_ENDPOINT)
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
		PublicIpOrder publicIpOrder = createPublicIpOrder();
		List<SecurityRuleInstance> securityRuleInstances = Mockito.spy(new ArrayList<SecurityRuleInstance>());

		PowerMockito.mockStatic(ApplicationFacade.class);
		BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
		Mockito.doReturn(securityRuleInstances).when(this.facade).getAllSecurityRules(Mockito.anyString(), Mockito.anyString(),
				Mockito.eq(ResourceType.PUBLIC_IP));

		HttpHeaders headers = getHttpHeaders();
		int expectedStatus = HttpStatus.OK.value();

		// exercise
		MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.get(PUBLIC_IP_ENDPOINT 
				+ ADDRESS_SEPARATOR 
				+ publicIpOrder.getId() 
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
				Mockito.eq(ResourceType.PUBLIC_IP));
	}
	
	// test case: Fail to get a list of security rules
	@Test
	public void testGetAllSecurityRulesFailed() throws Exception {
		// set up
		PublicIpOrder publicIpOrder = createPublicIpOrder();
		HttpHeaders headers = getHttpHeaders();

		try {
			// exercise
			this.mockMvc.perform(MockMvcRequestBuilders.get(PUBLIC_IP_ENDPOINT 
					+ ADDRESS_SEPARATOR 
					+ publicIpOrder.getId() 
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
	
	// test case: Delete an existing public IP security rule instance
	@Test
	public void testDeleteSecurityRule() throws Exception {
		// set up
		PublicIpOrder publicIpOrder = createPublicIpOrder();
		String orderId = publicIpOrder.getId();
		String ruleId = RULE_ID_EXAMPLE;

		PowerMockito.mockStatic(ApplicationFacade.class);
		BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
		Mockito.doNothing().when(this.facade).deleteSecurityRule(Mockito.eq(orderId), Mockito.eq(ruleId),
				Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));

		HttpHeaders headers = getHttpHeaders();
		int expectedStatus = HttpStatus.OK.value();

		// exercise
		MvcResult result = this.mockMvc.perform(MockMvcRequestBuilders.delete(PUBLIC_IP_ENDPOINT 
				+ ADDRESS_SEPARATOR 
				+ orderId 
				+ SECURITY_RULES_ENDPOINT 
				+ ADDRESS_SEPARATOR 
				+ ruleId)
				.headers(headers)
				.contentType(MediaType.APPLICATION_JSON))
				.andReturn();

		// verify
		Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
		Mockito.verify(this.facade, Mockito.times(1)).deleteSecurityRule(Mockito.eq(orderId),
				Mockito.eq(ruleId), Mockito.anyString(), Mockito.eq(ResourceType.PUBLIC_IP));
	}
	
	// test case: Fail to delete an existing public IP security rule instance
	@Test
	public void testDeleteSecurityRuleFailed() throws Exception {
		// set up
		PublicIpOrder publicIpOrder = createPublicIpOrder();
		String ruleId = RULE_ID_EXAMPLE;
		HttpHeaders headers = getHttpHeaders();

		try {
			// exercise
			this.mockMvc.perform(MockMvcRequestBuilders.delete(PUBLIC_IP_ENDPOINT 
					+ ADDRESS_SEPARATOR 
					+ publicIpOrder.getId() 
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

	private PublicIpInstance createPublicIpInstance() {
		String id = FAKE_INSTANCE_ID;
		String cloudState = FAKE_CLOUD_STATE;
		String ip = IP_ADDRESS_EXAMPLE;
		return new PublicIpInstance(id, cloudState, ip);
	}

	private HttpHeaders getHttpHeaders() {
		HttpHeaders headers = new HttpHeaders();
        String userToken = FAKE_USER_TOKEN;
        headers.set(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, userToken);
        return headers;
	}

	private String createPublicIpOrderBody() {
		String requestBody = "{\n" 
				+ "  \"cloudName\": \"fake-cloud-name\",\n"
				+ "  \"computeId\": \"fake-compute-order-id\",\n" 
				+ "  \"provider\": \"fake-identity-provider\"\n"
				+ "}";
		return requestBody;
	}
	
	private PublicIpOrder createPublicIpOrder() {
		String userId = FAKE_USER_ID;
        String userName = FAKE_USER_NAME;
        String identityProviderId = FAKE_IDENTITY_PROVIDER_ID;
    	SystemUser systemUser = new SystemUser(userId, userName, identityProviderId);

        PublicIpOrder publicIpOrder = Mockito.spy(new PublicIpOrder());
        publicIpOrder.setSystemUser(systemUser);
        return publicIpOrder;
	}

}
