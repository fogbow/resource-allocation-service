package org.fogbowcloud.ras.core.plugins.aaa.authentication.shibboleth;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.HashMap;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.UnauthenticTokenException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethToken;
import org.fogbowcloud.ras.util.RSAUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PowerMockIgnore({"javax.management.*"})
@PrepareForTest({ RSAUtil.class })
@RunWith(PowerMockRunner.class)
public class ShibbolethAuthenticationPluginTest {

	private ShibbolethAuthenticationPlugin shibbolethAuthenticationPlugin;
	private String tokenProviderId;

	@Before
	public void setUp() {
		this.tokenProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
		this.shibbolethAuthenticationPlugin = Mockito.spy(new ShibbolethAuthenticationPlugin(this.tokenProviderId));
	}
	
	// case: Is authenticated when token provider is the same than local provider id
	@Test
	public void testIsAuthentic() throws UnavailableProviderException, UnauthenticTokenException, InvalidKeyException,
			SignatureException, NoSuchAlgorithmException, UnsupportedEncodingException {
		// set up
		String requestingMember = "anything";
		ShibbolethToken shibbolethToken = createShibbotethToken();
	
		PowerMockito.mockStatic(RSAUtil.class);
		BDDMockito.given(RSAUtil.verify(Mockito.any(PublicKey.class), Mockito.anyString(), Mockito.anyString()))
					.willReturn(true);
		
		//exercise
		boolean isAuthentic = this.shibbolethAuthenticationPlugin.isAuthentic(requestingMember, shibbolethToken);
		
		// verify
		Assert.assertTrue(isAuthentic);
	}
	
	// case: Is not authenticated when token provider is the same than local provider id
	@Test
	public void testIsNotAuthentic() throws UnavailableProviderException, UnauthenticTokenException,
			InvalidKeyException, SignatureException, NoSuchAlgorithmException, UnsupportedEncodingException {
		// set up
		String requestingMember = "anything";
		ShibbolethToken shibbolethToken = createShibbotethToken();
	
		PowerMockito.mockStatic(RSAUtil.class);
		BDDMockito.given(RSAUtil.verify(Mockito.any(PublicKey.class), Mockito.anyString(), Mockito.anyString()))
					.willReturn(false);
		
		//exercise
		boolean isAuthentic = this.shibbolethAuthenticationPlugin.isAuthentic(requestingMember, shibbolethToken);
		
		// verify
		Assert.assertFalse(isAuthentic);
	}
	
	// case: Is authenticated when token provider is different than local provider id and requestingMember is equals
	@Test
	public void testIsAuthenticIsNotLocalProvider() throws UnavailableProviderException, UnauthenticTokenException {
		// set up
		String requestingMember = "anything";
		String tokenProviderSameThanRM = requestingMember;
		ShibbolethToken shibbolethToken = createShibbotethToken(tokenProviderSameThanRM);
	
		//exercise
		boolean isAuthentic = this.shibbolethAuthenticationPlugin.isAuthentic(requestingMember, shibbolethToken);
		
		// verify
		Assert.assertTrue(isAuthentic);
	}	
	
	// case: Is not authenticated when token provider is different than local provider id and requestingMember is equals
	@Test
	public void testIsNotAuthenticIsNotLocalProvider() throws UnavailableProviderException, UnauthenticTokenException {
		// set up
		String requestingMember = "anything";
		String tokenProviderDiffentRM = "different_of_requesting_member";
		ShibbolethToken shibbolethToken = createShibbotethToken(tokenProviderDiffentRM);
	
		//exercise
		boolean isAuthentic = this.shibbolethAuthenticationPlugin.isAuthentic(requestingMember, shibbolethToken);
		
		// verify
		Assert.assertFalse(isAuthentic);
	}		
	
	private ShibbolethToken createShibbotethToken() {
		String tokenProvider = this.tokenProviderId;		
		return createShibbotethToken(tokenProvider);
	}
	
	private ShibbolethToken createShibbotethToken(String tokenProvider) {		
		return new ShibbolethToken(tokenProvider, "", "", "", new HashMap<String, String>(), 0l, "");
	}
}
