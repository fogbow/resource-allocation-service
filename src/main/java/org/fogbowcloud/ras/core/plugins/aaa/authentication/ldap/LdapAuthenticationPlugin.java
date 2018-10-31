package org.fogbowcloud.ras.core.plugins.aaa.authentication.ldap;

import org.apache.commons.lang.StringUtils;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.LdapToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.RASTimestampedAuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap.LdapTokenGeneratorPlugin;

public class LdapAuthenticationPlugin extends RASTimestampedAuthenticationPlugin {

	@Override
	protected String getTokenMessage(FederationUserToken federationUserToken) {
		String rawTokenValue = federationUserToken.getTokenValue();
		String[] rawTokenValueSlices = rawTokenValue.split(LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR);
		
		String[] tokenValue = new String[] {
				rawTokenValueSlices[0],
				rawTokenValueSlices[1],
				rawTokenValueSlices[2],
				rawTokenValueSlices[3] 
		};
		
		return StringUtils.join(tokenValue, LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR);
	}

	@Override
	protected String getSignature(FederationUserToken federationUserToken) {
		LdapToken ldapToken = (LdapToken) federationUserToken;
		return ldapToken.getSignature();
	}

}
