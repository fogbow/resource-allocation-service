package cloud.fogbow.ras.core.models;

import java.util.Map;
import java.util.Set;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;

public interface RolePolicy {

	/**
	 * Returns the permission rules contained in this policy.
	 * 
	 * @return a Map containing the rules
	 */
    Map<String, Permission<RasOperation>> getPermissions();

    /**
     * Returns the roles rules contained in this policy.
     * 
     * @return a Map containing the rules
     */
    Map<String, Role<RasOperation>> getRoles();

    /**
     * Returns the users rules contained in this policy.
     * 
     * @return a Map containing the rules
     */
    Map<String, Set<String>> getUsersRoles();

    /**
     * Returns the default role contained in this policy.
     * 
     * @return a Set containing the role
     */
    Set<String> getDefaultRole();

    /**
     * Verifies if the user represented by the user String is authorized to perform the operation on the
     * type of resource indicated.
     * 
     * @param user a String representing the user to be authorized.
     * @param operation the Operation object describing the operation the user is requesting to perform.
     * @return a boolean stating whether the user is authorized or not.
     */
    boolean userIsAuthorized(String user, RasOperation operation);

    /**
     * Verifies if the policy instance is valid. The meaning of 'valid' depends on the
     * implementation, but this operation is expected to, at least, verify if the internal
     * structure is consistent.
     *  
     * @throws ConfigurationErrorException if the policy instance is not valid.
     */
    void validate() throws ConfigurationErrorException;
    
    /**
     * Updates the policy instance, using the given policy as reference. This operation
     * is expected to add rules if they do not exist, update if they exist and remove if required.
     * 
     * @param policy The policy object used to update.
     */
    void update(RolePolicy policy);

    /**
     * Creates a new policy object, containing the same rules.
     * 
     * @return the new policy object.
     */
    RolePolicy copy();
    
    /**
     * Persists the policy instance.
     * 
     * @throws ConfigurationErrorException if some error occurs.
     */
    void save() throws ConfigurationErrorException;
}