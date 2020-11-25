package cloud.fogbow.ras.core.plugins.authorization;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.gson.Gson;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ms.api.http.response.AuthorizationResponse;
import cloud.fogbow.ms.core.models.operation.RasAuthorizableOperation;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.RasPublicKeysHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class, RasPublicKeysHolder.class, 
    HttpRequestClient.class, ServiceAsymmetricKeysHolder.class, 
    AuthenticationUtil.class})
public class DistributedAuthorizationPluginTest {

    private DistributedAuthorizationPlugin plugin;
    private String MS_URL = "http://localhost";
    private String MS_PORT = "8080";
    private String token = "token";
    private String targetProvider = "targetProvider";
    private String returnedToken = "returned_token";
    private String userId = "userId";
    private String userName = "userName";
    private String providerId = "providerId";
    private String endpoint = String.format("%s:%s/%s", this.MS_URL, this.MS_PORT, 
            cloud.fogbow.ms.api.http.request.Authorization.AUTHORIZED_ENDPOINT);
    private Map<String, String> headers;
    private Map<String, String> body;
    private RasOperation operation;
    private RSAPublicKey msPublicKey;
    private RSAPrivateKey rasPrivateKey;
    private SystemUser user;
    private SystemUser userWithRoles;
    private String userRole = "role";
    private Set<String> userRolesSet;
    
    @Before
    public void setUp() throws FogbowException {
        this.user = new SystemUser(userId, userName, providerId);
        this.userWithRoles = new SystemUser(userId, userName, providerId);
        this.userRolesSet = new HashSet<String>();
        this.userRolesSet.add(userRole);
        this.userWithRoles.setUserRoles(userRolesSet);
        
        // keys
        this.msPublicKey = Mockito.mock(RSAPublicKey.class);
        this.rasPrivateKey = Mockito.mock(RSAPrivateKey.class);
        PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
        ServiceAsymmetricKeysHolder keysHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
        Mockito.doReturn(this.rasPrivateKey).when(keysHolder).getPrivateKey();
        BDDMockito.given(ServiceAsymmetricKeysHolder.getInstance()).willReturn(keysHolder);
        
        PowerMockito.mockStatic(RasPublicKeysHolder.class);
        RasPublicKeysHolder rasPublicKeysHolder = Mockito.mock(RasPublicKeysHolder.class);
        Mockito.doReturn(this.msPublicKey).when(rasPublicKeysHolder).getMSPublicKey();
        BDDMockito.given(RasPublicKeysHolder.getInstance()).willReturn(rasPublicKeysHolder);
        
        // token
        PowerMockito.mockStatic(AuthenticationUtil.class);
        BDDMockito.given(AuthenticationUtil.createFogbowToken(this.user, this.rasPrivateKey, this.msPublicKey)).willReturn(token);
        BDDMockito.given(AuthenticationUtil.authenticate(this.msPublicKey, returnedToken)).willReturn(this.userWithRoles);
        
        // request properties
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(this.MS_PORT).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.MS_PORT_KEY);
        Mockito.doReturn(this.MS_URL).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.MS_URL_KEY);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);

        // operation
        this.operation = new RasOperation(Operation.GET, ResourceType.ATTACHMENT);
        this.operation.setTargetProvider(this.targetProvider);
        
        // headers
        this.headers = new HashMap<String, String>();
        this.headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
        this.headers.put(CommonKeys.CONTENT_TYPE_KEY, DistributedAuthorizationPlugin.AUTHORIZATION_REQUEST_CONTENT_TYPE);
        
        // body
        this.body = new HashMap<String, String>();
        this.body.put(RasAuthorizableOperation.TARGET_PROVIDER_REQUEST_KEY, targetProvider);
        this.body.put(RasAuthorizableOperation.OPERATION_TYPE_REQUEST_KEY, operation.getOperationType().getValue());
    }
    
    // test case: when the "authorized" field of the response of an authorization request
    // is true and the response status code is equal to 200, the isAuthorized method will 
    // return true and update the user with the correct roles.
    @Test
    public void testIsAuthorizedOperationIsAuthorized() throws FogbowException {
        // response
        boolean authorized = true;
        Map<String, Object> responseContent = new HashMap<String, Object>();
        responseContent.put(AuthorizationResponse.AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD, authorized);
        responseContent.put(AuthorizationResponse.AUTHORIZATION_RESPONSE_TOKEN_FIELD, returnedToken);
        
        // success code
        Gson gson = new Gson();
        String responseString = gson.toJson(responseContent);
        Integer httpCode = HttpStatus.SC_OK;
        
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.doReturn(responseString).when(response).getContent();
        Mockito.doReturn(httpCode).when(response).getHttpCode();
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body)).willReturn(response);
        
        this.plugin = new DistributedAuthorizationPlugin();
        
        assertNull(this.user.getUserRoles());

        boolean isAuthorized = this.plugin.isAuthorized(this.user, operation);
        
        assertTrue(isAuthorized);
        assertTrue(this.user.getUserRoles().size() == 1);
        assertTrue(this.user.getUserRoles().contains(this.userRole));
    }
    
    // test case: when the "authorized" field of the response of an authorization request
    // is false and the response status code is equal to 200, the isAuthorized method will 
    // throw an UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class)
    public void testIsAuthorizedOperationIsNotAuthorized() throws FogbowException {
        // response
        boolean authorized = false;
        Map<String, Object> responseContent = new HashMap<String, Object>();
        responseContent.put(AuthorizationResponse.AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD, authorized);
        responseContent.put(AuthorizationResponse.AUTHORIZATION_RESPONSE_TOKEN_FIELD, returnedToken);
        
        // success code
        Gson gson = new Gson();
        String responseString = gson.toJson(responseContent);
        Integer httpCode = HttpStatus.SC_OK;
        
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.doReturn(responseString).when(response).getContent();
        Mockito.doReturn(httpCode).when(response).getHttpCode();
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body)).willReturn(response);
        
        this.plugin = new DistributedAuthorizationPlugin();
        this.plugin.isAuthorized(this.user, operation);
    }
    
    // test case: when the response status code of an authorization request is different from 200, 
    // the isAuthorized method will throw an UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class)
    public void testIsAuthorizedOperationNotSuccessfulReturnCode() throws FogbowException {
        // response
        boolean authorized = false;
        Map<String, Object> responseContent = new HashMap<String, Object>();
        responseContent.put(AuthorizationResponse.AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD, authorized);
        responseContent.put(AuthorizationResponse.AUTHORIZATION_RESPONSE_TOKEN_FIELD, returnedToken);
        
        // success code
        Gson gson = new Gson();
        String responseString = gson.toJson(responseContent);
        Integer httpCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.doReturn(responseString).when(response).getContent();
        Mockito.doReturn(httpCode).when(response).getHttpCode();
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body)).willReturn(response);
        
        this.plugin = new DistributedAuthorizationPlugin();
        this.plugin.isAuthorized(this.user, operation);
    }
    
    // test case: when the authorization request fails, the isAuthorized method will
    // throw an UnauthorizedRequestException
    @Test(expected = UnauthorizedRequestException.class)
    public void testIsAuthorizedOperationErrorOnRequest() throws FogbowException {
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body)).
                                willThrow(new FogbowException("error message"));
        
        this.plugin = new DistributedAuthorizationPlugin();
        this.plugin.isAuthorized(this.user, operation);
    }

}
