package org.fogbowcloud.ras.core.plugins.aaa.authentication.generic;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;

import org.fogbowcloud.ras.core.exceptions.UnauthenticTokenException;
import org.fogbowcloud.ras.core.models.tokens.GenericSignatureToken;
import org.fogbowcloud.ras.util.RSAUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

// TODO implements
public class GenericSignatureAuthenticationPluginTest {

	private final String PRIVATE_KEY_SUFIX_PATH = "private/private.key";
	private RSAPrivateKey privateKey;
	
	private GenericSignatureAuthenticationPlugin genericSignatureAuthenticationPlugin;

	@Before
	public void setUp() {
	}
	
	// case: success case
	@Test
	public void test() throws UnauthenticTokenException {
		// set up
		GenericSignatureToken genericSignatureToken = Mockito.mock(GenericSignatureToken.class);
		long expirationTime = System.currentTimeMillis();
		Mockito.doReturn(expirationTime).when(genericSignatureToken).getExpirationTime();
		String rawToken = "rawToken"; 
		Mockito.doReturn(rawToken).when(genericSignatureToken).getRawToken();
		Mockito.doReturn(rawToken).when(genericSignatureToken).getRawTokenSignature();
				
		this.genericSignatureAuthenticationPlugin.checkTokenValue(genericSignatureToken);		
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
