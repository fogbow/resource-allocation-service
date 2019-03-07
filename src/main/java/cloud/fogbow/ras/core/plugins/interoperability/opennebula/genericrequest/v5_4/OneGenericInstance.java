package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class OneGenericInstance {

	public static Object instantiate(Constructor constructor, List<Object> objects) {
		try {
			if (objects.isEmpty()) {
				return constructor.newInstance();
			} else {
				return constructor.newInstance(objects.toArray(new Object[objects.size()]));
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
}
