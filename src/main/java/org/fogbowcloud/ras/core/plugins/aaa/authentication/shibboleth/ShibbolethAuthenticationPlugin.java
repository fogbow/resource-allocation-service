package org.fogbowcloud.ras.core.plugins.aaa.authentication.shibboleth;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.generic.GenericSignatureAuthenticationPlugin;

public class ShibbolethAuthenticationPlugin implements AuthenticationPlugin<ShibbolethToken> {

	private String localProviderId;
	private GenericSignatureAuthenticationPlugin genericSignatureAuthenticationPlugin;

	public ShibbolethAuthenticationPlugin() {
		this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);		
		this.genericSignatureAuthenticationPlugin = new GenericSignatureAuthenticationPlugin(); 
	}
	
	// TODO understand better this method
	@Override
	public boolean isAuthentic(String requestingMember, ShibbolethToken shibbolethToken)
			throws UnavailableProviderException {
        if (shibbolethToken.getTokenProvider().equals(this.localProviderId)) {
            try {
            	this.genericSignatureAuthenticationPlugin.checkTokenValue(shibbolethToken);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (requestingMember.equals(shibbolethToken.getTokenProvider().toLowerCase())) {
            // XMPP does not differentiate lower and upper cases, thus requestingMember is always all lower case
            return true;
        } else {
            return false;
        }
	}

}
