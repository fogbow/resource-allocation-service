package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.fogbowcloud.ras.core.plugins.aaa.authentication.RASAuthenticationHolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class SecretManagerTest {

	private static final int GRACE_TIME = 1000;
	private static final long EXPIRATION_INTERVAL = RASAuthenticationHolder.EXPIRATION_INTERVAL;
	private SecretManager secretManager;
	private long now;
	
	@Before
	public void setUp() {
		this.secretManager = Mockito.spy(new SecretManager());
		
		this.now = System.currentTimeMillis();
		Mockito.doReturn(this.now).when(this.secretManager).getNow();
	}
	
	// test case: Success case
	@Test
	public void testVerify() {
		// set up
		String timeAfterNowRasStartingTime = String.valueOf(this.now + GRACE_TIME);
		String secret = timeAfterNowRasStartingTime;
		
		// exercise		
		boolean isValid = this.secretManager.verify(secret);
		
		//verify
		Assert.assertTrue(isValid);
	}
	
	// test case: The secret is not valid because this one was create before the RAS
	@Test
	public void testVerifySecretCreate() {
		// set up
		String timeBeforeNowRasStartingTime = String.valueOf(this.now - GRACE_TIME);
		String secret = timeBeforeNowRasStartingTime;
		
		// exercise		
		boolean isValid = this.secretManager.verify(secret);
		
		// verify
		Assert.assertFalse(isValid);
		Mockito.verify(this.secretManager, Mockito.times(1)).isValidSecret(Mockito.eq(secret));
	}
	
	// test case: The secret is not valid because the your format is invalid
	@Test
	public void testVerifySecretInvalidFormat() {
		// set up
		String secret = "invalid format";
		
		// exercise		
		boolean isValid = this.secretManager.verify(secret);
		
		// verify
		Assert.assertFalse(isValid);
		Mockito.verify(this.secretManager, Mockito.times(1)).isValidSecret(Mockito.eq(secret));
	}		
	
	// test case: Is not valid because the secret is being used two times
	@Test
	public void testVerifySameSecret() {
		// set up
		String timeAfterNowRasStartingTime = String.valueOf(this.now + GRACE_TIME);
		String secret = timeAfterNowRasStartingTime;
		
		// exercise		
		boolean isValidFirstTime = this.secretManager.verify(secret);
		boolean isValidSecondTime = this.secretManager.verify(secret);
		
		// verify
		Assert.assertTrue(isValidFirstTime);
		Assert.assertFalse(isValidSecondTime);
	}
	
	// test case: Checking validity after expiration secret time.
	@Test
	public void testVerifySameSecretAfterFinishedOwnValidity() {
		// set up
		String timeAfterNowRasStartingTime = String.valueOf(this.now + GRACE_TIME);
		String secret = timeAfterNowRasStartingTime;
		
		long graceTime = 1000;
		
		addingSecretsInLimit();
		
		// exercise		
		boolean isValidFirstTime = this.secretManager.verify(secret);
		boolean isNotValidSecondTime = this.secretManager.verify(secret);
		
		// verify
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
		
		String timeAfterNowRasStartingTime = String.valueOf(this.now + GRACE_TIME);
		String secret = timeAfterNowRasStartingTime;
		
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
