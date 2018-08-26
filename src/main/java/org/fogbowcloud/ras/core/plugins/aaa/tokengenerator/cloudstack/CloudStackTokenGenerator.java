package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;

import java.io.File;
import java.util.Map;
import java.util.Properties;

public class CloudStackTokenGenerator implements TokenGeneratorPlugin {
    private static final Logger LOGGER = Logger.getLogger(CloudStackTokenGenerator.class);

    public static final String API_KEY = "apiKey";
    public static final String CLOUDSTACK_URL = "cloudstack_api_url";
    public static final String TOKEN_VALUE_SEPARATOR = ":";
    protected static final String SECRET_KEY = "secretKey";
    private String endpoint;

    public CloudStackTokenGenerator() {
        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME);

        this.endpoint = properties.getProperty(CLOUDSTACK_URL);
    }

    @Override
    public String createTokenValue(Map<String, String> credentials) throws FogbowRasException {
        if ((credentials == null) || (credentials.get(API_KEY) == null) || (credentials.get(SECRET_KEY) == null)) {
            String errorMsg = "User credentials can't be null";
            throw new InvalidParameterException(errorMsg);
        }

        String apiKey = credentials.get(API_KEY);
        String secretKey = credentials.get(SECRET_KEY);
        String tokenValue = apiKey + TOKEN_VALUE_SEPARATOR + secretKey;

        return tokenValue;
    }
}
