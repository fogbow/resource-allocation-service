package org.fogbowcloud.ras.core.models.tokens;

import javax.persistence.Entity;

@Entity
public class CloudStackToken extends FederationUserToken {

    public CloudStackToken(String tokenProvider, String tokenValue, String userID, String userName) {
        super(tokenProvider, tokenValue, userID, userName);
    }

}
