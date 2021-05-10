package cloud.fogbow.ras.core.plugins.authorization;

import java.util.ArrayList;
import java.util.List;

import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.AuthorizationPluginInstantiator;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

public class SuperUserAwareAuthorizationPlugin  implements AuthorizationPlugin<RasOperation> {

    private AuthorizationPlugin<RasOperation> defaultPlugin;
    private List<Operation> superUserOnlyOperations;
    
    public SuperUserAwareAuthorizationPlugin() {
        superUserOnlyOperations = getSuperUserOnlyOperations();
        String defaultPluginName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.DEFAULT_AUTH_PLUGIN_KEY);
        defaultPlugin = AuthorizationPluginInstantiator.getAuthorizationPlugin(defaultPluginName);  
    }
        
    @Override
    public boolean isAuthorized(SystemUser systemUser, RasOperation operation) throws UnauthorizedRequestException {
        if (isSuperUser(systemUser)) {
            return true;
        } else {
            checkIfIsSuperUserOperation(operation);
            return defaultPlugin.isAuthorized(systemUser, operation);            
        }
    }

    private List<Operation> getSuperUserOnlyOperations() {
        List<Operation> superUserOnlyOperations = new ArrayList<Operation>();
        superUserOnlyOperations.add(Operation.RELOAD);
        return superUserOnlyOperations;
    }
    
    private void checkIfIsSuperUserOperation(RasOperation operation) throws UnauthorizedRequestException {
        if (superUserOnlyOperations.contains(operation.getOperationType())) {
            throw new UnauthorizedRequestException(Messages.Exception.USER_DOES_NOT_HAVE_REQUIRED_ROLE);            
        }
    }
    
    private boolean isSuperUser(SystemUser systemUser) {
        String superUserRole = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.SUPERUSER_ROLE_KEY, 
                ConfigurationPropertyDefaults.SUPERUSER_ROLE);
        return systemUser.getUserRoles().contains(superUserRole);
    }

	@Override
	public void setPolicy(String policy) {
		// Ignore
	}

	@Override
	public void updatePolicy(String policy) {
		// Ignore
	}
}
