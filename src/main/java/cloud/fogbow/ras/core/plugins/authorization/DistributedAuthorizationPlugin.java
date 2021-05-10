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
import cloud.fogbow.ras.constants.Messages;
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
        // if requesting == target
        //      return true
        // else if localprovider == requesting
        //      return isTargetAuthorized(target)
        // else
        //      return isRequesterAuthorized(requesting)
        
        boolean authorized = false;
        
        try {
            // if requesting provider == target provider then the operation is local
            // therefore, authorized
            if (operation.getRequester().equals(operation.getProvider())) {
                authorized = true;
            } else if (isRequesterLocal(operation)) {
                HttpResponse response = doTargetAuthorizedRequestAndCheckStatus(operation.getProvider());
                authorized = getAuthorized(response);
            } else {
                HttpResponse response = doRequesterAuthorizedRequestAndCheckStatus(operation.getRequester());
                authorized = getAuthorized(response);
            }
        } catch (InternalServerErrorException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        } catch (URISyntaxException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        } catch (FogbowException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        }
        
        if (!authorized) {
            throw new UnauthorizedRequestException(Messages.Exception.PROVIDER_IS_NOT_AUTHORIZED);
        }
        
        return authorized;
    }

    private boolean isRequesterLocal(RasOperation operation) {
        return PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY)
                .equals(operation.getRequester());
    }

    private HttpResponse doRequesterAuthorizedRequestAndCheckStatus(String requester) throws URISyntaxException, FogbowException {
        String endpoint = getAuthorizationEndpoint(cloud.fogbow.ms.api.http.request.Authorization.REQUESTER_AUTHORIZED_ENDPOINT);
        HttpResponse response = doRequest(endpoint, requester);

        if (response.getHttpCode() > HttpStatus.SC_OK) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        }
        
        return response;
    }

    private HttpResponse doTargetAuthorizedRequestAndCheckStatus(String provider) throws URISyntaxException, FogbowException {
        String endpoint = getAuthorizationEndpoint(cloud.fogbow.ms.api.http.request.Authorization.TARGET_AUTHORIZED_ENDPOINT);
        HttpResponse response = doRequest(endpoint, provider);

        if (response.getHttpCode() > HttpStatus.SC_OK) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        }
        
        return response;
    }
    
    private String getAuthorizationEndpoint(String path) throws URISyntaxException {
        URI uri = new URI(membershipServiceAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(membershipServicePort).
                path(path).
                build(true).toUri();
        return uri.toString();
    }
    
    private HttpResponse doRequest(String endpoint, String provider)
            throws URISyntaxException, FogbowException {
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

	@Override
	public void setPolicy(String policy) {
		// Ignore
	}

	@Override
	public void updatePolicy(String policy) {
		// Ignore
	}
}
