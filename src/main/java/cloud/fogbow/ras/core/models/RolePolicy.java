package cloud.fogbow.ras.core.models;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;

public interface RolePolicy {

    // TODO documentation
    HashMap<String, Permission<RasOperation>> getPermissions();

    // TODO documentation
    HashMap<String, Role<RasOperation>> getRoles();

    // TODO documentation
    HashMap<String, Set<String>> getUsersRoles();

    // TODO documentation
    HashSet<String> getDefaultRole();

    // TODO documentation
    boolean userIsAuthorized(String user, RasOperation operation);

    // TODO documentation
    void validate() throws ConfigurationErrorException;
    
    // TODO documentation
    void update(RolePolicy policy);

    // TODO documentation
    RolePolicy copy();
    
    // TODO documentation
    void save() throws ConfigurationErrorException;
}