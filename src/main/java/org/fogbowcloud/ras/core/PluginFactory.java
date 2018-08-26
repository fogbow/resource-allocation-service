package org.fogbowcloud.ras.core;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;

import java.lang.reflect.Constructor;

public class PluginFactory {
    private static final Logger LOGGER = Logger.getLogger(PluginFactory.class.getName());

    public Object createPluginInstance(String pluginClassName) throws FatalErrorException {

        Object pluginInstance = null;

        Class<?> classpath;
        Constructor<?> constructor;

        try {
            classpath = Class.forName(pluginClassName);
            constructor = classpath.getConstructor();
            pluginInstance = constructor.newInstance();
        } catch (ClassNotFoundException e) {
            String msg = "No " + pluginClassName + " class under this repository. Please inform a valid class.";
            throw new FatalErrorException(msg);
        } catch (Exception e) {
            throw new FatalErrorException(e.getMessage(), e);
        }

        return pluginInstance;
    }
}
