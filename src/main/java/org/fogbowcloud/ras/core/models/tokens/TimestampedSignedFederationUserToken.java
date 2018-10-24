package org.fogbowcloud.ras.core.models.tokens;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class TimestampedSignedFederationUserToken extends SignedFederationUserToken {

	@Column
	private long timestamp;
	
	public TimestampedSignedFederationUserToken(String tokenProvider, String federationUserTokenValue, String userId,
			String userName, String signature, long timestamp) {
		super(tokenProvider, federationUserTokenValue, userId, userName, signature);
		this.timestamp = timestamp;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

}
