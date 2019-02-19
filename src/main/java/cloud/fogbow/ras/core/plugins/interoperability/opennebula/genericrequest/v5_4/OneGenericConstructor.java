package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Constructor;
import java.util.List;

public class OneGenericConstructor {

	public static Constructor generate(Class classType, List<Class> parameters) {
		try {
			if (parameters.isEmpty()) {
				return classType.getConstructor();
			} else {
				return classType.getDeclaredConstructor(parameters.toArray(new Class[parameters.size()]));
			}
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
