package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Method;

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
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4.OpenNebulaGenericRequestPlugin.Parameters;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GsonHolder.class, OpenNebulaClientUtil.class, VirtualMachine.class})
public class OpenNebulaGenericRequestPluginTest {
	
	private static final String ANYTHING_STRING = "anything";
	private static final String CLIENT_RESOURCE = "Client";
	private static final String DEFAULT_URL = "http://localhost:2633/RPC2";
	private static final String DISK_ATTACH_METHOD = "diskAttach";
	private static final String EMPTY_STRING = "";
	private static final String ID_PARAMETER_KEY = "id";
	private static final String INFO_METHOD = "info";
	private static final String ONE_GENERIC_REQUEST_NULL_OBJECT = "{oneGenericRequest=null}";
	private static final String ONE_VALUE = "1";
	private static final String OPENNEBULA_TOKEN = "user:password";
	private static final String PARAMETER_HARD = "hard";
	private static final String POWER_OFF_METHOD = "poweroff";
	private static final String TRUE_VALUE = "true";
	private static final String VIRTUAL_MACHINE_POOL_RESOURCE = "VirtualMachinePool";
	private static final String VIRTUAL_MACHINE_RESOURCE = "VirtualMachine";
	
	private OpenNebulaGenericRequestPlugin plugin;
	
	@Before
	public void setUp() {
		this.plugin = Mockito.spy(OpenNebulaGenericRequestPlugin.class);
	}
	
	// test case: When calling the redirectGenericRequest method with a null
	// OneGenericRequest object, an InvalidParameterException must be thrown.
	@Test(expected = InvalidParameterException.class) // verify
	public void testRedirectGenericRequestWithNullContent() throws FogbowException {
		// set up
		String genericRequest = ONE_GENERIC_REQUEST_NULL_OBJECT;
		OpenNebulaUser cloudUser = Mockito.mock(OpenNebulaUser.class);

		// exercise
		this.plugin.redirectGenericRequest(genericRequest, cloudUser);
	}
	
	// test case: When calling the redirectGenericRequest method without a valid
	// URL in the request, an InvalidParameterException must be thrown.
	@Test(expected = InvalidParameterException.class) // verify
	public void testRedirectGenericRequestWithoutAValidUrl() throws FogbowException {
		// set up
		String url = EMPTY_STRING;
		String genericRequest = createGenericRequest(url, null, null, null, null, null);
		OpenNebulaUser cloudUser = Mockito.mock(OpenNebulaUser.class);

		// exercise
		this.plugin.redirectGenericRequest(genericRequest, cloudUser);
	}
	
	// test case: When calling the redirectGenericRequest method without a valid
	// resource in the request, an InvalidParameterException must be thrown.
	@Test(expected = InvalidParameterException.class) // verify
	public void testRedirectGenericRequestWithoutAValidResource() throws FogbowException {
		// set up
		String url = DEFAULT_URL;
		String oneResource = EMPTY_STRING;
		String genericRequest = createGenericRequest(url, oneResource, null, null, null, null);
		OpenNebulaUser cloudUser = Mockito.mock(OpenNebulaUser.class);

		// exercise
		this.plugin.redirectGenericRequest(genericRequest, cloudUser);
	}
	
	// test case: When calling the redirectGenericRequest method without a valid
	// method in the request, an InvalidParameterException must be thrown.
	@Test(expected = InvalidParameterException.class) // verify
	public void testRedirectGenericRequestWithoutAValidMethod() throws FogbowException {
		// set up
		String url = DEFAULT_URL;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = EMPTY_STRING;
		String genericRequest = createGenericRequest(url, oneResource, oneMethod, null, null, null);
		OpenNebulaUser cloudUser = Mockito.mock(OpenNebulaUser.class);

		// exercise
		this.plugin.redirectGenericRequest(genericRequest, cloudUser);
	}
	
	// test case: When calling the redirectGenericRequest method with a valid
	// genericRequest and cloudUser, it must verify that a sequence of methods was
	// executed correctly.
	@Test
	public void testRedirectGenericRequestSuccessfully() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		String url = DEFAULT_URL;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = POWER_OFF_METHOD;
		String resourceId = ONE_VALUE;
		String parameter = PARAMETER_HARD;
		String content = TRUE_VALUE;
		String genericRequest = createGenericRequest(url, oneResource, oneMethod, resourceId, parameter, content);

		String userId = null;
		String userName = null;
		String tokenValue = OPENNEBULA_TOKEN;
		OpenNebulaUser cloudUser = Mockito.spy(new OpenNebulaUser(userId, userName, tokenValue));

		// exercise
		this.plugin.redirectGenericRequest(genericRequest, cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.plugin, Mockito.times(1)).instantiateResource(Mockito.any(CreateOneGenericRequest.class),
				Mockito.eq(client), Mockito.eq(tokenValue));
		Mockito.verify(this.plugin, Mockito.times(1)).generateParametersMap(Mockito.any(CreateOneGenericRequest.class),
				Mockito.any(Object.class), Mockito.eq(client));
		Mockito.verify(this.plugin, Mockito.times(1)).generateMethod(Mockito.any(CreateOneGenericRequest.class),
				Mockito.any(OpenNebulaGenericRequestPlugin.Parameters.class));
		Mockito.verify(this.plugin, Mockito.times(1)).invokeGenericMethod(Mockito.any(Object.class),
				Mockito.any(OpenNebulaGenericRequestPlugin.Parameters.class), Mockito.any(Method.class));
		Mockito.verify(this.plugin, Mockito.times(1)).reproduceMessage(Mockito.any(Object.class));
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
		Mockito.when(oneResponse.getMessage()).thenReturn(ANYTHING_STRING);

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
		Mockito.when(oneResponse.getMessage()).thenReturn(ANYTHING_STRING);

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
		this.plugin.reproduceMessage(ANYTHING_STRING);

		// verify
		PowerMockito.verifyStatic(GsonHolder.class, VerificationModeFactory.times(1));
		GsonHolder.getInstance();
	}
	
	// test case: When the invokeGenericMethod method is called, it must be executed
	// invoke method successfully in the OneGenericMethod class.
	@Test
	public void testInvokeGenericMethodSuccessfully()
			throws NoSuchMethodException, SecurityException, InvalidParameterException, UnexpectedException {

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
	@Test
	public void testInstantiateResourceClientSuccessfully() throws InvalidParameterException {
		// set up
		String url = DEFAULT_URL;
		String oneResource = CLIENT_RESOURCE;
		String genericRequest = createGenericRequest(url, oneResource, null, null, null, null);
		CreateOneGenericRequest request = CreateOneGenericRequest.fromJson(genericRequest);

		Client client = null;
		String secret = OPENNEBULA_TOKEN;

		OneResource clientResource = OneResource.CLIENT;
		Class<Client> expected = clientResource.getClassType();

		// exercise
		Object instance = this.plugin.instantiateResource(request, client, secret);

		// verify
		Assert.assertEquals(expected, instance.getClass());
	}
	
	// test case: When calling the instantiateResource method, passing a resource
	// pool on the request, this should return an instance of this resource type.
	@Test
	public void testInstantiateResourcePoolSuccessfully() throws InvalidParameterException {
		// set up
		String url = DEFAULT_URL;
		String oneResource = VIRTUAL_MACHINE_POOL_RESOURCE;
		String json = createGenericRequest(url, oneResource, null, null, null, null);
		CreateOneGenericRequest request = CreateOneGenericRequest.fromJson(json);

		Client client = null;
		String secret = null;

		Class<VirtualMachinePool> expected = VirtualMachinePool.class;

		// exercise
		Object instance = this.plugin.instantiateResource(request, client, secret);

		// verify
		Assert.assertEquals(expected, instance.getClass());
	}
	
	// test case: When calling the instantiateResource method, with a valid
	// resourceId in the request, this must return an instance of the resource
	// passed in the generic request.
	@Test
	public void testInstantiateResourceWithIdSuccessfully() throws InvalidParameterException {
		// set up
		String url = DEFAULT_URL;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String resourceId = ONE_VALUE;

		String json = createGenericRequest(url, oneResource, null, resourceId, null, null);
		CreateOneGenericRequest request = CreateOneGenericRequest.fromJson(json);
		
		Client client = null;
		String secret = null;

		Class<VirtualMachine> expected = VirtualMachine.class;

		// exercise
		Object instance = this.plugin.instantiateResource(request, client, secret);

		// verify
		Assert.assertEquals(expected, instance.getClass());
	}
	
	// test case: When calling the generateParametersMap method, without a resource
	// instance, this implies that a static method will be invoked, and therefore
	// you do need to add a "Client" resource to the parameter map.
	@Test
	public void testGenerateParametersMapWithoutResourceInstance() throws InvalidParameterException {
		// set up
		String url = DEFAULT_URL;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String parameter = ID_PARAMETER_KEY;
		String content = ONE_VALUE;

		String json = createGenericRequest(url, oneResource, null, null, parameter, content);
		CreateOneGenericRequest request = CreateOneGenericRequest.fromJson(json);
		
		Client client = Mockito.mock(Client.class);
		Object instance = null;

		Parameters expected = new OpenNebulaGenericRequestPlugin.Parameters();
		expected.getClasses().add(Client.class);
		expected.getClasses().add(int.class);
		expected.getValues().add(client);
		expected.getValues().add(1);

		// exercise
		Parameters parameters = this.plugin.generateParametersMap(request, instance, client);

		// verify
		Assert.assertEquals(expected.getClasses().get(0), parameters.getClasses().get(0));
		Assert.assertEquals(expected.getClasses().get(1), parameters.getClasses().get(1));
		Assert.assertEquals(expected.getValues().get(0), parameters.getValues().get(0));
		Assert.assertEquals(expected.getValues().get(1), parameters.getValues().get(1));
	}
	
	// test case: When calling the generateParametersMap method, with a resource
	// instance of the type "Pool", this implies that not will need to add a
	// "Client" resource to the parameter map.
	@Test
	public void testGenerateParametersMapWithResourcePool() throws InvalidParameterException {
		// set up
		String oneResource = VIRTUAL_MACHINE_POOL_RESOURCE;
		String parameter = ID_PARAMETER_KEY;
		String content = ONE_VALUE;

		String json = createGenericRequest(null, oneResource, null, null, parameter, content);
		CreateOneGenericRequest request = CreateOneGenericRequest.fromJson(json);
		
		Client client = Mockito.mock(Client.class);
		String secret = OPENNEBULA_TOKEN;
		Object instance = this.plugin.instantiateResource(request, client, secret);

		Parameters expected = new OpenNebulaGenericRequestPlugin.Parameters();
		expected.getClasses().add(int.class);
		expected.getValues().add(1);

		// exercise
		Parameters parameters = this.plugin.generateParametersMap(request, instance, client);

		// verify
		Assert.assertEquals(expected.getClasses().get(0), parameters.getClasses().get(0));
		Assert.assertEquals(expected.getValues().get(0), parameters.getValues().get(0));
	}
	
	// test case: When calling the generateMethod method, without a resource
	// instance, this implies that a static method will be invoked.
	@Test
	public void testGenerateMethodWithoutResourceInstance() throws UnexpectedException {
		// set up
		String url = DEFAULT_URL;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = INFO_METHOD;
		String parameter = ID_PARAMETER_KEY;
		String content = ONE_VALUE;

		String json = createGenericRequest(url, oneResource, oneMethod, null, parameter, content);
		CreateOneGenericRequest request = CreateOneGenericRequest.fromJson(json);

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
	
	// test case: When calling the generateMethod method, with a resource instance,
	// this implies that a method this instance will be invoked.
	@Test
	public void testGenerateMethodWithResourceInstance() throws UnexpectedException {
		// set up
		String url = DEFAULT_URL;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = DISK_ATTACH_METHOD;
		String resourceId = ONE_VALUE;
		String parameter = ID_PARAMETER_KEY;
		String content = ONE_VALUE;

		String json = createGenericRequest(url, oneResource, oneMethod, resourceId, parameter, content);
		CreateOneGenericRequest request = CreateOneGenericRequest.fromJson(json);

		String template = getAttachDiskTemplate();
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
	
	// test case: When calling the convertToInteger method with an invalid numeric
	// string, it must throw an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testConvertToIntegerUnsuccessfully() throws InvalidParameterException {
		// set up
		String number = EMPTY_STRING;
		// exercise
		this.plugin.convertToInteger(number);
	}
	
	// test case: When calling the generateParametersMap method without a valid
	// parameter, an InvalidParameterException will be thrown.
	@Test(expected = InvalidParameterException.class) // verify
	public void testGenerateParametersMapThrowInvalidParameterException() throws InvalidParameterException {
		// set up
		String url = DEFAULT_URL;
		String oneResource = VIRTUAL_MACHINE_RESOURCE;
		String oneMethod = INFO_METHOD;
		String parameter = ANYTHING_STRING;
		String content = ONE_VALUE;

		String json = createGenericRequest(url, oneResource, oneMethod, null, parameter, content);
		CreateOneGenericRequest request = CreateOneGenericRequest.fromJson(json);
		Object instance = null;
		Client client = Mockito.mock(Client.class);

		// exercise
		this.plugin.generateParametersMap(request, instance, client);
	}
	
	// test case: When calling the isValidParameter method with a content null, this
	// will return false.
	@Test
	public void testIsValidParameterWithNullContent() {
		// set up
		String content = null;

		// exercise
		boolean status = this.plugin.isValidParameter(content);

		// verify
		Assert.assertFalse(status);
	}

	// test case: When calling the isValidParameter method with a valid parameter,
	// this will return true.
	@Test
	public void testIsValidParameterSuccessfully() {
		// set up
		String parameter = "action";

		// exercise
		boolean status = this.plugin.isValidParameter(parameter);

		// verify
		Assert.assertTrue(status);
	}
	
	// test case: When calling the isValidResource method with a content null, this
	// will return false.
	@Test
	public void testIsValidResourceWithNullContent() {
		// set up
		String content = null;

		// exercise
		boolean status = this.plugin.isValidResource(content);

		// verify
		Assert.assertFalse(status);
	}
	
	// test case: When calling the isValidResource method with a valid resource,
	// this will return true.
	@Test
	public void testIsValidResourceSuccessfully() {
		// set up
		String resource = "Client";

		// exercise
		boolean status = this.plugin.isValidResource(resource);

		// verify
		Assert.assertTrue(status);
	}
	
	// test case: When calling the isValidUrl method with a content null, this
	// will return false.
	@Test
	public void testIsValidUrlWithNullContent() {
		// set up
		String content = null;

		// exercise
		boolean status = this.plugin.isValidUrl(content);

		// verify
		Assert.assertFalse(status);
	}
	
	// test case: When calling the isValidUrl method with a valid url,
	// this will return true.
	@Test
	public void testIsValidUrlSuccessfully() {
		// set up
		String url = DEFAULT_URL;

		// exercise
		boolean status = this.plugin.isValidUrl(url);

		// verify
		Assert.assertTrue(status);
	}
	
	// test case: When calling the isValid method with a valid content,
	// this will return true.
	@Test
	public void testIsValidSuccessfully() {
		// set up
		String content = ANYTHING_STRING;

		// exercise
		boolean status = this.plugin.isValid(content);
		
		// verify
		Assert.assertTrue(status);
	}
	
	// test case: When calling the isValid method with a content null, this
	// will return false.
	@Test
	public void testIsValidWithNullContent() {
		// set up
		String content = null;

		// exercise
		boolean status = this.plugin.isValid(content);
		
		// verify
		Assert.assertFalse(status);
	}
	
	// test case: When calling the isValid method with a content empty, this
	// will return false.
	@Test
	public void testIsValidWithEmptyString() {
		// set up
		String content = EMPTY_STRING;

		// exercise
		boolean status = this.plugin.isValid(content);

		// verify
		Assert.assertFalse(status);
	}
	
	private String getAttachDiskTemplate() {
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" 
				+ "<TEMPLATE>\n"
				+ "    <DISK>\n" 
				+ "        <IMAGE_ID>1</IMAGE_ID>\n" 
				+ "    </DISK>\n" 
				+ "</TEMPLATE>\n";
		
		return template;
	}
	
	private String addOneParameters(String parameter, String content) {
		String template = "	,\"oneParameters\":{\"%s\":\"%s\"}\n";
		return String.format(template, parameter, content);
	}
	
	private String addResourceId(String id) {
		String template = "	,\"resourceId\": \"%s\"\n";
		return String.format(template, id);
	}
	
	private String createGenericRequest(String url, String resource, String method, String id, String parameter,
			String content) {
		
		String resourceId = id == null ? EMPTY_STRING : addResourceId(id);
		String parameters = parameter == null ? EMPTY_STRING : addOneParameters(parameter, content);
		String template = "{\n" 
				+ " \"oneGenericRequest\":{\n" 
				+ "	\"url\":\"%s\",\n" 
				+ "	\"oneResource\":\"%s\",\n"
				+ "	\"oneMethod\":\"%s\"\n" 
				+ "%s" 
				+ "%s" 
				+ "	}\n" 
				+ "}";

		return String.format(template, url, resource, method, resourceId, parameters);
	}
	
}
