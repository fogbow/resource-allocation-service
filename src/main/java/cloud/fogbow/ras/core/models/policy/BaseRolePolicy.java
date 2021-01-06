package cloud.fogbow.ras.core.models.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.RolePolicy;

public abstract class BaseRolePolicy implements RolePolicy {

    protected HashMap<String, Permission<RasOperation>> permissions;
    protected HashMap<String, Role<RasOperation>> availableRoles;
    protected HashMap<String, Set<String>> usersRoles;
    protected HashSet<String> defaultRoles;
    protected String adminRole;
    
    @Override
    public HashMap<String, Permission<RasOperation>> getPermissions() {
        return permissions;
    }

    @Override
    public HashMap<String, Role<RasOperation>> getRoles() {
        return availableRoles;
    }

    @Override
    public HashMap<String, Set<String>> getUsersRoles() {
        return usersRoles;
    }

    @Override
    public HashSet<String> getDefaultRole() {
        return defaultRoles;
    }

    @Override
    public void validate() throws ConfigurationErrorException {
        // check if all the role permissions exist
        for (Role<RasOperation> role : availableRoles.values()) {
            if (!permissions.keySet().contains(role.getPermission())) {
                // TODO add message
                throw new ConfigurationErrorException();
            }
        }

        // check if all users roles exist
        for (String user : usersRoles.keySet()) {
            for (String roleName : usersRoles.get(user)) {
                if (!availableRoles.keySet().contains(roleName)) {
                    // TODO add message
                    throw new ConfigurationErrorException();
                }
            }
        }

        // check if default role exists
        for (String roleName : defaultRoles) {
            if (!availableRoles.keySet().contains(roleName)) {
                // TODO add message
                throw new ConfigurationErrorException();
            }
        }
        
        if (!availableRoles.keySet().contains(this.adminRole)) {
            // TODO add message
            throw new ConfigurationErrorException();
        }
    }
    
    // TODO test
    @Override
    public boolean userIsAuthorized(String user, RasOperation operation) {
        Set<String> userRoles = getUserRoles(user);
        return checkRolesPermissions(operation, userRoles);
    }
    
    private Set<String> getUserRoles(String userId) {
        Set<String> userRoles;
        
        if (usersRoles.containsKey(userId)) {
            userRoles = usersRoles.get(userId);
        } else {
            userRoles = defaultRoles;
        }
        
        return userRoles;
    }
    
    private boolean checkRolesPermissions(RasOperation operation, Set<String> userRoles) {
        for (String roleName : userRoles) {
            Role<RasOperation> role = availableRoles.get(roleName);
            if (this.permissions.get(role.getPermission()).isAuthorized(operation)) {
                return true;
            }
        }
        
        return false;
    }
    
    // TODO test
    @Override
    public void update(RolePolicy policy) {
        // updatePolicy operation adds new fields if they do not exist, updates existing ones and 
        // removes if the field is empty
        
        HashMap<String, Permission<RasOperation>> permissions = policy.getPermissions();
        
        for (String permissionName : permissions.keySet()) {
            // FIXME find better contract
            if (permissions.get(permissionName) == null) {
                this.permissions.remove(permissionName);
            } else {
                this.permissions.put(permissionName, permissions.get(permissionName));                    
            }
        }
        
        HashMap<String, Role<RasOperation>> availableRoles = policy.getRoles();
        
        for (String roleName : availableRoles.keySet()) {
            // FIXME find better contract
            if (availableRoles.get(roleName) == null) {
                this.availableRoles.remove(roleName);
            } else {
                this.availableRoles.put(roleName, availableRoles.get(roleName));                    
            }
        }
        
        HashMap<String, Set<String>> usersRoles = policy.getUsersRoles();
        
        for (String userId : usersRoles.keySet()) {
            // FIXME find better contract
            if (usersRoles.get(userId) == null) {
                this.usersRoles.remove(userId);
            } else {
                this.usersRoles.put(userId, usersRoles.get(userId));                    
            }
        }
        
        HashSet<String> defaultRoles = policy.getDefaultRole();
        
        this.defaultRoles = defaultRoles;
    }
}
