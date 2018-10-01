package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth.ShibbolethTokenGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SecretManagerTest {

	private static final long EXPIRATION_INTERVAL = ShibbolethTokenGenerator.EXPIRATION_INTERVAL;
	private SecretManager secretManager;
	
	@Before
	public void setUp() {
		this.secretManager = Mockito.spy(new SecretManager());
	}
	
	// case: success case
	@Test
	public void testVerify() {
		// set up
		String secret = "any_secret";
		
		// exercise		
		boolean isValid = this.secretManager.verify(secret);
		
		//verify
		Assert.assertTrue(isValid);
	}
	
	// case: Is not valid because the secret is being used two times
	@Test
	public void testVerifySameSecret() {
		// set up
		String secret = "any_secret";
		
		// exercise		
		boolean isValidFirstTime = this.secretManager.verify(secret);
		boolean isValidSecondTime = this.secretManager.verify(secret);
		
		//verify
		Assert.assertTrue(isValidFirstTime);
		Assert.assertFalse(isValidSecondTime);
	}
	
	// case: Cleaning secrets that are invalid because the time
	@Test
	public void testCleanSecrets() {
		// set up
		long graceTime = 1000;
		
		// adding the secrets until limit in the map
		Map<String, Long> initialSecrets = new HashMap<String, Long>(); 
		for (int i = 0; i < SecretManager.MAXIMUM_MAP_SIZE; i++) {
			long now = System.currentTimeMillis();
			long initTimeAlwaysValid = now + (10 * EXPIRATION_INTERVAL);
			
			initialSecrets.put(UUID.randomUUID().toString(), initTimeAlwaysValid);
		}
		
		String secret = "secret";
		long now = System.currentTimeMillis();
		Mockito.doReturn(now).when(this.secretManager).getNow();
		boolean isValid = this.secretManager.verify(secret);
		Assert.assertTrue(isValid);
		
		// check if still exists the secret
		Assert.assertNotNull(this.secretManager.getSecrets().get(secret));
		
		// secret is valid yet
		Mockito.doReturn(now).when(this.secretManager).getNow();
		this.secretManager.cleanSecrets();
		
		// check if still exists the secret
		Assert.assertNotNull(this.secretManager.getSecrets().get(secret));
		
		// invalidating secret. The "Now" in the future !
		long invalidTimeInTheFuture = now + EXPIRATION_INTERVAL + graceTime;
		Mockito.doReturn(invalidTimeInTheFuture).when(this.secretManager).getNow();
		
		// exercise
		this.secretManager.cleanSecrets();
		
		// verify
		Assert.assertNull(this.secretManager.getSecrets().get(secret));
	}
	
}
