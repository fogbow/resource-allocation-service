package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;

public class GenericRequestMainTest {

	@SuppressWarnings({ "unchecked", "resource", "unused", "rawtypes" })
	public static void main(String args[]) throws Exception {

		Scanner read = new Scanner(System.in);
		
		// Get the OneResourse (A Pool or PoolElement)...
		System.out.print("Enter the desired resource: ");
		String resourse = read.next();
		
		// Get the resourse ID...
		System.out.print("Does the requisition have a resourse ID? (y/n): ");
		String option = read.next();
		String id;
		if (option.equals("y")) {
			System.out.print("Enter the value of the resourse ID: ");
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
		
		// Generate class type of resourse...
		OneResourse oneResourse = OneResourse.getValueOf(resourse);
		Class resourseClassType = oneResourse.getClassType();
		System.out.println("ClassType returned: " + resourseClassType.getName());

		List classes = new ArrayList<>();
		List values = new ArrayList<>();
		
		// Instanciate a resourse...
		Object instance = null;
		if (id != null) {
			instance = oneResourse.createInstance(Integer.parseInt(id), client);
			System.out.println("Instance returned: " + instance.toString());
		} else {
			if (oneResourse.equals(OneResourse.CLIENT)) {
				instance = (Client) oneResourse.createInstance(secret, endpoint);
				System.out.println("Instance returned: " + instance.toString());
			}
			if (!parameters.isEmpty()) {
				classes.add(Client.class);
				values.add(client);
			}
		}

		// Working with map of parameters...
		OneParameter oneParameter;
		for (Map.Entry<String, String> entries : parameters.entrySet()) {
			key = entries.getKey();
			oneParameter = OneParameter.getValueOf(key);
			classes.add(oneParameter.getClassType());
			values.add(oneParameter.getValue(entries.getValue()));
		}

		// Generate generic method...
		Method method = OneGenericMethod.generate(resourseClassType, strMethod, classes);
		System.out.println("Method returned: " + method.getName());

		// Invoke generic method...
		Object response = OneGenericMethod.invoke(instance, method, values);

		OneResponse oneResponse = (OneResponse) response;
		System.out.println("Response status returned: " + oneResponse.isError());
		System.out.println("Response boolean message returned: " + oneResponse.getBooleanMessage());
		System.out.println("Response int message returned: " + oneResponse.getIntMessage());
		System.out.println("Response message returned: " + oneResponse.getMessage());
		System.out.println("Response error message returned: " + oneResponse.getErrorMessage());

	}

}
