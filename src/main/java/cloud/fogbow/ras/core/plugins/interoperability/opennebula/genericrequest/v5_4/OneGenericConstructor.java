package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Constructor;

public class OneGenericConstructor {

	public static Constructor generate(Class classType, Class[] parameters) {
		switch (parameters.length) {
		case 1:
			try {
				return classType.getDeclaredConstructor(parameters[0]);
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
			break;
		
		case 2:
			try {
				return classType.getDeclaredConstructor(parameters[0], parameters[1]);
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
			break;
			
		default:
			try {
				return classType.getConstructor();
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
			break;
		}
		return null;
	}
	
}
