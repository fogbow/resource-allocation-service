package org.fogbowcloud.ras.core.models.tokens;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class OpenStackV3Token extends FederationUserToken {
    public static final int MAX_SIGNATURE_SIZE = 1024;
    @Column
    private String projectId;
    @Column(length = MAX_SIGNATURE_SIZE)
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

    public String getSignature() {
		return signature;
	}

    // Used in testing
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
