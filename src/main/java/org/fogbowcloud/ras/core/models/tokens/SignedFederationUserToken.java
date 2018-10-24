package org.fogbowcloud.ras.core.models.tokens;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class SignedFederationUserToken extends FederationUserToken {
	
    public static final int MAX_SIGNATURE_SIZE = 1024;

    @Column(length = MAX_SIGNATURE_SIZE)
	private String signature;

	public SignedFederationUserToken(String tokenProvider, String federationUserTokenValue, String userId,
			String userName, String signature) {
		super(tokenProvider, federationUserTokenValue, userId, userName);
		this.signature = signature;
	}

	public String getSignature() {
		return signature;
	}
	
}
