package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;

public class OneGenericMethod {

	public static Method generate(Class classType, String method, List<Class> parameters) throws UnexpectedException {
		try {
			if (parameters.isEmpty()) {
				return classType.getMethod(method);
			} else {
				return classType.getMethod(method, parameters.toArray(new Class[parameters.size()]));
			}
        } catch (NoSuchMethodException | SecurityException e) {
            throw new UnexpectedException(String.format(Messages.Exception.FAILED_TO_GENERATE_METHOD_S, method), e);
        }
	}

	public static Object invoke(Object instance, Method method, List<Object> values) throws UnexpectedException {
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
            throw new UnexpectedException(String.format(Messages.Exception.FAILED_TO_INVOKE_METHOD_S, method), e);
        }
	}
}
