package cloud.fogbow.ras.core.plugins.authorization;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ms.api.http.response.Authorized;
import cloud.fogbow.ms.api.parameters.Provider;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.RasOperation;

public class DistributedAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {

    public static final String AUTHORIZATION_REQUEST_CONTENT_TYPE = "application/json";
    private String membershipServiceAddress;
    private String membershipServicePort;
    
    public DistributedAuthorizationPlugin() throws FogbowException {
        this.membershipServiceAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MS_URL_KEY);
        this.membershipServicePort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MS_PORT_KEY);
    }
   
    @Override
    public boolean isAuthorized(SystemUser systemUser, RasOperation operation) throws UnauthorizedRequestException {
        // if user.provider is local
        //     if operation.provider is local
        //         return true
        //     if operation.provider is remote
        //         send isauthorized(operation.provider) 
        // else
        //     if operation.provider is local
        //         send isauthorized(user.provider)
        //     else
        //          error
        
        boolean authorized = false;
        
        String userProvider = systemUser.getIdentityProviderId();
        String operationProvider = operation.getTargetProvider();
        
        try {
            if (providerIsLocal(userProvider)) {
                authorized = authorizeLocalUser(operationProvider);
            } else {
                authorized = authorizeRemoteUser(userProvider, operationProvider);
            }
        } catch (InternalServerErrorException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        } catch (URISyntaxException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        } catch (FogbowException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        }
        
        if (!authorized) {
            throw new UnauthorizedRequestException();
        } 
        
        return authorized;
    }

    private boolean authorizeLocalUser(String operationProvider) throws URISyntaxException, FogbowException {
        boolean authorized = true;
        
        if (!providerIsLocal(operationProvider)) {
            HttpResponse response = doRequestAndCheckStatus(operationProvider);
            authorized = getAuthorized(response);
        }
        
        return authorized;
    }
    
    private boolean authorizeRemoteUser(String userProvider, String operationProvider)
            throws URISyntaxException, FogbowException, UnauthorizedRequestException {
        boolean authorized;
        
        if (providerIsLocal(operationProvider)) {
            HttpResponse response = doRequestAndCheckStatus(userProvider);
            authorized = getAuthorized(response);
        } else {
            // Not expected to happen
            throw new UnauthorizedRequestException();
        }
        return authorized;
    }

    private boolean providerIsLocal(String provider) {
        return PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY).equals(provider);
    }
    
    private HttpResponse doRequestAndCheckStatus(String provider) throws URISyntaxException, FogbowException {
        HttpResponse response = doRequest(provider);

        if (response.getHttpCode() > HttpStatus.SC_OK) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        }
        
        return response;
    }

    private HttpResponse doRequest(String provider)
            throws URISyntaxException, FogbowException {
        URI uri = new URI(membershipServiceAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(membershipServicePort).
                path(cloud.fogbow.ms.api.http.request.Authorization.AUTHORIZED_ENDPOINT).
                build(true).toUri();
        String endpoint = uri.toString();
        
        // header
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(CommonKeys.CONTENT_TYPE_KEY, AUTHORIZATION_REQUEST_CONTENT_TYPE);
        
        // body
        Provider providerRequest = new Provider(provider);
        Map<String, String> body = providerRequest.asRequestBody();
        
        return HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
    }
    
    private boolean getAuthorized(HttpResponse response) {
        Gson gson = new Gson();
        
        // extract response
        Map<String, Object> jsonResponse = gson.fromJson(response.getContent(), HashMap.class);
        Authorized responseAuthorized = new Authorized(jsonResponse);
        return responseAuthorized.getAuthorized();
    }
}
