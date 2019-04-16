package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
	
	// test case: When calling the redirectGenericRequest method without a valid
	// resource in the request, InvalidParameterException must be thrown.
	@Ignore
	@Test(expected = InvalidParameterException.class)
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
	
	// test case: When calling the redirectGenericRequest method without a valid
	// method in the request, InvalidParameterException must be thrown.
	@Ignore
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

	// test case: When calling the redirectGenericRequest method with a resource,
	// method, and other valid parameters in the request, a successful response must
	// be returned.
	@Ignore
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

		Mockito.verify(this.plugin, Mockito.times(1)).instantiateResource(
				Mockito.any(OneFogbowGenericRequest.class), Mockito.any(Client.class), Mockito.anyString());

		Mockito.verify(this.plugin, Mockito.times(1)).generateParametersMap(
				Mockito.any(OneFogbowGenericRequest.class),
				Mockito.any(OpenNebulaGenericRequestPlugin.Parameters.class), Mockito.any(Client.class));

		Mockito.verify(this.plugin, Mockito.times(1)).generateMethod(Mockito.any(OneFogbowGenericRequest.class),
				Mockito.any(OpenNebulaGenericRequestPlugin.Parameters.class));

		Mockito.verify(this.plugin, Mockito.times(1)).invokeGenericMethod(Mockito.isNull(),
				Mockito.any(OpenNebulaGenericRequestPlugin.Parameters.class), Mockito.any(Method.class));

		Mockito.verify(this.plugin, Mockito.times(1)).reproduceMessage(Mockito.any(OneResponse.class));
	}
	
	// test case: When calling the reproduceMessage method, passing a OneResponse
	// object without errors, this must return a successful response.
	@Test
	public void testReproduceMessageViaOneResponseSuccessfully() {
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
	
	// test case: When calling the reproduceMessage method, passing a OneResponse
	// object with errors, this must return an error message in response.
	@Test
	public void testReproduceMessageViaOneResponseWithFailure() {
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
	
	// test case: When calling the reproduceMessage method, passing any object in
	// response, it must convert that object to a valid return message.
	@Test
	public void testReproduceMessageWithoutOneResponse() {
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
	
	// test case: The experimental method must be invoked when the
	// invokeGenericMethod method is called.
	@Test
	public void testInvokeGenericMethodSuccessfully()
			throws NoSuchMethodException, SecurityException, InvalidParameterException {
		
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
	
	// test case: When calling the instantiateResource method, passing a "Client"
	// resource on the request, this should return an instance of this resource.
	@Ignore
	@Test
	public void testInstantiateResourceClientSuccessfully() throws InvalidParameterException {
		// set up
//		String url = SAMPLE_ENDPOINT;
//		String oneResource = CLIENT_RESOURCE;
//		String oneMethod = null;
//		String resourceId = null;
//		Map<String, String> parameters = null;

		String genericRequest = getGenericRequest();
		OneFogbowGenericRequest request = OneFogbowGenericRequest.fromJson(genericRequest);

		Client client = null;
		String secret = OPENNEBULA_TOKEN;

		Class<Client> expected = Client.class;

		// exercise
		Object instance = this.plugin.instantiateResource(request, client, secret);

		// verify
		Assert.assertEquals(expected, instance.getClass());
	}
	
	private String getGenericRequest() {
		String json = "{\n" + 
				"   \"oneGenericRequest\":{\n" + 
				"      \"url\":\"http://10.11.5.20:2633/RPC2\",\n" + 
				"      \"oneResource\":\"Client\",\n" + 
				"   }\n" + 
				"}";
		return null;
	}

	// test case: When calling the instantiateResource method, passing a resource
	// pool on the request, this should return an instance of this resource type.
	@Test
	public void testInstantiateResourcePoolSuccessfully() throws InvalidParameterException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_POOL_RESOURCE;
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parameters = null;

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, oneResource, oneMethod,
				resourceId, parameters);

		Client client = null;
		String secret = null;

		Class<VirtualMachinePool> expected = VirtualMachinePool.class;

		// exercise
//		Object instance = this.plugin.instantiateResource(request, client, secret);

		// verify
//		Assert.assertEquals(expected, instance.getClass());
	}
	
	// test case: When calling the instantiateResource method, with a valid
	// resourceId in the request, this must return an instance of the resource
	// passed in the generic request.
	@Test
	public void testInstantiateResourceWithIdSuccessfully() throws InvalidParameterException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = ONE_VALUE;
		Map<String, String> parameters = null;

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, oneResource, oneMethod,
				resourceId, parameters);

		Client client = null;
		String secret = null;

		Class<VirtualMachine> expected = VirtualMachine.class;

		// exercise
//		Object instance = this.plugin.instantiateResource(request, client, secret);

		// verify
//		Assert.assertEquals(expected, instance.getClass());
	}
	
	// test case: When calling the instantiateResource method, with a null
	// resourceId in the request, this must return a null instance of the resource.
	@Test
	public void testInstantiateResourceWithNullID() throws InvalidParameterException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parameters = null;

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, oneResource, oneMethod,
				resourceId, parameters);

		Client client = null;
		String secret = null;

		// exercise
//		Object instance = this.plugin.instantiateResource(request, client, secret);

		// verify
//		Assert.assertNull(instance);
	}
	
	// test case: When calling the instantiateResource method, with a resourceId
	// empty in the request, this must return a null instance of the resource.
	@Test
	public void testInstantiateResourceWithEmptyID() throws InvalidParameterException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = RESOURCE_ID_EMPTY;
		Map<String, String> parameters = null;

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, oneResource, oneMethod,
				resourceId, parameters);

		Client client = Mockito.mock(Client.class);
		String secret = null;

		// exercise
//		Object instance = this.plugin.instantiateResource(request, client, secret);

		// verify
//		Assert.assertNull(instance);
	}
	
	// test case: When calling the instantiateResource method, without a parsable
	// integer value in the resourceId of the request, this must throw an
	// InvalidParameterException.
	@Ignore
	@Test(expected = InvalidParameterException.class)
	public void testInstantiateResourceWithoutParsableIntegerID()
			throws InvalidParameterException {
		
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = FAKE_RESOURCE_ID;
		Map<String, String> parameters = null;

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, oneResource, oneMethod,
				resourceId, parameters);

		Client client = null;
		String secret = null;

		// exercise
//		this.plugin.instantiateResource(request, client, secret);
	}
	
	// test case: When calling the generateParametersMap method with a resource
	// instance, this implies that a static method will not be invoked, and
	// therefore you do not need to add a "Client" resource to the parameter map.
	@Test
	public void testGenerateParametersMapWithResourceInstance() throws InvalidParameterException {
		// set up
		String template = DISK_ATTACH_TEMPLATE;
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = ONE_VALUE;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(ATTACH_TEMPLATE_PARAMETER_KEY, template);

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, oneResource, oneMethod,
				resourceId, parametersMap);

		Client client = Mockito.mock(Client.class);
		String secret = OPENNEBULA_TOKEN;
//		Object instance = this.plugin.instantiateResource(request, client, secret);

		Parameters expected = new OpenNebulaGenericRequestPlugin.Parameters();
		expected.getClasses().add(String.class);
		expected.getValues().add(template);

		// exercise
//		Parameters parameters = this.plugin.generateParametersMap(request, instance, client);

		// verify
//		Assert.assertEquals(expected.getClasses().get(0), parameters.getClasses().get(0));
//		Assert.assertEquals(expected.getValues().get(0), parameters.getValues().get(0));
	}
	
	// test case: When calling the generateParametersMap method, without a resource
	// instance, this implies that a static method will be invoked, and therefore
	// you do need to add a "Client" resource to the parameter map.
	@Test
	public void testGenerateParametersMapWithoutResourceInstance() {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(ID_PARAMETER_KEY, ONE_VALUE);

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, oneResource, oneMethod,
				resourceId, parametersMap);

		Client client = Mockito.mock(Client.class);
		Object instance = null;

		Parameters expected = new OpenNebulaGenericRequestPlugin.Parameters();
		expected.getClasses().add(Client.class);
		expected.getClasses().add(int.class);
		expected.getValues().add(client);
		expected.getValues().add(1);

		// exercise
//		Parameters parameters = this.plugin.generateParametersMap(request, instance, client);

		// verify
//		Assert.assertEquals(expected.getClasses().get(0), parameters.getClasses().get(0));
//		Assert.assertEquals(expected.getClasses().get(1), parameters.getClasses().get(1));
//		Assert.assertEquals(expected.getValues().get(0), parameters.getValues().get(0));
//		Assert.assertEquals(expected.getValues().get(1), parameters.getValues().get(1));
	}
	
	// test case: When calling the generateParametersMap method, with a resource
	// instance of the type "Pool", this implies that not will need to add a
	// "Client" resource to the parameter map.
	@Test
	public void testGenerateParametersMapWithResourcePool() throws InvalidParameterException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_POOL_RESOURCE;
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(ID_PARAMETER_KEY, ONE_VALUE);

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, oneResource, oneMethod,
				resourceId, parametersMap);

		Client client = Mockito.mock(Client.class);
		String secret = OPENNEBULA_TOKEN;
//		Object instance = this.plugin.instantiateResource(request, client, secret);

		Parameters expected = new OpenNebulaGenericRequestPlugin.Parameters();
		expected.getClasses().add(int.class);
		expected.getValues().add(1);

		// exercise
//		Parameters parameters = this.plugin.generateParametersMap(request, instance, client);

		// verify
//		Assert.assertEquals(expected.getClasses().get(0), parameters.getClasses().get(0));
//		Assert.assertEquals(expected.getValues().get(0), parameters.getValues().get(0));
	}
	
	// test case: When calling the generateParametersMap method, without parameters,
	// this implies that the method does not receive parameters.
	@Test
	public void testGenerateParametersWithMapEmpty() throws InvalidParameterException {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parametersMap = new HashMap<>();

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, oneResource, oneMethod,
				resourceId, parametersMap);

		Client client = Mockito.mock(Client.class);
		String secret = OPENNEBULA_TOKEN;
//		Object instance = this.plugin.instantiateResource(request, client, secret);

		int expected = 0;

		// exercise
//		Parameters parameters = this.plugin.generateParametersMap(request, instance, client);

		// verify
//		Assert.assertEquals(expected, parameters.getClasses().size());
//		Assert.assertEquals(expected, parameters.getValues().size());
	}
	
	// test case: When calling the generateMethod method, without a resource
	// instance, this implies that a static method will be invoked.
	@Test
	public void testGenerateMethodWithoutResourceInstance() {
		// set up
		String url = null;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = INFO_METHOD;
		String resourceId = null;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put(ID_PARAMETER_KEY, ONE_VALUE);

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, oneResource, oneMethod,
				resourceId, parametersMap);

		Client client = Mockito.mock(Client.class);

		Parameters parameters = new OpenNebulaGenericRequestPlugin.Parameters();
		parameters.getClasses().add(Client.class);
		parameters.getClasses().add(int.class);
		parameters.getValues().add(client);
		parameters.getValues().add(1);

		String expected = oneMethod;
		int parametersNumber = 2;

		// exercise
//		Method method = this.plugin.generateMethod(request, parameters);

		// verify
//		Assert.assertEquals(expected, method.getName());
//		Assert.assertEquals(parametersNumber, method.getParameterCount());
	}
	
	// test case: When calling the generateMethod method, with a resource instance,
	// this implies that a method this instance will be invoked.
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

		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, oneResource, oneMethod,
				resourceId, parametersMap);

		Parameters parameters = new OpenNebulaGenericRequestPlugin.Parameters();
		parameters.getClasses().add(String.class);
		parameters.getValues().add(template);

		String expected = oneMethod;
		int parametersNumber = 1;

		// exercise
//		Method method = this.plugin.generateMethod(request, parameters);

		// verify
//		Assert.assertEquals(expected, method.getName());
//		Assert.assertEquals(parametersNumber, method.getParameterCount());
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
