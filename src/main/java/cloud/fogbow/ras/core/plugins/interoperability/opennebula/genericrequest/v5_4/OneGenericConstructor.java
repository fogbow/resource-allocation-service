package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Constructor;

public class OneGenericConstructor {

	public static Constructor generate(Class classType, Class[] parameters) {
		try {
			switch (parameters.length) {
			case 1:
				return classType.getDeclaredConstructor(parameters[0]);
			case 2:
				return classType.getDeclaredConstructor(parameters[0], parameters[1]);
			default:
				return classType.getConstructor();
			}
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
