package org.fogbowcloud.ras.core.models.tokens;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;

@Entity
public class OpenNebulaToken extends Token {

	@Column
	private String userName;
	
	@Column
	private String password;
	
	@Column
	private String authentication;
	
	@ElementCollection
	private List<Integer> groupIds;
	
	public OpenNebulaToken(String userName, String password, String authentication, List<Integer> groupIds) {
		this.userName = userName;
		this.password = password;
		this.authentication = authentication;
		this.groupIds = groupIds;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getAuthentication() {
		return authentication;
	}

	public List<Integer> getGroupIds() {
		return groupIds;
	}
	
}
