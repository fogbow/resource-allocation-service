package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class OneGenericMethod {

	public static Method generate(Class classType, String method, List<Class> parameters) {
		switch (parameters.size()) {
		case 1:
			try {
				return classType.getMethod(method, parameters.get(0));
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
			break;
		
		case 2:
			try {
				return classType.getMethod(method, parameters.get(0), parameters.get(1));
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
			break;

		default:
			try {
				return classType.getMethod(method);
			} catch (NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
			break;
		}
		return null;
	}

	public static Object invoke(Object instance, Method method, List<Object> values) {
		switch (values.size()) {
		case 1:
			try {
				return method.invoke(instance, values.get(0));
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				e1.printStackTrace();
			}
			break;

		default:
			try {
				return method.invoke(instance);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
			break;
		}
		return null;
	}

	public static Object invoke(Method method, List<Object> values) {
		// NOTE(pauloewerton): this is a static method invocation so making this explicit by passing a null object
		// where an instance would be expected.
		Object obj = null;

		try {
			if (values.isEmpty()) {
				return method.invoke(obj);
			} else {
				return method.invoke(obj, values.toArray(new Object[values.size()]));
			}
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
            e1.printStackTrace();
        }

		return null;
	}
}
