package org.fogbowcloud.ras.core.models.tokens;

import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;

@Entity
public class OpenNebulaToken extends FederationUserToken {

	@ElementCollection
	private List<Integer> groupIds;
	
	public OpenNebulaToken(String provider, String tokenValue, String userName, List<Integer> groupIds) {
		super(provider, tokenValue, userName, userName);
		this.groupIds = groupIds;
	}

	public List<Integer> getGroupIds() {
		return groupIds;
	}
	
}
