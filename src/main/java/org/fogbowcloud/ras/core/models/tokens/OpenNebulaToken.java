package org.fogbowcloud.ras.core.models.tokens;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class OpenNebulaToken extends FederationUserToken {

	@Column
	private String signature;
	
	public OpenNebulaToken(String provider, String tokenValue, String userId, String userName, String signature) {
		super(provider, tokenValue, userId, userName);
		this.signature = signature;
	}

	public String getSignature() {
		return signature;
	}
}
