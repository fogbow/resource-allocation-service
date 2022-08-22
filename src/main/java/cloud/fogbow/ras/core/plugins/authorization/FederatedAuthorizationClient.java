package cloud.fogbow.ras.core.plugins.authorization;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.PublicKeysHolder;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.core.models.RasOperation;

public class FederatedAuthorizationClient {
    private static final String AUTHORIZATION_REQUEST_CONTENT_TYPE = "application/json";
    private static final String OPERATION_STRING_KEY = "operation";
    private static final String AUTHORIZED_RESPONSE_KEY = "authorized";
    
    private String authorizationUrl;
    private String authorizationPort;
    private String authorizationPublicKeySuffix;
    private String authorizationAuthorizeSuffix;
    
    public FederatedAuthorizationClient(String mapperUrl, String mapperPort, String mapperPublicKeySuffix,
            String mapperMapSuffix) {
        this.authorizationUrl = mapperUrl;
        this.authorizationPort = mapperPort;
        this.authorizationPublicKeySuffix = mapperPublicKeySuffix;
        this.authorizationAuthorizeSuffix = mapperMapSuffix;
    }

    public boolean isAuthorized(String token, String federation, String serviceId, String userId, RasOperation operation) throws UnauthorizedRequestException {
        try {
            RSAPublicKey mapperPublicKey = PublicKeysHolder.getPublicKey(
                    this.authorizationUrl, this.authorizationPort, this.authorizationPublicKeySuffix);
            String rewrapToken = TokenProtector.rewrap(ServiceAsymmetricKeysHolder.getInstance().getPrivateKey(), 
                    mapperPublicKey, token, FogbowConstants.TOKEN_STRING_SEPARATOR);
            
            String endpoint = getMapperEndpoint(federation, serviceId, userId);
            Map<String, String> headers = new HashMap<String, String>();
            headers.put(CommonKeys.CONTENT_TYPE_KEY, AUTHORIZATION_REQUEST_CONTENT_TYPE);
            headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, rewrapToken);
            Map<String, String> body = new HashMap<String, String>();
            body.put(OPERATION_STRING_KEY, getOperationDescription(operation));
            
            HttpResponse response = HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
            checkRequestIsSuccessful(response);
            return extractAuthorizedFromResponse(response);
        } catch (URISyntaxException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        } catch (FogbowException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        }
    }
    
    private String getMapperEndpoint(String federation, String serviceId, String userId) throws URISyntaxException {
        URI uri = new URI(this.authorizationUrl);
        uri = UriComponentsBuilder.fromUri(uri).port(this.authorizationPort).path("/").path(this.authorizationAuthorizeSuffix).path("/").
                path(federation).path("/").path(serviceId).path("/").path(userId).path("/").build(true).toUri();
        return uri.toString();
    }

    private String getOperationDescription(RasOperation operation) {
        switch (operation.getOperationType()) {
            case GET:
            case GET_ALL:
            case GET_USER_ALLOCATION: return "GET";
            case CREATE:
            case PAUSE:
            case HIBERNATE:
            case STOP:
            case RESUME: 
            case PAUSE_ALL:
            case HIBERNATE_ALL:
            case STOP_ALL:
            case RESUME_ALL:
            case TAKE_SNAPSHOT:
            case RELOAD: return "POST";
            case DELETE: return "DELETE";
            default: return "GET";
        }
    }

    private void checkRequestIsSuccessful(HttpResponse response) throws UnavailableProviderException {
        if (response.getHttpCode() > HttpStatus.SC_OK) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        }
    }
    
    private boolean extractAuthorizedFromResponse(HttpResponse response) throws UnauthorizedRequestException {
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> content = new Gson().fromJson(response.getContent(), type);
        
        if (content.get(AUTHORIZED_RESPONSE_KEY).equals("true")) {
            return true;
        } else {
            throw new UnauthorizedRequestException();
        }
    }
}
