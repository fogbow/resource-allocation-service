package cloud.fogbow.ras.core.models.policy;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.WrongPolicyTypeException;
import cloud.fogbow.common.models.policy.BaseRolePolicy;
import cloud.fogbow.common.models.policy.PermissionInstantiator;
import cloud.fogbow.common.models.policy.Permission;
import cloud.fogbow.common.models.policy.Role;
import cloud.fogbow.common.models.policy.RolePolicy;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.RasClassFactory;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.permission.AllowAllExceptPermission;
import cloud.fogbow.ras.core.models.permission.AllowOnlyPermission;

// TODO test this class
// This implementation is not complete
public class PropertiesRolePolicy extends BaseRolePolicy implements RolePolicy {

	/*
		 	Expected string format	
		  
				<policy-type>
			section_separator
					<permission-name>
					field_separator
					<permission-type>
					field_separator
					<operations>
				permission_separator
					...
				permission_separator
					...
			section_separator
					<role-name>
					field_separator
					<role-permission-name>
				role_separator
					...
				role_separator
					...
			section_separator
				<default-role-name>
			section_separator
				user_separator
					<user-id>
					field_separator
					<role-1>role_separator<role-2>...<role-n>
				user_separator
		 */

    // TODO documentation
	public static final String VALUES_SEPARATOR = ";";

	// TODO documentation
	public static final String FIELDS_SEPARATOR = ".";

	// TODO documentation
    public static final String ITEMS_SEPARATOR = "|";

    // TODO documentation
    public static final String SECTIONS_SEPARATOR = ",";
	
    private HashMap<String, Permission<RasOperation>> permissions;
    private HashMap<String, Role<RasOperation>> availableRoles;
    private HashMap<String, Set<String>> usersRoles;
    private HashSet<String> defaultRoles;
	
    private PropertiesRolePolicy(HashMap<String, Permission<RasOperation>> permissions,
            HashMap<String, Role<RasOperation>> availableRoles, HashMap<String, Set<String>> usersRoles,
            HashSet<String> defaultRoles) {
        this.permissions = permissions;
        this.availableRoles = availableRoles;
        this.usersRoles = usersRoles;
        this.defaultRoles = defaultRoles;
    }
    
    // TODO documentation
    public PropertiesRolePolicy(File policyFile) throws ConfigurationErrorException, InvalidParameterException {
        setUpAvailableRoles(new PermissionInstantiator(new RasClassFactory()));
        setUpUsersRoles();
        setUpDefaultRole();
    }
    
    // TODO documentation
	public PropertiesRolePolicy(String policyString) throws WrongPolicyTypeException {
		String[] policySections = policyString.split(SECTIONS_SEPARATOR);
		
		String policyType = policySections[0];
		
		if (!policyType.equals(BaseRolePolicy.POLICY_TYPE)) {
			throw new WrongPolicyTypeException(BaseRolePolicy.POLICY_TYPE, policyType);
		}

		String permissionSection = policySections[1];
		String roleSection = policySections[2];
		String defaultRoleSection = policySections[3];
		String usersSection = policySections[4];

		setUpPermissionsPolicy(permissionSection);
		setUpRolePolicy(roleSection);
		setUpUsersPolicy(usersSection);
		setUpDefaultRole(defaultRoleSection);
	}
	
    private void setUpAvailableRoles(PermissionInstantiator permissionInstantiator) throws InvalidParameterException {
        this.availableRoles = new HashMap<String, Role<RasOperation>>();
        this.permissions = new HashMap<String, Permission<RasOperation>>();
        String rolesNamesString = PropertiesHolder.getInstance().getProperty(
                ConfigurationPropertyKeys.AUTHORIZATION_ROLES_KEY, ConfigurationPropertyDefaults.AUTHORIZATION_ROLES);
        for (String roleName : rolesNamesString.split(SystemConstants.ROLE_NAMES_SEPARATOR)) {
            String permissionName = PropertiesHolder.getInstance().getProperty(roleName);
            String permissionType = PropertiesHolder.getInstance().getProperty(permissionName);

            Permission<RasOperation> permission = permissionInstantiator.getPermissionInstance(permissionType,
                    permissionName);
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
	
	private void setUpPermissionsPolicy(String permissionsPolicy) {
		String[] permissionsString = permissionsPolicy.split(ITEMS_SEPARATOR);
		this.permissions = new HashMap<String, Permission<RasOperation>>();
		
		
		for (String permissionString : permissionsString) {
			String[] permissionFields = permissionString.split(FIELDS_SEPARATOR);
			String name = permissionFields[0];
			String type = permissionFields[1];
			String operationsString = permissionFields[2];
			
			Set<Operation> operations = new HashSet<Operation>();
			
	        if (!operationsString.isEmpty()) {
	            for (String operationString : operationsString.split(SystemConstants.OPERATION_NAME_SEPARATOR)) {
	                operations.add(Operation.fromString(operationString.trim()));
	            }
			
	            Permission<RasOperation> permission;
			
				// FIXME this code should be in permissioninstantiator
				switch (type) {
					case "cloud.fogbow.ras.core.models.permission.AllowAllExceptPermission": 
						permission = new AllowAllExceptPermission(name, operations); break;
					case "cloud.fogbow.ras.core.models.permission.AllowOnlyPermission": 
						permission = new AllowOnlyPermission(name, operations); break;
					default: permission = null;
				}
				
				this.permissions.put(name, permission);
	        }
		}
	}
	
	private void setUpRolePolicy(String rolesPolicy) {
		String[] rolesString = rolesPolicy.split(ITEMS_SEPARATOR);
		this.availableRoles = new HashMap<String, Role<RasOperation>>();
		
		for (String roleString : rolesString) {
			String[] roleFields = roleString.split(FIELDS_SEPARATOR);
			String name = roleFields[0];
			String permission = roleFields[1];
			
			Role<RasOperation> role = new Role<RasOperation>(name, permission);
			
			this.availableRoles.put(name, role);
		}
	}

	private void setUpUsersPolicy(String usersPolicy) {
		String[] usersString = usersPolicy.split(ITEMS_SEPARATOR);
		this.usersRoles = new HashMap<String, Set<String>>();
		
		for (String userString : usersString) {
			String[] userFields = userString.split(FIELDS_SEPARATOR);
			String name = userFields[0];
			String rolesString = userFields[1];
			
			HashSet<String> roles = new HashSet<String>();
			
			for (String roleName : rolesString.split(VALUES_SEPARATOR)) {
				roles.add(roleName);
			}
			
			this.usersRoles.put(name, roles);
		}
	}

	private void setUpDefaultRole(String defaultRolePolicy) {
		HashSet<Role<RasOperation>> defaultRoles = new HashSet<Role<RasOperation>>();
		
		defaultRoles.add(availableRoles.get(defaultRolePolicy));
	}

    @Override
    public RolePolicy copy() {
        return new PropertiesRolePolicy(permissions, availableRoles, 
                usersRoles, defaultRoles);
    }

    @Override
    public void save() throws ConfigurationErrorException {
        // TODO Auto-generated method stub
    }
}