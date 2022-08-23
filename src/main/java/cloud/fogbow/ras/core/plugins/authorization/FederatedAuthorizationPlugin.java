package cloud.fogbow.ras.core.plugins.authorization;

import java.security.GeneralSecurityException;

import org.apache.commons.lang.StringUtils;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.plugins.mapper.AuthenticationServiceClient;

public class FederatedAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {
    public static final String SERVICE_ID_METADATA_KEY = "serviceId";
    private static final String ADMIN_USERNAME_KEY = "admin_username";
    private static final String ADMIN_PASSWORD_KEY = "admin_password";
    private static final String AUTHORIZATION_URL_KEY = "authorization_url";
    private static final String AUTHORIZATION_PORT_KEY = "authorization_port";
    private static final String AUTHORIZATION_PUBLIC_KEY_SUFFIX_KEY = "authorization_public_key_suffix";
    private static final String AUTHORIZATION_AUTHORIZE_SUFFIX_KEY = "authorization_authorize_suffix";
    
    private AuthenticationServiceClient asClient;
    private String publicKeyString;
    private String adminUsername;
    private String adminPassword;
    private FederatedAuthorizationClient authorizationClient;
    
    public FederatedAuthorizationPlugin(AuthenticationServiceClient asClient, 
            FederatedAuthorizationClient authorizationClient, String publicKeyString,
            String adminUsername, String adminPassword) {
        this.asClient = asClient;
        this.authorizationClient = authorizationClient;
        this.publicKeyString = publicKeyString;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }
    
    public FederatedAuthorizationPlugin() throws ConfigurationErrorException {
        try {
            this.publicKeyString = CryptoUtil.toBase64(ServiceAsymmetricKeysHolder.getInstance().getPublicKey());
        } catch (InternalServerErrorException e) {
            throw new ConfigurationErrorException(e.getMessage());
        } catch (GeneralSecurityException e) {
            throw new ConfigurationErrorException(e.getMessage());
        }
        
        this.adminUsername = PropertiesHolder.getInstance().getProperty(ADMIN_USERNAME_KEY);
        this.adminPassword = PropertiesHolder.getInstance().getProperty(ADMIN_PASSWORD_KEY);
        
        String authorizationUrl = PropertiesHolder.getInstance().getProperty(AUTHORIZATION_URL_KEY);
        String authorizationPort = PropertiesHolder.getInstance().getProperty(AUTHORIZATION_PORT_KEY);
        String authorizationPublicKeySuffix = PropertiesHolder.getInstance().getProperty(AUTHORIZATION_PUBLIC_KEY_SUFFIX_KEY);
        String authorizationAuthorizeSuffix = PropertiesHolder.getInstance().getProperty(AUTHORIZATION_AUTHORIZE_SUFFIX_KEY);
        this.authorizationClient = new FederatedAuthorizationClient(authorizationUrl, authorizationPort, authorizationPublicKeySuffix, 
                authorizationAuthorizeSuffix);
        
        String asAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_URL_KEY);
        String asPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_PORT_KEY);
        String asTokensEndpoint = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_TOKENS_ENDPOINT_KEY);
        this.asClient = new AuthenticationServiceClient(asAddress, asPort, asTokensEndpoint);
    }
    
    @Override
    public boolean isAuthorized(SystemUser systemUser, RasOperation operation) throws UnauthorizedRequestException {
        String[] userIdFields = StringUtils.splitByWholeSeparator(systemUser.getId(), 
                FogbowConstants.FEDERATION_ID_SEPARATOR);
        String userId = userIdFields[0];
        String federation = userIdFields[1];
        String serviceId = systemUser.getMetadata().get(SERVICE_ID_METADATA_KEY);
        String token;
        
        try {
            token = this.asClient.getToken(this.publicKeyString, this.adminUsername, this.adminPassword);
        } catch (FogbowException e) {
            throw new UnauthorizedRequestException(e.getMessage());
        }
        
        return this.authorizationClient.isAuthorized(token, federation, serviceId, userId, operation);
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
