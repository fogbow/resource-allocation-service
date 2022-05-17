package cloud.fogbow.ras.core.plugins.mapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;

// TODO change this class name to FederatedAuthenticationServiceClient
public class AuthenticationServiceClient {
    /**
     * Key used in the header of the get token request to
     * represent the content type.
     */
    @VisibleForTesting
    static final String AUTHENTICATION_REQUEST_CONTENT_TYPE = "application/json";
    /**
     * Key used in the response body to represent the user token.
     */
    @VisibleForTesting
    static final String TOKEN_RESPONSE_KEY = "token";
    /**
     * Key used in the body of the get token request to represent 
     * the federation ID.
     */
    @VisibleForTesting
    static final String FEDERATION_ID = "federationId";
    /**
     * Key used in the body of the get token request to represent
     * the operator ID.
     */
    @VisibleForTesting
    static final String OPERATOR_ID = "operatorId";
    /**
     * Key used in the body of the get token request to represent
     * the credentials map.
     */
    @VisibleForTesting
    static final String CREDENTIALS_REQUEST_KEY = "credentials";
    /**
     * Key used in the credentials map to represent the user public key.
     */
    @VisibleForTesting
    static final String PUBLIC_KEY_REQUEST_KEY = "userPublicKey";
    /**
     * Key used in the credentials map to represent the password.
     */
    @VisibleForTesting
    static final String PASSWORD_REQUEST_KEY = "password";
    /**
     * Key used in the credentials map to represent the username.
     */
    @VisibleForTesting
    static final String USERNAME_REQUEST_KEY = "username";
    
    private String asTokenEndpoint;

    // TODO add parameters validation
    // TODO fix variable names
    public AuthenticationServiceClient() throws ConfigurationErrorException {
        this(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_URL_KEY), 
             PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_PORT_KEY),
             PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_TOKENS_ENDPOINT_KEY));
    }
    
    public AuthenticationServiceClient(String authorizationServiceAddress, 
            String authorizationServicePort, String authorizationServiceTokensEndpoint) throws ConfigurationErrorException {
        try {
            this.asTokenEndpoint = getAuthenticationEndpoint(authorizationServiceTokensEndpoint, 
                    authorizationServiceAddress, authorizationServicePort);
        } catch (URISyntaxException e) {
            throw new ConfigurationErrorException(e.getMessage());
        }
    }
    
    private String getAuthenticationEndpoint(String path, String authorizationServiceAddress, 
            String authorizationServicePort) throws URISyntaxException {
        URI uri = new URI(authorizationServiceAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(authorizationServicePort).
                path(path).
                build(true).toUri();
        return uri.toString();
    }
    
    public String getToken(String publicKey, String userName, String password) throws FogbowException {
        HttpResponse response = doRequestAndCheckStatus(publicKey, userName, password);
        return getTokenFromResponse(response);
    }

    private HttpResponse doRequestAndCheckStatus(String publicKey, 
            String userName, String password) throws FogbowException {
        HttpResponse response = doRequest(this.asTokenEndpoint, publicKey, userName, password);
        int responseCode = response.getHttpCode();
        
        if (responseCode == HttpStatus.SC_UNAUTHORIZED) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnauthenticatedUserException(e.getMessage());
        }
        
        if (response.getHttpCode() > HttpStatus.SC_CREATED) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        }
        
        return response;
    }
    
    private HttpResponse doRequest(String endpoint, String publicKey, 
            String userName, String password)
            throws FogbowException {
        // header
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(CommonKeys.CONTENT_TYPE_KEY, AUTHENTICATION_REQUEST_CONTENT_TYPE);
        
        // body
        Map<String, Object> body = asRequestBody(publicKey, userName, password);
        
        return HttpRequestClient.doGenericRequestGenericBody(HttpMethod.POST, endpoint, headers, body);
    }
    
    private Map<String, Object> asRequestBody(String publicKey, String userName, String password) {
        Map<String, String> credentials = new HashMap<String, String>();
        credentials.put(USERNAME_REQUEST_KEY, userName);
        credentials.put(PASSWORD_REQUEST_KEY, password);
        credentials.put(PUBLIC_KEY_REQUEST_KEY, publicKey);
        
        Map<String, Object> body = new HashMap<String, Object>();
        body.put(CREDENTIALS_REQUEST_KEY, credentials);
        body.put(OPERATOR_ID, userName);
        body.put(FEDERATION_ID, null);

        return body;
    }
    
    private String getTokenFromResponse(HttpResponse response) {
        Gson gson = new Gson();
        Map<String, String> jsonResponse = gson.fromJson(response.getContent(), HashMap.class);
        return jsonResponse.get(TOKEN_RESPONSE_KEY);
    }
}
