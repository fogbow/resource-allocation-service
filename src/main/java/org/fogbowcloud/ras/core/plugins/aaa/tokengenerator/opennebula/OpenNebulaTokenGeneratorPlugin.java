package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.opennebula;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.*;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.RSAUtil;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.user.UserPool;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Map;
import java.util.Properties;

// TODO implement tests
public class OpenNebulaTokenGeneratorPlugin implements TokenGeneratorPlugin {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaTokenGeneratorPlugin.class);

    public static final int FEDERATION_TOKEN_PARAMETER_SIZE = 5;
    public static final int PROVIDER_ID_TOKEN_VALUE_PARAMETER = 0;
    public static final int ONE_TOKEN_VALUE_PARAMETER = 1;
    public static final int USERNAME_TOKEN_VALUE_PARAMETER = 2;
    public static final int USERID_TOKEN_VALUE_PARAMETER = 3;
    public static final int SIGNATURE_TOKEN_VALUE_PARAMETER = 4;

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String OPENNEBULA_TOKEN_VALUE_SEPARETOR = ":";
    public static final String OPENNEBULA_FIELD_SEPARATOR = "#&#";

    private final OpenNebulaClientFactory factory;
    private RSAPrivateKey privateKey;
    private Properties properties;
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

    /*
     * The userId is the equals userName, because the userName is unique in the Opennebula
     */
    @Override
    public String createTokenValue(Map<String, String> userCredentials) throws UnexpectedException, FogbowRasException {
        if ((userCredentials == null) || (userCredentials.get(USERNAME) == null) || (userCredentials.get(PASSWORD) == null)) {
            throw new InvalidParameterException(Messages.Exception.NO_USER_CREDENTIALS);
        }

        String userName = this.properties.getProperty(USERNAME);
        String password = this.properties.getProperty(PASSWORD);
        String openNebulaTokenValue = userName + OPENNEBULA_TOKEN_VALUE_SEPARETOR + password;

        isAuthenticated(openNebulaTokenValue);

        String[] federationTokenParameters = new String[FEDERATION_TOKEN_PARAMETER_SIZE];
        federationTokenParameters[PROVIDER_ID_TOKEN_VALUE_PARAMETER] = String.valueOf(this.provider);
        federationTokenParameters[ONE_TOKEN_VALUE_PARAMETER] = openNebulaTokenValue;
        federationTokenParameters[USERNAME_TOKEN_VALUE_PARAMETER] = userName;
        String userId = userName;
        federationTokenParameters[USERID_TOKEN_VALUE_PARAMETER] = userId;
        String rawTokenValue = StringUtils.join(federationTokenParameters, OPENNEBULA_FIELD_SEPARATOR);

        final String signature;
        try {
            signature = RSAUtil.sign(this.privateKey, rawTokenValue);
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(String.format(Messages.Fatal.ERROR_READING_PRIVATE_KEY_FILE, e.getMessage()));
        }

        return rawTokenValue + OPENNEBULA_FIELD_SEPARATOR + signature;
    }

    /*
     * Using the Opennebula Java Library is necessary to do the operation na cloud to check if is authenticated.
     * In this case, we opted to use UserPool
     * TODO check to request directly in the XML-RPC API
     */
    protected void isAuthenticated(String openNebulaTokenValue) throws UnauthenticatedUserException {
        final Client client;
        try {
            client = this.factory.createClient(openNebulaTokenValue);
        } catch (UnexpectedException e) {
            LOGGER.error("There are a problem in the client creation", e);
            throw new UnauthenticatedUserException();
        }
        UserPool userPool = new UserPool(client);
        OneResponse info = userPool.info();
        if (info.isError()) {
            LOGGER.error(String.format("There are a problem in the client creation: %s", info.getMessage()));
            throw new UnauthenticatedUserException();
        }
    }
}
