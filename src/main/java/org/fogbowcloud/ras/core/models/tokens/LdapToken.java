package org.fogbowcloud.ras.core.models.tokens;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class LdapToken extends FederationUserToken {
    @Column
    private String expirationTime;

    public LdapToken() {
    }

    public LdapToken(String tokenProvider, String federationUserTokenValue, String userId, String userName,
                     String expirationTime) {
        super(tokenProvider, federationUserTokenValue, userId, userName);
        this.expirationTime = expirationTime;
    }

    public String getExpirationTime() {
        return this.expirationTime;
    }
}
