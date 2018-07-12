package org.fogbowcloud.manager.core.plugins.behavior.federationidentity.ldap;

import static org.mockito.Mockito.when;

import java.util.Properties;

import org.fogbowcloud.manager.core.exceptions.InvalidCredentialsUserException;
import org.fogbowcloud.manager.core.exceptions.TokenValueCreationException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesUtil.class)
public class LdapIdentityPluginTest {
	private LdapIdentityPlugin identityPlugin;
	
	@Before
	public void setUp() {
		Properties properties = Mockito.spy(Properties.class);
		
		PowerMockito.mockStatic(PropertiesUtil.class);
		when(PropertiesUtil.readProperties(Mockito.anyString())).thenReturn(properties);
		when(properties.getProperty(Mockito.anyString())).thenReturn(Mockito.anyString());
		
		this.identityPlugin = new LdapIdentityPlugin();
	}
	
	@Test(expected=InvalidCredentialsUserException.class)
	public void testCreateTokenWithoutCredentals() throws UnauthenticatedUserException, TokenValueCreationException {
		this.identityPlugin.createFederationTokenValue(Mockito.anyMap());
	}

}
