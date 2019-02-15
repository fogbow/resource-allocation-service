package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class OneGenericInstance {

	public static Object instantiate(Constructor constructor, Object[] objects) {
		switch (objects.length) {
		case 1:
			try {
				return constructor.newInstance(objects[0]);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
			break;
			
		case 2:
			try {
				return constructor.newInstance(objects[0], objects[1]);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
			break;

		default:
			try {
				return constructor.newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
			break;
		}
		return null;
	}

}
