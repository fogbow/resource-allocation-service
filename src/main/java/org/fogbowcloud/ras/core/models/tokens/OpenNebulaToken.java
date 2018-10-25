package org.fogbowcloud.ras.core.models.tokens;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;

@Entity
public class OpenNebulaToken extends FederationUserToken {

	@Column
	private String signature;
	
	public OpenNebulaToken(String provider, String tokenValue, String userName, String signature) {
		super(provider, tokenValue, userName, userName);
		this.signature = signature;
	}
}
