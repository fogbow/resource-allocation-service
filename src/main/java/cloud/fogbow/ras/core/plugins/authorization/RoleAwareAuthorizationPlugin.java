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
import cloud.fogbow.ras.core.models.RolePolicy;
import cloud.fogbow.ras.core.models.policy.DefaultRolePolicy;
import cloud.fogbow.ras.core.models.policy.DefaultRolePolicy.WrongPolicyType;

public class RoleAwareAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {

	// TODO documentation
    public static final String USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT = "%s.%s";
    private HashMap<String, Permission<RasOperation>> permissions;
    private HashMap<String, Role<RasOperation>> availableRoles;
    private HashMap<String, Set<String>> usersRoles;
    private HashSet<String> defaultRoles;
    
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
        this.permissions = new HashMap<String, Permission<RasOperation>>();
        String rolesNamesString = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AUTHORIZATION_ROLES_KEY, 
                                                                             ConfigurationPropertyDefaults.AUTHORIZATION_ROLES);
        for (String roleName : rolesNamesString.split(SystemConstants.ROLE_NAMES_SEPARATOR)) {
            String permissionName = PropertiesHolder.getInstance().getProperty(roleName);
            String permissionType = PropertiesHolder.getInstance().getProperty(permissionName);
            
            Permission<RasOperation> permission = permissionInstantiator.getPermissionInstance(permissionType, permissionName);
            permissions.put(permissionName, permission);
            Role<RasOperation> role = new Role<RasOperation>(roleName, permissionName);
            this.availableRoles.put(roleName, role);
        }
    }
    
    private void setUpUsersRoles() {
        this.usersRoles = new HashMap<String, Set<String>>();
        String userNamesString = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.USER_NAMES_KEY);
        
        for (String userName : userNamesString.split(SystemConstants.USER_NAME_SEPARATOR)) {
            String userRolesString = PropertiesHolder.getInstance().getProperty(userName);
            Set<String> userRoles = new HashSet<String>();
            
            for (String roleName : userRolesString.split(SystemConstants.USER_ROLES_SEPARATOR)) {
                userRoles.add(roleName);
            }
            
            this.usersRoles.put(userName, userRoles);
        }
    }
    
    private void setUpDefaultRole() throws ConfigurationErrorException {
        String defaultRoleName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.DEFAULT_ROLE_KEY);
        
        if (availableRoles.containsKey(defaultRoleName)) {
            this.defaultRoles = new HashSet<String>();
            this.defaultRoles.add(defaultRoleName);
        } else {
            throw new ConfigurationErrorException(Messages.Exception.DEFAULT_ROLE_NAME_IS_INVALID);
        }
    }
    
    @Override
    public boolean isAuthorized(SystemUser systemUser, RasOperation operation) throws UnauthorizedRequestException {
        // check permissions only for local operations
        if (operationIsLocal(operation)) {
            Set<String> userRoles = getUserRoles(getUserConfigurationString(systemUser));
            checkRolesPermissions(operation, userRoles);
        }
        
        return true;
    }

    private boolean operationIsLocal(RasOperation operation) {
        return operation.getProvider().equals(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY));
    }

    private String getUserConfigurationString(SystemUser systemUser) {
        return String.format(USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT, systemUser.getId(), systemUser.getIdentityProviderId());
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
    
    private boolean checkRolesPermissions(RasOperation operation, Set<String> userRoles)
            throws UnauthorizedRequestException {
        for (String roleName : userRoles) {
        	Role<RasOperation> role = availableRoles.get(roleName);
        	if (this.permissions.get(role.getPermission()).isAuthorized(operation)) {
        		return true;
        	}
        }
        
        throw new UnauthorizedRequestException(Messages.Exception.USER_DOES_NOT_HAVE_ENOUGH_PERMISSION);
    }

	@Override
	public void setPolicy(String policyString) throws ConfigurationErrorException {
		try {
			RolePolicy policy = new DefaultRolePolicy(policyString);
			validatePolicy(policy);
			
			this.setPermissions(policy.getPermissions());
			this.setRoles(policy.getRoles());
			this.setUsersRoles(policy.getUsersRoles());
			this.setDefaultRole(policy.getDefaultRole());			
		} catch (WrongPolicyType e) {
			e.printStackTrace();
		}
	}

	private void setPermissions(HashMap<String, Permission<RasOperation>> permissions) {
		this.permissions = permissions;
	}
	
	private void setRoles(HashMap<String, Role<RasOperation>> roles) {
		this.availableRoles = roles;
	}
	
	private void setUsersRoles(HashMap<String, Set<String>> usersRoles) {
		this.usersRoles = usersRoles;
	}
	
	private void setDefaultRole(HashSet<String> defaultRoles) {
		this.defaultRoles = defaultRoles;
	}

	private void validatePolicy(RolePolicy policy) throws ConfigurationErrorException {
	    HashMap<String, Permission<RasOperation>> permissions = policy.getPermissions();
	    HashMap<String, Role<RasOperation>> availableRoles = policy.getRoles();
	    HashMap<String, Set<String>> usersRoles = policy.getUsersRoles();
	    HashSet<String> defaultRoles = policy.getDefaultRole();
		
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
	}

	@Override
	public void updatePolicy(String policy) {
		// TODO Auto-generated method stub
	}
}
