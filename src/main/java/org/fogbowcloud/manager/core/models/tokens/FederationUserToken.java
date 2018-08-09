package org.fogbowcloud.manager.core.models.tokens;

public class FederationUserToken extends Token {

    // The id must uniquely identify a user in the federation; two tokens issued to the same user must have the same id.
    private String id;
    // This field is a human-friendly identification of the user, typically used by the CLI/GUI, but need not be unique.
    private String name;

    public FederationUserToken(String federationUserTokenValue, String id, String name) {
        super(federationUserTokenValue);
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        FederationUserToken other = (FederationUserToken) obj;
        if (this.id == null) {
            if (other.getId() != null) return false;
        } else if (!this.id.equals(other.getId())) return false;
        return true;
    }
    
}
