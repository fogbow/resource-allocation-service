package org.fogbowcloud.manager.core.services;

import java.util.Properties;

import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

// TODO change the name.
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AuthenticationServiceUtil.class })
public class AuthenticationServiceTest {

	private AuthenticationService authenticationService;
	private AuthorizationPlugin authorizationPlugin;
	private IdentityPlugin federatetionIdentityPlugin;
	private IdentityPlugin localIdentityPlugin;
	private Properties properties;

	@Before
	public void setUp() {
		this.properties = new Properties();
		this.federatetionIdentityPlugin = Mockito.mock(IdentityPlugin.class);
		this.localIdentityPlugin = Mockito.mock(IdentityPlugin.class);
		this.authorizationPlugin = Mockito.mock(AuthorizationPlugin.class);
		this.authenticationService = Mockito.spy(new AuthenticationService(this.federatetionIdentityPlugin, 
				this.localIdentityPlugin, this.authorizationPlugin, this.properties));
		PowerMockito.mockStatic(AuthenticationServiceUtil.class);
	}

	@Test
	public void testAuthenticate() throws UnauthorizedException {
		boolean isAuthenticated = true;
		Mockito.doReturn(isAuthenticated).when(this.federatetionIdentityPlugin).isValid(Mockito.anyString());
		this.authenticationService.authenticate(Mockito.anyString());
	}

	@Test(expected=UnauthorizedException.class)
	public void testAuthenticateNot() throws UnauthorizedException {
		Mockito.doThrow(UnauthorizedException.class).when(
				this.federatetionIdentityPlugin).isValid(Mockito.anyString());
		this.authenticationService.authenticate(Mockito.anyString());
	}
	
	@Test
	public void testAutenticateAndAuthorize() throws UnauthorizedException {
		String federationTokenId = "federationTokenId";
		// Authenticated
		Mockito.doReturn(true).when(this.federatetionIdentityPlugin).isValid(Mockito.eq(federationTokenId));
		Token token = Mockito.mock(Token.class);
		Mockito.doReturn(token).when(this.federatetionIdentityPlugin).getToken(Mockito.eq(federationTokenId));
		// Authorized
		Mockito.doReturn(true).when(this.authorizationPlugin).isAuthorized(Mockito.eq(token));
		
		try {
			this.authenticationService.authenticateAndAuthorize(federationTokenId);
		} catch (Exception e) {
			Assert.fail();
		}
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testGetLocalTokenWithLocalTokenIdNullAndOrderProvidingLocal() throws Exception {
		String localTokenId = null;
		String providingMember = "localmember";
		boolean isProvadingLocally = true;
		BDDMockito.given(AuthenticationServiceUtil.isOrderProvadingLocally(
				BDDMockito.anyString(), BDDMockito.any(Properties.class))).willReturn(isProvadingLocally);
		
		Token localToken = Mockito.mock(Token.class);
		Mockito.doReturn(localToken).when(this.localIdentityPlugin).createToken(Mockito.anyMap());
		
		Token tokenGenarated = this.authenticationService.getLocalToken(localTokenId, providingMember);
		Assert.assertEquals(localToken, tokenGenarated);
	}
	
	@Test
	public void testGetLocalTokenWithLocalTokenIdNotNullAndOrderProvidingLocal() throws Exception {
		String localTokenId = "localTokenId";
		String providingMember = "localmember";
		boolean isProvadingLocally = true;
		BDDMockito.given(AuthenticationServiceUtil.isOrderProvadingLocally(
				BDDMockito.anyString(), BDDMockito.any(Properties.class))).willReturn(isProvadingLocally);
		
		Token localToken = Mockito.mock(Token.class);
		Mockito.doReturn(localToken).when(this.localIdentityPlugin).getToken(Mockito.eq(localTokenId));
		
		Token tokenGenarated = this.authenticationService.getLocalToken(localTokenId, providingMember);
		Assert.assertEquals(localToken, tokenGenarated);
	}
	
	@Test
	public void testGetLocalTokenWithLocalTokenIdNotNullAndOrderProvidingRemote() throws Exception {
		String localTokenId = "localTokenId";
		String providingMember = "remoteMember";
		boolean isProvadingLocally = false;
		BDDMockito.given(AuthenticationServiceUtil.isOrderProvadingLocally(
				BDDMockito.anyString(), BDDMockito.any(Properties.class))).willReturn(isProvadingLocally);
		
		Token token = Mockito.mock(Token.class);
		Mockito.doReturn(token).when(this.authenticationService).createTokenBypass(Mockito.eq(localTokenId));
		
		Token tokenGenarated = this.authenticationService.getLocalToken(localTokenId, providingMember);
		
		Token localTokenExpected = this.authenticationService.createTokenBypass(localTokenId);
		Assert.assertEquals(localTokenExpected, tokenGenarated);
	}
}
