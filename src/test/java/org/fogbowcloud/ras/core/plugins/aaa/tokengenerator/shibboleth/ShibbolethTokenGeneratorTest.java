package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.util.RSAUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class ShibbolethTokenGeneratorTest {

	private final String PRIVATE_KEY_SUFIX_PATH = "private/private.key";
	private final String PUBLIC_KEY_SUFIX_PATH = "private/public.key";
	private ShibbolethTokenGenerator shibbolethTokenGenerator;
	private RSAPrivateKey privateKey;
	private RSAPublicKey publicKey;
	
	@Before
	public void setUp() throws IOException, GeneralSecurityException {
		String privateKeyPath = getResourceFilePath(PRIVATE_KEY_SUFIX_PATH);
		String publicKeyPath = getResourceFilePath(PUBLIC_KEY_SUFIX_PATH);
		
		this.privateKey = RSAUtil.getPrivateKey(privateKeyPath);
		this.publicKey = RSAUtil.getPublicKey(publicKeyPath);
		
		this.shibbolethTokenGenerator = Mockito.spy(new ShibbolethTokenGenerator(this.privateKey, this.publicKey));		
	}

	// incomplete
	// test case:
	@Ignore
	@Test
	public void testCreateTokenValue() throws IOException, GeneralSecurityException, UnexpectedException, FogbowRasException {
		// setup		
		String secret = "secret";
		String assertionUrl = "http://localhost";
		String identityProvider = "idp_one";
		String eduPrincipalName = "fulano@ufcg.br";
		String commonName = "fulano";
		String samlAttributesStr = "[\"key\": \"valeu\"]";
		String shibToken = createShibToken(secret, assertionUrl, identityProvider, eduPrincipalName, commonName, samlAttributesStr);
		String shibTokenSignature = sign(shibToken);	
		
		Map<String, String> userCredentials = new HashMap<String, String>();
		userCredentials.put(ShibbolethTokenGenerator.TOKEN_CREDENTIAL, shibToken);
		userCredentials.put(ShibbolethTokenGenerator.TOKEN_SIGNATURE_CREDENTIAL, shibTokenSignature);
		
		// exercise
		String tokenValue = this.shibbolethTokenGenerator.createTokenValue(userCredentials);		
		
		//verify
	}
	
	private String createShibToken(String secret, String assertionUrl, String identityProvider, 
			String eduPrincipalName, String commonName, String samlAttributesStr) throws IOException, GeneralSecurityException {
		
		String[] parameters = new String[] {
				secret,
				assertionUrl,
				identityProvider,
				eduPrincipalName,
				commonName,
				samlAttributesStr
			};
		String rawShibToken = StringUtils.join(parameters, ShibbolethTokenGenerator.SHIBBOLETH_SEPARETOR);
		
		return RSAUtil.encrypt(rawShibToken, this.publicKey);
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
