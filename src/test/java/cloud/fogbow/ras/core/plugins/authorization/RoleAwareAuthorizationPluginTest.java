package cloud.fogbow.ras.core.plugins.authorization;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesHolder.class)
public class RoleAwareAuthorizationPluginTest {

    private static String userId1 = "userId1";
    private static String userName1 = "userName1";
    private static String identityProviderId1 = "providerId1";

    private static String userId2 = "userId2";
    private static String userName2 = "userName2";
    private static String identityProviderId2 = "providerId2";
    
    private static String adminString = "admin";
    private static String userString = "user";
    private static String unassignedString = "unassigned";
    
    private static String authorizationRolesString = String.format("%s,%s,%s", adminString, 
            userString, unassignedString);
    
    /*
     * Operations reload and delete are allowed to admin role only
     */
    private RasOperation reloadOperation = new RasOperation(Operation.RELOAD, ResourceType.CONFIGURATION);
    private RasOperation deleteOperation = new RasOperation(Operation.DELETE, ResourceType.CONFIGURATION);
    /*
     * Operation get is allowed to user role only
     */
    private RasOperation getOperation = new RasOperation(Operation.GET, ResourceType.ATTACHMENT);
    /*
     * Operation create is allowed to any user
     */
    private RasOperation createOperation = new RasOperation(Operation.CREATE, ResourceType.IMAGE);
    /*
     * Operation getAll is allowed to unassigned role only
     */
    private RasOperation getAllOperation = new RasOperation(Operation.GET_ALL, ResourceType.IMAGE);
    
    
    private static String adminOperations = String.format("%s,%s", Operation.RELOAD.getValue(),
                                                                   Operation.DELETE.getValue());
    private static String userOperations = String.format("%s", Operation.GET.getValue());
    private static String unassignedOperations = String.format("%s", Operation.GET_ALL.getValue());
    
    private RoleAwareAuthorizationPlugin plugin;
    private SystemUser adminUser;
    private SystemUser user;
    
    
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder properties = Mockito.mock(PropertiesHolder.class);

        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(properties);

        Mockito.when(properties.getProperty(ConfigurationPropertyKeys.AUTHORIZATION_ROLES_KEY)).thenReturn(authorizationRolesString);
        Mockito.when(properties.getProperty(adminString)).thenReturn(adminOperations);
        Mockito.when(properties.getProperty(userString)).thenReturn(userOperations);
        Mockito.when(properties.getProperty(unassignedString)).thenReturn(unassignedOperations);
        
        plugin = new RoleAwareAuthorizationPlugin();
        
        // Creating user with the 'admin' and 'user' roles
        adminUser = new SystemUser(userId1, userName1, identityProviderId1);
        
        Set<String> adminUserRoles = new HashSet<String>();
        adminUserRoles.add(adminString);
        adminUserRoles.add(userString);
        adminUser.setUserRoles(adminUserRoles);
        
        // Creating user with 'user' role
        user = new SystemUser(userId2, userName2, identityProviderId2);
        
        Set<String> userRoles = new HashSet<String>();
        userRoles.add(userString);
        user.setUserRoles(userRoles);
    }

    @Test
    public void testUserHasRequiredRolesForOperation() throws UnauthorizedRequestException {
        /*
         * admin is authorized to perform reload, delete, create and get operations
         */
        assertTrue(plugin.isAuthorized(adminUser, reloadOperation));
        assertTrue(plugin.isAuthorized(adminUser, deleteOperation));
        assertTrue(plugin.isAuthorized(adminUser, createOperation));
        assertTrue(plugin.isAuthorized(adminUser, getOperation));
        
        /*
         * user is authorized to perform get and create operations
         */
        assertTrue(plugin.isAuthorized(user, getOperation));
        assertTrue(plugin.isAuthorized(user, createOperation));
    }

    @Test(expected = UnauthorizedRequestException.class)
    public void testUserDoesNotHaveRequiredRolesForOperationCase1() throws UnauthorizedRequestException {
        /*
         * admin is not authorized to perform getAll operations
         */
        plugin.isAuthorized(adminUser, getAllOperation);
    }

    @Test(expected = UnauthorizedRequestException.class)
    public void testUserDoesNotHaveRequiredRolesForOperationCase2() throws UnauthorizedRequestException {
        /*
         * user is not authorized to perform reload operations
         */
        plugin.isAuthorized(user, reloadOperation);
    }
    
    @Test(expected = UnauthorizedRequestException.class)
    public void testUserDoesNotHaveRequiredRolesForOperationCase3() throws UnauthorizedRequestException { 
        /*
         * user is not authorized to perform delete operations
         */
        plugin.isAuthorized(user, deleteOperation);
    }
    
    @Test(expected = UnauthorizedRequestException.class)
    public void testUserDoesNotHaveRequiredRolesForOperationCase4() throws UnauthorizedRequestException {
        /*
         * user is not authorized to perform getAll operations
         */
        plugin.isAuthorized(user, getAllOperation);
    }
}
