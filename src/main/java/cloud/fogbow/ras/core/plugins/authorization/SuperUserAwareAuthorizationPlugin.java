package cloud.fogbow.ras.core.plugins.authorization;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.FogbowOperation;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.ClassFactory;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

public class SuperUserAwareAuthorizationPlugin  implements AuthorizationPlugin<RasOperation> {

    private ClassFactory classFactory;
    private AuthorizationPlugin<FogbowOperation> defaultPlugin;

    public SuperUserAwareAuthorizationPlugin() {
        classFactory = new ClassFactory();
    }
        
    @Override
    public boolean isAuthorized(SystemUser requester, RasOperation operation) throws UnauthorizedRequestException {
        if (defaultPlugin == null) {
            String defaultPluginName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.DEFAULT_AUTH_PLUGIN_KEY);
            defaultPlugin = (AuthorizationPlugin<FogbowOperation>) classFactory.createPluginInstance(defaultPluginName);            
        }
        
        if (isSuperUser(requester)) {
            return true;
        } else {
            checkIfIsReloadOperation(operation);
            return defaultPlugin.isAuthorized(requester, operation);            
        }
    }

    private void checkIfIsReloadOperation(RasOperation operation) throws UnauthorizedRequestException {
        if (operation.getOperationType().equals(Operation.RELOAD)) {
            throw new UnauthorizedRequestException(Messages.Exception.USER_DOES_NOT_HAVE_REQUIRED_ROLE);
        }
    }
    
    @VisibleForTesting
    void setDefaultPlugin(AuthorizationPlugin<FogbowOperation> defaultPlugin) {
        this.defaultPlugin = defaultPlugin; 
    }
    
    private boolean isSuperUser(SystemUser requester) {
        String superUserRole = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.SUPERUSER_ROLE_KEY, 
                ConfigurationPropertyDefaults.SUPERUSER_ROLE);
        return requester.getUserRoles().contains(superUserRole);
    }
}
