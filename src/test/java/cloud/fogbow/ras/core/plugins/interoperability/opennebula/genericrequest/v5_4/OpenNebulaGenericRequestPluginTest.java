package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vm.VirtualMachinePool;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaFogbowGenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4.OpenNebulaGenericRequestPlugin.Parameters;

public class OpenNebulaGenericRequestPluginTest {

private OpenNebulaGenericRequestPlugin plugin;
	
	@Before
	public void setUp() {
		this.plugin = Mockito.spy(OpenNebulaGenericRequestPlugin.class);
	}
	
	// test case: ...
	@Test
	public void testInstantiateResourceClientSuccessfully() throws InvalidParameterException, UnexpectedException {
		// set up
		String url = "http://localhost:2633/RPC2";
		String oneResource = "Client";
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parameters = null;
		
		OpenNebulaFogbowGenericRequest request = new OpenNebulaFogbowGenericRequest(url, 
				oneResource, oneMethod, resourceId, parameters);

		Client client = null;
		String secret = "user:password";

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
		String oneResource = "VirtualMachinePool";
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
		String oneResource = "VirtualMachine";
		String oneMethod = null;
		String resourceId = "1";
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
		String oneResource = "VirtualMachine";
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
		String oneResource = "VirtualMachine";
		String oneMethod = null;
		String resourceId = "";
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
		String oneResource = "VirtualMachine";
		String oneMethod = null;
		String resourceId = "fake-resource-id";
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
		String oneResource = "VirtualMachine";
		String oneMethod = null;
		String resourceId = "1";
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put("id", "1");
		
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
		String oneResource = "VirtualMachine";
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put("id", "1");

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
		String oneResource = "VirtualMachine";
		String oneMethod = null;
		String resourceId = "";
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put("id", "1");

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
		String oneResource = "VirtualMachinePool";
		String oneMethod = null;
		String resourceId = null;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put("id", "1");

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
		String oneResource = "VirtualMachine";
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
		String oneResource = "VirtualMachine";
		String oneMethod = "info";
		String resourceId = null;
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put("id", "1");

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
		String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" 
				+ "<TEMPLATE>\n"
				+ "    <DISK>\n" 
				+ "        <IMAGE_ID>1</IMAGE_ID>\n" 
				+ "    </DISK>\n" 
				+ "</TEMPLATE>\n";

		String url = null;
		String oneResource = "VirtualMachine";
		String oneMethod = "diskAttach";
		String resourceId = "1";
		Map<String, String> parametersMap = new HashMap<>();
		parametersMap.put("diskTemplate", template);

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
}
