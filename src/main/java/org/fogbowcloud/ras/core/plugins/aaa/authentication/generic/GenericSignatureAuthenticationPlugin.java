package org.fogbowcloud.ras.core.plugins.aaa.authentication.generic;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticTokenException;
import org.fogbowcloud.ras.core.models.tokens.GenericSignatureToken;
import org.fogbowcloud.ras.util.RSAUtil;

public class GenericSignatureAuthenticationPlugin {

	private RSAPublicKey publicKey;

	public GenericSignatureAuthenticationPlugin() {

        try {
            this.publicKey = getPublicKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(Messages.Fatal.ERROR_READING_PUBLIC_KEY_FILE);
        }
	}
	
    public void checkTokenValue(GenericSignatureToken genericSignatureToken) throws UnauthenticTokenException {
        Date currentDate = new Date(getNow());
        long expirationTime = genericSignatureToken.getExpirationTime();
		Date expirationDate = new Date(expirationTime);

        String tokenValue = genericSignatureToken.getRawToken();
        String signature = genericSignatureToken.getRawTokenSignature();

        if (expirationDate.before(currentDate)) {
            throw new UnauthenticTokenException(String.format(Messages.Exception.EXPIRED_TOKEN, expirationDate));
        }

        if (!verifySign(tokenValue, signature)) {
            throw new UnauthenticTokenException(String.format(Messages.Exception.INVALID_TOKEN));
        }
    }

    protected boolean verifySign(String tokenMessage, String signature) {
        try {
            return RSAUtil.verify(this.publicKey, tokenMessage, signature);
        } catch (Exception e) {
            throw new RuntimeException(Messages.Exception.INVALID_TOKEN_SIGNATURE, e);
        }
    }

    protected RSAPublicKey getPublicKey() throws IOException, GeneralSecurityException {
        return RSAUtil.getPublicKey();
    }

    protected long getNow() {
    	return System.currentTimeMillis();
    }
    
}
