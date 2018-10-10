package org.fogbowcloud.ras.core.models.tokens;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class LdapToken extends TimestampedSignedFederationUserToken  {
	
    public LdapToken(String tokenProvider, String federationUserTokenValue, String userId, String userName,
                     String expirationTime, String signature) {
    	super(tokenProvider, federationUserTokenValue, userId, userName, signature, Long.valueOf(expirationTime));
    }

}
