package org.fogbowcloud.ras.core.plugins.aaa.identity.shibboleth;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethToken;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethTokenHolder;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethTokenHolder.CreateTokenException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ShibbolethTokenHolder.class })
public class ShibbolethIdentityPluginTest {

	private ShibbolethIdentityPlugin shibbolethIdentityPlugin;

	@Before
	public void setUp() {
		this.shibbolethIdentityPlugin = Mockito.spy(new ShibbolethIdentityPlugin());
	}
	
	// case: success case
	@Test
	public void testCreateToken() throws InvalidParameterException {
		// set up
		String shibTokenValue = "shibTokenValue";
		String tokenProvider = "tokenProvider";
		String userId = "userId";
		String userName = "userName";
		Map<String, String> samlAttributes = new HashMap<String, String>();
		String samlAttributesStr = ShibbolethTokenHolder.normalizeSamlAttribute(samlAttributes);
		long expirationTime = System.currentTimeMillis();
		String expirationTimeStr = ShibbolethTokenHolder.normalizeExpirationTime(expirationTime);
		String rawToken = ShibbolethTokenHolder.createRawToken(shibTokenValue, tokenProvider, 
				userId, userName, samlAttributesStr, expirationTimeStr);
		String rawTokenSignature = "anything";
		String tokenValue = ShibbolethTokenHolder.generateTokenValue(rawToken, rawTokenSignature);
		
		// exercise
		ShibbolethToken shibbolethToken = this.shibbolethIdentityPlugin.createToken(tokenValue);
		
		// verify
		Assert.assertEquals(shibTokenValue, shibbolethToken.getTokenValue());
		Assert.assertEquals(tokenProvider, shibbolethToken.getTokenProvider());
		Assert.assertEquals(userId, shibbolethToken.getUserId());
		Assert.assertEquals(userName, shibbolethToken.getUserName());
		Assert.assertEquals(samlAttributes, shibbolethToken.getSamlAttributes());
		Assert.assertEquals(expirationTime, shibbolethToken.getExpirationTime());
		Assert.assertEquals(rawTokenSignature, shibbolethToken.getSignature());
	}
	
	// case
	@Test(expected=InvalidParameterException.class)
	public void testCreateTokenException() throws InvalidParameterException, CreateTokenException {
		String tokenValue = "anytring";
		// set up
        PowerMockito.mockStatic(ShibbolethTokenHolder.class);
        BDDMockito.given(ShibbolethTokenHolder.createShibbolethToken(Mockito.eq(tokenValue)))
        		.willThrow(new ShibbolethTokenHolder.CreateTokenException(""));
		
		// exercise
		this.shibbolethIdentityPlugin.createToken(tokenValue);
	}	
	
}
