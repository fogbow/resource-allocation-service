package cloud.fogbow.ras.core.plugins.authorization;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PermissionInstantiator;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.permission.AllowOnlyPermission;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesHolder.class)
public class RoleAwareAuthorizationPluginTest {
    
    /*
     * user1 has role1
     * role1 has permission1
     * user2 has role2
     * role2 has permission2
     */
    private String permissionName1 = "permissionName1";
    private String permissionType1 = "permissionType1";
    
    private String permissionName2 = "permissionName2";
    private String permissionType2 = "permissionType2";
    
    private String roleName1 = "role1";
    private String roleName2 = "role2";
    private String defaultRoleName = roleName1;
    private String invalidRoleName = "invalidrole";
    private String rolesNames = String.format("%s,%s", roleName1, roleName2);
    
    private String role1Permissions = permissionName1;
    private String role2Permissions = permissionName2;
    
    private String userId1 = "userId1";
    private String userId2 = "userId2";
    private String userIdWithDefaultRoles = "userIdWithDefaultRole";
    
    private String userName1 = "user1";
    private String userName2 = "user2";
    private String userWithDefaultRole = "user3";
    private String userIds = String.format("%s,%s", userId1, userId2);
    
    private String rolesUser1 = roleName1;
    private String rolesUser2 = String.format("%s,%s", roleName1, roleName2);
    
    private String identityProviderId = "provider";
    
    private RoleAwareAuthorizationPlugin manager;
    private PropertiesHolder propertiesHolder;
    
    private AllowOnlyPermission permission1;
    private AllowOnlyPermission permission2;

    private RasOperation operationGet;
    private RasOperation operationCreate;
    private RasOperation operationReload;
    
    @Before
    public void setUp() throws ConfigurationErrorException {
        // set up PropertiesHolder 
        PowerMockito.mockStatic(PropertiesHolder.class);
        this.propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(rolesNames).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.AUTHORIZATION_ROLES_KEY, 
                                                                    ConfigurationPropertyDefaults.AUTHORIZATION_ROLES);
        Mockito.doReturn(role1Permissions).when(propertiesHolder).getProperty(roleName1);
        Mockito.doReturn(role2Permissions).when(propertiesHolder).getProperty(roleName2);
        Mockito.doReturn(permissionType1).when(propertiesHolder).getProperty(permissionName1);
        Mockito.doReturn(permissionType2).when(propertiesHolder).getProperty(permissionName2);
        Mockito.doReturn(userIds).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.USER_NAMES_KEY);
        Mockito.doReturn(rolesUser1).when(propertiesHolder).getProperty(userId1);
        Mockito.doReturn(rolesUser2).when(propertiesHolder).getProperty(userId2);
        Mockito.doReturn(defaultRoleName).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.DEFAULT_ROLE_KEY);
        Mockito.doReturn(identityProviderId).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        this.permission1 = Mockito.mock(AllowOnlyPermission.class);
        this.permission2 = Mockito.mock(AllowOnlyPermission.class);
        
        PermissionInstantiator instantiator = Mockito.mock(PermissionInstantiator.class);
        
        Mockito.when(instantiator.getPermissionInstance(permissionType1, permissionName1)).thenReturn(permission1);
        Mockito.when(instantiator.getPermissionInstance(permissionType2, permissionName2)).thenReturn(permission2);
        
        this.manager = new RoleAwareAuthorizationPlugin(instantiator);
        
        this.operationGet = new RasOperation(Operation.GET, ResourceType.ATTACHMENT);
        this.operationCreate = new RasOperation(Operation.CREATE, ResourceType.ATTACHMENT);
        this.operationReload = new RasOperation(Operation.RELOAD, ResourceType.CONFIGURATION);
        
        Mockito.when(this.permission1.isAuthorized(operationGet)).thenReturn(true);
        Mockito.when(this.permission2.isAuthorized(operationGet)).thenReturn(true);
        
        Mockito.when(this.permission1.isAuthorized(operationCreate)).thenReturn(false);
        Mockito.when(this.permission2.isAuthorized(operationCreate)).thenReturn(true);

        Mockito.when(this.permission1.isAuthorized(operationReload)).thenReturn(false);
        Mockito.when(this.permission2.isAuthorized(operationReload)).thenReturn(false);
    }
    
    // TODO documentation
    @Test
    public void constructorReadsRolesInformationCorrectly() {
        // Reads correctly roles names
        Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(ConfigurationPropertyKeys.AUTHORIZATION_ROLES_KEY,
                                                                        ConfigurationPropertyDefaults.AUTHORIZATION_ROLES);
        // Reads correctly roles permissions
        Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(roleName1);
        Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(roleName2);
        // Reads correctly permission types
        Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(permissionName1);
        Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(permissionName2);
        // Reads correctly user names
        Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(ConfigurationPropertyKeys.USER_NAMES_KEY);
        // Reads correctly user roles
        Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(userId1);
        Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(userId2);
        Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(ConfigurationPropertyKeys.DEFAULT_ROLE_KEY);
        PowerMockito.verifyStatic(PropertiesHolder.class, Mockito.atLeastOnce());
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void constructorThrowsExceptionIfInvalidDefaultRoleIsPassed() throws ConfigurationErrorException {
        Mockito.doReturn(invalidRoleName).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.DEFAULT_ROLE_KEY);
        
        PermissionInstantiator instantiator = Mockito.mock(PermissionInstantiator.class);
        
        Mockito.when(instantiator.getPermissionInstance(permissionType1, permissionName1)).thenReturn(permission1);
        Mockito.when(instantiator.getPermissionInstance(permissionType2, permissionName2)).thenReturn(permission2);
        
        new RoleAwareAuthorizationPlugin(instantiator);
    }
    
    // TODO documentation
    @Test
    public void testIsAuthorized() throws UnauthorizedRequestException {
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        SystemUser user2 = new SystemUser(userId2, userName2, identityProviderId);
        
        // user1 has role1
        // role1 has permission1
        // permission1 allows only get operations
        assertTrue(this.manager.isAuthorized(user1, operationGet));
        assertIsAuthorizedThrowsException(user1, operationCreate);
        assertIsAuthorizedThrowsException(user1, operationReload);

        // user2 has role2
        // role2 has permission2
        // permission2 allows only get and create operations
        assertTrue(this.manager.isAuthorized(user2, operationGet));
        assertTrue(this.manager.isAuthorized(user2, operationCreate));
        assertIsAuthorizedThrowsException(user2, operationReload);
    }

    @Test
    public void testIsAuthorizedUserIsNotOnUsersList() throws UnauthorizedRequestException {
        SystemUser userWithDefaultRoles = new SystemUser(userIdWithDefaultRoles, userWithDefaultRole, identityProviderId);

        // user3 is not listed on users names list
        // therefore user3 will have the default role, role 1
        assertTrue(this.manager.isAuthorized(userWithDefaultRoles, operationGet));
        assertIsAuthorizedThrowsException(userWithDefaultRoles, operationCreate);
        assertIsAuthorizedThrowsException(userWithDefaultRoles, operationReload);
    }
    
    private void assertIsAuthorizedThrowsException(SystemUser user, RasOperation operation) {
        try {
            this.manager.isAuthorized(user, operation);
            Assert.fail("isAuthorized call should fail.");
        } catch (UnauthorizedRequestException e) {

        }
    }
}
