package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vm.VirtualMachinePool;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenNebulaUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaFogbowGenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4.OpenNebulaGenericRequestPlugin.Parameters;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GsonHolder.class, OpenNebulaClientUtil.class, VirtualMachine.class})
public class OpenNebulaGenericRequestPluginTest {
	
	private static final String ANYTHING_RESPONSE = "anything";
	private static final String ATTACH_TEMPLATE_PARAMETER_KEY = "diskTemplate";
	private static final String CLIENT_RESOURCE = "Client";
	private static final String DISK_ATTACH_METHOD = "diskAttach";
	private static final String FAKE_RESOURCE_ID = "fake-resource-id";
	private static final String ID_PARAMETER_KEY = "id";
	private static final String INFO_METHOD = "info";
	private static final String ONE_VALUE = "1";
	private static final String OPENNEBULA_TOKEN = "user:password";
	private static final String RESOURCE_ID_EMPTY = "";
	private static final String SAMPLE_ENDPOINT = "http://localhost:2633/RPC2";
	private static final String VIRTUAL_MACHINE_POOL_RESOURCE = "VirtualMachinePool";
	private static final String VIRTUAL_MACHINE_RESOURCE = "VirtualMachine";

	private static final String DISK_ATTACH_TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" 
			+ "<TEMPLATE>\n"
			+ "    <DISK>\n" 
			+ "        <IMAGE_ID>1</IMAGE_ID>\n" 
			+ "    </DISK>\n" 
			+ "</TEMPLATE>\n";

	private static final String GENERIC_REQUEST_TEMPLATE = "{\n" 
			+ "  \"url\": \"http://localhost::2633/RPC2\",\n" 
			+ "	\"oneResource\": %s,\n" 
			+ "  \"oneMethod\": %s,\n" 
			+ "  \"resourceId\": null,\n" 
			+ "  \"parameters\": {\"id\":\"1\"}\n" 
			+ "}";

	
	private OpenNebulaGenericRequestPlugin plugin;
	
	@Before
	public void setUp() {
		this.plugin = Mockito.spy(OpenNebulaGenericRequestPlugin.class);
	}
	
	// test case: ...
	@Test (expected = InvalidParameterException.class)
	public void testRedirectGenericRequestWithoutAValidResource() throws FogbowException {
		// set up
		String resource = null;
		String method = INFO_METHOD;
		String genericRequest = createGenericRequest(resource, method);
		
		String userId = null;
		String userName = null;
		String token = OPENNEBULA_TOKEN;
		OpenNebulaUser cloudUser = new OpenNebulaUser(userId, userName, token);
		
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		// exercise
		this.plugin.redirectGenericRequest(genericRequest, cloudUser);
	}
	
	// test case: ...
	@Test(expected = InvalidParameterException.class)
	public void testRedirectGenericRequestWithoutAValidMethod() throws FogbowException {
		// set up
		String resource = VIRTUAL_MACHINE_RESOURCE;
		String method = null;
		String genericRequest = createGenericRequest(resource, method);

		String userId = null;
		String userName = null;
		String token = OPENNEBULA_TOKEN;
		OpenNebulaUser cloudUser = new OpenNebulaUser(userId, userName, token);

		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		// exercise
		this.plugin.redirectGenericRequest(genericRequest, cloudUser);
	}

	// test case: ...
	@Test
	public void testRedirectGenericRequestSuccessfully() throws FogbowException {
		// set up
		String resource = VIRTUAL_MACHINE_RESOURCE;
		String method = INFO_METHOD;
		String genericRequest = createGenericRequest(resource, method);
		
		String userId = null;
		String userName = null;
		String token = OPENNEBULA_TOKEN;
		OpenNebulaUser cloudUser = new OpenNebulaUser(userId, userName, token);
		
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		OneResponse oneResponse = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualMachine.class);
		BDDMockito.given(VirtualMachine.info(Mockito.any(Client.class), Mockito.anyInt())).willReturn(oneResponse);
		Mockito.when(oneResponse.isError()).thenReturn(true);
		Mockito.when(oneResponse.getMessage()).thenReturn(ANYTHING_RESPONSE);
		
		// exercise
		this.plugin.redirectGenericRequest(genericRequest, cloudUser);
		
		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
		
		Mockito.verify(this.plugin, Mockito.times(1)).instantiateResource(Mockito.any(OpenNebulaFogbowGenericRequest.class), 
				Mockito.any(Client.class), Mockito.anyString());
		
		Mockito.verify(this.plugin, Mockito.times(1)).generateParametersMap(Mockito.any(OpenNebulaFogbowGenericRequest.class), 
				Mockito.any(Client.class));
		
		Mockito.verify(this.plugin, Mockito.times(1)).generateMethod(Mockito.any(OpenNebulaFogbowGenericRequest.class), 
				Mockito.any(OpenNebulaGenericRequestPlugin.Parameters.class));
		
		Mockito.verify(this.plugin, Mockito.times(1)).invokeGenericMethod(Mockito.isNull(), 
				Mockito.any(OpenNebulaGenericRequestPlugin.Parameters.class), Mockito.any(Method.class));
		
		Mockito.verify(this.plugin, Mockito.times(1)).reproduceMessage(Mockito.any(OneResponse.class));
	}
	
	// test case: ...
	@Test
	public void testReproduceMessageViaOneResponseSuccessfully() throws NoSuchMethodException, SecurityException {
		// set up
		Class<Client> clientParameter = Client.class;

		OneResponse oneResponse = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualMachine.class);
		BDDMockito.given(VirtualMachine.info(Mockito.any(clientParameter), Mockito.anyInt())).willReturn(oneResponse);
		Mockito.when(oneResponse.isError()).thenReturn(false);
		Mockito.when(oneResponse.getMessage()).thenReturn(ANYTHING_RESPONSE);

		// exercise
		this.plugin.reproduceMessage(oneResponse);

		// verify
		Mockito.verify(oneResponse, Mockito.times(1)).isError();
		Mockito.verify(oneResponse, Mockito.times(1)).getMessage();
	}
	
	// test case: ...
	@Test
	public void testReproduceMessageViaOneResponseWithFailure() throws NoSuchMethodException, SecurityException {
		// set up
		Class<Client> clientParameter = Client.class;

		OneResponse oneResponse = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualMachine.class);
		BDDMockito.given(VirtualMachine.info(Mockito.any(clientParameter), Mockito.anyInt())).willReturn(oneResponse);
		Mockito.when(oneResponse.isError()).thenReturn(true);
		Mockito.when(oneResponse.getMessage()).thenReturn(ANYTHING_RESPONSE);

		// exercise
		this.plugin.reproduceMessage(oneResponse);

		// verify
		Mockito.verify(oneResponse, Mockito.times(1)).isError();
		Mockito.verify(oneResponse, Mockito.times(1)).getErrorMessage();
	}
	
	// test case: ...
	@Test
	public void testReproduceMessageWithoutOneResponse() throws NoSuchMethodException, SecurityException {
		// set up
		Gson gson = new Gson();
		PowerMockito.mockStatic(GsonHolder.class);
		BDDMockito.given(GsonHolder.getInstance()).willReturn(gson);
		
		// exercise
		this.plugin.reproduceMessage(ANYTHING_RESPONSE);

		// verify
		PowerMockito.verifyStatic(GsonHolder.class, VerificationModeFactory.times(1));
		GsonHolder.getInstance();
	}
	
	// test case: ...
	@Test
	public void testInvokeGenericMethodSuccessfully() throws NoSuchMethodException, SecurityException {
		// set up
		String oneMethod = INFO_METHOD;
		Class<Integer> intParameter = int.class;
		Class<Client> clientParameter = Client.class;
		Class<VirtualMachine> resourceType = VirtualMachine.class;
		
		Method method = resourceType.getMethod(oneMethod, clientParameter, intParameter);
		
		Client client = Mockito.mock(Client.class);
		Parameters parameters = new OpenNebulaGenericRequestPlugin.Parameters();
		parameters.getClasses().add(clientParameter);
		parameters.getClasses().add(intParameter);
		parameters.getValues().add(client);
		parameters.getValues().add(1);
		
		Object instance = null;
		
		OneResponse response = Mockito.mock(OneResponse.class);
		PowerMockito.mockStatic(VirtualMachine.class);
		BDDMockito.given(VirtualMachine.info(Mockito.any(clientParameter), Mockito.anyInt())).willReturn(response);
		
		// exercise
		this.plugin.invokeGenericMethod(instance, parameters, method);
		
		// verify
		PowerMockito.verifyStatic(VirtualMachine.class, VerificationModeFactory.times(1));
		VirtualMachine.info(Mockito.any(clientParameter), Mockito.anyInt());
	}
	
	// test case: ...
	@Test
	public void testInstantiateResourceClientSuccessfully() throws InvalidParameterException, UnexpectedException {
		// set up
		String url = SAMPLE_ENDPOINT;
		String oneResource = CLIENT_RESOURCE;
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parameters = null;
		
		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parameters);

		Client client = null;
		String secret = OPENNEBULA_TOKEN;

		Class<Client> expected = Client.class;

		// exercise
		Object instance = this.plugin.instantiateResource(request, client, secret);

		// verify
		Assert.assertEquals(expected, instance.getClass());
	}
	
	// test case: ...
	@Test
	public void testInstantiateResourcePoolSuccessfully() throws InvalidParameterException, UnexpectedException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_POOL_RESOURCE;
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parameters = null;
		
		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parameters);
		
		Client client = null;
		String secret = null;
		
		Class<VirtualMachinePool> expected = VirtualMachinePool.class;
		
		// exercise
		Object instance = this.plugin.instantiateResource(request, client, secret);
		
		//verify
		Assert.assertEquals(expected, instance.getClass());
	}
	
	// test case: ...
	@Test
	public void testInstantiateResourceWithIdSuccessfully() throws InvalidParameterException, UnexpectedException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = ONE_VALUE;
		Map<String, String> parameters = null;
		
		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parameters);
		
		Client client = null;
		String secret = null;
		
		Class<VirtualMachine> expected = VirtualMachine.class;
		
		// exercise
		Object instance = this.plugin.instantiateResource(request, client, secret);
		
		//verify
		Assert.assertEquals(expected, instance.getClass());
	}
	
	// test case: ...
	@Test
	public void testInstantiateResourceWithNullID() throws InvalidParameterException, UnexpectedException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parameters = null;

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parameters);

		Client client = null;
		String secret = null;

		// exercise
		Object instance = this.plugin.instantiateResource(request, client, secret);

		// verify
		Assert.assertNull(instance);
	}
	
	// test case: ...
	@Test
	public void testInstantiateResourceWithEmptyID() throws InvalidParameterException, UnexpectedException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = RESOURCE_ID_EMPTY;
		Map<String, String> parameters = null;
		
		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parameters);

		Client client = Mockito.mock(Client.class);
		String secret = null;

		// exercise
		Object instance = this.plugin.instantiateResource(request, client, secret);
		
		// verify
		Assert.assertNull(instance);
	}
	
	// test case: ...
	@Test(expected = InvalidParameterException.class)
	public void testInstantiateResourceWithoutParsableIntegerID() throws InvalidParameterException, UnexpectedException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = FAKE_RESOURCE_ID;
		Map<String, String> parameters = null;
		
		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parameters);

		Client client = null;
		String secret = null;

		// exercise
		this.plugin.instantiateResource(request, client, secret);
	}
	
	// test case: ...
	@Test
	public void testGenerateParametersMapWithValidResourceID() throws UnexpectedException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = ONE_VALUE;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(ID_PARAMETER_KEY, ONE_VALUE);
		
		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parametersMap);

		Client client = Mockito.mock(Client.class);

		Parameters expected = new OpenNebulaGenericRequestPlugin.Parameters();
		expected.getClasses().add(int.class);
		expected.getValues().add(1);

		// exercise
		Parameters parameters = this.plugin.generateParametersMap(request, client);

		// verify
		Assert.assertEquals(expected.getClasses().get(0), parameters.getClasses().get(0));
		Assert.assertEquals(expected.getValues().get(0), parameters.getValues().get(0));
	}
	
	// test case: ...
	@Test
	public void testGenerateParametersMapWithNullResourceID() throws UnexpectedException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(ID_PARAMETER_KEY, ONE_VALUE);

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parametersMap);

		Client client = Mockito.mock(Client.class);

		Parameters expected = new OpenNebulaGenericRequestPlugin.Parameters();
		expected.getClasses().add(Client.class);
		expected.getClasses().add(int.class);
		expected.getValues().add(client);
		expected.getValues().add(1);

		// exercise
		Parameters parameters = this.plugin.generateParametersMap(request, client);

		// verify
		Assert.assertEquals(expected.getClasses().get(0), parameters.getClasses().get(0));
		Assert.assertEquals(expected.getClasses().get(1), parameters.getClasses().get(1));
		Assert.assertEquals(expected.getValues().get(0), parameters.getValues().get(0));
		Assert.assertEquals(expected.getValues().get(1), parameters.getValues().get(1));
	}
	
	// test case: ...
	@Test
	public void testGenerateParametersMapWithEmptyResourceID() throws UnexpectedException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = RESOURCE_ID_EMPTY;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(ID_PARAMETER_KEY, ONE_VALUE);

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parametersMap);

		Client client = Mockito.mock(Client.class);

		Parameters expected = new OpenNebulaGenericRequestPlugin.Parameters();
		expected.getClasses().add(Client.class);
		expected.getClasses().add(int.class);
		expected.getValues().add(client);
		expected.getValues().add(1);

		// exercise
		Parameters parameters = this.plugin.generateParametersMap(request, client);

		// verify
		Assert.assertEquals(expected.getClasses().get(0), parameters.getClasses().get(0));
		Assert.assertEquals(expected.getClasses().get(1), parameters.getClasses().get(1));
		Assert.assertEquals(expected.getValues().get(0), parameters.getValues().get(0));
		Assert.assertEquals(expected.getValues().get(1), parameters.getValues().get(1));
	}
	
	// test case: ...
	@Test
	public void testGenerateParametersMapWithResourcePool() throws UnexpectedException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_POOL_RESOURCE;
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(ID_PARAMETER_KEY, ONE_VALUE);

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parametersMap);

		Client client = Mockito.mock(Client.class);

		Parameters expected = new OpenNebulaGenericRequestPlugin.Parameters();
		expected.getClasses().add(int.class);
		expected.getValues().add(1);

		// exercise
		Parameters parameters = this.plugin.generateParametersMap(request, client);

		// verify
		Assert.assertEquals(expected.getClasses().get(0), parameters.getClasses().get(0));
		Assert.assertEquals(expected.getValues().get(0), parameters.getValues().get(0));
	}
	
	// test case: ...
	@Test
	public void testGenerateParametersWithMapEmpty() throws UnexpectedException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parametersMap = new HashMap<>();

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parametersMap);

		Client client = Mockito.mock(Client.class);
		
		int expected = 0;

		// exercise
		Parameters parameters = this.plugin.generateParametersMap(request, client);

		// verify
		Assert.assertEquals(expected, parameters.getClasses().size());
		Assert.assertEquals(expected, parameters.getValues().size());
	}
	
	// test case: ...
	@Test
	public void testGenerateMethodWithoutResourceInstance() {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = INFO_METHOD;
		String resourceId = null;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(ID_PARAMETER_KEY, ONE_VALUE);

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parametersMap);

		Client client = Mockito.mock(Client.class);
		
		Parameters parameters = new OpenNebulaGenericRequestPlugin.Parameters();
		parameters.getClasses().add(Client.class);
		parameters.getClasses().add(int.class);
		parameters.getValues().add(client);
		parameters.getValues().add(1);

		String expected = oneMethod;
		int parametersNumber = 2;
		
		// exercise
		Method method = this.plugin.generateMethod(request, parameters);

		// verify
		Assert.assertEquals(expected, method.getName());
		Assert.assertEquals(parametersNumber, method.getParameterCount());
	}
	
	// test case: ...
	@Test
	public void testGenerateMethodWithResourceInstance() {
		// set up
		String template = DISK_ATTACH_TEMPLATE;

		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = DISK_ATTACH_METHOD;
		String resourceId = ONE_VALUE;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(ATTACH_TEMPLATE_PARAMETER_KEY, template);

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parametersMap);

		Parameters parameters = new OpenNebulaGenericRequestPlugin.Parameters();
		parameters.getClasses().add(String.class);
		parameters.getValues().add(template);
		
		String expected = oneMethod;
		int parametersNumber = 1;

		// exercise
		Method method = this.plugin.generateMethod(request, parameters);

		// verify
		Assert.assertEquals(expected, method.getName());
		Assert.assertEquals(parametersNumber, method.getParameterCount());
	}
	
	/* Exemple of json for genericRequest string
	 * 
	 * { 
	 * 		"url": "http://localhost:2633/RPC2", 
	 * 		"oneResource": "VirtualMachine",
	 * 		"oneMethod": "info", 
	 * 		"resourceId": null, 
	 * 		"parameters": {"id":"1"} 
	 * }
	 * 
	 */
	private String createGenericRequest(String resource, String method) {
		String template = GENERIC_REQUEST_TEMPLATE;
		String genericRequest = String.format(template, resource, method);
		return genericRequest;
	}
	
}
