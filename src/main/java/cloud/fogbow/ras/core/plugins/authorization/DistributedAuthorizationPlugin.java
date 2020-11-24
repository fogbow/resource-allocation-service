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
                Gson gson = new Gson();
                
                // extract response
                Map<String, Object> jsonResponse = gson.fromJson(response.getContent(), HashMap.class);
                // TODO set new token
                String tokenResponse = (String) jsonResponse.get("token");
                authorized = (boolean) jsonResponse.get("authorized");
            }
        } catch (InternalServerErrorException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (URISyntaxException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (FogbowException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        if (!authorized) {
            throw new UnauthorizedRequestException();
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
        headers.put("Content-Type", "application/json");
        
        // body
        HashMap<String, String> body = new HashMap<String, String>();
        body.put("targetProvider", provider);
        body.put("operationType", operation.getOperationType().getValue());

        HttpResponse response = HttpRequestClient.doGenericRequest(HttpMethod.POST, endpoint, headers, body);
        return response;
    }
    
    protected RSAPublicKey getMSPublicKey() throws FogbowException {
        if (this.msPublicKey == null) {
            this.msPublicKey = RasPublicKeysHolder.getInstance().getMSPublicKey();
        }
        return this.msPublicKey;
    }
}
