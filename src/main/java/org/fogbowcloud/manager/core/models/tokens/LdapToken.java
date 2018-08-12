package org.fogbowcloud.manager.core.models.tokens;

public class LdapToken extends FederationUserToken {
    private String expirationTime;

    public LdapToken(String tokenProvider, String federationUserTokenValue, String userId, String userName,
                     String expirationTime) {
        super(tokenProvider, federationUserTokenValue, userId, userName);
        this.expirationTime = expirationTime;
    }

    public String getExpirationTime() {
        return this.expirationTime;
    }
}
