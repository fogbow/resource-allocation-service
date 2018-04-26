package org.fogbowcloud.manager.core.services;

import static org.junit.Assert.*;

import java.util.Properties;

import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.identity.ldap.LdapIdentityPlugin;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

public class AuthenticationServiceTest {
	
	AuthenticationService authenticationService;
	IdentityPlugin identityPlugin;
	
	
	@Before
	public void setUp() {
		this.identityPlugin = new LdapIdentityPlugin(Mockito.any(Properties.class));
	}
	
	@Test
	public void authenticateService() {
		
		String accessid = Mockito.mock(String.class);
		
		
		
		String accessId = "my-access-id";
		AuthenticationService mockedAuthenticationService = Mockito.mock(AuthenticationService.class);
		
		ArgumentCaptor<IdentityPlugin> argument = ArgumentCaptor.forClass(IdentityPlugin.class);
		verify(mockedAuthenticationService).
		assertEquals("John", argument.getValue().getName());
		
	}

}
