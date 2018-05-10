package org.fogbowcloud.manager.core.plugins;

import java.lang.reflect.Constructor;
import java.util.Properties;
import java.util.logging.Logger;

public class PluginFactory {

    private static final Logger LOGGER = Logger.getLogger(PluginFactory.class.getName());
    private static final int EXIT_ERROR_CODE = 128;


    public PluginFactory() {

    }

    public Object createPluginInstance(String pluginClassName, Properties properties)  {

        Object pluginInstance = null;

        try {
            Class mClass = Class.forName(pluginClassName);
            Constructor constructor = mClass.getConstructor(Properties.class);
            pluginInstance = constructor.newInstance(properties);
        } catch (ClassNotFoundException e) {
            String msg = "No " + pluginClassName + " class under this repository. Please inform a valid class.";
            LOGGER.severe(msg);
            System.exit(EXIT_ERROR_CODE);
        } catch (Exception e) {
            // TODO: do something
        }

        return pluginInstance ;
    }

    public Object getIdentityPluginByPrefix(String prefix, String className, Properties properties) {

        Properties pluginProperties = new Properties();

        for (Object keyObj : properties.keySet()) {
            String key = keyObj.toString();
            pluginProperties.put(key, properties.get(key));
            if (key.startsWith(prefix)) {
                String newKey = key.replace(prefix, "");
                pluginProperties.put(newKey, properties.get(key));
            }
        }

        return createPluginInstance(className, pluginProperties);
    }
}