package cloud.fogbow.ras.core.models.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.RolePolicy;

public abstract class BaseRolePolicy implements RolePolicy {
    
    private static final Logger LOGGER = Logger.getLogger(BaseRolePolicy.class);
    
    protected HashMap<String, Permission<RasOperation>> permissions;
    protected HashMap<String, Role<RasOperation>> availableRoles;
    protected HashMap<String, Set<String>> usersRoles;
    protected HashSet<String> defaultRoles;
    protected String adminRole;
    
    public static final String POLICY_TYPE = "role";
    
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
        checkAllRolePermissionsExist();
        checkAllUserRolesExist();
        checkDefaultUserRoleExists();
        checkAdminRoleExists();
        checkAtLeastOneUserIsAdmin();
    }
    
    private void checkAllRolePermissionsExist() throws ConfigurationErrorException {
        for (Role<RasOperation> role : availableRoles.values()) {
            if (!permissions.keySet().contains(role.getPermission())) {
                throw new ConfigurationErrorException(String.format(Messages.Exception.ROLE_PERMISSION_DOES_NOT_EXIST, role.getPermission(), role.getName()));
            }
        }
    }
    
    private void checkAllUserRolesExist() throws ConfigurationErrorException {
        for (String user : usersRoles.keySet()) {
            for (String roleName : usersRoles.get(user)) {
                if (!availableRoles.keySet().contains(roleName)) {
                    throw new ConfigurationErrorException(String.format(Messages.Exception.USER_ROLE_DOES_NOT_EXIST, roleName, user));
                }
            }
        }
    }
    
    private void checkDefaultUserRoleExists() throws ConfigurationErrorException {
        for (String roleName : defaultRoles) {
            if (!availableRoles.keySet().contains(roleName)) {
                throw new ConfigurationErrorException(String.format(Messages.Exception.DEFAULT_USER_ROLE_DOES_NOT_EXIST, roleName));
            }
        }
    }

    private void checkAdminRoleExists() throws ConfigurationErrorException {
        if (!availableRoles.keySet().contains(this.adminRole)) {
            throw new ConfigurationErrorException(String.format(Messages.Exception.ADMIN_ROLE_DOES_NOT_EXIST, this.adminRole));
        }
    }
    
    private void checkAtLeastOneUserIsAdmin() throws ConfigurationErrorException {
        for (String user : usersRoles.keySet()) {
            if (usersRoles.get(user).contains(this.adminRole)) {
                return;
            }
        }
        
        // TODO add message
        throw new ConfigurationErrorException();
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
    
    @Override
    public void update(RolePolicy policy) {
        // updatePolicy operation adds new fields if they do not exist, updates existing ones and 
        // removes if the field is empty
        
        updatePermissions(policy);
        updateRoles(policy);
        updateUsers(policy);
        updateDefaultRole(policy);
    }
    
    private void updatePermissions(RolePolicy policy) {
        HashMap<String, Permission<RasOperation>> permissions = policy.getPermissions();
        
        for (String permissionName : permissions.keySet()) {
            // Currently, if the permission value is null, we treat
            // it as a removal operation. Maybe we should think about
            // a better contract for this.
            if (permissions.get(permissionName) == null) {
                this.permissions.remove(permissionName);
            } else {
                this.permissions.put(permissionName, permissions.get(permissionName));                    
            }
        }
    }
    
    private void updateRoles(RolePolicy policy) {
        HashMap<String, Role<RasOperation>> availableRoles = policy.getRoles();
        
        for (String roleName : availableRoles.keySet()) {
            // Currently, if the role value is null, we treat
            // it as a removal operation. Maybe we should think about
            // a better contract for this.
            if (availableRoles.get(roleName) == null) {
                this.availableRoles.remove(roleName);
            } else {
                this.availableRoles.put(roleName, availableRoles.get(roleName));                    
            }
        }
    }
    
    private void updateUsers(RolePolicy policy) {
        HashMap<String, Set<String>> usersRoles = policy.getUsersRoles();
        
        for (String userId : usersRoles.keySet()) {
            // Currently, if the user value is null, we treat
            // it as a removal operation. Maybe we should think about
            // a better contract for this.
            if (usersRoles.get(userId) == null) {
                this.usersRoles.remove(userId);
            } else {
                this.usersRoles.put(userId, usersRoles.get(userId));                    
            }
        }
    }

    private void updateDefaultRole(RolePolicy policy) {
        HashSet<String> defaultRoles = policy.getDefaultRole();
        
        this.defaultRoles = defaultRoles;
    }
    
    public abstract void save() throws ConfigurationErrorException;
}
