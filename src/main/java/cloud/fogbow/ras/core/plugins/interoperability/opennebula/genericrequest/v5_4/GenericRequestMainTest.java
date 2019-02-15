package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;

public class GenericRequestMainTest {

	public static void main(String args[]) throws Exception {

		Scanner read = new Scanner(System.in);
		
		// Get the resourse (poolElement)...
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
		String value;
		int counter = 0;
		while (option.equals("y")) {
			System.out.print("enter the key parameters you want to add:  ");
			key = read.next();
			System.out.print("enter the value parameters you want to add:  ");
			value = read.next();
			parameters.put(key, value);
			System.out.print("continue adding parameters (y/n): ");
			option = read.next();
			counter++;
		}
		
		// Generate class type of resourse...
		OneResourse oneResourse = OneResourse.getValueOf(resourse);
		Class resourseClassType = oneResourse.getClassType();
		System.out.println("ClassType returned: " +resourseClassType.getName());
		
		// Instanciate a resourse...
		Object instance;
		if (oneResourse.getName().equals("Client")) {
			instance = (Client) oneResourse.createInstance("oneadmin:opennebula","http://10.11.5.20:2633/RPC2");
			System.out.println("Instance returned: " +instance.toString());
		} else {
			Client client = new Client("oneadmin:opennebula","http://10.11.5.20:2633/RPC2");
			instance = oneResourse.createInstance(Integer.parseInt(id),client);
			System.out.println("Instance returned: " +instance.toString());
		}
		
		Method method = resourseClassType.getMethod(strMethod);
		System.out.println("Method returned: " +method.getName());
		
		OneResponse response = (OneResponse) method.invoke(instance);
		if (response.isError()) {
			System.out.println("Error response returned: " +response.getErrorMessage());
		}
		System.out.println("Response returned: " +response.getMessage());
		
		
	}

}
