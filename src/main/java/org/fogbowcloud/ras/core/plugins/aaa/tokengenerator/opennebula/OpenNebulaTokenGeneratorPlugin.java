package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.opennebula;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.util.PropertiesUtil;

import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;

public class OpenNebulaTokenGeneratorPlugin implements TokenGeneratorPlugin {

    private Properties properties;
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String OPENNEBULA_TOKEN_SEPARTOR = ":";

    public OpenNebulaTokenGeneratorPlugin() {
         this.properties = PropertiesUtil.readProperties(DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME);
    }

    @Override
    public String createTokenValue(Map<String, String> userCredentials) throws UnexpectedException, FogbowRasException {
        if ((userCredentials == null) || (userCredentials.get(USERNAME) == null) || (userCredentials.get(PASSWORD) == null)) {
            throw new InvalidParameterException(Messages.Exception.NO_USER_CREDENTIALS);
        }

        OpenNebulaClientFactory factory = new OpenNebulaClientFactory();

        String tokenValue = properties.getProperty(USERNAME) + OPENNEBULA_TOKEN_SEPARTOR + properties.getProperty(PASSWORD);

        // Trying to create a new client fo checking if login data from conf file is correct
        Client oneClient = factory.createClient(tokenValue);

        return tokenValue;
    }
}
