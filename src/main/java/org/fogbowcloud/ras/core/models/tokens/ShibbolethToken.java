package org.fogbowcloud.ras.core.models.tokens;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class ShibbolethToken extends TimestampedSignedFederationUserToken {

    @Column
    private Map<String, String> samlAttributes;
    
	public ShibbolethToken(String tokenProvider, String tokenValue, String userId,
			String name, Map<String, String> samlAttributes, long expirationTime, String signature) {
		super(tokenProvider, tokenValue, userId, name, signature, expirationTime);
		this.samlAttributes = samlAttributes;
	}
	
	public Map<String, String> getSamlAttributes() {
		return this.samlAttributes;
	}
	
}
