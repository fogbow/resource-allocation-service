package org.fogbowcloud.manager.core.services;

import java.util.Date;
import java.util.HashMap;

import org.apache.http.HttpStatus;
import org.fogbowcloud.manager.core.exceptions.UnauthorizedException;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AuthenticationServiceTest {

	AuthenticationService authenticationService;
	IdentityPlugin identityPlugin;

	@Before
	public void setUp() {
		this.identityPlugin = Mockito.mock(IdentityPlugin.class);
		this.authenticationService = new AuthenticationService(identityPlugin);
	}

	@Test
	public void testAuthenticateService() throws UnauthorizedException {
		Token token = new Token("accessId", Mockito.mock(Token.User.class), new Date(), new HashMap<String, String>());
		Mockito.doReturn(token).when(identityPlugin).getToken(Mockito.anyString());
		Assert.assertEquals(token, authenticationService.authenticate(Mockito.anyString()));
	}

	@Test
	public void testInvalidAccessid() throws UnauthorizedException {
		Integer statusResponse = HttpStatus.SC_UNAUTHORIZED;
		Mockito.doThrow(new RuntimeException(statusResponse.toString())).when(identityPlugin)
				.getToken(Mockito.anyString());

		try {
			authenticationService.authenticate(Mockito.anyString());
			Assert.fail();
		} catch (RuntimeException runtimeException) {
			Assert.assertEquals(statusResponse.toString(), runtimeException.getMessage());
		}
	}

}
