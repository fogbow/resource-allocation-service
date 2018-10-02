package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethTokenHolder;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.TokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth.util.SecretManager;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.RSAUtil;

// TODO add logs
public class ShibbolethTokenGenerator implements TokenGeneratorPlugin {

	private static final int SHIB_TOKEN_PARAMETERS_SIZE = 6;

	private static final Logger LOGGER = Logger.getLogger(ShibbolethTokenGenerator.class);

	// shib token parameter
	private static final int SAML_ATTRIBUTES_ATTR_SHIB_INDEX = 5;
	private static final int COMMON_NAME_ATTR_SHIB_INDEX = 4;
	private static final int EDU_PRINCIPAL_NAME_ATTR_SHIB_INDEX = 3;
	private static final int IDENTITY_PROVIDE_ATTR_SHIB_INDEX = 2;
	private static final int ASSERTION_URL_ATTR_SHIB_INDEX = 1;
	protected static final int SECREC_ATTR_SHIB_INDEX = 0;
	// properties
	private static final String SHIB_PUBLIC_FILE_PATH_PROPERTIE = "shib_public_key_file_path";
	// credentails
	protected static final String TOKEN_SIGNATURE_CREDENTIAL = "signature";
	protected static final String TOKEN_CREDENTIAL = "token";
	
    public static final long EXPIRATION_INTERVAL = TimeUnit.DAYS.toMillis(1); // One day
	public static final String SHIBBOLETH_SEPARETOR = "!#!";
	
	private RSAPrivateKey rasPrivateKey;
	private RSAPublicKey shibPublicKey;
	private Properties properties;
	private SecretManager secretManager;
	
	public ShibbolethTokenGenerator() {
        this.properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.SHIBBOLETH_CONF_FILE_NAME);
        
        try {
            this.rasPrivateKey = RSAUtil.getPrivateKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(
            		String.format(Messages.Fatal.ERROR_READING_PRIVATE_KEY_FILE, e.getMessage()));
        }   
        
        try {
            this.shibPublicKey = getPublicKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(
            		String.format(Messages.Fatal.ERROR_READING_PUBLIC_KEY_FILE, e.getMessage()));
        }           
        
        this.secretManager = new SecretManager();
        
	}
	
	protected ShibbolethTokenGenerator(RSAPrivateKey rasPrivateKey, RSAPublicKey shibPublicKey) {
		this.rasPrivateKey = rasPrivateKey;
		this.shibPublicKey = shibPublicKey;
		
		this.secretManager = new SecretManager();
	}
	
	@Override
	public String createTokenValue(Map<String, String> userCredentials) throws UnexpectedException, FogbowRasException {
		String tokenShibAppEncrypted = userCredentials.get(TOKEN_CREDENTIAL);
		String tokenShibAppSignature = userCredentials.get(TOKEN_SIGNATURE_CREDENTIAL);
		
		String tokenShibApp = decryptTokenShib(tokenShibAppEncrypted);
		
		verifyShibTokenAuthenticity(tokenShibAppSignature, tokenShibApp);
		
		String[] tokenShibAppParameters = tokenShibApp.split(SHIBBOLETH_SEPARETOR);		
		checkTokenFormat(tokenShibAppParameters);
		
		verifySecretShibToken(tokenShibAppParameters);
		
		String rawToken = createRawToken(tokenShibAppParameters);
		String rawTokenSignature = createSignature(rawToken);
		String tokenValue = ShibbolethTokenHolder.generateTokenValue(rawToken, rawTokenSignature);
		
		return tokenValue;
	}

	protected void verifySecretShibToken(String[] tokenShibParameters) throws UnauthenticatedUserException {
		String secret = tokenShibParameters[SECREC_ATTR_SHIB_INDEX];
		boolean isValid = this.secretManager.verify(secret);
		if (!isValid) {
        	String errorMsg = String.format(Messages.Exception.AUTHENTICATION_ERROR);
            throw new UnauthenticatedUserException(errorMsg);			
		}
	}

	protected void checkTokenFormat(String[] tokenShibParameters) throws UnauthenticatedUserException {
		if (tokenShibParameters.length != SHIB_TOKEN_PARAMETERS_SIZE) {
        	String errorMsg = String.format(Messages.Exception.AUTHENTICATION_ERROR);
            throw new UnauthenticatedUserException(errorMsg);
		}
	}

	protected String createRawToken(String[] tokenShibParameters) {
		String assertionUrl = tokenShibParameters[ASSERTION_URL_ATTR_SHIB_INDEX];
		String identityProvider = tokenShibParameters[IDENTITY_PROVIDE_ATTR_SHIB_INDEX];
		String eduPrincipalName = tokenShibParameters[EDU_PRINCIPAL_NAME_ATTR_SHIB_INDEX];
		String commonName = tokenShibParameters[COMMON_NAME_ATTR_SHIB_INDEX];
		// attributes in json format
		String samlAttributes = tokenShibParameters[SAML_ATTRIBUTES_ATTR_SHIB_INDEX];
		
        String expirationTime = generateExpirationTime();
		
        return ShibbolethTokenHolder.createRawToken(assertionUrl, identityProvider, eduPrincipalName,
			commonName, samlAttributes, expirationTime);
	}

	protected String generateExpirationTime() {
		Date expirationDate = new Date(new Date().getTime() + EXPIRATION_INTERVAL);
        String expirationTime = Long.toString(expirationDate.getTime());
		return expirationTime;
	}

	private void verifyShibTokenAuthenticity(String tokenSignature, String tokenShibApp) throws UnauthenticatedUserException {
		try {
			RSAUtil.verify(this.shibPublicKey, tokenShibApp, tokenSignature);
		} catch (Exception e) {
        	String errorMsg = String.format(Messages.Exception.AUTHENTICATION_ERROR);
            throw new UnauthenticatedUserException(errorMsg, e);
		}
	}

	protected String decryptTokenShib(String tokenShibAppEncrypted) throws UnauthenticatedUserException {
		String tokenShibApp = null;
		try {
			tokenShibApp = RSAUtil.decrypt(tokenShibAppEncrypted, this.rasPrivateKey);
		} catch (Exception e) {
        	String errorMsg = String.format(Messages.Exception.AUTHENTICATION_ERROR);
            throw new UnauthenticatedUserException(errorMsg, e);
		}
		return tokenShibApp;
	}
	
    protected RSAPublicKey getPublicKey() throws IOException, GeneralSecurityException {
        String filename = this.properties.getProperty(SHIB_PUBLIC_FILE_PATH_PROPERTIE);
        LOGGER.debug("Public key path: " + filename);
        String publicKeyPEM = RSAUtil.getKey(filename);
        LOGGER.debug("Public key: " + publicKeyPEM);
        return RSAUtil.getPublicKeyFromString(publicKeyPEM);
    }
    
    protected String createSignature(String message) throws UnauthenticatedUserException {
    	try {
    		return RSAUtil.sign(this.rasPrivateKey, message);
		} catch (Exception e) {
	    	String errorMsg = String.format(Messages.Exception.AUTHENTICATION_ERROR);
	        throw new UnauthenticatedUserException(errorMsg, e);
		}
    }      

}
