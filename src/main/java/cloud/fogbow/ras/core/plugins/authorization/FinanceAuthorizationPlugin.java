package cloud.fogbow.ras.core.plugins.authorization;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.fs.api.http.response.Authorized;
import cloud.fogbow.fs.api.parameters.AuthorizableUser;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.RasOperation;

public class FinanceAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {

	public static final String AUTHORIZATION_REQUEST_CONTENT_TYPE = "application/json";
	private String financeServiceAddress;
	private String financeServicePort;
	
	public FinanceAuthorizationPlugin() {
		this.financeServiceAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.FS_URL_KEY);
		this.financeServicePort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.FS_PORT_KEY);
	}

	@Override
	public boolean isAuthorized(SystemUser systemUser, RasOperation operation) throws UnauthorizedRequestException {
		boolean authorized = false;

		try {
			HttpResponse response = doIsAuthorizedRequestAndCheckStatus(systemUser.getId(), 
					getOperationParameters(operation));
			authorized = getAuthorized(response);
		} catch (InternalServerErrorException e) {
			throw new UnauthorizedRequestException(e.getMessage());
		} catch (URISyntaxException e) {
			throw new UnauthorizedRequestException(e.getMessage());
		} catch (FogbowException e) {
			throw new UnauthorizedRequestException(e.getMessage());
		}

		if (!authorized) {
			throw new UnauthorizedRequestException(Messages.Exception.USER_IS_NOT_FINANCIALLY_AUTHORIZED);
		}
			
		return authorized;
	}

	private HashMap<String, String> getOperationParameters(RasOperation operation) {
		HashMap<String, String> operationParameters = new HashMap<String, String>();
		
		// FIXME constant
		operationParameters.put("operationType", operation.getOperationType().getValue());
		operationParameters.put("resourceType", operation.getResourceType().getValue());
		return operationParameters;
	}

	// TODO part of this code is duplicated in DistributedAuthorizationPlugin.
	// We should refactor both classes, removing this duplication.
	private HttpResponse doIsAuthorizedRequestAndCheckStatus(String userId, 
				Map<String, String> operationParameters) throws URISyntaxException, FogbowException {
        String endpoint = getAuthorizationEndpoint(cloud.fogbow.fs.api.http.request.Authorization.AUTHORIZED_ENDPOINT);
        HttpResponse response = doRequest(endpoint, userId, operationParameters);

        if (response.getHttpCode() > HttpStatus.SC_OK) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        }
        
        return response;
    }
	
    private String getAuthorizationEndpoint(String path) throws URISyntaxException {
        URI uri = new URI(financeServiceAddress);
        uri = UriComponentsBuilder.fromUri(uri).port(financeServicePort).
                path(path).
                build(true).toUri();
        return uri.toString();
    }
    
    private HttpResponse doRequest(String endpoint, String userId, Map<String, String> operationParameters)
            throws URISyntaxException, FogbowException {
        // header
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(CommonKeys.CONTENT_TYPE_KEY, AUTHORIZATION_REQUEST_CONTENT_TYPE);
        
        // body
        AuthorizableUser authorizableUser = new AuthorizableUser(userId, operationParameters);
        Map<String, String> body = authorizableUser.asRequestBody();
        
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
	public void setPolicy(String policy) throws ConfigurationErrorException {
		// Ignore
	}

	@Override
	public void updatePolicy(String policy) throws ConfigurationErrorException {
		// Ignore
	}
}
