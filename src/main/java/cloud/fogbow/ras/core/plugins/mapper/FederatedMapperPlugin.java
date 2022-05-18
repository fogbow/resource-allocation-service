package cloud.fogbow.ras.core.plugins.mapper;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.RasClassFactory;

public class FederatedMapperPlugin implements SystemToCloudMapperPlugin<CloudUser, SystemUser> {
    public static final String SERVICE_ID_METADATA_KEY = "serviceId";
    private static final String ADMIN_USERNAME_KEY = "admin_username";
    private static final String ADMIN_PASSWORD_KEY = "admin_password";
    private static final String INTERNAL_MAPPER_PLUGIN_CLASS_NAME = "internal_mapper";
    private static final String MAPPER_URL_KEY = "mapper_url";
    private static final String MAPPER_PORT_KEY = "mapper_port";
    private static final String MAPPER_PUBLIC_KEY_SUFFIX_KEY = "mapper_public_key_suffix";
    private static final String MAPPER_MAP_SUFFIX_KEY = "mapper_map_suffix";
    private static final String CLOUD_NAME_KEY = "cloud_name";
    
    private String internalMapperPluginClassName;
    private String idpUrl;
    private String adminUsername;
    private String adminPassword;
    private RasClassFactory classFactory;
    private AuthenticationServiceClient asClient;
    private MapperClient mapperClient;
    private String publicKeyString;
    
    public FederatedMapperPlugin(String internalMapperPluginClassName, String idpUrl, String publicKeyString, 
            String adminUsername, String adminPassword, RasClassFactory classFactory, 
            AuthenticationServiceClient asClient, MapperClient mapperClient) {
        this.internalMapperPluginClassName = internalMapperPluginClassName;
        this.idpUrl = idpUrl;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.classFactory = classFactory;
        this.asClient = asClient;
        this.mapperClient = mapperClient;
        this.publicKeyString = publicKeyString;
    }

    public FederatedMapperPlugin(String mapperConfFilePath) throws ConfigurationErrorException {
        try {
            this.publicKeyString = CryptoUtil.toBase64(ServiceAsymmetricKeysHolder.getInstance().getPublicKey());
        } catch (InternalServerErrorException e) {
            throw new ConfigurationErrorException(e.getMessage());
        } catch (GeneralSecurityException e) {
            throw new ConfigurationErrorException(e.getMessage());
        }
        
        Properties properties = PropertiesUtil.readProperties(mapperConfFilePath);
        this.idpUrl = properties.getProperty(ConfigurationPropertyKeys.CLOUD_IDENTITY_PROVIDER_URL_KEY);
        this.adminUsername = properties.getProperty(ADMIN_USERNAME_KEY);
        this.adminPassword = properties.getProperty(ADMIN_PASSWORD_KEY);
        this.internalMapperPluginClassName = properties.getProperty(INTERNAL_MAPPER_PLUGIN_CLASS_NAME);
        
        String mapperUrl = properties.getProperty(MAPPER_URL_KEY);
        String mapperPort = properties.getProperty(MAPPER_PORT_KEY);
        String mapperPublicKeySuffix = properties.getProperty(MAPPER_PUBLIC_KEY_SUFFIX_KEY);
        String mapperMapSuffix = properties.getProperty(MAPPER_MAP_SUFFIX_KEY);
        String cloudName = properties.getProperty(CLOUD_NAME_KEY);
        this.mapperClient = new MapperClient(mapperUrl, mapperPort, mapperPublicKeySuffix, 
                mapperMapSuffix, cloudName);
        
        String asAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_URL_KEY);
        String asPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_PORT_KEY);
        String asTokensEndpoint = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_TOKENS_ENDPOINT_KEY);
        this.asClient = new AuthenticationServiceClient(asAddress, asPort, asTokensEndpoint);
        
        this.classFactory = new RasClassFactory();
    }
    
    @Override
    public CloudUser map(SystemUser systemUser) throws FogbowException {
        HashMap<String, String> credentials = getCredentials(systemUser);
        SystemToCloudMapperPlugin<CloudUser, SystemUser> internalPlugin = getPlugin(credentials);
        return internalPlugin.map(systemUser);
    }

    private HashMap<String, String> getCredentials(SystemUser systemUser) throws FogbowException {
        String[] userIdFields = StringUtils.splitByWholeSeparator(systemUser.getId(), 
                FogbowConstants.FEDERATION_ID_SEPARATOR);
        String userId = userIdFields[0];
        String federation = userIdFields[1];
        String serviceId = systemUser.getMetadata().get(SERVICE_ID_METADATA_KEY);
        String token = this.asClient.getToken(this.publicKeyString, this.adminUsername, this.adminPassword);
        return this.mapperClient.getCredentials(token, federation, serviceId, userId);
    }

    private SystemToCloudMapperPlugin<CloudUser, SystemUser> getPlugin(HashMap<String, String> credentials) {
        return (SystemToCloudMapperPlugin<CloudUser, SystemUser>) 
                this.classFactory.createPluginInstance(internalMapperPluginClassName, this.idpUrl, credentials);
    }
}
