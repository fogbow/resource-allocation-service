package cloud.fogbow.ras.core.plugins.authorization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cloud.fogbow.as.constants.SystemConstants;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.RasOperation;

public class RoleAwareAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {

    /*
     * This is a map role -> set of operations. To create this 
     * map, the plugin requires a configuration in the format
     * 
     * role1=operation1,operation2,...
     * role2=operation1,operation3,...
     */
    private Map<String, Set<String>> authorizedRolesForOperation;
    
    public RoleAwareAuthorizationPlugin() {
        authorizedRolesForOperation = new HashMap<String, Set<String>>();
        
        String rolesListString = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AUTHORIZATION_ROLES_KEY, 
                                                                            ConfigurationPropertyDefaults.AUTHORIZATION_ROLES);
        
        for (String role : rolesListString.trim().split(SystemConstants.ROLE_NAMES_SEPARATOR)) {
            if (!role.isEmpty()) {
                getOperationsThatRequireRole(role);
            }
        }
    }

    private void getOperationsThatRequireRole(String role) {
        String operationListString = PropertiesHolder.getInstance().getProperty(role);
        
        for (String operation : operationListString.trim().split(SystemConstants.ROLE_NAMES_SEPARATOR)) {
            
            if (!authorizedRolesForOperation.containsKey(operation)) {
                authorizedRolesForOperation.put(operation, new HashSet<String>());
            }
            
            authorizedRolesForOperation.get(operation).add(role);
        }
    }
    
    @Override
    public boolean isAuthorized(SystemUser systemUser, RasOperation operation) throws UnauthorizedRequestException {
        if (operationHasRolesRequirements(operation)) {
            if (!userHasRequiredRolesForOperation(systemUser, operation)) {;
                throw new UnauthorizedRequestException(Messages.Exception.USER_DOES_NOT_HAVE_REQUIRED_ROLE);
            }
        }
        
        return true;
    }

    private boolean operationHasRolesRequirements(RasOperation operation) {
        String operationName = operation.getOperationType().getValue();
        return authorizedRolesForOperation.containsKey(operationName);
    }

    private boolean userHasRequiredRolesForOperation(SystemUser systemUser, RasOperation operation) {
        String operationName = operation.getOperationType().getValue();
        Set<String> userRoles = systemUser.getUserRoles();
        Set<String> rolesForOperation = authorizedRolesForOperation.get(operationName);
        
        for (String role : rolesForOperation) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        
        return false;
    }
}
