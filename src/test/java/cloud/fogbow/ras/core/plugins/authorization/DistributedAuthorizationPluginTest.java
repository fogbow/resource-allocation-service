package cloud.fogbow.ras.core.plugins.authorization;

import static org.junit.Assert.assertTrue;

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

import com.google.gson.Gson;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ms.api.http.response.Authorized;
import cloud.fogbow.ms.api.parameters.Provider;
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
    private String userId = "userId";
    private String remoteUserId = "remoteUserId";
    private String userName = "userName";
    private String remoteUserName = "remoteUserName";
    private String localProviderId = "providerId";
    private String remoteProviderId = "remoteProviderId";
    private String endpoint = String.format("%s:%s/%s", this.MS_URL, this.MS_PORT, 
            cloud.fogbow.ms.api.http.request.Authorization.AUTHORIZED_ENDPOINT);
    private Map<String, String> headers;
    private Map<String, String> body;
    private RasOperation operation;
    private SystemUser localUser;
    private SystemUser remoteUser;
    
    @Before
    public void setUp() throws FogbowException {
        this.localUser = new SystemUser(userId, userName, localProviderId);
        this.remoteUser = new SystemUser(remoteUserId, remoteUserName, remoteProviderId);
        
        // request properties
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(this.MS_PORT).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.MS_PORT_KEY);
        Mockito.doReturn(this.MS_URL).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.MS_URL_KEY);
        Mockito.doReturn(this.localProviderId).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
    }
    
    // test case: when the "authorized" field of the response of an authorization request
    // is true and the response status code is equal to 200, the isAuthorized method will 
    // return true and update the user with the correct roles.
    @Test
    public void testOperationIsAuthorizedUserIsLocalOperationIsLocal() throws FogbowException {
        setUpAuthorizedOperationRequest(this.localProviderId, this.localProviderId);

        // response
        boolean authorized = true;
        Map<String, Object> responseContent = new HashMap<String, Object>();
        responseContent.put(Authorized.AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD, authorized);

        // success code
        Gson gson = new Gson();
        String responseString = gson.toJson(responseContent);
        Integer httpCode = HttpStatus.SC_OK;
        
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.doReturn(responseString).when(response).getContent();
        Mockito.doReturn(httpCode).when(response).getHttpCode();
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.POST, this.endpoint, this.headers, this.body)).willReturn(response);
        
        this.plugin = new DistributedAuthorizationPlugin();
        
        boolean isAuthorized = this.plugin.isAuthorized(this.localUser, operation);
        
        assertTrue(isAuthorized);
    }
    
    // TODO: documentation
    @Test
    public void testOperationIsAuthorizedUserIsLocalOperationIsRemote() throws FogbowException {
        setUpAuthorizedOperationRequest(this.remoteProviderId, this.remoteProviderId);

        // response
        boolean authorized = true;
        HashMap<String, Object> responseContent = new HashMap<String, Object>();
        responseContent.put(Authorized.AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD, authorized);

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
        
        boolean isAuthorized = this.plugin.isAuthorized(this.localUser, operation);
        
        assertTrue(isAuthorized);
    }
    
    // TODO: documentation
    @Test
    public void testIsAuthorizedOperationIsAuthorizedUserIsRemote() throws FogbowException {
        setUpAuthorizedOperationRequest(this.remoteProviderId, this.localProviderId);

        // response
        boolean authorized = true;
        Map<String, Object> responseContent = new HashMap<String, Object>();
        responseContent.put(Authorized.AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD, authorized);
        
        // success code
        Gson gson = new Gson();
        String responseString = gson.toJson(responseContent);
        Integer httpCode = HttpStatus.SC_OK;
        
        HttpResponse response = Mockito.mock(HttpResponse.class);
        Mockito.doReturn(responseString).when(response).getContent();
        Mockito.doReturn(httpCode).when(response).getHttpCode();
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        
        BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.POST, this.endpoint, this.headers, this.body)).willReturn(response);
        
        this.plugin = new DistributedAuthorizationPlugin();
        
        boolean isAuthorized = this.plugin.isAuthorized(this.remoteUser, operation);
        
        assertTrue(isAuthorized);
    }
    
    // test case: when the "authorized" field of the response of an authorization request
    // is false and the response status code is equal to 200, the isAuthorized method will 
    // throw an UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class)
    public void testOperationIsNotAuthorized() throws FogbowException {
        setUpAuthorizedOperationRequest(this.remoteProviderId, this.remoteProviderId);

        // response
        boolean authorized = false;
        Map<String, Object> responseContent = new HashMap<String, Object>();
        responseContent.put(Authorized.AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD, authorized);

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
        this.plugin.isAuthorized(this.localUser, operation);
    }
    
    // test case: when the response status code of an authorization request is different from 200, 
    // the isAuthorized method will throw an UnauthorizedRequestException.
    @Test(expected = UnauthorizedRequestException.class)
    public void testIsAuthorizedOperationNotSuccessfulReturnCode() throws FogbowException {
        setUpAuthorizedOperationRequest(this.remoteProviderId, this.remoteProviderId);

        // response
        boolean authorized = false;
        Map<String, Object> responseContent = new HashMap<String, Object>();
        responseContent.put(Authorized.AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD, authorized);

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
        this.plugin.isAuthorized(this.localUser, operation);
    }
    
    // test case: when the authorization request fails, the isAuthorized method will
    // throw an UnauthorizedRequestException
    @Test(expected = UnauthorizedRequestException.class)
    public void testIsAuthorizedOperationErrorOnRequest() throws FogbowException {
        setUpAuthorizedOperationRequest(this.remoteProviderId, this.remoteProviderId);
        
        PowerMockito.mockStatic(HttpRequestClient.class);
        BDDMockito.given(HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body)).
                                willThrow(new FogbowException("error message"));
        
        this.plugin = new DistributedAuthorizationPlugin();
        this.plugin.isAuthorized(this.localUser, operation);
    }
    
    private void setUpAuthorizedOperationRequest(String providerToAuthorize, String operationProvider) {
        // operation
        this.operation = new RasOperation(Operation.GET, ResourceType.ATTACHMENT);
        this.operation.setTargetProvider(operationProvider);
        
        // headers
        this.headers = new HashMap<String, String>();
        this.headers.put(CommonKeys.CONTENT_TYPE_KEY, DistributedAuthorizationPlugin.AUTHORIZATION_REQUEST_CONTENT_TYPE);
        
        // body
        this.body = new HashMap<String, String>();
        this.body.put(Provider.PROVIDER_KEY, providerToAuthorize);
    }
}
