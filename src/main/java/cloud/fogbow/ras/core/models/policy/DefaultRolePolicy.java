package cloud.fogbow.ras.core.models.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.RolePolicy;
import cloud.fogbow.ras.core.models.permission.AllowAllExceptPermission;
import cloud.fogbow.ras.core.models.permission.AllowOnlyPermission;

public class DefaultRolePolicy implements RolePolicy {

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

	private HashMap<String, Permission<RasOperation>> permissions;
    private HashMap<String, Role<RasOperation>> availableRoles;
    private HashMap<String, Set<String>> usersRoles;
    private HashSet<String> defaultRoles;
	
	public DefaultRolePolicy(String policyString) throws WrongPolicyType {
		String[] policySections = policyString.split("");
		
		String policyType = policySections[0];
		
		// FIXME constant
		if (!policyType.equals("role")) {
			throw new WrongPolicyType();
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
	
	private void setUpPermissionsPolicy(String permissionsPolicy) {
		// FIXME constant
		String[] permissionsString = permissionsPolicy.split("|");
		this.permissions = new HashMap<String, Permission<RasOperation>>();
		
		
		for (String permissionString : permissionsString) {
			// FIXME constant
			String[] permissionFields = permissionString.split(".");
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
		// FIXME constant
		String[] rolesString = rolesPolicy.split("|");
		this.availableRoles = new HashMap<String, Role<RasOperation>>();
		
		for (String roleString : rolesString) {
			// FIXME constant
			String[] roleFields = roleString.split(".");
			String name = roleFields[0];
			String permission = roleFields[1];
			
			Role<RasOperation> role = new Role<RasOperation>(name, permission);
			
			this.availableRoles.put(name, role);
		}
	}

	private void setUpUsersPolicy(String usersPolicy) {
		// FIXME constant
		String[] usersString = usersPolicy.split("|");
		this.usersRoles = new HashMap<String, Set<String>>();
		
		for (String userString : usersString) {
			// FIXME constant
			String[] userFields = userString.split(".");
			String name = userFields[0];
			String rolesString = userFields[1];
			
			HashSet<String> roles = new HashSet<String>();
			
			// FIXME constant
			for (String roleName : rolesString.split(";")) {
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

	public class WrongPolicyType extends Exception {

		private static final long serialVersionUID = 1L;

	}
}