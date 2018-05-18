package org.fogbowcloud.manager.core.services;

import java.util.Properties;

import org.fogbowcloud.manager.core.manager.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.manager.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.manager.plugins.identity.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.token.Token;
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
@PrepareForTest({ AuthenticationControllerUtil.class })
public class AAAControllerTest {
//
//	private AAAController AAAController;
//	private AuthorizationPlugin authorizationPlugin;
//	private IdentityPlugin federationIdentityPlugin;
//	private IdentityPlugin localIdentityPlugin;
//	private Properties properties;
//
//	@Before
//	public void setUp() {
//		this.properties = new Properties();
//		this.federationIdentityPlugin = Mockito.mock(IdentityPlugin.class);
//		this.localIdentityPlugin = Mockito.mock(IdentityPlugin.class);
//		this.authorizationPlugin = Mockito.mock(AuthorizationPlugin.class);
//		this.AAAController = Mockito.spy(new AAAController(this.federationIdentityPlugin,
//				this.localIdentityPlugin, this.authorizationPlugin, this.properties));
//		PowerMockito.mockStatic(AuthenticationControllerUtil.class);
//	}
//
//	@Test
//	public void testAuthenticate() throws UnauthorizedException {
//		boolean isAuthenticated = true;
//		Mockito.doReturn(isAuthenticated).when(this.federationIdentityPlugin).isValid(Mockito.anyString());
//		this.AAAController.authenticate(Mockito.anyString());
//	}
//
//	@Test(expected=UnauthorizedException.class)
//	public void testAuthenticateNot() throws UnauthorizedException {
//		Mockito.doThrow(UnauthorizedException.class).when(
//				this.federationIdentityPlugin).isValid(Mockito.anyString());
//		this.AAAController.authenticate(Mockito.anyString());
//	}
//
//	@Test
//	public void testAutenticateAndAuthorize() throws UnauthorizedException {
//		String federationTokenId = "federationTokenId";
//		// Authenticated
//		Mockito.doReturn(true).when(this.federationIdentityPlugin).isValid(Mockito.eq(federationTokenId));
//		Token token = Mockito.mock(Token.class);
//		Mockito.doReturn(token).when(this.federationIdentityPlugin).getToken(Mockito.eq(federationTokenId));
//		// Authorized
//		Mockito.doReturn(true).when(this.authorizationPlugin).isAuthorized(Mockito.eq(token), operation, order);
//
//		try {
//			this.AAAController.authenticateAndAuthorize(federationTokenId);
//		} catch (Exception e) {
//			Assert.fail();
//		}
//	}
//
//	@SuppressWarnings("unchecked")
//	@Test
//	public void testGetLocalTokenWithLocalTokenIdNullAndOrderProvidingLocal() throws Exception {
//		String localTokenId = null;
//		String providingMember = "localmember";
//		boolean isProvadingLocally = true;
//		BDDMockito.given(AuthenticationControllerUtil.isOrderProvidingLocally(
//				BDDMockito.anyString(), BDDMockito.any(Properties.class))).willReturn(isProvadingLocally);
//
//		Token localToken = Mockito.mock(Token.class);
//		Mockito.doReturn(localToken).when(this.localIdentityPlugin).createToken(Mockito.anyMap());
//
//		Token tokenGenarated = this.AAAController.getLocalToken(localTokenId, providingMember);
//		Assert.assertEquals(localToken, tokenGenarated);
//	}
//
//	@Test
//	public void testGetLocalTokenWithLocalTokenIdNotNullAndOrderProvidingLocal() throws Exception {
//		String localTokenId = "localTokenId";
//		String providingMember = "localmember";
//		boolean isProvadingLocally = true;
//		BDDMockito.given(AuthenticationControllerUtil.isOrderProvidingLocally(
//				BDDMockito.anyString(), BDDMockito.any(Properties.class))).willReturn(isProvadingLocally);
//
//		Token localToken = Mockito.mock(Token.class);
//		Mockito.doReturn(localToken).when(this.localIdentityPlugin).getToken(Mockito.eq(localTokenId));
//
//		Token tokenGenarated = this.AAAController.getLocalToken(localTokenId, providingMember);
//		Assert.assertEquals(localToken, tokenGenarated);
//	}
//
//	@Test
//	public void testGetLocalTokenWithLocalTokenIdNotNullAndOrderProvidingRemote() throws Exception {
//		String localTokenId = "localTokenId";
//		String providingMember = "remoteMember";
//		boolean isProvadingLocally = false;
//		BDDMockito.given(AuthenticationControllerUtil.isOrderProvidingLocally(
//				BDDMockito.anyString(), BDDMockito.any(Properties.class))).willReturn(isProvadingLocally);
//
//		Token token = Mockito.mock(Token.class);
//		Mockito.doReturn(token).when(this.AAAController).createTokenBypass(Mockito.eq(localTokenId));
//
//		Token tokenGenarated = this.AAAController.getLocalToken(localTokenId, providingMember);
//
//		Token localTokenExpected = this.AAAController.createTokenBypass(localTokenId);
//		Assert.assertEquals(localTokenExpected, tokenGenarated);
//	}

}
