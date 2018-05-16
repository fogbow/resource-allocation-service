package org.fogbowcloud.manager.core.services;

import org.fogbowcloud.manager.core.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

//FIXME implement tests. leave for later because of the project decision
//TODO change the name.
public class AuthenticationServiceTest {

	AuthenticationService authenticationService;
	IdentityPlugin identityPlugin;
	AuthorizationPlugin authorizationPlugin;

	@Before
	public void setUp() {
		this.identityPlugin = Mockito.mock(IdentityPlugin.class);
		this.authorizationPlugin = Mockito.mock(AuthorizationPlugin.class);
		this.authenticationService = new AuthenticationService(this.identityPlugin, this.authorizationPlugin);
	}

	@Test
	public void testAuthenticate() throws UnauthorizedException {
		boolean isAuthenticated = true;
		Mockito.doReturn(isAuthenticated).when(this.identityPlugin).isValid(Mockito.anyString());
		this.authenticationService.authenticate(Mockito.anyString());
	}

	@Test(expected=UnauthorizedException.class)
	public void testAuthenticateNot() throws UnauthorizedException {
		Mockito.doThrow(UnauthorizedException.class).when(this.identityPlugin).isValid(Mockito.anyString());
		this.authenticationService.authenticate(Mockito.anyString());
	}
	
}
