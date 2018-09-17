package org.fogbowcloud.ras.core.plugins.aaa.authentication.openstack;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3.OpenStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.util.RSAUtil;

public class OpenStackAuthenticationPlugin implements AuthenticationPlugin<OpenStackV3Token> {

    private String localProviderId;
    private RSAPublicKey publicKey;

    public OpenStackAuthenticationPlugin() {
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        try {
            this.publicKey = getPublicKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new FatalErrorException(Messages.Fatal.ERROR_READING_PUBLIC_KEY_FILE);
        }
    }

    @Override
    public boolean isAuthentic(OpenStackV3Token openStackV3Token) throws UnavailableProviderException {
        if (openStackV3Token.getTokenProvider().equals(this.localProviderId)) {
            String tokenMessage = getTokenMessage(openStackV3Token);
			String signature = getSignature(openStackV3Token);
			return verifySign(tokenMessage, signature);
        } else {
        	// TODO check: why is true ?
            return true;
        }
    }
    
    // TODO refactor
    protected String getTokenMessage(OpenStackV3Token openStackV3Token) {
    	String[] parameters = new String[] {
    			openStackV3Token.getTokenProvider(), 
    			openStackV3Token.getTokenValue(),
    			openStackV3Token.getUserId(),
    			openStackV3Token.getUserName(),
    			openStackV3Token.getProjectId()} ;
		return StringUtils.join(parameters, OpenStackTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR);
	}

    // TODO refactor    
	protected String getSignature(OpenStackV3Token openStackV3Token) {
		return openStackV3Token.getSignature();
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
    
}
