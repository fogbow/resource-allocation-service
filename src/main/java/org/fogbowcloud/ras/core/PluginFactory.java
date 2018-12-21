package org.fogbowcloud.ras.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;

import java.lang.reflect.Constructor;

public class PluginFactory {
    private static final Logger LOGGER = Logger.getLogger(PluginFactory.class);

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
            String msg = Messages.Fatal.UNABLE_TO_FIND_CLASS;
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
            String msg = Messages.Fatal.UNABLE_TO_FIND_CLASS;
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
            String msg = Messages.Fatal.UNABLE_TO_FIND_CLASS;
            throw new FatalErrorException(String.format(msg, pluginClassName));
        } catch (Exception e) {
            throw new FatalErrorException(e.getMessage(), e);
        }

        return pluginInstance;
    }
}
