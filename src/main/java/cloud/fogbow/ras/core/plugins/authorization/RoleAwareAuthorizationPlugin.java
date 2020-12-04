package cloud.fogbow.ras.core.plugins.authorization;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PermissionInstantiator;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.RasOperation;

public class RoleAwareAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {

    private HashMap<String, Role<RasOperation>> availableRoles;
    private HashMap<String, Set<Role<RasOperation>>> usersRoles;
    private HashSet<Role<RasOperation>> defaultRoles;
    
    public RoleAwareAuthorizationPlugin() throws ConfigurationErrorException {
        this(new PermissionInstantiator());
    }
    
    public RoleAwareAuthorizationPlugin(PermissionInstantiator permissionInstantiator) throws ConfigurationErrorException {
        setUpAvailableRoles(permissionInstantiator);
        setUpUsersRoles();
        setUpDefaultRole();
    }
    
    private void setUpAvailableRoles(PermissionInstantiator permissionInstantiator) {
        this.availableRoles = new HashMap<String, Role<RasOperation>>();
        String rolesNamesString = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AUTHORIZATION_ROLES_KEY, 
                                                                             ConfigurationPropertyDefaults.AUTHORIZATION_ROLES);
        for (String roleName : rolesNamesString.split(SystemConstants.ROLE_NAMES_SEPARATOR)) {
            String permissionName = PropertiesHolder.getInstance().getProperty(roleName);
            String permissionType = PropertiesHolder.getInstance().getProperty(permissionName);
            
            Permission<RasOperation> permission = permissionInstantiator.getPermissionInstance(permissionType, permissionName);
            Role<RasOperation> role = new Role<RasOperation>(roleName, permission);
            this.availableRoles.put(roleName, role);
        }
    }
    
    private void setUpUsersRoles() {
        this.usersRoles = new HashMap<String, Set<Role<RasOperation>>>();
        String userNamesString = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.USER_NAMES_KEY);
        
        for (String userName : userNamesString.split(SystemConstants.USER_NAME_SEPARATOR)) {
            String userRolesString = PropertiesHolder.getInstance().getProperty(userName);
            Set<Role<RasOperation>> userRoles = new HashSet<Role<RasOperation>>();
            
            for (String roleName : userRolesString.split(SystemConstants.USER_ROLES_SEPARATOR)) {
                userRoles.add(this.availableRoles.get(roleName));
            }
            
            this.usersRoles.put(userName, userRoles);
        }
    }
    
    private void setUpDefaultRole() throws ConfigurationErrorException {
        String defaultRoleName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.DEFAULT_ROLE_KEY);
        
        if (availableRoles.containsKey(defaultRoleName)) {
            this.defaultRoles = new HashSet<Role<RasOperation>>();
            Role<RasOperation> defaultRole = availableRoles.get(defaultRoleName);
            this.defaultRoles.add(defaultRole);
        } else {
            throw new ConfigurationErrorException(Messages.Exception.DEFAULT_ROLE_NAME_IS_INVALID);
        }
    }
    
    @Override
    public boolean isAuthorized(SystemUser systemUser, RasOperation operation) throws UnauthorizedRequestException {
        if (userIsLocal(systemUser)) {
            return authorizeLocalUser(systemUser, operation);
        } else {
            return authorizeRemoteUser(systemUser, operation);
        }
    }

    private boolean userIsLocal(SystemUser systemUser) {
        String userProvider = systemUser.getIdentityProviderId();
        return PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY).equals(userProvider);
    }
    
    private boolean authorizeLocalUser(SystemUser systemUser, RasOperation operation)
            throws UnauthorizedRequestException {
        String userId = systemUser.getId();
        Set<Role<RasOperation>> userRoles = getUserRoles(userId);
        updateUserWithRoles(systemUser, userRoles);
        return checkRolesPermissions(operation, userRoles);
    }

    private Set<Role<RasOperation>> getUserRoles(String userId) {
        Set<Role<RasOperation>> userRoles;
        
        if (usersRoles.containsKey(userId)) {
            userRoles = usersRoles.get(userId);
        } else {
            userRoles = defaultRoles;
        }
        
        return userRoles;
    }
    
    private void updateUserWithRoles(SystemUser systemUser, Set<Role<RasOperation>> userRoles) {
        Set<String> userRolesNames = new HashSet<String>();
        
        for (Role<RasOperation> role : userRoles) { 
            userRolesNames.add(role.getName());
        }
            
        systemUser.setUserRoles(userRolesNames);
    }
    
    private boolean checkRolesPermissions(RasOperation operation, Set<Role<RasOperation>> userRoles)
            throws UnauthorizedRequestException {
        for (Role<RasOperation> role : userRoles) {
            if (role.canPerformOperation(operation)) {
                return true;
            }
        }
        
        throw new UnauthorizedRequestException();
    }

    private boolean authorizeRemoteUser(SystemUser systemUser, RasOperation operation)
            throws UnauthorizedRequestException {
        Set<String> userRolesNames = systemUser.getUserRoles();
        
        for (String roleName : userRolesNames) {
            if (availableRoles.containsKey(roleName)) {
                Role<RasOperation> role = availableRoles.get(roleName);

                if (role.canPerformOperation(operation)) {
                    return true;
                }
            }
        }
        
        throw new UnauthorizedRequestException();
    }
}
