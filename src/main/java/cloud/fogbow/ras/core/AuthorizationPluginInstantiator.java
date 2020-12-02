package cloud.fogbow.ras.core;

import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.plugins.authorization.ComposedAuthorizationPlugin;
import cloud.fogbow.ras.core.models.RasOperation;

public class AuthorizationPluginInstantiator {
    private static RasClassFactory classFactory = new RasClassFactory();

    public static AuthorizationPlugin<RasOperation> getAuthorizationPlugin(String className) {
    	AuthorizationPlugin<RasOperation> plugin = 
    			(AuthorizationPlugin<RasOperation>) AuthorizationPluginInstantiator.classFactory.createPluginInstance(className);
    	if (className.equals("cloud.fogbow.common.plugins.authorization.ComposedAuthorizationPlugin")) {
    		ComposedAuthorizationPlugin<RasOperation> composedPlugin = (ComposedAuthorizationPlugin<RasOperation>) plugin;
    		composedPlugin.startPlugin(classFactory);
    	}
        return plugin;
    }
}
