package org.fogbowcloud.ras.core.plugins.aaa.authentication.generic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.util.concurrent.TimeUnit;

import org.fogbowcloud.ras.core.exceptions.UnauthenticTokenException;
import org.fogbowcloud.ras.core.models.tokens.GenericSignatureToken;
import org.fogbowcloud.ras.util.RSAUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class GenericSignatureAuthenticationHolderTest {

	private final String PRIVATE_KEY_SUFIX_PATH = "private/private.key";
	private RSAPrivateKey privateKey;
	
	private GenericSignatureAuthenticationHolder genericSignatureAuthenticationPlugin;

	@Before
	public void setUp() throws IOException, GeneralSecurityException {
		String privateKeyPath = getResourceFilePath(PRIVATE_KEY_SUFIX_PATH);
		this.privateKey = RSAUtil.getPrivateKey(privateKeyPath);
		this.genericSignatureAuthenticationPlugin = Mockito.spy(new GenericSignatureAuthenticationHolder());
	}
	
	// case: success case
	@Test
	public void testCheckTokenValue() throws UnauthenticTokenException, IOException, GeneralSecurityException {
		// set up
		GenericSignatureToken genericSignatureToken = Mockito.mock(GenericSignatureToken.class);
		long expirationTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
		Mockito.doReturn(expirationTime).when(genericSignatureToken).getExpirationTime();
		String rawToken = "rawToken";
		String rawTokenSignature = sign(rawToken);
		Mockito.doReturn(rawToken).when(genericSignatureToken).getRawToken();
		Mockito.doReturn(rawTokenSignature).when(genericSignatureToken).getRawTokenSignature();
				
		// exercise
		this.genericSignatureAuthenticationPlugin.checkTokenValue(genericSignatureToken);
		
		//verify 
		Mockito.verify(this.genericSignatureAuthenticationPlugin, Mockito.times(1))
				.verifySignature(Mockito.eq(rawToken), Mockito.eq(rawTokenSignature));
	}
	
	// case: The token value is expired
	@Test
	public void testCheckTokenValueExpired() throws UnauthenticTokenException, IOException, GeneralSecurityException {
		// set up
		GenericSignatureToken genericSignatureToken = Mockito.mock(GenericSignatureToken.class);
		long expirationTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
		Mockito.doReturn(expirationTime).when(genericSignatureToken).getExpirationTime();
		String rawToken = "rawToken";
		String rawTokenSignature = sign(rawToken);
		Mockito.doReturn(rawToken).when(genericSignatureToken).getRawToken();
		Mockito.doReturn(rawTokenSignature).when(genericSignatureToken).getRawTokenSignature();

		// exercise
		try {
			this.genericSignatureAuthenticationPlugin.checkTokenValue(genericSignatureToken);
			Assert.fail();
		} catch (Exception e) {}
		
		//verify 
		Mockito.verify(this.genericSignatureAuthenticationPlugin, Mockito.never())
				.verifySignature(Mockito.eq(rawToken), Mockito.eq(rawTokenSignature));
	}	
	
	// case: The token value is expired
	@Test
	public void testCheckTokenSignatureIsNotValid() throws UnauthenticTokenException, IOException, GeneralSecurityException {
		// set up
		GenericSignatureToken genericSignatureToken = Mockito.mock(GenericSignatureToken.class);
		long expirationTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
		Mockito.doReturn(expirationTime).when(genericSignatureToken).getExpirationTime();
		String rawToken = "rawToken";
		String rawTokenSignature = "wrong_signature";
		Mockito.doReturn(rawToken).when(genericSignatureToken).getRawToken();
		Mockito.doReturn(rawTokenSignature).when(genericSignatureToken).getRawTokenSignature();

		// exercise
		try {
			this.genericSignatureAuthenticationPlugin.checkTokenValue(genericSignatureToken);
			Assert.fail();
		} catch (Exception e) {}
		
		//verify 
		Mockito.verify(this.genericSignatureAuthenticationPlugin, Mockito.timeout(1))
				.verifySignature(Mockito.eq(rawToken), Mockito.eq(rawTokenSignature));
	}	
	
	@Test
	public void testGenerateExpirationTime() {
		// set up		
		long now = System.currentTimeMillis();
		String expirationTimeExpected = String.valueOf(now + GenericSignatureAuthenticationHolder.EXPIRATION_INTERVAL);
		
		Mockito.when(this.genericSignatureAuthenticationPlugin.getNow()).thenReturn(now);
		
		// exercise		
		String expirationTime = this.genericSignatureAuthenticationPlugin.generateExpirationTime();
		
		// verify
		Assert.assertEquals(expirationTimeExpected, expirationTime);
	}
	
	private String sign(String message) throws IOException, GeneralSecurityException {
		return RSAUtil.sign(this.privateKey, message);
	}
	
	private String getResourceFilePath(String filename) throws FileNotFoundException {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(filename).getFile());
		return file.getAbsolutePath();
	}
}
