package cloud.fogbow.ras.core.plugins.authorization;

import static org.junit.Assert.assertTrue;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.util.PublicKeysHolder;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

@PrepareForTest({ PublicKeysHolder.class, TokenProtector.class, HttpRequestClient.class, 
    ServiceAsymmetricKeysHolder.class })
@RunWith(PowerMockRunner.class)
public class FederatedAuthorizationClientTest {
    private static final String PROVIDER_ID = "providerId";
    private static final String REMOTE_PROVIDER_ID = "remoteProviderId";
    private static final String TOKEN = "token";
    private static final String REWRAP_TOKEN = "rewrapToken";
    private static final String FEDERATION_ID = "federationId";
    private static final String SERVICE_ID = "serviceId";
    private static final String USER_ID = "userId";
    private static final String AUTHORIZATION_URL = "http://0.0.0.0";
    private static final String AUTHORIZATION_PORT = "8080";
    private static final String AUTHORIZATION_PUBLIC_KEY_SUFFIX = "/service/publicKey";
    private static final String AUTHORIZATION_AUTHORIZE_SUFFIX = "/service/authorize";
    private static final String ENDPOINT = AUTHORIZATION_URL + ":" + AUTHORIZATION_PORT + 
            AUTHORIZATION_AUTHORIZE_SUFFIX + "/" + FEDERATION_ID + "/" + SERVICE_ID + "/" + USER_ID + "/";
    private static final String RESPONSE_CONTENT = String.format("{\"%s\":\"%s\"}", 
            FederatedAuthorizationClient.AUTHORIZED_RESPONSE_KEY, "true");
    private static final String NOT_AUTHORIZED_RESPONSE_CONTENT = String.format("{\"%s\":\"%s\"}", 
            FederatedAuthorizationClient.AUTHORIZED_RESPONSE_KEY, "false");
    
    private FederatedAuthorizationClient client;
    private RasOperation operation;
    private Map<String, String> headers;
    private Map<String, String> body;
    private HttpResponse response;
    private RSAPublicKey publicKey;
    private ServiceAsymmetricKeysHolder keysHolder;
    private RSAPrivateKey privateKey;
    
    @Before
    public void setUp() throws FogbowException {
      this.publicKey = Mockito.mock(RSAPublicKey.class);
      PowerMockito.mockStatic(PublicKeysHolder.class);
      BDDMockito.given(PublicKeysHolder.getPublicKey(AUTHORIZATION_URL, AUTHORIZATION_PORT, 
              AUTHORIZATION_PUBLIC_KEY_SUFFIX)).willReturn(publicKey);
      
      this.keysHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
      Mockito.when(this.keysHolder.getPrivateKey()).thenReturn(privateKey);
      
      PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
      BDDMockito.given(ServiceAsymmetricKeysHolder.getInstance()).willReturn(keysHolder);
      
      PowerMockito.mockStatic(TokenProtector.class);
      BDDMockito.given(TokenProtector.rewrap(privateKey, publicKey, TOKEN, 
              FogbowConstants.TOKEN_STRING_SEPARATOR)).willReturn(REWRAP_TOKEN);
      
      this.headers = new HashMap<String, String>();
      headers.put(CommonKeys.CONTENT_TYPE_KEY, FederatedAuthorizationClient.AUTHORIZATION_REQUEST_CONTENT_TYPE);
      headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, REWRAP_TOKEN);
      
      this.response = Mockito.mock(HttpResponse.class);
      Mockito.when(this.response.getContent()).thenReturn(RESPONSE_CONTENT);
      Mockito.when(this.response.getHttpCode()).thenReturn(HttpStatus.SC_OK);
      
      this.client = new FederatedAuthorizationClient(AUTHORIZATION_URL, AUTHORIZATION_PORT, 
              AUTHORIZATION_PUBLIC_KEY_SUFFIX, AUTHORIZATION_AUTHORIZE_SUFFIX);
    }
    
    @Test
    public void testIsAuthorizedGetOperation() throws FogbowException {
        this.body = new HashMap<String, String>();
        body.put(FederatedAuthorizationClient.OPERATION_STRING_KEY, "GET");
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.POST, ENDPOINT, headers, body)).willReturn(response);
        
        this.operation = new RasOperation(Operation.GET, ResourceType.COMPUTE, PROVIDER_ID, REMOTE_PROVIDER_ID);
        
        boolean returnedValue = client.isAuthorized(TOKEN, FEDERATION_ID, SERVICE_ID, USER_ID, operation);
        
        assertTrue(returnedValue);
    }
    
    @Test
    public void testIsAuthorizedPostOperation() throws FogbowException {
        this.body = new HashMap<String, String>();
        body.put(FederatedAuthorizationClient.OPERATION_STRING_KEY, "POST");
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.POST, ENDPOINT, headers, body)).willReturn(response);
        
        this.operation = new RasOperation(Operation.CREATE, ResourceType.COMPUTE, PROVIDER_ID, REMOTE_PROVIDER_ID);
        
        boolean returnedValue = client.isAuthorized(TOKEN, FEDERATION_ID, SERVICE_ID, USER_ID, operation);
        
        assertTrue(returnedValue);
    }
    
    @Test
    public void testIsAuthorizedDeleteOperation() throws FogbowException {
        this.body = new HashMap<String, String>();
        body.put(FederatedAuthorizationClient.OPERATION_STRING_KEY, "DELETE");
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.POST, ENDPOINT, headers, body)).willReturn(response);
        
        this.operation = new RasOperation(Operation.DELETE, ResourceType.COMPUTE, PROVIDER_ID, REMOTE_PROVIDER_ID);
        
        boolean returnedValue = client.isAuthorized(TOKEN, FEDERATION_ID, SERVICE_ID, USER_ID, operation);
        
        assertTrue(returnedValue);
    }
    
    @Test(expected = UnauthorizedRequestException.class)
    public void testIsNotAuthorized() throws FogbowException {
        this.body = new HashMap<String, String>();
        body.put(FederatedAuthorizationClient.OPERATION_STRING_KEY, "DELETE");
        
        Mockito.when(this.response.getContent()).thenReturn(NOT_AUTHORIZED_RESPONSE_CONTENT);
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.POST, ENDPOINT, headers, body)).willReturn(response);
        
        this.operation = new RasOperation(Operation.DELETE, ResourceType.COMPUTE, PROVIDER_ID, REMOTE_PROVIDER_ID);
        
        client.isAuthorized(TOKEN, FEDERATION_ID, SERVICE_ID, USER_ID, operation);
    }
}
