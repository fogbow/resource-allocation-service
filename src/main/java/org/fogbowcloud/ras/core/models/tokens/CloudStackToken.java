package org.fogbowcloud.ras.core.models.tokens;

import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class CloudStackToken extends FederationUserToken {
    public static final int MAX_SIGNATURE_SIZE = 1024;

    @Column(length = MAX_SIGNATURE_SIZE)
    private String signature;

    public CloudStackToken() {}

    public CloudStackToken(String tokenProvider, String tokenValue, String userID, String userName, String signature) {
        super(tokenProvider, tokenValue, userID, userName);
        this.signature = signature;
    }

    public String getSignature() {
        return this.signature;
    }
}
