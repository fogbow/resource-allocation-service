package org.fogbowcloud.manager.core.models.tokens;

public class FederationUserToken extends Token {

    // The userId must uniquely identify a user in the federation; two tokens issued to the same user must have the same userId.
    private String userId;
    // This field is a human-friendly identification of the user, typically used by the CLI/GUI, but need not be unique.
    private String userName;

    public FederationUserToken(String federationUserTokenValue, String userId, String userName) {
        super(federationUserTokenValue);
        this.userId = userId;
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FederationUserToken other = (FederationUserToken) obj;
        if (this.userId == null) {
            if (other.getUserId() != null) return false;
        } else if (!this.userId.equals(other.getUserId())) return false;
        return true;
    }
    
}
