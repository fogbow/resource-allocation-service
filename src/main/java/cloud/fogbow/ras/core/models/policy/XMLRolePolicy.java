package cloud.fogbow.ras.core.models.policy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.XMLUtils;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PermissionInstantiator;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.RolePolicy;

public class XMLRolePolicy extends BaseRolePolicy implements RolePolicy {

    /*
         This policy implementation uses the following XML structure as the format
         to express policy rules. The type value must be equal to BaseRolePolicy.POLICY_TYPE.
         The structure contains one root node for each rule type (permissions, roles and users). 
         Each rule is stored in a node of its kind.
      
        <?xml version="1.0"?>
        <policy>
            <type>BaseRolePolicy.POLICY_TYPE</type>
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
            <defaultrole></defaultrole>
            <users>
                <user>
                    <userId></userId>
                    <roles></roles>
                </user>
            </users>
        </policy>
    */
    
    /*
     * The name of the root node of the policy
     */
    public static final String POLICY_LABEL = "policy";
    /*
     * The name of the policy type node 
     */
    public static final String POLICY_TYPE_LABEL = "type";
    /*
     * The name of the permissions root node
     */
    public static final String PERMISSIONS_LABEL = "permissions";
    /*
     * The name of the roles root node
     */
    public static final String ROLES_LABEL = "roles";
    /*
     * The name of the default role node
     */
    public static final String DEFAULT_ROLE_LABEL = "defaultrole";
    /*
     * The name of the users root node
     */
    public static final String USERS_LABEL = "users";

    /*
     * The name of the permission name node
     */
    public static final String PERMISSION_NAME_NODE = "name";
    /*
     * The name of the permission type node
     */
    public static final String PERMISSION_TYPE_NODE = "type";
    /*
     * The name of the permission operations node
     */
    public static final String PERMISSION_OPERATIONS_NODE = "operations";
    /*
     * The name of the role name node
     */
    public static final String ROLE_NAME_NODE = "name";
    /*
     * The name of the role permission node
     */
    public static final String ROLE_PERMISSION_NODE = "permission";
    /*
     * The name of the user ID node
     */
    public static final String USER_ID_NODE = "userId";
    /*
     * The name of the user roles node
     */
    public static final String USER_ROLES_NODE = "roles";
    /*
     * String used to separate roles names
     */
    public static final String ROLE_SEPARATOR = ";";
    
    private PermissionInstantiator permissionInstantiator;
    
    private String policyFilePath;
    
    private XMLRolePolicy(PermissionInstantiator permissionInstantiator, String adminRole, HashMap<String, Permission<RasOperation>> permissions,
            HashMap<String, Role<RasOperation>> availableRoles, HashMap<String, Set<String>> usersRoles,
            HashSet<String> defaultRoles) {
        this.policyFilePath = getPolicyFilePath();
        this.permissionInstantiator = permissionInstantiator;
        this.permissions = permissions;
        this.availableRoles = availableRoles;
        this.usersRoles = usersRoles;
        this.defaultRoles = defaultRoles;
        this.adminRole = adminRole;
    }
    
    public XMLRolePolicy(PermissionInstantiator permissionInstantiator, String policyString) throws ConfigurationErrorException, WrongPolicyTypeException {
        this.permissionInstantiator = permissionInstantiator;
        this.policyFilePath = getPolicyFilePath();
        this.adminRole = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        Element root = XMLUtils.getRootNodeFromXMLString(policyString);
        
        checkPolicyType(root);
        setUpPermissionsPolicy(root);
        setUpRolePolicy(root);
        setUpUsersPolicy(root);
        setUpDefaultRole(root);
    }

    public XMLRolePolicy(String policyString) throws ConfigurationErrorException, WrongPolicyTypeException {
        this(new PermissionInstantiator(), policyString);
    }
    
    public XMLRolePolicy(PermissionInstantiator permissionInstantiator, File policyFile) throws ConfigurationErrorException, WrongPolicyTypeException {
        this.permissionInstantiator = permissionInstantiator;
        this.policyFilePath = getPolicyFilePath();
        this.adminRole = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        Element root = XMLUtils.getRootNodeFromXMLFile(policyFile);
        
        checkPolicyType(root);
        setUpPermissionsPolicy(root);
        setUpRolePolicy(root);
        setUpUsersPolicy(root);
        setUpDefaultRole(root);
    }
    
    public XMLRolePolicy(File policyFile) throws ConfigurationErrorException, WrongPolicyTypeException {
        this(new PermissionInstantiator(), policyFile);
    }
    
    private String getPolicyFilePath() {
        String policyFileName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        String path = HomeDir.getPath();
        return path + policyFileName;
    }

    @Override
    public void save() throws ConfigurationErrorException {
        List<Element> permissionsXML = writePermissions(permissions);
        List<Element> rolesXML = writeRoles(availableRoles);
        List<Element> usersXML = writeUsers(usersRoles);
        Element defaultRoleXML = writeDefaultRole(defaultRoles);
        Element policyTypeXML = new Element(POLICY_TYPE_LABEL);
        policyTypeXML.setText(BaseRolePolicy.POLICY_TYPE);
        
        Element root = new Element(POLICY_LABEL);
        
        Element permissionsNode = new Element(PERMISSIONS_LABEL);
        permissionsNode.addContent(permissionsXML);
        
        Element rolesNode = new Element(ROLES_LABEL);
        rolesNode.addContent(rolesXML);
        
        Element usersNode = new Element(USERS_LABEL);
        usersNode.addContent(usersXML);
        
        root.addContent(policyTypeXML);
        root.addContent(permissionsNode);
        root.addContent(rolesNode);
        root.addContent(defaultRoleXML);
        root.addContent(usersNode);

        XMLUtils.writeXMLToFile(root, this.policyFilePath);
     }
    
    private void setUpPermissionsPolicy(Element root) {
        List<Element> permissionsList = root.getChild(PERMISSIONS_LABEL).getChildren();
        this.permissions = readPermissions(permissionsList); 
    }
    
    private void setUpRolePolicy(Element root) {
        List<Element> rolesList = root.getChild(ROLES_LABEL).getChildren();
        this.availableRoles = readRoles(rolesList); 
    }

    private void setUpUsersPolicy(Element root) {
        List<Element> usersList = root.getChild(USERS_LABEL).getChildren();
        this.usersRoles = readUsers(usersList); 
    }
    
    private void setUpDefaultRole(Element root) {
        this.defaultRoles = readDefaultRoles(root.getChild(DEFAULT_ROLE_LABEL));
    }

    private void checkPolicyType(Element root) throws WrongPolicyTypeException {
        String policyType = root.getChild(POLICY_TYPE_LABEL).getText();

        if (!policyType.equals(BaseRolePolicy.POLICY_TYPE)) {
            throw new WrongPolicyTypeException(BaseRolePolicy.POLICY_TYPE, policyType);
        }
    }
    
    /*
     * 
     * Permission methods
     * 
     */
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
            
            // This mapping permissionName -> null is used to indicate
            // this permission is to be deleted in an update operation
            if (type.isEmpty()) {
                permissions.put(name, null);
            } else {
                Permission<RasOperation> permission = permissionInstantiator.getPermissionInstance(type, name, operations);
                
                permission.getClass().toString();
                permissions.put(name, permission);
            }
        }
        
        return permissions;
    }
    
    private List<Element> writePermissions(HashMap<String, Permission<RasOperation>> permissions) {
        List<Element> permissionsXML = new ArrayList<Element>();
        
        for (String permissionName : permissions.keySet()) {
            Permission<RasOperation> permission = permissions.get(permissionName);
            
            Set<Operation> operations = (Set<Operation>) permission.getOperationsTypes();
            Set<String> operationsStringsSet = new HashSet<String>(); 
            
            for (Operation operation : operations) {
                operationsStringsSet.add(operation.getValue());
            }
            
            String operationsCombinedStrings = String.join(SystemConstants.OPERATION_NAME_SEPARATOR, operationsStringsSet);
            String permissionType = permission.getClass().getTypeName();
            
            Element nameElement = new Element(PERMISSION_NAME_NODE);
            nameElement.setText(permissionName);
            
            Element permissionOperationsElement = new Element(PERMISSION_OPERATIONS_NODE);
            permissionOperationsElement.setText(operationsCombinedStrings);
            
            Element permissionTypeElement = new Element(PERMISSION_TYPE_NODE);
            permissionTypeElement.setText(permissionType);
            
            Element permissionElement = new Element("permission");
            permissionElement.addContent(nameElement);
            permissionElement.addContent(permissionTypeElement);
            permissionElement.addContent(permissionOperationsElement);
            
            permissionsXML.add(permissionElement);
        } 
        
        return permissionsXML;
    }
    
    /*
     * 
     * Roles methods
     * 
     */
    private HashMap<String, Role<RasOperation>> readRoles(List<Element> rolesList) {
        HashMap<String, Role<RasOperation>> availableRoles = new HashMap<String, Role<RasOperation>>();

        for (Element e : rolesList) {
            String name = e.getChild(ROLE_NAME_NODE).getText();
            String permission = e.getChild(ROLE_PERMISSION_NODE).getText();

            // This mapping roleName -> null is used to indicate
            // this role is to be deleted in an update operation
            if (permission.isEmpty()) {
                availableRoles.put(name, null);
            } else {
                Role<RasOperation> role = new Role<RasOperation>(name, permission);
                availableRoles.put(name, role);
            }
        }            
        
        return availableRoles;
    }
    
    private List<Element> writeRoles(HashMap<String, Role<RasOperation>> roles) {
        List<Element> rolesXML = new ArrayList<Element>();
        
        for (String roleName : roles.keySet()) {
            Element roleElement = new Element("role");
            Element nameElement = new Element(ROLE_NAME_NODE);
            Element permissionElement = new Element(ROLE_PERMISSION_NODE);
            
            Role<RasOperation> role = roles.get(roleName);
            
            // TODO documentation
            String permission = role.getPermission() == null ? "": role.getPermission();
            
            nameElement.setText(role.getName());
            permissionElement.setText(permission);
            
            roleElement.addContent(nameElement);
            roleElement.addContent(permissionElement);
            
            rolesXML.add(roleElement);
        }
        
        return rolesXML;
    }
    
    /*
     * 
     * Users methods
     * 
     */
    private HashMap<String, Set<String>> readUsers(List<Element> usersList) {
        HashMap<String, Set<String>> usersRoles = new HashMap<String, Set<String>>();
        
        for (Element e : usersList) {
            String userId = e.getChild(USER_ID_NODE).getText();
            String rolesString = e.getChild(USER_ROLES_NODE).getText();
            
            HashSet<String> roles = new HashSet<String>();
            
            for (String roleName : rolesString.split(ROLE_SEPARATOR)) {
                roles.add(roleName);
            }
            
            // This mapping userId -> null is used to indicate
            // this user is to be deleted in an update operation
            if (rolesString.isEmpty()) {
                usersRoles.put(userId, null);
            } else {
                usersRoles.put(userId, roles);                
            }
        }
        
        return usersRoles;
    }
    
    private List<Element> writeUsers(HashMap<String, Set<String>> users) {
        List<Element> usersXML = new ArrayList<Element>();
        
        for (String userName : users.keySet()) {
            Element userElement = new Element("user");
            Element nameElement = new Element(USER_ID_NODE);
            Element rolesElement = new Element(USER_ROLES_NODE);
            
            Set<String> userRoles = users.get(userName);
            
            String rolesString = "";
            // TODO documentation
            if (userRoles != null) {
                rolesString = String.join(ROLE_SEPARATOR, userRoles);
            }
            
            nameElement.setText(userName);
            rolesElement.setText(rolesString);
            
            userElement.addContent(nameElement);
            userElement.addContent(rolesElement);
            
            usersXML.add(userElement);
        }
        
        return usersXML;
    }

    /*
     * 
     * Default roles methods
     * 
     */
    private HashSet<String> readDefaultRoles(Element defaultRole) {
        HashSet<String> defaultRoles = new HashSet<String>();
        defaultRoles.add(defaultRole.getText());
        return defaultRoles;
    }
    
    private Element writeDefaultRole(HashSet<String> defaultRole) {
        Element element = new Element(DEFAULT_ROLE_LABEL);
        element.setText(defaultRole.iterator().next());
        return element;
    }
    
    @Override
    public RolePolicy copy() {
        return new XMLRolePolicy(permissionInstantiator, adminRole, permissions, availableRoles, 
                usersRoles, defaultRoles);
    }
}
