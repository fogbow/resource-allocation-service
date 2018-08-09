package org.fogbowcloud.manager.core.models.tokens.generators;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;

public class CloudStackTokenGenerator implements LocalTokenGenerator<Token> {

    private static final Logger LOGGER = Logger.getLogger(CloudStackTokenGenerator.class);
    
    private static final String CLOUDSTACK_URL = "cloudstack_identity_v49_url";
    protected static final String API_KEY = "apiKey";
    protected static final String SECRET_KEY = "secretKey";
    
    private String endpoint;
    private HttpRequestClientUtil client;
    
    public CloudStackTokenGenerator() {
        HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME);
        
        this.endpoint = properties.getProperty(CLOUDSTACK_URL);
        this.client = new HttpRequestClientUtil();
    }
    
    @Override
    public Token createToken(Map<String, String> credentials) throws FogbowManagerException {
        LOGGER.debug("Creating token with credentials: " + credentials);

        if ((credentials == null) || (credentials.get(API_KEY) == null)
                || (credentials.get(SECRET_KEY) == null)) {
            String errorMsg = "User credentials can't be null";
            throw new InvalidParameterException(errorMsg);
        }

        String apiKey = credentials.get(API_KEY);
        String secretKey = credentials.get(SECRET_KEY);
        String tokenValue = apiKey + ":" + secretKey;

        Token token = new Token(tokenValue);

        return token;
    }
    
}
