package org.fogbowcloud.ras.core.plugins.aaa.authentication.ldap;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.models.tokens.LdapToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.generic.GenericSignatureAuthenticationHolder;

public class LdapAuthenticationPlugin implements AuthenticationPlugin<LdapToken> {

    private String localProviderId;
	private GenericSignatureAuthenticationHolder genericSignatureAuthenticationHolder;

    public LdapAuthenticationPlugin() throws FatalErrorException {
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        this.genericSignatureAuthenticationHolder = GenericSignatureAuthenticationHolder.getInstance();
    }

    @Override
    public boolean isAuthentic(String requestingMember, LdapToken ldapToken) {
        if (ldapToken.getTokenProvider().equals(this.localProviderId)) {
            try {
            	this.genericSignatureAuthenticationHolder.checkTokenValue(ldapToken);
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (requestingMember.equals(ldapToken.getTokenProvider().toLowerCase())) {
            // XMPP does not differentiate lower and upper cases, thus requestingMember is always all lower case
            return true;
        } else {
            return false;
        }
    }

}
