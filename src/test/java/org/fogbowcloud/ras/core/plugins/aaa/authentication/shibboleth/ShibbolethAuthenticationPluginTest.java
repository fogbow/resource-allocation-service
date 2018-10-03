package org.fogbowcloud.ras.core.plugins.aaa.authentication.shibboleth;

import java.util.HashMap;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.UnauthenticTokenException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.GenericSignatureToken;
import org.fogbowcloud.ras.core.models.tokens.ShibbolethToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.generic.GenericSignatureAuthenticationPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ShibbolethAuthenticationPluginTest {

	private ShibbolethAuthenticationPlugin shibbolethAuthenticationPlugin;
	private String tokenProviderId;
	private GenericSignatureAuthenticationPlugin genericSignatureAuthenticationPlugin;

	@Before
	public void setUp() {
		this.tokenProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
		this.shibbolethAuthenticationPlugin = new ShibbolethAuthenticationPlugin();
		this.genericSignatureAuthenticationPlugin = Mockito.mock(GenericSignatureAuthenticationPlugin.class);
		this.shibbolethAuthenticationPlugin.setGenericSignatureAuthenticationPlugin(genericSignatureAuthenticationPlugin);
	}
	
	// case: Is authenticated when token provider is the same than local provider id
	@Test
	public void testIsAuthentic() throws UnavailableProviderException, UnauthenticTokenException {
		// set up
		String requestingMember = "anything";
		ShibbolethToken shibbolethToken = createShibbotethToken();
	
		Mockito.doNothing().when(this.genericSignatureAuthenticationPlugin)
				.checkTokenValue(Mockito.any(GenericSignatureToken.class));
		
		//exercise
		boolean isAuthentic = this.shibbolethAuthenticationPlugin.isAuthentic(requestingMember, shibbolethToken);
		
		// verify
		Assert.assertTrue(isAuthentic);
		Mockito.verify(this.genericSignatureAuthenticationPlugin, Mockito.times(1))
				.checkTokenValue(Mockito.any(GenericSignatureToken.class));
	}
	
	// case: Is not authenticated when token provider is the same than local provider id
	@Test
	public void testIsNotAuthentic() throws UnavailableProviderException, UnauthenticTokenException {
		// set up
		String requestingMember = "anything";
		ShibbolethToken shibbolethToken = createShibbotethToken();
	
		Mockito.doThrow(Exception.class).when(this.genericSignatureAuthenticationPlugin)
				.checkTokenValue(Mockito.any(GenericSignatureToken.class));
		
		//exercise
		boolean isAuthentic = this.shibbolethAuthenticationPlugin.isAuthentic(requestingMember, shibbolethToken);
		
		// verify
		Assert.assertFalse(isAuthentic);
		Mockito.verify(this.genericSignatureAuthenticationPlugin, Mockito.times(1))
				.checkTokenValue(Mockito.any(GenericSignatureToken.class));
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
		Mockito.verify(this.genericSignatureAuthenticationPlugin, Mockito.times(0))
				.checkTokenValue(Mockito.any(GenericSignatureToken.class));
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
		Mockito.verify(this.genericSignatureAuthenticationPlugin, Mockito.times(0))
				.checkTokenValue(Mockito.any(GenericSignatureToken.class));
	}		
	
	private ShibbolethToken createShibbotethToken() {
		String tokenProvider = this.tokenProviderId;		
		return createShibbotethToken(tokenProvider);
	}
	
	private ShibbolethToken createShibbotethToken(String tokenProvider) {		
		return new ShibbolethToken(tokenProvider, "", "", "", new HashMap<String, String>(), 0l, "");
	}
}
