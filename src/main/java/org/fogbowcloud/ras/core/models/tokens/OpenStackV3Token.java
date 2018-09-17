package org.fogbowcloud.ras.core.models.tokens;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class OpenStackV3Token extends FederationUserToken {
    private String projectId;
    @Column
    private String signature;

    public OpenStackV3Token() {
    }

    public OpenStackV3Token(String tokenProvider, String tokenValue, String userId, String name, 
    						String projectId, String signature) {
        super(tokenProvider, tokenValue, userId, name);
        this.projectId = projectId;
        this.signature = signature;
    }

    public String getProjectId() {
        return this.projectId;
    }

    // Used in testing
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public String getSignature() {
		return signature;
	}
}
