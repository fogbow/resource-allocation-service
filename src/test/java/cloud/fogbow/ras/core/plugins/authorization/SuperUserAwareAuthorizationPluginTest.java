package cloud.fogbow.ras.core.plugins.authorization;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;

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
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.AuthorizationPluginInstantiator;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class, AuthorizationPluginInstantiator.class})
public class SuperUserAwareAuthorizationPluginTest {

    private static String userId1 = "userId1";
    private static String userName1 = "userName1";
    private static String identityProviderId1 = "providerId1";

    private static String userId2 = "userId2";
    private static String userName2 = "userName2";
    private static String identityProviderId2 = "providerId2";
    
    private static final String normalUserRole = "user";
    private static final String superUserRole = "system_admin";
    private static final String defaultAuthPlugin = "plugin";
    
    private static ArrayList<Operation> adminOnly;
    
    private SystemUser user;
    private SystemUser superUser;
    
    private SuperUserAwareAuthorizationPlugin authPlugin;
    
    AuthorizationPlugin<RasOperation> defaultPlugin;
    
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder properties = Mockito.mock(PropertiesHolder.class);

        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(properties);
        
        Mockito.when(properties.getProperty(ConfigurationPropertyKeys.SUPERUSER_ROLE_KEY, 
                ConfigurationPropertyDefaults.SUPERUSER_ROLE)).thenReturn(superUserRole);
        
        Mockito.when(properties.getProperty(ConfigurationPropertyKeys.DEFAULT_AUTH_PLUGIN_KEY)).thenReturn(defaultAuthPlugin);
        
        adminOnly = new ArrayList<Operation>();
        adminOnly.add(Operation.RELOAD);
        
        user = new SystemUser(userId1, userName1, identityProviderId1);
        HashSet<String> userRoles = new HashSet<String>();
        userRoles.add(normalUserRole);
        user.setUserRoles(userRoles);
        
        superUser = new SystemUser(userId2, userName2, identityProviderId2);
        HashSet<String> superUserRoles = new HashSet<String>();
        superUserRoles.add(superUserRole);
        superUser.setUserRoles(superUserRoles);
    }

    // test case: the SuperUserAware authorization plugin must not call its default plugin in the
    // authorization process of a user with super user role.
    @Test
    public void testSuperUserDoesNotRequireAdditionalAuthorization() throws UnauthorizedRequestException {
        defaultPlugin = Mockito.mock(AuthorizationPlugin.class);
        Mockito.when(defaultPlugin.isAuthorized(Mockito.any(), Mockito.any())).thenReturn(true);
        
        PowerMockito.mockStatic(AuthorizationPluginInstantiator.class);
        BDDMockito.given(AuthorizationPluginInstantiator.getAuthorizationPlugin(Mockito.any())).willReturn(defaultPlugin);
        
        authPlugin = new SuperUserAwareAuthorizationPlugin();
        
        for (Operation o : Operation.values()) {
            for (ResourceType r : ResourceType.values()) {
                RasOperation operation = new RasOperation(o, r, identityProviderId1, identityProviderId1);
                assertTrue(authPlugin.isAuthorized(superUser, operation));
            }
        }

        Mockito.verify(defaultPlugin, Mockito.never()).isAuthorized(Mockito.any(), Mockito.any());
    }
    
    // test case: the SuperUserAware authorization plugin must call its default plugin in the
    // authorization process of a user without super user role.
    @Test
    public void testNormalUserRequiresAdditionalAuthorization() throws UnauthorizedRequestException {
        defaultPlugin = Mockito.mock(AuthorizationPlugin.class);
        Mockito.when(defaultPlugin.isAuthorized(Mockito.any(), Mockito.any())).thenReturn(true);
        
        PowerMockito.mockStatic(AuthorizationPluginInstantiator.class);
        BDDMockito.given(AuthorizationPluginInstantiator.getAuthorizationPlugin(Mockito.any())).willReturn(defaultPlugin);
        
        authPlugin = new SuperUserAwareAuthorizationPlugin();
        
        for (Operation o : Operation.values()) {
            for (ResourceType r : ResourceType.values()) {
                if (!adminOnly.contains(o)) {
                    RasOperation operation = new RasOperation(o, r, identityProviderId1, identityProviderId1);
                    assertTrue(authPlugin.isAuthorized(user, operation));
                    Mockito.verify(defaultPlugin).isAuthorized(user, operation);
                }
            }
        }
    }
    
    // test case: SuperUserAware plugin must throw an exception if a user
    // without super user role attempts to perform a super user only operation
    @Test
    public void testReloadOperationIsNotAllowedForNormalUsers() throws UnauthorizedRequestException {
        defaultPlugin = Mockito.mock(AuthorizationPlugin.class);
        Mockito.when(defaultPlugin.isAuthorized(Mockito.any(), Mockito.any())).thenReturn(true);
        
        PowerMockito.mockStatic(AuthorizationPluginInstantiator.class);
        BDDMockito.given(AuthorizationPluginInstantiator.getAuthorizationPlugin(Mockito.any())).willReturn(defaultPlugin);
        
        authPlugin = new SuperUserAwareAuthorizationPlugin();
        
        for (Operation o : Operation.values()) {
            for (ResourceType r : ResourceType.values()) {
                if (adminOnly.contains(o)) {
                    try {
                        RasOperation operation = new RasOperation(o, r, identityProviderId1, identityProviderId1);
                        authPlugin.isAuthorized(user, operation);
                        fail("Expected UnauthorizedRequestException");
                    } catch (UnauthorizedRequestException e) {
                        
                    }
                }
            }
        }
    }
    
    // test case: SuperUserAware plugin authorization must fail by 
    // throwing an exception if authorization of non-superusers using
    // its default plugin fails.
    @Test(expected = UnauthorizedRequestException.class)
    public void testAuthenticationFailsIfDefaultPluginThrowsException() throws UnauthorizedRequestException {
        defaultPlugin = Mockito.mock(AuthorizationPlugin.class);
        Mockito.when(defaultPlugin.isAuthorized(Mockito.any(), Mockito.any())).thenReturn(true);
        
        PowerMockito.mockStatic(AuthorizationPluginInstantiator.class);
        BDDMockito.given(AuthorizationPluginInstantiator.getAuthorizationPlugin(Mockito.any())).willReturn(defaultPlugin);
        
        authPlugin = new SuperUserAwareAuthorizationPlugin();
        
        RasOperation operation = new RasOperation(Operation.RELOAD, ResourceType.COMPUTE, identityProviderId1, identityProviderId1);
        authPlugin.isAuthorized(user, operation);
    }
}
