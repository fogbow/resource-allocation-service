package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.log4j.Logger;

import cloud.fogbow.ras.constants.Messages;

public class OneGenericMethod {

	private static final Logger LOGGER = Logger.getLogger(OneGenericMethod.class);
	
	public static Method generate(Class classType, String method, List<Class> parameters) {
		try {
			if (parameters.isEmpty()) {
				return classType.getMethod(method);
			} else {
				return classType.getMethod(method, parameters.toArray(new Class[parameters.size()]));
			}
        } catch (NoSuchMethodException | SecurityException e) {
            LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e), e);
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
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        	LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e), e);
        }
		return null;
	}

}
