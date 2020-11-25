package cloud.fogbow.ras.core.plugins.authorization;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.RasPublicKeysHolder;
import cloud.fogbow.ras.core.models.RasOperation;

public class DistributedAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {

    public static final String AUTHORIZATION_REQUEST_CONTENT_TYPE = "application/json";
    private static final String OPERATION_TYPE_REQUEST_KEY = "operationType";
    private static final String TARGET_PROVIDER_REQUEST_KEY = "targetProvider";
    private static final String AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD = "authorized";
    private static final String AUTHORIZATION_RESPONSE_TOKEN_FIELD = "token";
    private RSAPublicKey msPublicKey;
    private String membershipServiceAddress;
    private String membershipServicePort;
    
    public DistributedAuthorizationPlugin() throws FogbowException {
        getMSPublicKey();
        this.membershipServiceAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MS_URL_KEY);
        this.membershipServicePort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MS_PORT_KEY);
    }
    
    @Override
    public boolean isAuthorized(SystemUser systemUser, RasOperation operation) throws UnauthorizedRequestException {
        boolean authorized = false;
        
        try {
            // get token from systemuser
            String token = getToken(systemUser);
            
            // make request
            HttpResponse response = doRequest(operation, token);
            
            if (response.getHttpCode() > HttpStatus.SC_OK) {
                Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
                throw new UnavailableProviderException(e.getMessage());
            } else {
                // get information from request response
                authorized = getAuthorizedAndUpdateUser(response, systemUser);
            }
            
            if (!authorized) {
                throw new UnauthorizedRequestException();
            }
        } catch (InternalServerErrorException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        } catch (URISyntaxException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        } catch (FogbowException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        }
        
        return authorized;
    }

    private String getToken(SystemUser systemUser) throws InternalServerErrorException {
        RSAPrivateKey privateKey = ServiceAsymmetricKeysHolder.getInstance().getPrivateKey();
        return AuthenticationUtil.createFogbowToken(systemUser, privateKey, this.msPublicKey);
    }

    private HttpResponse doRequest(RasOperation operation, String token)
            throws URISyntaxException, FogbowException {
        String provider = operation.getTargetProvider();
        URI uri = new URI(membershipServiceAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(membershipServicePort).
                path(cloud.fogbow.ms.api.http.request.Authorization.AUTHORIZED_ENDPOINT).
                build(true).toUri();
        String endpoint = uri.toString();
        
        // header
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, token);
        headers.put(CommonKeys.CONTENT_TYPE_KEY, AUTHORIZATION_REQUEST_CONTENT_TYPE);
        
        // body
        Map<String, String> body = new HashMap<String, String>();
        body.put(TARGET_PROVIDER_REQUEST_KEY, provider);
        body.put(OPERATION_TYPE_REQUEST_KEY, operation.getOperationType().getValue());
        
        return HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
    }
    
    private boolean getAuthorizedAndUpdateUser(HttpResponse response, SystemUser systemUser) throws UnauthenticatedUserException {
        Gson gson = new Gson();
        
        // extract response
        Map<String, Object> jsonResponse = gson.fromJson(response.getContent(), HashMap.class);
        // set new token information
        String tokenResponse = (String) jsonResponse.get(AUTHORIZATION_RESPONSE_TOKEN_FIELD);
        SystemUser user = AuthenticationUtil.authenticate(msPublicKey, tokenResponse);
        systemUser.setUserRoles(user.getUserRoles());
        return (boolean) jsonResponse.get(AUTHORIZATION_RESPONSE_AUTHORIZED_FIELD);
    }
    
    protected RSAPublicKey getMSPublicKey() throws FogbowException {
        if (this.msPublicKey == null) {
            this.msPublicKey = RasPublicKeysHolder.getInstance().getMSPublicKey();
        }
        return this.msPublicKey;
    }
}
