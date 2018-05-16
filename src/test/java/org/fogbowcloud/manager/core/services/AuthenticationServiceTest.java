package org.fogbowcloud.manager.core.services;

import java.util.Properties;

import org.fogbowcloud.manager.core.plugins.AuthorizationPlugin;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

//FIXME implement tests. leave for later because of the project decision
//TODO change the name.
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
		this.authenticationService = new AuthenticationService(this.federatetionIdentityPlugin, 
				this.localIdentityPlugin, this.authorizationPlugin, this.properties);
	}

	@Test
	public void testAuthenticate() throws UnauthorizedException {
		boolean isAuthenticated = true;
		Mockito.doReturn(isAuthenticated).when(this.federatetionIdentityPlugin).isValid(Mockito.anyString());
		this.authenticationService.authenticate(Mockito.anyString());
	}

	@Test(expected=UnauthorizedException.class)
	public void testAuthenticateNot() throws UnauthorizedException {
		Mockito.doThrow(UnauthorizedException.class).when(this.federatetionIdentityPlugin).isValid(Mockito.anyString());
		this.authenticationService.authenticate(Mockito.anyString());
	}
	
}
