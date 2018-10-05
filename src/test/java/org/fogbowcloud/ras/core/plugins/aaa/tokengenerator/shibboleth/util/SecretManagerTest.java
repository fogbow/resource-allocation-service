package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.fogbowcloud.ras.core.plugins.aaa.authentication.generic.GenericSignatureAuthenticationHolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SecretManagerTest {

	private static final long EXPIRATION_INTERVAL = GenericSignatureAuthenticationHolder.EXPIRATION_INTERVAL;
	private SecretManager secretManager;
	
	@Before
	public void setUp() {
		this.secretManager = Mockito.spy(new SecretManager());
	}
	
	// test case: Success case
	@Test
	public void testVerify() {
		// set up
		String secret = "any_secret";
		
		// exercise		
		boolean isValid = this.secretManager.verify(secret);
		
		//verify
		Assert.assertTrue(isValid);
	}
	
	// test case: Is not valid because the secret is being used two times
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
	
	// test case: Checking validity after expiration secret time.
	@Test
	public void testVerifySameSecretAfterFinishedOwnValidity() {
		// set up
		String secret = "any_secret";
		long graceTime = 1000;
		
		addingSecretsInLimit();
		
		// exercise		
		boolean isValidFirstTime = this.secretManager.verify(secret);
		boolean isNotValidSecondTime = this.secretManager.verify(secret);
		
		//verify
		Assert.assertTrue(isValidFirstTime);
		Assert.assertFalse(isNotValidSecondTime);
		
		long nowTest = System.currentTimeMillis();
		// invalidating secret. The "Now" in the future !
		long invalidTimeInTheFuture = nowTest + EXPIRATION_INTERVAL + graceTime;
		Mockito.doReturn(invalidTimeInTheFuture).when(this.secretManager).getNow();
		
		boolean isValidThirdTime = this.secretManager.verify(secret);
		Assert.assertTrue(isValidThirdTime);
	}	
	
	// test case: Cleaning secrets that are invalid because the time
	@Test
	public void testCleanSecrets() {
		// set up
		long graceTime = 1000;
		
		addingSecretsInLimit();
		
		String secret = "secret";
		long nowTest = System.currentTimeMillis();
		Mockito.doReturn(nowTest).when(this.secretManager).getNow();
		boolean isValid = this.secretManager.verify(secret);
		Assert.assertTrue(isValid);
		
		// check if still exists the secret
		Assert.assertNotNull(this.secretManager.getSecrets().get(secret));
		
		// secret is valid yet
		Mockito.doReturn(nowTest).when(this.secretManager).getNow();
		this.secretManager.cleanSecrets();
		
		// check if still exists the secret
		Assert.assertNotNull(this.secretManager.getSecrets().get(secret));
		
		// invalidating secret. The "Now" in the future !
		long invalidTimeInTheFuture = nowTest + EXPIRATION_INTERVAL + graceTime;
		Mockito.doReturn(invalidTimeInTheFuture).when(this.secretManager).getNow();
		
		// exercise
		this.secretManager.cleanSecrets();
		
		// verify
		Assert.assertNull(this.secretManager.getSecrets().get(secret));
	}

	private void addingSecretsInLimit() {
		// adding the secrets until limit in the map
		Map<String, Long> initialSecrets = new HashMap<String, Long>(); 
		for (int i = 0; i < SecretManager.MAXIMUM_MAP_SIZE; i++) {
			long now = System.currentTimeMillis();
			long initTimeAlwaysValid = now + (10 * EXPIRATION_INTERVAL);
			
			initialSecrets.put(UUID.randomUUID().toString(), initTimeAlwaysValid);
		}
		
		this.secretManager.setSecrets(initialSecrets);
	}
	
}
