package org.fogbowcloud.manager.core.plugins;

import java.lang.reflect.Constructor;
import org.apache.log4j.Logger;

public class PluginFactory {

    private static final Logger LOGGER = Logger.getLogger(PluginFactory.class.getName());
    private static final int EXIT_ERROR_CODE = 128;

    public Object createPluginInstance(String pluginClassName) {

        Object pluginInstance = null;

        Class<?> classpath;
        Constructor<?> constructor;

        try {
            classpath = Class.forName(pluginClassName);
            constructor = classpath.getConstructor();
            pluginInstance = constructor.newInstance();
        } catch (ClassNotFoundException e) {
            String msg = "No " + pluginClassName
                    + " class under this repository. Please inform a valid class.";
            LOGGER.fatal(msg);
            System.exit(EXIT_ERROR_CODE);
        } catch (Exception e) {
             LOGGER.fatal(e.getMessage());
            System.exit(EXIT_ERROR_CODE);
        }

        return pluginInstance;
    }
}
