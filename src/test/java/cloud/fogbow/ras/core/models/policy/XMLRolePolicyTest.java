package cloud.fogbow.ras.core.models.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;
import cloud.fogbow.common.util.XMLUtils;
import cloud.fogbow.ras.core.PermissionInstantiator;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.permission.AllowAllExceptPermission;
import cloud.fogbow.ras.core.models.policy.SeparatorRolePolicy.WrongPolicyType;


@RunWith(PowerMockRunner.class)
@PrepareForTest({XMLUtils.class})
public class XMLRolePolicyTest {

    private PermissionInstantiator permissionInstantiator;
    
    private Permission<RasOperation> permission1;
    private AllowAllExceptPermission permission2;
    private AllowAllExceptPermission permission3;
    private String operationsPermission1 = "reload,create,getAll";
    private String operationsPermission2 = "reload";
    private String operationsPermission3 = "hibernate";
    private String type1 = "permissiontype";
    private String permissionName1 = "permission1";
    private String permissionName2 = "permission2";
    private String permissionName3 = "permission3";
    private String roleName1 = "admin";
    private String roleName2 = "user";
    private String userId1 = "userid1";
    
    private Set<Operation> operationsPermission1Set;
    private Set<Operation> operationsPermission2Set;
    private Set<Operation> operationsPermission3Set;
    
    private Element root;
    private Element typeRoot;
    private Element permissionRoot;
    private Element rolesRoot;
    private Element usersRoot;
    private Element defaultRoleRoot;
    
    private Element permissionNode1;
    private Element permissionNameNode1;
    private Element permissionTypeNode1;
    private Element permissionOperationsNode1;
    
    private Element permissionNode2;
    private Element permissionNameNode2;
    private Element permissionTypeNode2;
    private Element permissionOperationsNode2;
    
    private Element permissionNode3;
    private Element permissionNameNode3;
    private Element permissionTypeNode3;
    private Element permissionOperationsNode3;
    
    private Element roleNode1;
    private Element roleNameNode1;
    private Element rolePermissionNode1;
    
    private Element roleNode2;
    private Element roleNameNode2;
    private Element rolePermissionNode2;
    
    private Element userNode1;
    private Element userIdNode1;
    private Element userRolesNode1;
    
    private List<Element> rootChildren;
    private List<Element> permissionNodes;
    private List<Element> rolesNodes;
    private List<Element> userNodes;
    
    private String xmLString = "data";

    @Before
    public void setUp() {
        this.operationsPermission1Set = setUpOperations(Operation.RELOAD, Operation.CREATE, Operation.GET_ALL);
        this.permission1 = new AllowAllExceptPermission(permissionName1, operationsPermission1Set);
        
        this.operationsPermission2Set = setUpOperations(Operation.RELOAD);
        this.permission2 = new AllowAllExceptPermission(permissionName2, operationsPermission2Set);
        
        this.operationsPermission3Set = setUpOperations(Operation.HIBERNATE);
        this.permission3 = new AllowAllExceptPermission(permissionName3, operationsPermission3Set);
    }
    
    // TODO documentation
    @Test
    public void testXMLRolePolicyConstructorWithValidString() throws ConfigurationErrorException, WrongPolicyType {
        setUpMocks();
        
        XMLRolePolicy policy = new XMLRolePolicy(permissionInstantiator, xmLString);
        
        HashMap<String, Permission<RasOperation>> permissions = policy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRoles = policy.getRoles();
        HashMap<String, Set<String>> usersRoles = policy.getUsersRoles();
        HashSet<String> defaultRoles = policy.getDefaultRole();
        
        assertEquals(3, permissions.size());
        assertEquals(permission1, permissions.get(permissionName1));
        assertEquals(permission2, permissions.get(permissionName2));
        assertEquals(permission3, permissions.get(permissionName3));
        
        assertEquals(2, availableRoles.size());
        assertEquals(roleName1 , availableRoles.get(roleName1).getName());
        assertEquals(permissionName3 , availableRoles.get(roleName1).getPermission());
        assertEquals(roleName2, availableRoles.get(roleName2).getName());
        assertEquals(permissionName1 , availableRoles.get(roleName2).getPermission());
        
        assertEquals(1, usersRoles.size());
        assertEquals(1, usersRoles.get(userId1).size());
        assertTrue(usersRoles.get(userId1).contains(roleName1));
        
        assertEquals(1, defaultRoles.size());
        assertTrue(defaultRoles.contains(roleName2));
    }

    private void setUpMocks() throws ConfigurationErrorException {
        this.permissionInstantiator = Mockito.mock(PermissionInstantiator.class);
        Mockito.doReturn(this.permission1).when(permissionInstantiator).getPermissionInstance(type1, permissionName1, operationsPermission1Set);
        Mockito.doReturn(this.permission2).when(permissionInstantiator).getPermissionInstance(type1, permissionName2, operationsPermission2Set);
        Mockito.doReturn(this.permission3).when(permissionInstantiator).getPermissionInstance(type1, permissionName3, operationsPermission3Set);
        
        setUpPermissionMocks();
        setUpRoleMocks();
        setUpUserMocks();
        setUpRootMocks();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmLString)).willReturn(root);
    }
    
    private void setUpPermissionMocks() {
        /*
         * Permission 1
         */
        permissionNode1 = Mockito.mock(Element.class);
        
        permissionNameNode1 = Mockito.mock(Element.class);
        Mockito.doReturn(permissionName1).when(permissionNameNode1).getText();
        permissionTypeNode1 = Mockito.mock(Element.class);
        Mockito.doReturn(type1).when(permissionTypeNode1).getText();
        permissionOperationsNode1 = Mockito.mock(Element.class);
        
        Mockito.doReturn(operationsPermission1).when(permissionOperationsNode1).getText();
        Mockito.doReturn(permissionNameNode1).when(permissionNode1).getChild(XMLRolePolicy.PERMISSION_NAME_NODE);
        Mockito.doReturn(permissionTypeNode1).when(permissionNode1).getChild(XMLRolePolicy.PERMISSION_TYPE_NODE);
        Mockito.doReturn(permissionOperationsNode1).when(permissionNode1).getChild(XMLRolePolicy.PERMISSION_OPERATIONS_NODE);
        
        /*
         * Permission 2
         */
        permissionNode2 = Mockito.mock(Element.class);
        
        permissionNameNode2 = Mockito.mock(Element.class);
        Mockito.doReturn(permissionName2).when(permissionNameNode2).getText();
        permissionTypeNode2 = Mockito.mock(Element.class);
        Mockito.doReturn(type1).when(permissionTypeNode2).getText();
        permissionOperationsNode2 = Mockito.mock(Element.class);
        
        Mockito.doReturn(operationsPermission2).when(permissionOperationsNode2).getText();
        Mockito.doReturn(permissionNameNode2).when(permissionNode2).getChild(XMLRolePolicy.PERMISSION_NAME_NODE);
        Mockito.doReturn(permissionTypeNode2).when(permissionNode2).getChild(XMLRolePolicy.PERMISSION_TYPE_NODE);
        Mockito.doReturn(permissionOperationsNode2).when(permissionNode2).getChild(XMLRolePolicy.PERMISSION_OPERATIONS_NODE);
        
        /*
         * Permission 3
         */
        permissionNode3 = Mockito.mock(Element.class);
        
        permissionNameNode3 = Mockito.mock(Element.class);
        Mockito.doReturn(permissionName3).when(permissionNameNode3).getText();
        permissionTypeNode3 = Mockito.mock(Element.class);
        Mockito.doReturn(type1).when(permissionTypeNode3).getText();
        permissionOperationsNode3 = Mockito.mock(Element.class);
        
        Mockito.doReturn(operationsPermission3).when(permissionOperationsNode3).getText();
        Mockito.doReturn(permissionNameNode3).when(permissionNode3).getChild(XMLRolePolicy.PERMISSION_NAME_NODE);
        Mockito.doReturn(permissionTypeNode3).when(permissionNode3).getChild(XMLRolePolicy.PERMISSION_TYPE_NODE);
        Mockito.doReturn(permissionOperationsNode3).when(permissionNode3).getChild(XMLRolePolicy.PERMISSION_OPERATIONS_NODE);
    }

    private void setUpRoleMocks() {
        /*
         * Role 1
         */
        roleNode1 = Mockito.mock(Element.class);
        
        roleNameNode1 = Mockito.mock(Element.class);
        Mockito.doReturn(roleName1).when(roleNameNode1).getText();
        rolePermissionNode1 = Mockito.mock(Element.class);
        Mockito.doReturn(permissionName3).when(rolePermissionNode1).getText();
        
        Mockito.doReturn(roleNameNode1).when(roleNode1).getChild(XMLRolePolicy.ROLE_NAME_NODE);
        Mockito.doReturn(rolePermissionNode1).when(roleNode1).getChild(XMLRolePolicy.ROLE_PERMISSION_NODE);
        
        /*
         * Role 2
         */
        roleNode2 = Mockito.mock(Element.class);
        
        roleNameNode2 = Mockito.mock(Element.class);
        Mockito.doReturn(roleName2).when(roleNameNode2).getText();
        rolePermissionNode2 = Mockito.mock(Element.class);
        Mockito.doReturn(permissionName1).when(rolePermissionNode2).getText();
        
        Mockito.doReturn(roleNameNode2).when(roleNode2).getChild(XMLRolePolicy.ROLE_NAME_NODE);
        Mockito.doReturn(rolePermissionNode2).when(roleNode2).getChild(XMLRolePolicy.ROLE_PERMISSION_NODE);
    }

    private void setUpUserMocks() {
        userNode1 = Mockito.mock(Element.class);
        
        userIdNode1 = Mockito.mock(Element.class);
        Mockito.doReturn(userId1).when(userIdNode1).getText();
        userRolesNode1 = Mockito.mock(Element.class);
        Mockito.doReturn(roleName1).when(userRolesNode1).getText();
        
        Mockito.doReturn(userIdNode1).when(userNode1).getChild(XMLRolePolicy.USER_ID_NODE);
        Mockito.doReturn(userRolesNode1).when(userNode1).getChild(XMLRolePolicy.USER_ROLES_NODE);
    }
    
    private void setUpRootMocks() {
        root = Mockito.mock(Element.class);
        typeRoot = Mockito.mock(Element.class);
        permissionRoot = Mockito.mock(Element.class);
        rolesRoot = Mockito.mock(Element.class);
        usersRoot = Mockito.mock(Element.class);
        defaultRoleRoot = Mockito.mock(Element.class);
        Mockito.doReturn(roleName2).when(defaultRoleRoot).getText();

        rootChildren = Arrays.asList(typeRoot, permissionRoot, rolesRoot, defaultRoleRoot, usersRoot);
        permissionNodes = Arrays.asList(permissionNode1, permissionNode2, permissionNode3);
        rolesNodes = Arrays.asList(roleNode1, roleNode2);
        userNodes = Arrays.asList(userNode1); 
        
        Mockito.doReturn(rootChildren).when(root).getChildren();
        Mockito.doReturn(permissionNodes).when(permissionRoot).getChildren();
        Mockito.doReturn(rolesNodes).when(rolesRoot).getChildren();
        Mockito.doReturn(userNodes).when(usersRoot).getChildren();
    }

    private Set<Operation> setUpOperations(Operation ... operations) {
        Set<Operation> operationsSet = new HashSet<Operation>();
        
        for (Operation operation : operations) {
            operationsSet.add(operation);
        }
        
        return operationsSet;
    }
}
