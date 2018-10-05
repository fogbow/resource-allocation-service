package org.fogbowcloud.ras.core.plugins.aaa.authentication.generic;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticTokenException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.models.tokens.GenericSignatureToken;
import org.fogbowcloud.ras.util.RSAUtil;

public class GenericSignatureAuthenticationHolder {

	private static final Logger LOGGER = Logger.getLogger(GenericSignatureAuthenticationHolder.class);
	
    public static final long EXPIRATION_INTERVAL = TimeUnit.DAYS.toMillis(1); // One day
	
	private RSAPublicKey rasPublicKey;
	private RSAPrivateKey rasPrivateKey;
	private static GenericSignatureAuthenticationHolder instance;

	public GenericSignatureAuthenticationHolder() {
        try {
            this.rasPublicKey = getPublicKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(Messages.Fatal.ERROR_READING_PUBLIC_KEY_FILE);
        }
        
        try {
            this.rasPrivateKey = getPrivateKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(Messages.Fatal.ERROR_READING_PRIVATE_KEY_FILE);
        }        
	}
	
    public static synchronized GenericSignatureAuthenticationHolder getInstance() throws FatalErrorException {
        if (instance == null) {
            instance = new GenericSignatureAuthenticationHolder();
        }
        return instance;
    }
	
	public void checkTokenValue(GenericSignatureToken genericSignatureToken) throws UnauthenticTokenException {
        Date currentDate = new Date(getNow());
        long expirationTime = genericSignatureToken.getExpirationTime();
		Date expirationDate = new Date(expirationTime);

        String tokenValue = genericSignatureToken.getRawToken();
        String signature = genericSignatureToken.getRawTokenSignature();

        if (expirationDate.before(currentDate)) {
        	LOGGER.error(String.format(Messages.Exception.EXPIRED_TOKEN, expirationDate.toString()));
            throw new UnauthenticTokenException(String.format(Messages.Exception.EXPIRED_TOKEN, expirationDate));
        }

        if (!verifySignature(tokenValue, signature)) {
        	LOGGER.error(Messages.Exception.INVALID_TOKEN);
            throw new UnauthenticTokenException(String.format(Messages.Exception.INVALID_TOKEN));
        }
    }

    public String createSignature(String message) throws UnauthenticatedUserException {
    	try {
    		return RSAUtil.sign(this.rasPrivateKey, message);
		} catch (Exception e) {
	    	String errorMsg = String.format(Messages.Exception.AUTHENTICATION_ERROR);
	    	LOGGER.error(errorMsg, e);
	        throw new UnauthenticatedUserException(errorMsg, e);
		}
    }
    
	public String generateExpirationTime() {
		Date expirationDate = new Date(getNow() + EXPIRATION_INTERVAL);
        String expirationTime = Long.toString(expirationDate.getTime());
		return expirationTime;
	}
    
    protected boolean verifySignature(String tokenMessage, String signature) {
        try {
            return RSAUtil.verify(this.rasPublicKey, tokenMessage, signature);
        } catch (Exception e) {
        	LOGGER.error(Messages.Exception.EXPIRED_TOKEN, e);
            throw new RuntimeException(Messages.Exception.INVALID_TOKEN_SIGNATURE, e);
        }
    }

    protected RSAPublicKey getPublicKey() throws IOException, GeneralSecurityException {
        return RSAUtil.getPublicKey();
    }
    
    protected RSAPrivateKey getPrivateKey() throws IOException, GeneralSecurityException {
        return RSAUtil.getPrivateKey();
    }    

    protected long getNow() {
    	return System.currentTimeMillis();
    }
    
}
