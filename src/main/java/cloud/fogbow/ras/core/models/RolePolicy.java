package cloud.fogbow.ras.core.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;

public interface RolePolicy {

	HashMap<String, Permission<RasOperation>> getPermissions();

	HashMap<String, Role<RasOperation>> getRoles();

	HashMap<String, Set<String>> getUsersRoles();

	HashSet<String> getDefaultRole();

}