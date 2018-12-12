package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.opennebula;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.util.PropertiesUtil;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.util.RSAUtil;
import org.opennebula.client.Client;
import sun.security.provider.PolicySpiFile;

public class OpenNebulaTokenGeneratorPlugin implements TokenGeneratorPlugin {

    private final OpenNebulaClientFactory factory;
    private RSAPrivateKey privateKey;
    private Properties properties;
    
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String OPENNEBULA_TOKEN_VALUE_SEPARTOR = ":";
    public static final String OPENNEBULA_FIELD_SEPARATOR = "#&#";

    private String provider;

    public OpenNebulaTokenGeneratorPlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.provider = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        this.factory = new OpenNebulaClientFactory(confFilePath);
        
        try {

            this.privateKey = RSAUtil.getPrivateKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(String.format(Messages.Fatal.ERROR_READING_PRIVATE_KEY_FILE, e.getMessage()));
        }
    }

    @Override
    public String createTokenValue(Map<String, String> userCredentials) throws UnexpectedException, FogbowRasException {
        if ((userCredentials == null) || (userCredentials.get(USERNAME) == null) || (userCredentials.get(PASSWORD) == null)) {
            throw new InvalidParameterException(Messages.Exception.NO_USER_CREDENTIALS);
        }

        String userName = properties.getProperty(USERNAME);
        String password = properties.getProperty(PASSWORD);

        String openNebulaTokenValue = userName + OPENNEBULA_TOKEN_VALUE_SEPARTOR + password;

        // Trying to create a new client fo checking if login data from conf file is correct
        Client oneClient = factory.createClient(openNebulaTokenValue);

        String rawTokenValue = this.provider + OPENNEBULA_FIELD_SEPARATOR + openNebulaTokenValue + OPENNEBULA_FIELD_SEPARATOR
                + userName;

        try {
            String signature = RSAUtil.sign(this.privateKey, rawTokenValue);
            String federationTokenValue = rawTokenValue + OPENNEBULA_FIELD_SEPARATOR + signature;
            return federationTokenValue;

        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(String.format(Messages.Fatal.ERROR_READING_PRIVATE_KEY_FILE, e.getMessage()));
        }
    }
}
