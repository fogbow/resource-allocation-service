package cloud.fogbow.ras.core.models.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom2.Element;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;
import cloud.fogbow.common.util.XMLUtils;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PermissionInstantiator;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.RolePolicy;
import cloud.fogbow.ras.core.models.policy.SeparatorRolePolicy.WrongPolicyType;

public class XMLRolePolicy implements RolePolicy {
    
    private static final Logger LOGGER = Logger.getLogger(XMLRolePolicy.class);
    
    /*
     TODO improve this documentation
      
     XML model reference
      
    <?xml version="1.0"?>
    <policy>
        <type>role</type>
        <permissions>
            <permission>
                <name></name>
                <type></type>
                <operations></operations>
            </permission>
        </permissions>

        <roles>
            <role>
                <name></name>
                <permission></permission>
            </role>
        </roles>
        <defaultrole>
            <name></name>
        </defaultrole>
        <users>
            <user>
                <userId></userId>
                <roles></roles>
            </user>
        </users>
    </policy>
    */
    
    // TODO documentation
    public static final int PERMISSIONS_LIST_INDEX = 1;
    // TODO documentation
    public static final int ROLES_LIST_INDEX = 2;
    // TODO documentation
    public static final int DEFAULT_ROLE_INDEX = 3;
    // TODO documentation
    public static final int USERS_LIST_INDEX = 4;
    // TODO documentation
    public static final String PERMISSION_NAME_NODE = "name";
    // TODO documentation
    public static final String PERMISSION_TYPE_NODE = "type";
    // TODO documentation
    public static final String PERMISSION_OPERATIONS_NODE = "operations";
    // TODO documentation
    public static final String ROLE_NAME_NODE = "name";
    // TODO documentation
    public static final String ROLE_PERMISSION_NODE = "permission";
    // TODO documentation
    public static final String USER_ID_NODE = "userId";
    // TODO documentation
    public static final String USER_ROLES_NODE = "roles";
    // TODO documentation
    public static final String ROLE_SEPARATOR = ";";
    
    private HashMap<String, Permission<RasOperation>> permissions;
    private HashMap<String, Role<RasOperation>> availableRoles;
    private HashMap<String, Set<String>> usersRoles;
    private HashSet<String> defaultRoles;
    private PermissionInstantiator permissionInstantiator;
    
    public XMLRolePolicy(PermissionInstantiator permissionInstantiator, String policyString) throws ConfigurationErrorException, WrongPolicyType {
        // TODO add role type validation
        this.permissionInstantiator = permissionInstantiator;
        Element root = XMLUtils.getRootNodeFromXMLString(policyString);
        
        setUpPermissionsPolicy(root);
        setUpRolePolicy(root);
        setUpUsersPolicy(root);
        setUpDefaultRole(root);
    }
    
    public XMLRolePolicy(String policyString) throws ConfigurationErrorException, WrongPolicyType {
        this(new PermissionInstantiator(), policyString);
    }
    
    private void setUpPermissionsPolicy(Element root) {
        List<Element> permissionsList = root.getChildren().get(PERMISSIONS_LIST_INDEX).getChildren();
        this.permissions = readPermissions(permissionsList); 
    }
    
    private void setUpRolePolicy(Element root) {
        List<Element> rolesList = root.getChildren().get(ROLES_LIST_INDEX).getChildren();
        this.availableRoles = readRoles(rolesList); 
    }

    private void setUpUsersPolicy(Element root) {
        List<Element> usersList = root.getChildren().get(USERS_LIST_INDEX).getChildren();
        this.usersRoles = readUsers(usersList); 
    }
    
    private void setUpDefaultRole(Element root) {
        this.defaultRoles = readDefaultRoles(root.getChildren().get(DEFAULT_ROLE_INDEX));
    }

    private HashMap<String, Permission<RasOperation>> readPermissions(List<Element> permissionList) {
        HashMap<String, Permission<RasOperation>> permissions = new HashMap<String, Permission<RasOperation>>();
        
        for (Element e : permissionList) {
            String name = e.getChild(PERMISSION_NAME_NODE).getText();
            String type = e.getChild(PERMISSION_TYPE_NODE).getText();
            String operationsString = e.getChild(PERMISSION_OPERATIONS_NODE).getText();
            
            Set<Operation> operations = new HashSet<Operation>();
            
            if (!operationsString.isEmpty()) {
                for (String operationString : operationsString.split(SystemConstants.OPERATION_NAME_SEPARATOR)) {
                    operations.add(Operation.fromString(operationString.trim()));
                }
            }
            
            Permission<RasOperation> permission = permissionInstantiator.getPermissionInstance(type, name, operations);
            permissions.put(name, permission);
        }
        
        return permissions;
    }
    
    private HashMap<String, Role<RasOperation>> readRoles(List<Element> rolesList) {
        HashMap<String, Role<RasOperation>> availableRoles = new HashMap<String, Role<RasOperation>>();
        
        for (Element e : rolesList) {
            String name = e.getChild(ROLE_NAME_NODE).getText();
            String permission = e.getChild(ROLE_PERMISSION_NODE).getText();
            
            Role<RasOperation> role = new Role<RasOperation>(name, permission);
            availableRoles.put(name, role);
        }
        
        return availableRoles;
    }
    
    private HashMap<String, Set<String>> readUsers(List<Element> usersList) {
        HashMap<String, Set<String>> usersRoles = new HashMap<String, Set<String>>();
        
        for (Element e : usersList) {
            String name = e.getChild(USER_ID_NODE).getText();
            String rolesString = e.getChild(USER_ROLES_NODE).getText();
            
            HashSet<String> roles = new HashSet<String>();
            
            for (String roleName : rolesString.split(ROLE_SEPARATOR)) {
                roles.add(roleName);
            }
            
            usersRoles.put(name, roles);
        }
        
        return usersRoles;
    }

    private HashSet<String> readDefaultRoles(Element defaultRole) {
        HashSet<String> defaultRoles = new HashSet<String>();
        defaultRoles.add(defaultRole.getText());
        return defaultRoles;
    }

    @Override
    public HashMap<String, Permission<RasOperation>> getPermissions() {
        return this.permissions;
    }

    @Override
    public HashMap<String, Role<RasOperation>> getRoles() {
        return this.availableRoles;
    }

    @Override
    public HashMap<String, Set<String>> getUsersRoles() {
        return this.usersRoles;
    }

    @Override
    public HashSet<String> getDefaultRole() {
        return this.defaultRoles;
    }
}
