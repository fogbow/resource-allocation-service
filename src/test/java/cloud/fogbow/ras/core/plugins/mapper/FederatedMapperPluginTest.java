package cloud.fogbow.ras.core.plugins.mapper;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.RasClassFactory;

public class FederatedMapperPluginTest {
    private static final String INTERNAL_MAPPER_PLUGIN_CLASS_NAME = "className";
    private static final String IDP_URL = "idpUrl";
    private static final String PUBLIC_KEY_STRING = "publicKey";
    private static final String ADMIN_USERNAME = "adminUsername";
    private static final String ADMIN_PASSWORD = "adminPassword";
    private static final String USER_BASE_ID = "userBaseId";
    private static final String FEDERATION_ID = "federationId";
    private static final String SERVICE_ID = "serviceId";
    private static final String TOKEN = "token";
    private static final String USER_ID = USER_BASE_ID + 
            FogbowConstants.FEDERATION_ID_SEPARATOR + FEDERATION_ID;
    private static final HashMap<String, String> CREDENTIALS = new HashMap<String, String>();
    
    private FederatedMapperPlugin mapper;
    private RasClassFactory classFactory;
    private AuthenticationServiceClient asClient;
    private MapperClient mapperClient;
    private SystemUser systemUser;
    private Map<String, String> metadata;
    
    @Test
    public void testMap() throws FogbowException {
        this.asClient = Mockito.mock(AuthenticationServiceClient.class);
        Mockito.when(this.asClient.getToken(PUBLIC_KEY_STRING, ADMIN_USERNAME, ADMIN_PASSWORD)).thenReturn(TOKEN);
        
        this.metadata = new HashMap<String, String>();
        this.metadata.put(FederatedMapperPlugin.SERVICE_ID_METADATA_KEY, SERVICE_ID);
        
        this.systemUser = Mockito.mock(SystemUser.class);
        Mockito.when(this.systemUser.getId()).thenReturn(USER_ID);
        Mockito.when(this.systemUser.getMetadata()).thenReturn(metadata);

        this.mapperClient = Mockito.mock(MapperClient.class);
        Mockito.when(this.mapperClient.getCredentials(TOKEN, FEDERATION_ID, SERVICE_ID, USER_ID)).thenReturn(CREDENTIALS);
        
        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        SystemToCloudMapperPlugin<CloudUser, SystemUser> internalPlugin = 
                Mockito.mock(SystemToCloudMapperPlugin.class);
        Mockito.when(internalPlugin.map(systemUser)).thenReturn(cloudUser);
        
        this.classFactory = Mockito.mock(RasClassFactory.class);
        Mockito.when(this.classFactory.createPluginInstance(
                INTERNAL_MAPPER_PLUGIN_CLASS_NAME, IDP_URL, CREDENTIALS)).thenReturn(internalPlugin);
        
        mapper = new FederatedMapperPlugin(INTERNAL_MAPPER_PLUGIN_CLASS_NAME, IDP_URL, 
                PUBLIC_KEY_STRING, ADMIN_USERNAME, ADMIN_PASSWORD, classFactory, asClient, 
                mapperClient);
        
        CloudUser response = mapper.map(this.systemUser);
        
        assertEquals(cloudUser, response);
    }
}
