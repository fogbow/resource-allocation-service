package cloud.fogbow.ras.core.models.policy;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import cloud.fogbow.common.constants.Messages;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.models.FogbowOperation;
import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;
import cloud.fogbow.ras.core.models.RolePolicy;

public abstract class BaseRolePolicy<T extends FogbowOperation> implements RolePolicy<T> {
    
    private static final Logger LOGGER = Logger.getLogger(BaseRolePolicy.class);
    
    protected Map<String, Permission<T>> permissions;
    protected Map<String, Role<T>> availableRoles;
    protected Map<String, Set<String>> usersRoles;
    protected Set<String> defaultRoles;
    protected String adminRole;
    
    public static final String POLICY_TYPE = "role";
    
    @Override
    public Map<String, Permission<T>> getPermissions() {
        return permissions;
    }

    @Override
    public Map<String, Role<T>> getRoles() {
        return availableRoles;
    }

    @Override
    public Map<String, Set<String>> getUsersRoles() {
        return usersRoles;
    }

    @Override
    public Set<String> getDefaultRole() {
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
        for (Role<T> role : availableRoles.values()) {
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
        
        throw new ConfigurationErrorException(Messages.Exception.NO_ADMIN_USER);
    }

    @Override
    public boolean userIsAuthorized(String user, T operation) {
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
    
    private boolean checkRolesPermissions(T operation, Set<String> userRoles) {
        for (String roleName : userRoles) {
            Role<T> role = availableRoles.get(roleName);
            if (this.permissions.get(role.getPermission()).isAuthorized(operation)) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void update(RolePolicy<T> policy) {
        // updatePolicy operation adds new fields if they do not exist, updates existing ones and 
        // removes if the field is empty
        
        updatePermissions(policy);
        updateRoles(policy);
        updateUsers(policy);
        updateDefaultRole(policy);
    }
    
    private void updatePermissions(RolePolicy<T> policy) {
        Map<String, Permission<T>> permissions = policy.getPermissions();
        
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
    
    private void updateRoles(RolePolicy<T> policy) {
        Map<String, Role<T>> availableRoles = policy.getRoles();
        
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
    
    private void updateUsers(RolePolicy<T> policy) {
        Map<String, Set<String>> usersRoles = policy.getUsersRoles();
        
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

    private void updateDefaultRole(RolePolicy<T> policy) {
        Set<String> defaultRoles = policy.getDefaultRole();
        
        this.defaultRoles = defaultRoles;
    }
    
    public abstract void save() throws ConfigurationErrorException;
}
