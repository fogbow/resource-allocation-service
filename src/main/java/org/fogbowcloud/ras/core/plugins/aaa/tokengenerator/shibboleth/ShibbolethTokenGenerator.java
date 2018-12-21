package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethTokenHolder;
import org.fogbowcloud.ras.core.plugins.aaa.RASAuthenticationHolder;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth.util.SecretManager;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.RSAUtil;

public class ShibbolethTokenGenerator implements TokenGeneratorPlugin {

	private static final Logger LOGGER = Logger.getLogger(ShibbolethTokenGenerator.class);

	// shib token parameters
	private static final int SHIB_TOKEN_PARAMETERS_SIZE = 5;
	private static final int SAML_ATTRIBUTES_ATTR_SHIB_INDEX = 4;
	private static final int COMMON_NAME_ATTR_SHIB_INDEX = 3;
	private static final int EDU_PRINCIPAL_NAME_ATTR_SHIB_INDEX = 2;
	private static final int ASSERTION_URL_ATTR_SHIB_INDEX = 1;
	protected static final int SECREC_ATTR_SHIB_INDEX = 0;
	// properties
	private static final String SHIB_PUBLIC_FILE_PATH_KEY = "shib_public_key_file_path";
	// credentails
	protected static final String TOKEN_CREDENTIAL = "token";
	protected static final String KEY_SIGNATURE_CREDENTIAL = "keySignature";
	protected static final String KEY_CREDENTIAL = "key";
	
	public static final String SHIBBOLETH_SEPARATOR = "!#!";
	
	private RASAuthenticationHolder rasAuthenticationHolder;
	private SecretManager secretManager;
	
	private Properties properties;
	private String tokenProviderId;
	private RSAPrivateKey rasPrivateKey;
	private RSAPublicKey shibAppPublicKey;
	
	public ShibbolethTokenGenerator(String confFilePath) {
		this.tokenProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
		
        this.properties = PropertiesUtil.readProperties(confFilePath);
        
        this.rasAuthenticationHolder = RASAuthenticationHolder.getInstance();
        
        try {
			this.rasPrivateKey = RASAuthenticationHolder.getInstance().getPrivateKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(
            		String.format(Messages.Fatal.ERROR_READING_PRIVATE_KEY_FILE, e.getMessage()));
        }   
        
        try {
            this.shibAppPublicKey = getShibbolethApplicationPublicKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(
            		String.format(Messages.Fatal.ERROR_READING_PUBLIC_KEY_FILE, e.getMessage()));
        }           
        
        this.secretManager = new SecretManager();
        
	}
	
	@Override
	public String createTokenValue(Map<String, String> userCredentials) throws UnexpectedException, FogbowRasException {
		LOGGER.debug(String.format(
					"Trying to create Shibboleth token with credentials: %s",
					userCredentials.toString()));
		
		String tokenShibAppEncrypted = userCredentials.get(TOKEN_CREDENTIAL);
		String keyShibAppEncrypted = userCredentials.get(KEY_CREDENTIAL);
		String keySignatureShibApp = userCredentials.get(KEY_SIGNATURE_CREDENTIAL);
		
		String keyShibApp = decryptKeyShib(keyShibAppEncrypted);
		String tokenShib = decryptTokenShib(keyShibApp, tokenShibAppEncrypted);
		verifyShibAppKeyAuthenticity(keySignatureShibApp, keyShibApp);
		
		String[] tokenShibAppParameters = tokenShib.split(SHIBBOLETH_SEPARATOR);
		checkTokenFormat(tokenShibAppParameters);
		
		verifySecretShibAppToken(tokenShibAppParameters);
		
		String rawToken = createRawToken(tokenShibAppParameters);
		String rawTokenSignature = this.rasAuthenticationHolder.createSignature(rawToken);
		String tokenValue = ShibbolethTokenHolder.generateTokenValue(rawToken, rawTokenSignature);
		
		return tokenValue;
	}

	protected void verifySecretShibAppToken(String[] tokenShibParameters) throws UnauthenticatedUserException {
		String secret = tokenShibParameters[SECREC_ATTR_SHIB_INDEX];
		boolean isValid = this.secretManager.verify(secret);
		if (!isValid) {
        	String errorMsg = String.format(Messages.Exception.AUTHENTICATION_ERROR);
        	LOGGER.error(errorMsg);
            throw new UnauthenticatedUserException(errorMsg);			
		}
	}

	protected void checkTokenFormat(String[] tokenShibParameters) throws UnauthenticatedUserException {
		if (tokenShibParameters.length != SHIB_TOKEN_PARAMETERS_SIZE) {
        	String errorMsg = String.format(Messages.Exception.AUTHENTICATION_ERROR);
        	LOGGER.error(errorMsg);
            throw new UnauthenticatedUserException(errorMsg);
		}
	}

	protected String createRawToken(String[] tokenShibParameters) {
		LOGGER.debug("Creating raw token");
		String assertionUrl = tokenShibParameters[ASSERTION_URL_ATTR_SHIB_INDEX];
		String identityProvider = this.tokenProviderId;
		String eduPrincipalName = tokenShibParameters[EDU_PRINCIPAL_NAME_ATTR_SHIB_INDEX];
		String commonName = tokenShibParameters[COMMON_NAME_ATTR_SHIB_INDEX];
		// attributes in json format, like this "{\"key\": \"value\"}"
		String samlAttributes = tokenShibParameters[SAML_ATTRIBUTES_ATTR_SHIB_INDEX];
		
        String expirationTime = generateExpirationTime();
		
        return ShibbolethTokenHolder.createRawToken(assertionUrl, identityProvider, eduPrincipalName,
			commonName, samlAttributes, expirationTime);
	}
	
	protected String generateExpirationTime() {
		return this.rasAuthenticationHolder.generateExpirationTime();
	}

	protected void verifyShibAppKeyAuthenticity(String signature, String message) throws UnauthenticatedUserException {
		try {
			RSAUtil.verify(this.shibAppPublicKey, message, signature);
		} catch (Exception e) {
        	String errorMsg = String.format(Messages.Exception.AUTHENTICATION_ERROR);
        	LOGGER.error(errorMsg, e);
            throw new UnauthenticatedUserException(errorMsg, e);
		}
	}

	protected String decryptTokenShib(String keyShib, String rasToken) throws UnauthenticatedUserException {
		String tokenShibApp = null;
		try {
			tokenShibApp = RSAUtil.decryptAES(keyShib.getBytes(RSAUtil.UTF_8), rasToken);
		} catch (Exception e) {
        	String errorMsg = String.format(Messages.Exception.AUTHENTICATION_ERROR);
        	LOGGER.error(errorMsg, e);
            throw new UnauthenticatedUserException(errorMsg, e);
		}
		return tokenShibApp;
	}
	
	protected String decryptKeyShib(String keyShibAppEncrypted) throws UnauthenticatedUserException {
		String keyShibApp = null;
		try {
			keyShibApp = RSAUtil.decrypt(keyShibAppEncrypted, this.rasPrivateKey);
		} catch (Exception e) {
        	String errorMsg = String.format(Messages.Exception.AUTHENTICATION_ERROR);
        	LOGGER.error(errorMsg, e);
            throw new UnauthenticatedUserException(errorMsg, e);
		}
		return keyShibApp;
	}
	
    protected RSAPublicKey getShibbolethApplicationPublicKey() throws IOException, GeneralSecurityException {
        String filename = this.properties.getProperty(SHIB_PUBLIC_FILE_PATH_KEY);
        LOGGER.debug("Shibboleth application public key path: " + filename);
        String publicKeyPEM = RSAUtil.getKey(filename);
        LOGGER.debug("Shibboleth application Public key: " + publicKeyPEM);
        return RSAUtil.getPublicKeyFromString(publicKeyPEM);
    }

}
