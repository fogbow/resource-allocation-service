package org.fogbowcloud.ras.core.plugins.aaa.authentication.generic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;

import org.fogbowcloud.ras.util.RSAUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class GenericSignatureAuthenticationHolderTest {

	private final String PRIVATE_KEY_SUFIX_PATH = "private/private.key";
	
	private GenericSignatureAuthenticationHolder genericSignatureAuthenticationPlugin;

	@Before
	public void setUp() throws IOException, GeneralSecurityException {
		this.genericSignatureAuthenticationPlugin = Mockito.spy(new GenericSignatureAuthenticationHolder());
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
	
	private String getResourceFilePath(String filename) throws FileNotFoundException {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(filename).getFile());
		return file.getAbsolutePath();
	}
}
