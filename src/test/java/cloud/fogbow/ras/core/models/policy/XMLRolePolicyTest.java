package cloud.fogbow.ras.core.models.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import cloud.fogbow.common.models.FogbowOperation;
import cloud.fogbow.common.models.Permission;
import cloud.fogbow.common.models.Role;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.XMLUtils;
import cloud.fogbow.ras.core.PermissionInstantiator;


@RunWith(PowerMockRunner.class)
@PrepareForTest({XMLUtils.class})
public class XMLRolePolicyTest {

    private PermissionInstantiator<FogbowOperation> permissionInstantiator;
    
    private String adminRole = "admin";
    private StubPermission permission1;
    private StubPermission permission2;
    private StubPermission permission3;
    private StubPermission updatedPermission3;
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
    
    private Set<String> operationsPermission1StringSet;
    private Set<String> operationsPermission2StringSet;
    private Set<String> operationsPermission3StringSet;
    private Set<String> operationsUpdatedPermission3StringSet;
    
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

    @Before
    public void setUp() {
        this.operationsPermission1StringSet = new HashSet<String>();
        this.operationsPermission1StringSet.add("reload");
        this.operationsPermission1StringSet.add("create");
        this.operationsPermission1StringSet.add("getAll");
        this.permission1 = new StubPermission(operationsPermission1StringSet, permissionName1);
        
        this.operationsPermission2StringSet = new HashSet<String>();
        this.operationsPermission2StringSet.add("reload");
        this.permission2 = new StubPermission(operationsPermission2StringSet, permissionName2);
        
        this.operationsPermission3StringSet = new HashSet<String>();
        this.operationsPermission3StringSet.add("hibernate");
        
        this.permission3 = new StubPermission(operationsPermission3StringSet, permissionName3);
        this.operationsUpdatedPermission3StringSet = new HashSet<String>();
        this.operationsUpdatedPermission3StringSet.add("reload");
        
        this.updatedPermission3 = new StubPermission(operationsPermission2StringSet, permissionName2);
        
        builder = new RootMockBuilder();
    }
    
    @After
    public void tearDown() throws IOException {
        deleteTestFiles();
    }
    
    // test case: when calling the isAuthorized method, it must verify whether or not
    // the user has enough permission to perform the given operation
    @Test
    public void testUserIsAuthorized() throws ConfigurationErrorException, WrongPolicyTypeException {
        // set up operations
        FogbowOperation operation1 = new FogbowOperation();
        FogbowOperation operation2 = new FogbowOperation();

        this.permission1 = Mockito.mock(StubPermission.class);
        Mockito.when(this.permission1.isAuthorized(Mockito.any())).thenReturn(true);
        
        this.permission2 = Mockito.mock(StubPermission.class);
        Mockito.when(this.permission2.isAuthorized(Mockito.any())).thenReturn(false);
        
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        
        XMLRolePolicy<FogbowOperation> policy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, adminRole, policyFilePath);
        
        assertFalse(policy.userIsAuthorized(userId1, operation1));
        assertTrue(policy.userIsAuthorized(userId2, operation2));
        
        Mockito.verify(this.permission1).isAuthorized(operation2);
        Mockito.verify(this.permission2).isAuthorized(operation1);
    }
    
    // test case: when creating a new policy instance from a XML string, the constructor
    // must read correctly the policy rules and set up the policy maps.
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
        
        XMLRolePolicy<FogbowOperation> policy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, adminRole, policyFilePath);
        
        Map<String, Permission<FogbowOperation>> permissions = policy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRoles = policy.getRoles();
        Map<String, Set<String>> usersRoles = policy.getUsersRoles();
        Set<String> defaultRoles = policy.getDefaultRole();
        
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
    
    // test case: when creating a new policy instance from a XML string and the 
    // policy type is not the expected, the constructor must throw a
    // WrongPolicyTypeException.
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
        
        new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, adminRole, policyFilePath);
    }
    
    // test case: when calling the method update using a version of the policy
    // with additional rules, the method must add the rules that are not present
    // and ignore the others.
    @Test
    public void testUpdateAddsElementsToPolicy() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();
        
        Element rootWithMissingData = builder.usingPermissions(permissionNode1, permissionNode2).
                usingRoles(roleNode1).
                usingUsers(userNode1).
                usingDefaultRole(roleName1).
                build();

        // rules present in rootWithDataToAdd and not in rootWithMissingData
        // permissionNode3
        // roleNode2
        // userNode2
        // These rules must be present in rootWithMissingData after the update
        Element rootWithDataToAdd = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                usingRoles(roleNode1, roleNode2).
                usingUsers(userNode1, userNode2).
                usingDefaultRole(roleName1).
                build();

        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithMissingData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToAdd);
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, adminRole, policyFilePath);
        
        // before updating
        checkPolicyMissingData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a version of the policy
    // with fewer rules, the method must remove the rules that are not present
    // and ignore the others.
    @Test
    public void testUpdateRemovesElementsFromPolicy() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        // rules present in rootWithAllData and not in rootWithDataToRemove
        // permissionNode3
        // roleNode2
        // userNode2
        // These rules must not be present in rootWithAllData after the update
        Element rootWithDataToRemove = builder.usingPermissions(permissionNode1, permissionNode2, emptyPermissionNode3).
                                               usingRoles(roleNode1, emptyRoleNode2).
                                               usingUsers(userNode1, emptyUserNode2).
                                               usingDefaultRole(roleName2).
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToRemove);
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyAllData(basePolicy);        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a version of the policy
    // with the same rules names, but different parameters, the method must 
    // update all rules that are different.
    @Test
    public void testUpdateUpdatesElementsFromPolicy() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();

        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        // rules differences
        // permissionNode3
        // roleNode2
        // userNode2
        // rootWithAllData must contain the updated version of these rules after the update
        Element rootWithDataToUpdate = builder.usingPermissions(permissionNode1, permissionNode2, updatedPermissionNode3).
                                               usingRoles(roleNode1, updatedRoleNode2).
                                               usingUsers(userNode1, updatedUserNode2).
                                               usingDefaultRole(roleName1).
                                               build();
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringBeforeUpdate)).willReturn(rootWithAllData);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAfterUpdate)).willReturn(rootWithDataToUpdate);
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a new policy containing 
    // only a permission already present in the policy, it must update the permission.
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
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a new policy containing 
    // only a role already present in the policy, it must update the role.
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
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a new policy containing 
    // only a user already present in the policy, it must update the user.
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
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a new policy containing 
    // only a new defaultRole, it must update the defaultRole.
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
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a new policy containing 
    // only a permission not present in the policy, it must add the permission.
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
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyMissingData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a new policy containing 
    // only a role not present in the policy, it must add the role.
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
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyMissingData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a new policy containing 
    // only a user not present in the policy, it must add the user.
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
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyMissingData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a new policy containing 
    // only an empty permission present in the policy, it must remove the permission.
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
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a new policy containing 
    // only an empty role present in the policy, it must remove the role.
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
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the method update using a new policy containing 
    // only an empty user present in the policy, it must remove the user.
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
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringBeforeUpdate, 
                adminRole, policyFilePath);
        XMLRolePolicy<FogbowOperation> updatePolicy = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAfterUpdate, 
                adminRole, policyFilePath);
        
        // before updating
        checkPolicyAllData(basePolicy);
        
        
        basePolicy.update(updatePolicy);
        
        
        // after updating
        Map<String, Permission<FogbowOperation>> permissionsAfter = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesAfter = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesAfter = basePolicy.getUsersRoles();
        Set<String> defaultRolesAfter = basePolicy.getDefaultRole();
        
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
    
    // test case: when calling the validate method, it must verify
    // that all permissions referenced by roles exist, that all
    // roles referenced by users exist, that the admin and default 
    // role exist and that at least one user is admin.
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
        
        XMLRolePolicy<FogbowOperation> minimal = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringMinimal, 
                adminRole, policyFilePath);
        
        minimal.validate();
        
      
        Element rootWithAllData = builder.usingPermissions(permissionNode1, permissionNode2, permissionNode3).
                                          usingRoles(roleNode1, roleNode2).
                                          usingUsers(userNode1, userNode2).
                                          usingDefaultRole(roleName2).
                                          build();
        
        String xmlStringAllData = "allData";
        
        PowerMockito.mockStatic(XMLUtils.class);
        BDDMockito.given(XMLUtils.getRootNodeFromXMLString(xmlStringAllData)).willReturn(rootWithAllData);
        
        XMLRolePolicy<FogbowOperation> allData = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAllData, 
                adminRole, policyFilePath);
        
        allData.validate();
    }
    
    // test case: when calling the validate method in a policy instance
    // with no default role specified, it must throw a ConfigurationErrorException.
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
        
        XMLRolePolicy<FogbowOperation> noDefaultRole = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringNoDefaultRole, 
                adminRole, policyFilePath);
        
        noDefaultRole.validate();
    }
    
    // test case: when calling the validate method in a policy instance
    // with users referencing non-existing roles, it must throw a ConfigurationErrorException.
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
        
        XMLRolePolicy<FogbowOperation> roleDoesNotExist = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringRoleDoesNotExist, 
                adminRole, policyFilePath);
        
        roleDoesNotExist.validate();
    }
    
    // test case: when calling the validate method in a policy instance
    // with roles referencing non-existing permissions, it must throw a ConfigurationErrorException.
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
        
        XMLRolePolicy<FogbowOperation> permissionDoesNotExist = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringPermissionDoesNotExist, 
                adminRole, policyFilePath);
        
        permissionDoesNotExist.validate();
    }
    
    // test case: when calling the validate method in a policy instance
    // with non-existing default role, it must throw a ConfigurationErrorException.
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
        
        XMLRolePolicy<FogbowOperation> defaultRoleDoesNotExist = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringDefaultRoleDoesNotExist, 
                adminRole, policyFilePath);
        
        defaultRoleDoesNotExist.validate();
    }
    
    // test case: when calling the validate method in a policy instance
    // that does not contain the admin role, it must throw a ConfigurationErrorException.
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
        
        XMLRolePolicy<FogbowOperation> adminRoleDoesNotExist = new XMLRolePolicy<FogbowOperation>(permissionInstantiator, xmlStringAdminRoleDoesNotExist, 
                adminRole, policyFilePath);
        
        adminRoleDoesNotExist.validate();
    }
    
    // test case: when calling the save method, it must write the policy rules
    // in XML format in the file specified by the property 'policy_file'.
    @Test
    public void testSave() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpMocks();
        
        String permissionName = permissionName2;
        // This test does not mock PermissionInstantiator, therefore, there is this
        // dependency.
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
        
        XMLRolePolicy<FogbowOperation> basePolicy = new XMLRolePolicy<FogbowOperation>(new StubPermissionInstantiator(), xmlString, 
                adminRole, policyFilePath);
        basePolicy.save();
        
        assertTrue(fileExists(policyFilePath));
        
        File policyFile = new File(policyFilePath);
        XMLRolePolicy<FogbowOperation> loadedPolicy = new XMLRolePolicy<FogbowOperation>(new StubPermissionInstantiator(), policyFile, 
                adminRole, policyFilePath);
        
        Map<String, Permission<FogbowOperation>> permissionsBefore = loadedPolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesBefore = loadedPolicy.getRoles();
        Map<String, Set<String>> usersRolesBefore = loadedPolicy.getUsersRoles();
        Set<String> defaultRolesBefore = loadedPolicy.getDefaultRole();
        
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
    
    private void checkPolicyAllData(XMLRolePolicy<FogbowOperation> basePolicy) {
        Map<String, Permission<FogbowOperation>> permissionsBefore = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesBefore = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesBefore = basePolicy.getUsersRoles();
        Set<String> defaultRolesBefore = basePolicy.getDefaultRole();
        
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
    
    private void checkPolicyMissingData(XMLRolePolicy<FogbowOperation> basePolicy) {
        Map<String, Permission<FogbowOperation>> permissionsBefore = basePolicy.getPermissions();
        Map<String, Role<FogbowOperation>> availableRolesBefore = basePolicy.getRoles();
        Map<String, Set<String>> usersRolesBefore = basePolicy.getUsersRoles();
        Set<String> defaultRolesBefore = basePolicy.getDefaultRole();
        
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
        this.permissionInstantiator = Mockito.mock(PermissionInstantiator.class);
        Mockito.doReturn(this.permission1).when(permissionInstantiator).getPermissionInstance(type1, permissionName1, operationsPermission1StringSet);
        Mockito.doReturn(this.permission2).when(permissionInstantiator).getPermissionInstance(type1, permissionName2, operationsPermission2StringSet);
        Mockito.doReturn(this.permission3).when(permissionInstantiator).getPermissionInstance(type1, permissionName3, operationsPermission3StringSet);
        Mockito.doReturn(this.updatedPermission3).when(permissionInstantiator).getPermissionInstance(type2, permissionName3, operationsUpdatedPermission3StringSet);
        
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
    
    public class StubPermissionInstantiator implements PermissionInstantiator<FogbowOperation> {

        @Override
        public Permission<FogbowOperation> getPermissionInstance(String type, String... params) {
            return new StubPermission();
        }

        @Override
        public Permission<FogbowOperation> getPermissionInstance(String type, String name, Set<String> operations) {
            return new StubPermission(operations, name);
        }
    }

    public class StubPermission implements Permission<FogbowOperation> {

        private Set<String> operations;
        private String name;

        public StubPermission() {
            this.operations = new HashSet<String>();
        }

        public StubPermission(Set<String> operations, String name) {
            this.operations = operations;
            this.name = name;
        }

        @Override
        public boolean isAuthorized(FogbowOperation operation) {
            return true;
        }

        @Override
        public Set<String> getOperationsTypes() {
            return this.operations;
        }

        @Override
        public void setOperationTypes(Set operations) {
            this.operations = (Set<String>) operations;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof StubPermission) {
                return this.name.equals(((StubPermission) o).name);
            }

            return false;
        }
    }
}
