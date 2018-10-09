package org.fogbowcloud.ras.core.models.tokens;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.apache.commons.lang.StringUtils;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap.LdapTokenGeneratorPlugin;

@Entity
public class LdapToken extends FederationUserToken implements GenericSignatureToken {
	
    @Column
    private long expirationTime;

    public LdapToken(String tokenProvider, String federationUserTokenValue, String userId, String userName,
                     String expirationTime) {
        super(tokenProvider, federationUserTokenValue, userId, userName);
        this.expirationTime = Long.valueOf(expirationTime);
    }

    @Override
    public long getExpirationTime() {
        return this.expirationTime;
    }

	@Override
	public String getRawToken() {
		String rawTokenValue = this.getTokenValue();
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
	public String getRawTokenSignature() {
		String tokenValue = this.getTokenValue();
		String split[] = tokenValue.split(LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR);
		return split[4];
	}
	
}
