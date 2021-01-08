package cloud.fogbow.ras.core.models.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Element;
import org.junit.After;
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
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.XMLUtils;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PermissionInstantiator;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.permission.AllowAllExceptPermission;


@RunWith(PowerMockRunner.class)
@PrepareForTest({XMLUtils.class, PropertiesHolder.class})
public class XMLRolePolicyTest {

    private PermissionInstantiator permissionInstantiator;
    
    private String adminRole = "admin";
    private Permission<RasOperation> permission1;
    private AllowAllExceptPermission permission2;
    private AllowAllExceptPermission permission3;
    private AllowAllExceptPermission updatedPermission3;
    private String operationsPermission1 = "reload,create,getAll";
    private String operationsPermission2 = "reload";
    private String operationsPermission3 = "hibernate";
    private String type1 = "permissiontype1";
    private String type2 = "permissiontype2";
    private String permissionName1 = "permission1";
    private String permissionName2 = "permission2";
    private String permissionName3 = "permission3";
    private String roleName1 = "admin";
    private String roleName2 = "user";
    private String userId1 = "userid1";
    private String userId2 = "userid2";
    private String userId3 = "userid3";
    
    private Set<Operation> operationsPermission1Set;
    private Set<Operation> operationsPermission2Set;
    private Set<Operation> operationsPermission3Set;
    
    private Element permissionNode1;
    private Element permissionNode2;
    private Element permissionNode3;
    private Element emptyPermissionNode3;
    private Element updatedPermissionNode3;
    
    private Element roleNode1;
    private Element roleNode2;
    private Element emptyRoleNode2;
    private Element updatedRoleNode2;
    
    private Element userNode1;
    private Element userNode2;
    private Element userNode3;
    private Element emptyUserNode2;
    private Element updatedUserNode2;
    
    private String xmlStringBeforeUpdate = "beforeUpdate";
    private String xmlStringAfterUpdate = "afterUpdate";

    private RootMockBuilder builder;

    private String policyFileName = "policy.xml";
    private String policyFilePath = HomeDir.getPath() + policyFileName;

    private RasOperation operationGet;
    private RasOperation operationCreate;
    private RasOperation operationReload;

    private String identityProviderId = "providerId";

    @Before
    public void setUp() {
        this.operationsPermission1Set = setUpOperations(Operation.RELOAD, Operation.CREATE, Operation.GET_ALL);
        this.permission1 = new AllowAllExceptPermission(permissionName1, operationsPermission1Set);
        
        this.operationsPermission2Set = setUpOperations(Operation.RELOAD);
        this.permission2 = new AllowAllExceptPermission(permissionName2, operationsPermission2Set);
        
        this.operationsPermission3Set = setUpOperations(Operation.HIBERNATE);
        this.permission3 = new AllowAllExceptPermission(permissionName3, operationsPermission3Set);
        
        this.updatedPermission3 = new AllowAllExceptPermission(permissionName2, operationsPermission2Set); 
        
        builder = new RootMockBuilder();
    }
    
    @After
    public void tearDown() throws IOException {
        deleteTestFiles();
    }
    
    // TODO documentation
    @Test
    public void testUserIsAuthorized() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        // set up operations
        this.operationGet = new RasOperation(Operation.GET, ResourceType.ATTACHMENT, 
                identityProviderId , identityProviderId);
        this.operationCreate = new RasOperation(Operation.CREATE, ResourceType.ATTACHMENT, 
                identityProviderId, identityProviderId);
        this.operationReload = new RasOperation(Operation.RELOAD, ResourceType.CONFIGURATION, 
                identityProviderId, identityProviderId);
        
        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        
        XMLRolePolicy policy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        
        assertTrue(policy.userIsAuthorized(userId1, this.operationGet));
        assertTrue(policy.userIsAuthorized(userId1, this.operationCreate));
        assertFalse(policy.userIsAuthorized(userId1, this.operationReload));
        
        assertTrue(policy.userIsAuthorized(userId2, this.operationGet));
        assertFalse(policy.userIsAuthorized(userId2, this.operationCreate));
        assertFalse(policy.userIsAuthorized(userId2, this.operationReload));
    }
    
    // TODO documentation
    @Test
    public void testConstructorWithValidString() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();
        
        Element root1 = builder.usingPermissions(permissionNode1, permissionNode2).
                                usingRoles(roleNode1).
                                usingUsers(userNode1).
                                usingDefaultRole(roleName2).
                                build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(root1);
        
        XMLRolePolicy policy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        
        HashMap<String, Permission<RasOperation>> permissions = policy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRoles = policy.getRoles();
        HashMap<String, Set<String>> usersRoles = policy.getUsersRoles();
        HashSet<String> defaultRoles = policy.getDefaultRole();
        
        assertEquals(2, permissions.size());
        assertEquals(permission1, permissions.get(permissionName1));
        assertEquals(permission2, permissions.get(permissionName2));
        
        assertEquals(1, availableRoles.size());
        assertEquals(roleName1 , availableRoles.get(roleName1).getName());
        assertEquals(permissionName2 , availableRoles.get(roleName1).getPermission());
        
        assertEquals(1, usersRoles.size());
        assertEquals(1, usersRoles.get(userId1).size());
        assertTrue(usersRoles.get(userId1).contains(roleName1));
        
        assertEquals(1, defaultRoles.size());
        assertTrue(defaultRoles.contains(roleName2));
    }
    
    // TODO documentation
    @Test(expected = WrongPolicyTypeException.class)
    public void testConstructorWithInvalidPolicyType() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();
        
        Element root1 = builder.usingPermissions(permissionNode1, permissionNode2).
                                usingRoles(roleNode1).
                                usingUsers(userNode1).
                                usingDefaultRole(roleName2).
                                usingPolicyType("invalidPolicyType").
                                build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(root1);
        
        new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
    }
    
    // TODO documentation
    @Test
    public void testUpdateAddsElementsToPolicy() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();
        
        Element rootWithMissingData = builder.usingPermissions(permissionNode1, permissionNode2).
                usingRoles(roleNode1).
                usingUsers(userNode1).
                usingDefaultRole(roleName1).
                build();

        Element rootWithDataToAdd = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                usingRoles(roleNode1, roleNode2).
                usingUsers(userNode1, userNode2).
                usingDefaultRole(roleName1).
                build();

        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithMissingData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToAdd);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyMissingData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        assertEquals(3, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));
        assertEquals(permission3, permissionsAfter.get(permissionName3));
        
        assertEquals(2, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        assertEquals(roleName2, availableRolesAfter.get(roleName2).getName());
        assertEquals(permissionName1 , availableRolesAfter.get(roleName2).getPermission());
        
        assertEquals(2, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertEquals(1, usersRolesAfter.get(userId2).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        assertTrue(usersRolesAfter.get(userId2).contains(roleName2));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName1));
    }
    
    // TODO documentation
    @Test
    public void testUpdateRemovesElementsFromPolicy() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        Element rootWithDataToRemove = builder.usingPermissions(permissionNode1, permissionNode2, emptyPermissionNode3).
                                               usingRoles(roleNode1, emptyRoleNode2).
                                               usingUsers(userNode1, emptyUserNode2).
                                               usingDefaultRole(roleName2).
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToRemove);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyAllData(basePolicy);        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        assertEquals(2, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));

        assertEquals(1, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());

        assertEquals(1, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName2));
    }
    
    // TODO documentation
    @Test
    public void testXMLRolePolicyUpdateUpdatesElementsFromPolicy() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        Element rootWithDataToUpdate = builder.usingPermissions(permissionNode1, permissionNode2, updatedPermissionNode3).
                                               usingRoles(roleNode1, updatedRoleNode2).
                                               usingUsers(userNode1, updatedUserNode2).
                                               usingDefaultRole(roleName1).
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        // TODO think on how to test this
        assertEquals(3, permissionsAfter.size());
        
        assertEquals(2, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        assertEquals(roleName2, availableRolesAfter.get(roleName2).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName2).getPermission());
        
        assertEquals(2, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertEquals(1, usersRolesAfter.get(userId2).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        assertTrue(usersRolesAfter.get(userId2).contains(roleName1));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName1));
    }
    
    // TODO documentation
    @Test
    public void testUpdateOnlyPermissions() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        Element rootWithDataToUpdate = builder.usingPermissions(updatedPermissionNode3).
                                               usingRoles().
                                               usingUsers().
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        // TODO think on how to test the change on permission
        assertEquals(3, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));
        
        assertEquals(2, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        assertEquals(roleName2, availableRolesAfter.get(roleName2).getName());
        assertEquals(permissionName1 , availableRolesAfter.get(roleName2).getPermission());
        
        assertEquals(2, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertEquals(1, usersRolesAfter.get(userId2).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        assertTrue(usersRolesAfter.get(userId2).contains(roleName2));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName2));
    }
    
    // TODO documentation
    @Test
    public void testUpdateOnlyRoles() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        Element rootWithDataToUpdate = builder.usingPermissions().
                                               usingRoles(updatedRoleNode2).
                                               usingUsers().
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        assertEquals(3, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));
        assertEquals(permission3, permissionsAfter.get(permissionName3));
        
        assertEquals(2, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        assertEquals(roleName2, availableRolesAfter.get(roleName2).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName2).getPermission());
        
        assertEquals(2, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertEquals(1, usersRolesAfter.get(userId2).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        assertTrue(usersRolesAfter.get(userId2).contains(roleName2));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName2));
    }
    
    // TODO documentation
    @Test
    public void testUpdateOnlyUsers() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        Element rootWithDataToUpdate = builder.usingPermissions().
                                               usingRoles().
                                               usingUsers(updatedUserNode2).
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        assertEquals(3, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));
        assertEquals(permission3, permissionsAfter.get(permissionName3));
        
        assertEquals(2, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        assertEquals(roleName2, availableRolesAfter.get(roleName2).getName());
        assertEquals(permissionName1 , availableRolesAfter.get(roleName2).getPermission());
        
        assertEquals(2, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertEquals(1, usersRolesAfter.get(userId2).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        assertTrue(usersRolesAfter.get(userId2).contains(roleName1));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName2));
    }
    
    // TODO documentation
    @Test
    public void testUpdateOnlyDefaultRole() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        Element rootWithDataToUpdate = builder.usingPermissions().
                                               usingRoles().
                                               usingUsers().
                                               usingDefaultRole(roleName1).
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        assertEquals(3, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));
        assertEquals(permission3, permissionsAfter.get(permissionName3));
        
        assertEquals(2, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        assertEquals(roleName2, availableRolesAfter.get(roleName2).getName());
        assertEquals(permissionName1 , availableRolesAfter.get(roleName2).getPermission());
        
        assertEquals(2, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertEquals(1, usersRolesAfter.get(userId2).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        assertTrue(usersRolesAfter.get(userId2).contains(roleName2));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName1));
    }
    
    // TODO documentation
    @Test
    public void testUpdateAddOnlyPermission() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithMissingData = builder.usingPermissions(permissionNode1, permissionNode2).
                                              usingRoles(roleNode1).
                                              usingUsers(userNode1).
                                              usingDefaultRole(roleName1).
                                              build();
        
        Element rootWithDataToUpdate = builder.usingPermissions(permissionNode3).
                                               usingRoles().
                                               usingUsers().
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithMissingData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyMissingData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        assertEquals(3, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));
        assertEquals(permission3, permissionsAfter.get(permissionName3));
        
        assertEquals(1, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        
        assertEquals(1, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName1));
    }
    
    // TODO documentation
    @Test
    public void testUpdateAddOnlyRole() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithMissingData = builder.usingPermissions(permissionNode1, permissionNode2).
                                              usingRoles(roleNode1).
                                              usingUsers(userNode1).
                                              usingDefaultRole(roleName1).
                                              build();
        
        Element rootWithDataToUpdate = builder.usingPermissions().
                                               usingRoles(roleNode2).
                                               usingUsers().
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithMissingData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyMissingData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        assertEquals(2, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));
        
        assertEquals(2, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        assertEquals(roleName2, availableRolesAfter.get(roleName2).getName());
        assertEquals(permissionName1 , availableRolesAfter.get(roleName2).getPermission());
        
        assertEquals(1, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName1));
    }
    
    // TODO documentation
    @Test
    public void testUpdateAddOnlyUser() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithMissingData = builder.usingPermissions(permissionNode1, permissionNode2).
                                              usingRoles(roleNode1).
                                              usingUsers(userNode1).
                                              usingDefaultRole(roleName1).
                                              build();
        
        Element rootWithDataToUpdate = builder.usingPermissions().
                                               usingRoles().
                                               usingUsers(userNode3).
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithMissingData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyMissingData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        assertEquals(2, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));
        
        assertEquals(1, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        
        assertEquals(2, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertEquals(1, usersRolesAfter.get(userId3).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        assertTrue(usersRolesAfter.get(userId3).contains(roleName1));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName1));
    }
    
    // TODO documentation
    @Test
    public void testUpdateRemoveOnlyPermission() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        Element rootWithDataToUpdate = builder.usingPermissions(emptyPermissionNode3).
                                               usingRoles().
                                               usingUsers().
                                               usingDefaultRole(roleName1).
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        assertEquals(2, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));
        
        assertEquals(2, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        assertEquals(roleName2, availableRolesAfter.get(roleName2).getName());
        assertEquals(permissionName1 , availableRolesAfter.get(roleName2).getPermission());
        
        assertEquals(2, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertEquals(1, usersRolesAfter.get(userId2).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        assertTrue(usersRolesAfter.get(userId2).contains(roleName2));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName1));
    }
    
    // TODO documentation
    @Test
    public void testUpdateRemoveOnlyRole() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        Element rootWithDataToUpdate = builder.usingPermissions().
                                               usingRoles(emptyRoleNode2).
                                               usingUsers().
                                               usingDefaultRole(roleName1).
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        assertEquals(3, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));
        assertEquals(permission3, permissionsAfter.get(permissionName3));
        
        assertEquals(1, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        
        assertEquals(2, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertEquals(1, usersRolesAfter.get(userId2).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        assertTrue(usersRolesAfter.get(userId2).contains(roleName2));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName1));
    }
    
    // TODO documentation
    @Test
    public void testUpdateRemoveOnlyUser() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        Element rootWithDataToUpdate = builder.usingPermissions().
                                               usingRoles().
                                               usingUsers(emptyUserNode2).
                                               usingDefaultRole(roleName1).
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringBeforeUpdate);
        XMLRolePolicy updatePolicy = new XMLRolePolicy(permissionInstantiator, xmlStringAfterUpdate);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        HashMap<String, Permission<RasOperation>> permissionsAfter = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesAfter = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
        assertEquals(3, permissionsAfter.size());
        assertEquals(permission1, permissionsAfter.get(permissionName1));
        assertEquals(permission2, permissionsAfter.get(permissionName2));
        assertEquals(permission3, permissionsAfter.get(permissionName3));
        
        assertEquals(2, availableRolesAfter.size());
        assertEquals(roleName1 , availableRolesAfter.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesAfter.get(roleName1).getPermission());
        assertEquals(roleName2, availableRolesAfter.get(roleName2).getName());
        assertEquals(permissionName1 , availableRolesAfter.get(roleName2).getPermission());
        
        assertEquals(1, usersRolesAfter.size());
        assertEquals(1, usersRolesAfter.get(userId1).size());
        assertTrue(usersRolesAfter.get(userId1).contains(roleName1));
        
        assertEquals(1, defaultRolesAfter.size());
        assertTrue(defaultRolesAfter.contains(roleName1));
    }
    
    // TODO documentation
    @Test
    public void testValidateValidPolicy() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootMinimal = builder.usingPermissions(permissionNode2).
                                              usingRoles(roleNode1).
                                              usingUsers(userNode1).
                                              usingDefaultRole(roleName1).
                                              build();

        String xmlStringMinimal = "minimal";
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringMinimal)).willReturn(rootMinimal);
        
        XMLRolePolicy minimal = new XMLRolePolicy(permissionInstantiator, xmlStringMinimal);
        
        minimal.validate();
        
      
        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        String xmlStringAllData = "allData";
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAllData)).willReturn(rootWithAllData);
        
        XMLRolePolicy allData = new XMLRolePolicy(permissionInstantiator, xmlStringAllData);
        
        allData.validate();
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testValidatePolicyMustHaveDefaultRole() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootNoDefaultRole = builder.usingPermissions(permissionNode1).
                                              usingRoles(roleNode2).
                                              usingUsers(userNode2).
                                              build();

        String xmlStringNoDefaultRole = "noDefaultRole";
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringNoDefaultRole)).willReturn(rootNoDefaultRole);
        
        XMLRolePolicy noDefaultRole = new XMLRolePolicy(permissionInstantiator, xmlStringNoDefaultRole);
        
        noDefaultRole.validate();
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testValidateAllUsersMustHaveExistingRoles() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootRoleDoesNotExist = builder.usingPermissions(permissionNode1).
                                              usingRoles(roleNode2).
                                              usingUsers(userNode1, userNode2).
                                              usingDefaultRole(roleName2).
                                              build();

        String xmlStringRoleDoesNotExist = "roleDoesNotExist";
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringRoleDoesNotExist)).willReturn(rootRoleDoesNotExist);
        
        XMLRolePolicy roleDoesNotExist = new XMLRolePolicy(permissionInstantiator, xmlStringRoleDoesNotExist);
        
        roleDoesNotExist.validate();
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testValidateAllRolesMustHaveExistingPermissions() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootPermissionDoesNotExist = builder.usingPermissions(permissionNode1).
                                              usingRoles(roleNode1, roleNode2).
                                              usingUsers(userNode2).
                                              usingDefaultRole(roleName2).
                                              build();

        String xmlStringPermissionDoesNotExist = "permissionDoesNotExist";
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringPermissionDoesNotExist)).willReturn(rootPermissionDoesNotExist);
        
        XMLRolePolicy permissionDoesNotExist = new XMLRolePolicy(permissionInstantiator, xmlStringPermissionDoesNotExist);
        
        permissionDoesNotExist.validate();
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testValidateDefaultRoleMustExist() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootDefaultRoleDoesNotExist = builder.usingPermissions(permissionNode1).
                                              usingRoles(roleNode2).
                                              usingUsers(userNode2).
                                              usingDefaultRole(roleName1).
                                              build();

        String xmlStringDefaultRoleDoesNotExist = "defaultRoleDoesNotExist";
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringDefaultRoleDoesNotExist)).willReturn(rootDefaultRoleDoesNotExist);
        
        XMLRolePolicy defaultRoleDoesNotExist = new XMLRolePolicy(permissionInstantiator, xmlStringDefaultRoleDoesNotExist);
        
        defaultRoleDoesNotExist.validate();
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testValidateAdminRoleMustExist() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootAdminRoleDoesNotExist = builder.usingPermissions(permissionNode1).
                                              usingRoles(roleNode2).
                                              usingUsers(userNode2).
                                              usingDefaultRole(roleName2).
                                              build();

        String xmlStringAdminRoleDoesNotExist = "adminRoleDoesNotExist";
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAdminRoleDoesNotExist)).willReturn(rootAdminRoleDoesNotExist);
        
        XMLRolePolicy adminRoleDoesNotExist = new XMLRolePolicy(permissionInstantiator, xmlStringAdminRoleDoesNotExist);
        
        adminRoleDoesNotExist.validate();
    }
    
    // TODO documentation
    @Test
    public void testSave() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();
        
        String permissionName = permissionName2;
        // FIXME
        String type = "cloud.fogbow.ras.core.models.permission.AllowAllExceptPermission";
        String operations = operationsPermission3;
        String roleName = roleName1;
        String userId = userId1;
        
        String xmlString = "<?xml version=\"1.0\"?> " +
                           "                <policy>" +
                           "        <type>role</type>" +
                           "       <permissions>    "   +
                           "            <permission>" +
                           "                <name>" + permissionName + "</name>" +
                           "                <type>" + type + "</type>" +
                           "                <operations>" + operations + "</operations>" +
                           "            </permission>" +
                           "        </permissions>" +
                           ""  +
                           "        <roles>" +
                           "            <role>" +
                           "                <name>" + roleName + "</name>" +
                           "                <permission>" + permissionName + "</permission>" +
                           "            </role>" +
                           "        </roles>" +
                           "    <defaultrole>" + roleName + "</defaultrole>" +
                           "    <users>" +
                           "        <user>" +
                           "            <userId>" + userId + "</userId>" +
                           "            <roles>" + roleName + "</roles>" +
                           "        </user>" +
                           "    </users>" +
                           "</policy>";

        assertFalse(fileExists(policyFilePath));
        
        XMLRolePolicy basePolicy = new XMLRolePolicy(new PermissionInstantiator(), xmlString);
        basePolicy.save();
        
        assertTrue(fileExists(policyFilePath));
        
        File policyFile = new File(policyFilePath);
        XMLRolePolicy loadedPolicy = new XMLRolePolicy(new PermissionInstantiator(), policyFile);
        
        HashMap<String, Permission<RasOperation>> permissionsBefore = loadedPolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesBefore = loadedPolicy.getRoles();
        HashMap<String, Set<String>> usersRolesBefore = loadedPolicy.getUsersRoles();
        HashSet<String> defaultRolesBefore = loadedPolicy.getDefaultRole();
        
        assertEquals(1, permissionsBefore.size());
        assertEquals(permission2, permissionsBefore.get(permissionName2));
        
        assertEquals(1, availableRolesBefore.size());
        assertEquals(roleName1 , availableRolesBefore.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesBefore.get(roleName1).getPermission());
        
        assertEquals(1, usersRolesBefore.size());
        assertEquals(1, usersRolesBefore.get(userId1).size());
        assertTrue(usersRolesBefore.get(userId1).contains(roleName1));
        
        assertEquals(1, defaultRolesBefore.size());
        assertTrue(defaultRolesBefore.contains(roleName1));
    }
    
    private void deleteTestFiles() throws IOException {
        File file = new File(policyFilePath);
        Files.deleteIfExists(file.toPath());
    }
    
    private boolean fileExists(String fileName) {
        File file = new File(fileName);
        return Files.exists(file.toPath());
    }
    
    private void checkPolicyAllData(XMLRolePolicy basePolicy) {
        HashMap<String, Permission<RasOperation>> permissionsBefore = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesBefore = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesBefore = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesBefore = basePolicy.getDefaultRole();
        
        assertEquals(3, permissionsBefore.size());
        assertEquals(permission1, permissionsBefore.get(permissionName1));
        assertEquals(permission2, permissionsBefore.get(permissionName2));
        assertEquals(permission3, permissionsBefore.get(permissionName3));
        
        assertEquals(2, availableRolesBefore.size());
        assertEquals(roleName1 , availableRolesBefore.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesBefore.get(roleName1).getPermission());
        assertEquals(roleName2, availableRolesBefore.get(roleName2).getName());
        assertEquals(permissionName1 , availableRolesBefore.get(roleName2).getPermission());
        
        assertEquals(2, usersRolesBefore.size());
        assertEquals(1, usersRolesBefore.get(userId1).size());
        assertEquals(1, usersRolesBefore.get(userId2).size());
        assertTrue(usersRolesBefore.get(userId1).contains(roleName1));
        assertTrue(usersRolesBefore.get(userId2).contains(roleName2));
        
        assertEquals(1, defaultRolesBefore.size());
        assertTrue(defaultRolesBefore.contains(roleName2));
    }
    
    private void checkPolicyMissingData(XMLRolePolicy basePolicy) {
        HashMap<String, Permission<RasOperation>> permissionsBefore = basePolicy.getPermissions();
        HashMap<String, Role<RasOperation>> availableRolesBefore = basePolicy.getRoles();
        HashMap<String, Set<String>> usersRolesBefore = basePolicy.getUsersRoles();
        HashSet<String> defaultRolesBefore = basePolicy.getDefaultRole();
        
        assertEquals(2, permissionsBefore.size());
        assertEquals(permission1, permissionsBefore.get(permissionName1));
        assertEquals(permission2, permissionsBefore.get(permissionName2));

        assertEquals(1, availableRolesBefore.size());
        assertEquals(roleName1 , availableRolesBefore.get(roleName1).getName());
        assertEquals(permissionName2 , availableRolesBefore.get(roleName1).getPermission());

        assertEquals(1, usersRolesBefore.size());
        assertEquals(1, usersRolesBefore.get(userId1).size());
        assertTrue(usersRolesBefore.get(userId1).contains(roleName1));
        
        assertEquals(1, defaultRolesBefore.size());
        assertTrue(defaultRolesBefore.contains(roleName1));
    }
    
    private void setUpMocks() throws ConfigurationErrorException {
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(adminRole).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        Mockito.doReturn(policyFileName).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        this.permissionInstantiator = Mockito.mock(PermissionInstantiator.class);
        Mockito.doReturn(this.permission1).when(permissionInstantiator).getPermissionInstance(type1, permissionName1, operationsPermission1Set);
        Mockito.doReturn(this.permission2).when(permissionInstantiator).getPermissionInstance(type1, permissionName2, operationsPermission2Set);
        Mockito.doReturn(this.permission3).when(permissionInstantiator).getPermissionInstance(type1, permissionName3, operationsPermission3Set);
        Mockito.doReturn(this.updatedPermission3).when(permissionInstantiator).getPermissionInstance(type2, permissionName3, operationsPermission2Set);
        
        setUpPermissionMocks();
        setUpRoleMocks();
        setUpUserMocks();
    }

    private void setUpPermissionMocks() {
        permissionNode1 = setUpPermissionMock(permissionName1, type1, operationsPermission1);
        permissionNode2 = setUpPermissionMock(permissionName2, type1, operationsPermission2);
        permissionNode3 = setUpPermissionMock(permissionName3, type1, operationsPermission3);
        emptyPermissionNode3 = setUpPermissionMock(permissionName3, "", "");
        updatedPermissionNode3 = setUpPermissionMock(permissionName3, type2, operationsPermission2);
    }
    
    private void setUpRoleMocks() {
        roleNode1 = setUpRoleMock(roleName1, permissionName2);
        roleNode2 = setUpRoleMock(roleName2, permissionName1);
        emptyRoleNode2 = setUpRoleMock(roleName2, "");
        updatedRoleNode2 = setUpRoleMock(roleName2, permissionName2);
    }
    
    private void setUpUserMocks() {
        userNode1 = setUpUserMock(userId1, roleName1);
        userNode2 = setUpUserMock(userId2, roleName2);
        userNode3 = setUpUserMock(userId3, roleName1);
        emptyUserNode2 = setUpUserMock(userId2, "");
        updatedUserNode2 = setUpUserMock(userId2, roleName1);
    }

    private Element setUpPermissionMock(String permissionName, String permissionType, String operationsPermission) {
        Element permissionNode = Mockito.mock(Element.class);
        
        Element permissionNameNode = Mockito.mock(Element.class);
        Mockito.doReturn(permissionName).when(permissionNameNode).getText();
        Element permissionTypeNode = Mockito.mock(Element.class);
        Mockito.doReturn(permissionType).when(permissionTypeNode).getText();
        Element permissionOperationsNode = Mockito.mock(Element.class);
        Mockito.doReturn(operationsPermission).when(permissionOperationsNode).getText();
        
        Mockito.doReturn(permissionNameNode).when(permissionNode).getChild(XMLRolePolicy.PERMISSION_NAME_NODE);
        Mockito.doReturn(permissionTypeNode).when(permissionNode).getChild(XMLRolePolicy.PERMISSION_TYPE_NODE);
        Mockito.doReturn(permissionOperationsNode).when(permissionNode).getChild(XMLRolePolicy.PERMISSION_OPERATIONS_NODE);
        
        return permissionNode;
    }

    private Element setUpRoleMock(String roleName, String permissionName) {
        Element roleNode = Mockito.mock(Element.class);
        
        Element roleNameNode = Mockito.mock(Element.class);
        Mockito.doReturn(roleName).when(roleNameNode).getText();
        Element rolePermissionNode = Mockito.mock(Element.class);
        Mockito.doReturn(permissionName).when(rolePermissionNode).getText();
        
        Mockito.doReturn(roleNameNode).when(roleNode).getChild(XMLRolePolicy.ROLE_NAME_NODE);
        Mockito.doReturn(rolePermissionNode).when(roleNode).getChild(XMLRolePolicy.ROLE_PERMISSION_NODE);
        
        return roleNode;
    }
    
    private Element setUpUserMock(String userId, String role) {
        Element user = Mockito.mock(Element.class);
        
        Element userIdNode = Mockito.mock(Element.class);
        Mockito.doReturn(userId).when(userIdNode).getText();
        Element userRolesNode = Mockito.mock(Element.class);
        Mockito.doReturn(role).when(userRolesNode).getText();
        
        Mockito.doReturn(userIdNode).when(user).getChild(XMLRolePolicy.USER_ID_NODE);
        Mockito.doReturn(userRolesNode).when(user).getChild(XMLRolePolicy.USER_ROLES_NODE);
        
        return user;
    }

    private Set<Operation> setUpOperations(Operation ... operations) {
        Set<Operation> operationsSet = new HashSet<Operation>();
        
        for (Operation operation : operations) {
            operationsSet.add(operation);
        }
        
        return operationsSet;
    }
    
    private class RootMockBuilder {
        private List<Element> permissions;
        private List<Element> roles;
        private List<Element> users;
        private String defaultRole;
        private String policyType = BaseRolePolicy.POLICY_TYPE;

        public RootMockBuilder usingPermissions(Element ... permissions) {
            this.permissions = Arrays.asList(permissions);
            return this;
        }
        
        public RootMockBuilder usingRoles(Element ... roles) {
            this.roles = Arrays.asList(roles);
            return this;
        }
        
        public RootMockBuilder usingUsers(Element ... users) {
            this.users = Arrays.asList(users);
            return this;
        }
        
        public RootMockBuilder usingDefaultRole(String defaultRole) {
            this.defaultRole = defaultRole;
            return this;
        }
        
        public RootMockBuilder usingPolicyType(String policyType) {
            this.policyType = policyType;
            return this;
        }
        
        public Element build() {
            Element root = Mockito.mock(Element.class);
            Element typeRoot1 = Mockito.mock(Element.class);
            Element permissionRoot1 = Mockito.mock(Element.class);
            Element rolesRoot1 = Mockito.mock(Element.class);
            Element usersRoot1 = Mockito.mock(Element.class);
            Element defaultRoleRoot1 = Mockito.mock(Element.class);

            List<Element> rootChildren1 = Arrays.asList(typeRoot1, permissionRoot1, rolesRoot1, defaultRoleRoot1, usersRoot1);
            
            Mockito.doReturn(rootChildren1).when(root).getChildren();
            
            Mockito.doReturn(permissionRoot1).when(root).getChild(XMLRolePolicy.PERMISSIONS_LABEL);
            Mockito.doReturn(permissions).when(permissionRoot1).getChildren();
            
            Mockito.doReturn(rolesRoot1).when(root).getChild(XMLRolePolicy.ROLES_LABEL);
            Mockito.doReturn(roles).when(rolesRoot1).getChildren();
            
            Mockito.doReturn(usersRoot1).when(root).getChild(XMLRolePolicy.USERS_LABEL);
            Mockito.doReturn(users).when(usersRoot1).getChildren();
            
            Mockito.doReturn(defaultRoleRoot1).when(root).getChild(XMLRolePolicy.DEFAULT_ROLE_LABEL);
            Mockito.doReturn(defaultRole).when(defaultRoleRoot1).getText();
            
            Mockito.doReturn(typeRoot1).when(root).getChild(XMLRolePolicy.POLICY_TYPE_LABEL);
            Mockito.doReturn(policyType).when(typeRoot1).getText();
            
            return root;
        }
    }
}
