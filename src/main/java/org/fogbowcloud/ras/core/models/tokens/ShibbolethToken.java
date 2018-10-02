package org.fogbowcloud.ras.core.models.tokens;

import java.util.Map;

import javax.persistence.Column;

public class ShibbolethToken extends FederationUserToken implements GenericSignatureToken {

    public static final int MAX_SIGNATURE_SIZE = 1024;
    
    @Column
    private Map<String, String> samlAttributes;
    @Column(length = MAX_SIGNATURE_SIZE)
    private String signature;
    @Column
    private long expirationTime;
    
	public ShibbolethToken(String tokenProvider, String tokenValue, String userId,
			String name, Map<String, String> samlAttributes, long expirationTime, String signature) {
		super(tokenProvider, tokenValue, userId, name);
		this.samlAttributes = samlAttributes;
		this.expirationTime = expirationTime;
		this.signature = signature;
	}
	
	public Map<String, String> getSamlAttributes() {
		return samlAttributes;
	}
	
	public String getSignature() {
		return signature;
	}

	@Override
	public String getRawToken() {
		String samlAttributesStr = ShibbolethTokenHolder.normalizeSamlAttribute(this.samlAttributes);
		String expirationTimeStr = ShibbolethTokenHolder.normalizeExpirationTime(this.expirationTime);
		
		return ShibbolethTokenHolder.createRawToken(getTokenValue(), getTokenProvider(), 
				getUserId(), getUserName(), samlAttributesStr, expirationTimeStr);
	}

	@Override
	public String getRawTokenSignature() {
		return this.signature;
	}

	@Override
	public long getExpirationTime() {
		return this.expirationTime;
	}
	
}
