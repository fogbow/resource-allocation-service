package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class OneGenericMethod {

	public static Method generate(Class classType, String method, List<Class> parameters) {
		try {
			if (parameters.isEmpty()) {
				return classType.getMethod(method);
			} else {
				return classType.getMethod(method, parameters.toArray(new Class[parameters.size()]));
			}
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
		return null;
	}

	public static Object invoke(Object instance, Method method, List<Object> values) {
		if (instance == null) {
			instance = method;
		}
		try {
			if (values.isEmpty()) {
				return method.invoke(instance);
			} else {
				return method.invoke(instance, values.toArray(new Object[values.size()]));
			}
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
            e1.printStackTrace();
        }
		return null;
	}

}
