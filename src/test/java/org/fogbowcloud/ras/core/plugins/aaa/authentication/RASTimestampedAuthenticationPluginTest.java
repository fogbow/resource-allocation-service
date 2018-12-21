package org.fogbowcloud.ras.core.plugins.aaa.authentication;

import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.RASAuthenticationHolder;
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
@PrepareForTest({RASAuthenticationHolder.class})
@RunWith(PowerMockRunner.class)
public class RASTimestampedAuthenticationPluginTest {

	private RASTimestampedAuthenticationPlugin rasTimestamped;
	private RASAuthenticationHolder rasAuthenticationHolder;
	
	@Before
	public void setUp() {
		this.rasAuthenticationHolder = Mockito.mock(RASAuthenticationHolder.class);
		PowerMockito.mockStatic(RASAuthenticationHolder.class);
		BDDMockito.given(RASAuthenticationHolder.getInstance()).willReturn(this.rasAuthenticationHolder);
		
		this.rasTimestamped = new RASTimestampedAuthenticationPluginWraper();
	}

	// test case: Success case
	@Test
	public void testCheckValidity() {
		// set up
		long timestamp = System.currentTimeMillis();
		Mockito.when(this.rasAuthenticationHolder.checkValidity(Mockito.eq(timestamp))).thenReturn(true);
		
		// exercise and verify 
		Assert.assertTrue(this.rasTimestamped.checkValidity(timestamp));
	}
	
	private class RASTimestampedAuthenticationPluginWraper extends RASTimestampedAuthenticationPlugin {

		@Override
		protected String getTokenMessage(FederationUserToken federationUserToken) {return null;}

		@Override
		protected String getSignature(FederationUserToken federationUserToken) {return null;}
		
	}
	
}
