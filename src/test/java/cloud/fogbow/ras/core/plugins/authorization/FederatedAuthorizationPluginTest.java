package cloud.fogbow.ras.core.plugins.authorization;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.mapper.AuthenticationServiceClient;

public class FederatedAuthorizationPluginTest {
    private static final String PUBLIC_KEY_STRING = "publicKey";
    private static final String ADMIN_USERNAME = "adminUsername";
    private static final String ADMIN_PASSWORD = "adminPassword";
    private static final String TOKEN = "token";
    private static final String FEDERATION_ID = "federationId";
    private static final String SERVICE_ID = "serviceId";
    private static final String BASE_USER_ID = "userId";
    private static final String USER_ID = BASE_USER_ID + FogbowConstants.FEDERATION_ID_SEPARATOR + FEDERATION_ID;
    private static final String USER_NAME = "userName";
    private static final String PROVIDER_ID = "providerId";
    private static final String REMOTE_PROVIDER_ID = "remoteProviderId";
    
    private FederatedAuthorizationPlugin plugin;
    private SystemUser user;
    private RasOperation operation;
    private AuthenticationServiceClient asClient;
    private FederatedAuthorizationClient authorizationClient;
    private Map<String, String> metadata;
    
    @Test
    public void testIsAuthorized() throws FogbowException {
        this.operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, PROVIDER_ID, REMOTE_PROVIDER_ID);
        
        this.metadata = new HashMap<String, String>();
        this.metadata.put(FederatedAuthorizationPlugin.SERVICE_ID_METADATA_KEY, SERVICE_ID);
        
        this.user = new SystemUser(USER_ID, USER_NAME, PROVIDER_ID);
        this.user.setMetadata(metadata);
        
        this.asClient = Mockito.mock(AuthenticationServiceClient.class);
        Mockito.when(this.asClient.getToken(PUBLIC_KEY_STRING, ADMIN_USERNAME, ADMIN_PASSWORD)).thenReturn(TOKEN);
        
        this.authorizationClient = Mockito.mock(FederatedAuthorizationClient.class);
        Mockito.when(this.authorizationClient.isAuthorized(TOKEN, FEDERATION_ID, SERVICE_ID, BASE_USER_ID, operation)).thenReturn(true);
        
        this.plugin = new FederatedAuthorizationPlugin(asClient, authorizationClient, PUBLIC_KEY_STRING, 
                ADMIN_USERNAME, ADMIN_PASSWORD);
        
        boolean returnedValue = plugin.isAuthorized(user, operation); 
        
        assertTrue(returnedValue);
        Mockito.verify(this.authorizationClient).isAuthorized(TOKEN, FEDERATION_ID, SERVICE_ID, BASE_USER_ID, operation);
    }
}
