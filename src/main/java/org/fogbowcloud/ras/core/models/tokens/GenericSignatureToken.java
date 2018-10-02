package org.fogbowcloud.ras.core.models.tokens;

public interface GenericSignatureToken {
	
	String getRawToken();
	
	String getRawTokenSignature();
	
	long getExpirationTime();

}
