package org.fogbowcloud.manager.core.services;

import org.apache.http.HttpStatus;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.identity.ldap.LdapIdentityPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;


public class AuthenticationServiceTest {
	
	AuthenticationService authenticationService;
	IdentityPlugin ldapIdentityPlugin;
	
	
	@Before
	public void setUp() {
		this.ldapIdentityPlugin = Mockito.mock(LdapIdentityPlugin.class);
		this.authenticationService = new AuthenticationService(ldapIdentityPlugin);
	}
	
	@Test
	public void testAuthenticateService() {
		Token token = new Token();
		Mockito.doReturn(token).when(ldapIdentityPlugin).getToken(Mockito.anyString());
		Assert.assertEquals(token, authenticationService.authenticate(Mockito.anyString()));
	}
	
	@Test
	public void testInvalidAccessid() {
		Integer statusResponse = HttpStatus.SC_UNAUTHORIZED;
		Mockito.doThrow(new RuntimeException(statusResponse.toString())).when(ldapIdentityPlugin).getToken(Mockito.anyString());
		
		try {
			authenticationService.authenticate(Mockito.anyString());
			Assert.fail();
		} catch (RuntimeException runtimeException) {
			Assert.assertEquals(statusResponse.toString(), runtimeException.getMessage());
		}
		
		
	}

}
