package cloud.fogbow.ras.core.plugins.mapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.util.PublicKeysHolder;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.api.http.CommonKeys;

public class MapperClient {
    private String mapperUrl;
    private String mapperPort;
    private String mapperPublicKeySuffix;
    private String mapperMapSuffix;
    private String cloudName;
    private Gson jsonToMapConverter;

    public MapperClient(String mapperUrl, String mapperPort, String mapperPublicKeySuffix, 
            String mapperMapSuffix, String cloudName) {
        this.mapperUrl = mapperUrl;
        this.mapperPort = mapperPort;
        this.mapperPublicKeySuffix = mapperPublicKeySuffix;
        this.mapperMapSuffix = mapperMapSuffix;
        this.cloudName = cloudName;
        this.jsonToMapConverter = new Gson();
    }
    
    public HashMap<String, String> getCredentials(String token, String federation, 
            String serviceId, String userId) throws FogbowException {
        try {
            RSAPublicKey mapperPublicKey = PublicKeysHolder.getPublicKey(
                    this.mapperUrl, this.mapperPort, this.mapperPublicKeySuffix);
            String rewrapToken = TokenProtector.rewrap(ServiceAsymmetricKeysHolder.getInstance().getPrivateKey(), 
                    mapperPublicKey, token, FogbowConstants.TOKEN_STRING_SEPARATOR);
            
            String endpoint = getMapperEndpoint(federation, serviceId, userId);
            Map<String, String> headers = new HashMap<String, String>();
            headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, rewrapToken);
            Map<String, String> body = new HashMap<String, String>();
            
            HttpResponse response = HttpRequestClient.doGenericRequest(HttpMethod.GET, endpoint, headers, body);
            checkRequestIsSuccessful(response);
            return extractCredentialsFromResponse(response);
        } catch (URISyntaxException e) {
            throw new FogbowException(e.getMessage());
        }
    }

    private void checkRequestIsSuccessful(HttpResponse response) throws UnavailableProviderException {
        if (response.getHttpCode() > HttpStatus.SC_OK) {
            Throwable e = new HttpResponseException(response.getHttpCode(), response.getContent());
            throw new UnavailableProviderException(e.getMessage());
        }
    }
    
    private String getMapperEndpoint(String federation, String serviceId, String userId) throws URISyntaxException {
        URI uri = new URI(this.mapperUrl);
        uri = UriComponentsBuilder.fromUri(uri).port(this.mapperPort).path("/").path(this.mapperMapSuffix).path("/").
                path(federation).path("/").path(serviceId).path("/").path(userId).path("/").path(this.cloudName).build(true).toUri();
        return uri.toString();
    }
    
    private HashMap<String, String> extractCredentialsFromResponse(HttpResponse response) {
        String responseContent = response.getContent();
        LinkedTreeMap<String, String> baseCredentialsMap = 
                this.jsonToMapConverter.fromJson(responseContent, 
                        new TypeToken<LinkedTreeMap<String, String>>(){}.getType());
        return new HashMap<String, String>(baseCredentialsMap);
    }
}
