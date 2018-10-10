package org.fogbowcloud.ras.core.plugins.aaa.authentication;

import org.fogbowcloud.ras.core.models.tokens.TimestampedSignedFederationUserToken;

public abstract class RASTimestampedAuthenticationPlugin extends RASAuthenticationPlugin {

    public boolean isAuthentic(String requestingMember, TimestampedSignedFederationUserToken timestampedSignedFederationUserToken)  {
    	boolean isAuthenticated = super.isAuthentic(requestingMember, timestampedSignedFederationUserToken);
    	boolean isValid = checkValidity(timestampedSignedFederationUserToken.getTimestamp()); 
    	return isAuthenticated && isValid;
    }
    
    protected boolean checkValidity(long timestamp) {
    	return this.rasAuthenticationHolder.checkValidity(timestamp);
	}

}
