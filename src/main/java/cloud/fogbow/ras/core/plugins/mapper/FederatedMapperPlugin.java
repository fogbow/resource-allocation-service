package cloud.fogbow.ras.core.plugins.mapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import cloud.fogbow.as.core.util.TokenProtector;
import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.PublicKeysHolder;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.HttpRequestClient;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.RasClassFactory;

// TODO test
public class FederatedMapperPlugin implements SystemToCloudMapperPlugin<CloudUser, SystemUser> {
    private String cloudName;
    private String mapperUrl;
    private String mapperPort;
    private String mapperMapSuffix;
    private String mapperPublicKeySuffix;
    private String internalMapperPluginClassName;
    private String idpUrl;
    private String adminUsername;
    private String adminPassword;
    private RasClassFactory classFactory;
    private AuthenticationServiceClient asClient;
    
    public FederatedMapperPlugin(String mapperConfFilePath) throws ConfigurationErrorException {
        this.classFactory = new RasClassFactory();
        
        // FIXME constants
        Properties properties = PropertiesUtil.readProperties(mapperConfFilePath);
        this.idpUrl = properties.getProperty(ConfigurationPropertyKeys.CLOUD_IDENTITY_PROVIDER_URL_KEY);
        this.adminUsername = properties.getProperty("admin_username");
        this.adminPassword = properties.getProperty("admin_password");
        this.internalMapperPluginClassName = properties.getProperty("internal_mapper");
        this.mapperUrl = properties.getProperty("mapper_url");
        this.mapperPort = properties.getProperty("mapper_port");
        this.mapperPublicKeySuffix = properties.getProperty("mapper_public_key_suffix");
        this.mapperMapSuffix = properties.getProperty("mapper_map_suffix");
        this.cloudName = properties.getProperty("cloud_name");
        
        String asAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_URL_KEY);
        String asPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_PORT_KEY);
        this.asClient = new AuthenticationServiceClient(asAddress, asPort);
    }
    
    @Override
    public CloudUser map(SystemUser systemUser) throws FogbowException {
        String federation = getFederationFromSystemUser(systemUser);
        HashMap<String, String> credentials = getCredentials(federation);
        SystemToCloudMapperPlugin<CloudUser, SystemUser> internalPlugin = getPlugin(credentials);
        return internalPlugin.map(systemUser);
    }

    private String getFederationFromSystemUser(SystemUser systemUser) {
        return StringUtils.splitByWholeSeparator(systemUser.getId(), 
                FogbowConstants.FEDERATION_ID_SEPARATOR)[1];
    }

    private HashMap<String, String> getCredentials(String federation) throws FogbowException {
        try {
            String token = this.asClient.getToken(CryptoUtil.toBase64(ServiceAsymmetricKeysHolder.getInstance().getPublicKey()), 
                    this.adminUsername, this.adminPassword);
            RSAPublicKey mapperPublicKey = PublicKeysHolder.getPublicKey(this.mapperUrl, this.mapperPort, this.mapperPublicKeySuffix);
            
            String rewrapToken = TokenProtector.rewrap(ServiceAsymmetricKeysHolder.getInstance().getPrivateKey(), 
                    mapperPublicKey, token, FogbowConstants.TOKEN_STRING_SEPARATOR);
            
            Map<String, String> headers = new HashMap<String, String>();
            headers.put(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, rewrapToken);
            Map<String, String> body = new HashMap<String, String>();
            String endpoint = getMapperEndpoint(federation);
            
            HttpResponse response = HttpRequestClient.doGenericRequest(HttpMethod.GET, endpoint, headers, body);
            // TODO check errors
            String responseContent = response.getContent();
            Gson gson = new Gson();
            
            LinkedTreeMap<String, LinkedTreeMap<String, String>> baseCredentialsMap = 
                    (LinkedTreeMap<String, LinkedTreeMap<String, String>>) gson.fromJson(responseContent, LinkedTreeMap.class);
            // FIXME constant
            return new HashMap<String, String>(baseCredentialsMap.get("credentials"));
        } catch (GeneralSecurityException e) {
            // TODO Handle this exception
            e.printStackTrace();
            return null;
        } catch (URISyntaxException e) {
            // TODO Handle this exception
            e.printStackTrace();
            return null;
        }
    }
    
    private String getMapperEndpoint(String federation) throws URISyntaxException {
        URI uri = new URI(this.mapperUrl);
        uri = UriComponentsBuilder.fromUri(uri).port(this.mapperPort).path("/").path(this.mapperMapSuffix).path("/").
                path(federation).path("/").path(this.cloudName).build(true).toUri();
        return uri.toString();
    }
    
    public SystemToCloudMapperPlugin<CloudUser, SystemUser> getPlugin(HashMap<String, String> credentials) {
        return (SystemToCloudMapperPlugin<CloudUser, SystemUser>) 
                this.classFactory.createPluginInstance(internalMapperPluginClassName, this.idpUrl, credentials);
    }
}
