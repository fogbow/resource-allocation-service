package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.log4j.Logger;

import cloud.fogbow.ras.constants.Messages;

public class OneGenericInstance {

	private static final Logger LOGGER = Logger.getLogger(OneGenericInstance.class);
	
	public static Object instantiate(Constructor constructor, List<Object> objects) {
		try {
			if (objects.isEmpty()) {
				return constructor.newInstance();
			} else {
				return constructor.newInstance(objects.toArray(new Object[objects.size()]));
			}
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e), e);
		}
		return null;
	}
}
