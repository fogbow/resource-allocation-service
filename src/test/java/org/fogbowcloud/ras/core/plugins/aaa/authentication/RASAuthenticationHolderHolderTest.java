package org.fogbowcloud.ras.core.plugins.aaa.authentication;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RASAuthenticationHolderHolderTest {

	private static final long GRACE_TIME = 100;
	
	private RASAuthenticationHolder genericSignatureAuthenticationPlugin;

	@Before
	public void setUp() throws IOException, GeneralSecurityException {
		this.genericSignatureAuthenticationPlugin = Mockito.spy(new RASAuthenticationHolder());
	}
	
	// test case: Generate expirationTime
	@Test
	public void testGenerateExpirationTime() {
		// set up		
		long now = System.currentTimeMillis();
		String expirationTimeExpected = String.valueOf(now + RASAuthenticationHolder.EXPIRATION_INTERVAL);
		
		Mockito.when(this.genericSignatureAuthenticationPlugin.getNow()).thenReturn(now);
		
		// exercise		
		String expirationTime = this.genericSignatureAuthenticationPlugin.generateExpirationTime();
		
		// verify
		Assert.assertEquals(expirationTimeExpected, expirationTime);
	}

	// test case: Success case
	@Test 
	public void testCheckValidity() {
		// set up
		long expirationTime = System.currentTimeMillis();
		
		long nowInFuture = expirationTime + GRACE_TIME;
		Mockito.when(this.genericSignatureAuthenticationPlugin.getNow()).thenReturn(nowInFuture);
		
		// exercise and check
		Assert.assertTrue(this.genericSignatureAuthenticationPlugin.checkValidity(expirationTime));
	}
	
	// test case: Expiration time is over
	@Test 
	public void testCheckValidityNotValid() {
		// set up
		long expirationTime = System.currentTimeMillis();
		
		long nowInPass = expirationTime - GRACE_TIME;
		Mockito.when(this.genericSignatureAuthenticationPlugin.getNow()).thenReturn(nowInPass);
		
		// exercise and check
		Assert.assertFalse(this.genericSignatureAuthenticationPlugin.checkValidity(expirationTime));
	}	
	
	// test case: Success case
	@Test
	public void testVerifySignature() throws Exception {
		// set up
		String message = "anystring";
		
		// exercise
		String messageSignated = this.genericSignatureAuthenticationPlugin.createSignature(message);
		
		// verify
		Assert.assertNotEquals(message, messageSignated);
		Assert.assertTrue(this.genericSignatureAuthenticationPlugin.verifySignature(message, messageSignated));
	}
	
	// test case: Is not the same signature
	@Test
	public void testVerifySignatureWrong() throws Exception {
		// exercise
		String messageSignated = this.genericSignatureAuthenticationPlugin.createSignature("one");
		// verify
		Assert.assertFalse(this.genericSignatureAuthenticationPlugin.verifySignature("two", messageSignated));
	}
	
}
