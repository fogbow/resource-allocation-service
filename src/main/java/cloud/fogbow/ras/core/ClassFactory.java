package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.ras.constants.Messages;
import org.apache.log4j.Logger;

import java.lang.reflect.Constructor;

// Each package has to have its own ClassFactory

public class ClassFactory {
    private static final Logger LOGGER = Logger.getLogger(ClassFactory.class);

    public Object createPluginInstance(String pluginClassName, String parameter1, String parameter2)
            throws FatalErrorException {

        Object pluginInstance = null;

        Class<?> classpath;
        Constructor<?> constructor;

        try {
            classpath = Class.forName(pluginClassName);
            constructor = classpath.getConstructor(String.class, String.class);
            pluginInstance = constructor.newInstance(parameter1, parameter2);
        } catch (ClassNotFoundException e) {
            String msg = Messages.Fatal.UNABLE_TO_FIND_CLASS_S;
            throw new FatalErrorException(String.format(msg, pluginClassName));
        } catch (Exception e) {
            throw new FatalErrorException(e.getMessage(), e);
        }

        return pluginInstance;
    }

    public Object createPluginInstance(String pluginClassName, String parameter) throws FatalErrorException {

        Object pluginInstance = null;

        Class<?> classpath;
        Constructor<?> constructor;

        try {
            classpath = Class.forName(pluginClassName);
            constructor = classpath.getConstructor(String.class);
            pluginInstance = constructor.newInstance(parameter);
        } catch (ClassNotFoundException e) {
            String msg = Messages.Fatal.UNABLE_TO_FIND_CLASS_S;
            throw new FatalErrorException(String.format(msg, pluginClassName));
        } catch (Exception e) {
            throw new FatalErrorException(e.getMessage(), e);
        }

        return pluginInstance;
    }

    public Object createPluginInstance(String pluginClassName) throws FatalErrorException {

        Object pluginInstance = null;

        Class<?> classpath;
        Constructor<?> constructor;

        try {
            classpath = Class.forName(pluginClassName);
            constructor = classpath.getConstructor();
            pluginInstance = constructor.newInstance();
        } catch (ClassNotFoundException e) {
            String msg = Messages.Fatal.UNABLE_TO_FIND_CLASS_S;
            throw new FatalErrorException(String.format(msg, pluginClassName));
        } catch (Exception e) {
            throw new FatalErrorException(e.getMessage(), e);
        }

        return pluginInstance;
    }
}
