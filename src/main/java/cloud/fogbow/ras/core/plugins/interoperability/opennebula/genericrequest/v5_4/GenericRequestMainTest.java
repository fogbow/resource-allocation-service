package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;

import cloud.fogbow.common.util.GsonHolder;

// this class is temporary for performing manual tests in the OpenNebula API,
// and will be removed at the end of the plugin implementation.
public class GenericRequestMainTest {

	@SuppressWarnings({ "unchecked", "resource", "unused", "rawtypes" })
	public static void main(String args[]) throws Exception {

		Scanner read = new Scanner(System.in);
		
		// Get the OneResource (A Pool or PoolElement)...
		System.out.print("Enter the desired resource: ");
		String resource = read.next();
		
		// Get the resource ID...
		System.out.print("Does the requisition have a resource ID? (y/n): ");
		String option = read.next();
		String id;
		if (option.equals("y")) {
			System.out.print("Enter the value of the resource ID: ");
			id = read.next();
		} else {
			id = null;
		}
		
		// Get the method to be used...
		System.out.print("Enter the method to be used: ");
		String strMethod = read.next();
		
		// Add the parameters to be used for method... 
		System.out.print("Want to add parameters (y/n): ");
		option = read.next();
		Map<String, String> parameters = new HashMap<>();
		String key;
		String strValue;
		int counter = 0;
		while (option.equals("y")) {
			System.out.print("enter the key parameters you want to add:  ");
			key = read.next();
			System.out.print("enter the value parameters you want to add:  ");
			strValue = read.next();
			parameters.put(key, strValue);
			System.out.print("continue adding parameters (y/n): ");
			option = read.next();
			counter++;
		}
		
		String secret = "oneadmin:opennebula";
		String endpoint = "http://10.11.5.20:2633/RPC2";
		Client client = new Client(secret, endpoint);
		
		OpenNebulaGenericRequest request = new OpenNebulaGenericRequest(endpoint, resource, strMethod, id, parameters);
		
		// Generate class type of resource...
		OneResource oneResource = OneResource.getValueOf(request.getOneResource());
		Class resourceClassType = oneResource.getClassType();
		System.out.println("ClassType returned: " + resourceClassType.getName());

		// Instanciate a resource...
		Object instance = null;
        if (request.getOneResource().endsWith("Pool")) {
        	instance = oneResource.createInstance(client);
			System.out.println("Instance returned: " + instance.toString());
        } else if (request.getResourceId() != null) {
			instance = oneResource.createInstance(Integer.parseInt(request.getResourceId()), client);
			System.out.println("Instance returned: " + instance.toString());
		} else if (oneResource.equals(OneResource.CLIENT)) {
				instance = (Client) oneResource.createInstance(secret, request.getUrl());
				System.out.println("Instance returned: " + instance.toString());
		}
		
        // Working with map of parameters...
        List classes = new ArrayList<>();
		List values = new ArrayList<>();
		if (request.getResourceId() != null && !request.getOneResource().endsWith("Pool") && !request.getParameters().isEmpty()) {
			classes.add(Client.class);
			values.add(client);
		}
		for (Map.Entry<String, String> entries : request.getParameters().entrySet()) {
			OneParameter oneParameter = OneParameter.getValueOf(entries.getKey());
			classes.add(oneParameter.getClassType());
			values.add(oneParameter.getValue(entries.getValue()));
		}

		// Generate generic method...
		Method method = OneGenericMethod.generate(resourceClassType, request.getOneMethod(), classes);
		System.out.println("Method returned: " + method.getName());

		// Invoke generic method...
		Object response = OneGenericMethod.invoke(instance, method, values);

		// Requesting a response...
		OneResponse oneResponse;
		if (response instanceof OneResponse) {
			oneResponse = (OneResponse) response;
			System.out.println("Response error status returned: " + oneResponse.isError());
			System.out.println("Response boolean message returned: " + oneResponse.getBooleanMessage());
			System.out.println("Response int message returned: " + oneResponse.getIntMessage());
			System.out.println("Response message returned: " + oneResponse.getMessage());
			System.out.println("Response error message returned: " + oneResponse.getErrorMessage());
		} else {
			String message = GsonHolder.getInstance().toJson(response);
			System.out.println(new OpenNebulaGenericRequestResponse(message, false));	
		}
	}

}
