package org.fogbowcloud.ras.core.models.tokens;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.ras.core.models.tokens.ShibbolethTokenHolder.CreateTokenException;
import org.junit.Assert;
import org.junit.Test;

import net.minidev.json.JSONObject;

public class ShibbolethTokenHolderTest {

	// case: success case
	@Test
	public void testCreateRawToken() {
		// set up
		String tokenValue = "tokenValue";
		String tokenProvider = "tokenProvider";
		String userId = "userId";
		String userName = "userName";
		String samlAttributes = "samlAttributes";
		String expirationTime = "expirationTime";
		
		// exercise
		String rawToken = ShibbolethTokenHolder.createRawToken(
				tokenValue, tokenProvider, userId, userName, samlAttributes, expirationTime);
		
		// verify
		String[] rawTokenSlices = rawToken.split(ShibbolethTokenHolder.SHIBBOLETH_SEPARETOR);
		Assert.assertEquals(tokenValue, rawTokenSlices[ShibbolethTokenHolder.STR_TOKEN_VALUE_INDEX]);
		Assert.assertEquals(tokenProvider, rawTokenSlices[ShibbolethTokenHolder.TOKEN_PROVIDER_TOKEN_VALUE_INDEX]);
		Assert.assertEquals(userId, rawTokenSlices[ShibbolethTokenHolder.USER_ID_TOKEN_VALUE_INDEX]);
		Assert.assertEquals(userName, rawTokenSlices[ShibbolethTokenHolder.USER_NAME_TOKEN_VALUE_INDEX]);
		Assert.assertEquals(samlAttributes, rawTokenSlices[ShibbolethTokenHolder.SAML_ATTRIBUTES_TOKEN_VALUE_INDEX]);
		Assert.assertEquals(expirationTime, rawTokenSlices[ShibbolethTokenHolder.EXPIRATION_TIME_TOKEN_VALUE_INDEX]);
	}
	
	// case: create raw token with null values
	@Test
	public void testCreateRawTokenWithNullValues() {
		// set up
		String tokenValue = null;
		String tokenProvider = "tokenProvider";
		String userId = "userId";
		String userName = "userName";
		String samlAttributes = null;
		String expirationTime = "expirationTime";
		
		// exercise
		String rawToken = ShibbolethTokenHolder.createRawToken(
				tokenValue, tokenProvider, userId, userName, samlAttributes, expirationTime);
		
		// verify
		String[] rawTokenSlices = rawToken.split(ShibbolethTokenHolder.SHIBBOLETH_SEPARETOR);
		Assert.assertTrue(rawTokenSlices[ShibbolethTokenHolder.STR_TOKEN_VALUE_INDEX].isEmpty());
		Assert.assertEquals(tokenProvider, rawTokenSlices[ShibbolethTokenHolder.TOKEN_PROVIDER_TOKEN_VALUE_INDEX]);
		Assert.assertEquals(userId, rawTokenSlices[ShibbolethTokenHolder.USER_ID_TOKEN_VALUE_INDEX]);
		Assert.assertEquals(userName, rawTokenSlices[ShibbolethTokenHolder.USER_NAME_TOKEN_VALUE_INDEX]);
		Assert.assertTrue(samlAttributes, rawTokenSlices[ShibbolethTokenHolder.SAML_ATTRIBUTES_TOKEN_VALUE_INDEX].isEmpty());
		Assert.assertEquals(expirationTime, rawTokenSlices[ShibbolethTokenHolder.EXPIRATION_TIME_TOKEN_VALUE_INDEX]);
	}
	
	// case: success case
	@Test
	public void testCreateShibbolethToken() throws CreateTokenException {
		// set up
		String tokenValue = "token_value";
		String tokenProvider = "tokenProvider";
		Map<String, String> samlAttributes = new HashMap<>();
		samlAttributes.put("key", "value");
		String samlAttributesStr = new JSONObject(samlAttributes).toString();
		String userName = "userName";
		String userId = "userId";
		String signature = "signature";
		long timestampNow = System.currentTimeMillis();
		String expirationTime = String.valueOf(timestampNow);
		
		String[] tokenValueSlices = new String[10];
		tokenValueSlices[ShibbolethTokenHolder.STR_TOKEN_VALUE_INDEX] = tokenValue;
		tokenValueSlices[ShibbolethTokenHolder.TOKEN_PROVIDER_TOKEN_VALUE_INDEX] = tokenProvider;
		tokenValueSlices[ShibbolethTokenHolder.USER_ID_TOKEN_VALUE_INDEX] = userId;
		tokenValueSlices[ShibbolethTokenHolder.USER_NAME_TOKEN_VALUE_INDEX] = userName;
		tokenValueSlices[ShibbolethTokenHolder.SAML_ATTRIBUTES_TOKEN_VALUE_INDEX] = samlAttributesStr;
		tokenValueSlices[ShibbolethTokenHolder.EXPIRATION_TIME_TOKEN_VALUE_INDEX] = expirationTime;
		tokenValueSlices[ShibbolethTokenHolder.SIGNATURE_TOKEN_VALUE_INDEX] = signature;
		
		// exercise
		ShibbolethToken shibbolethToken = ShibbolethTokenHolder.createShibbolethToken(tokenValueSlices);
		
		// verify
		Assert.assertEquals(tokenValue, shibbolethToken.getTokenValue());
		Assert.assertEquals(tokenProvider, shibbolethToken.getTokenProvider());
		Assert.assertEquals(samlAttributes, shibbolethToken.getSamlAttributes());
		Assert.assertEquals(userName, shibbolethToken.getUserName());
		Assert.assertEquals(userId, shibbolethToken.getUserId());
		Assert.assertEquals(signature, shibbolethToken.getSignature());
		Assert.assertEquals(timestampNow, shibbolethToken.getTimestamp());
	}
	
}
