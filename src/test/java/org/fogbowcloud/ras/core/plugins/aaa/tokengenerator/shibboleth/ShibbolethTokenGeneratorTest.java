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
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.util.RSAUtil;
import org.junit.Assert;
import org.junit.Before;
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

	// test case: success case
	@Test
	public void testCreateTokenValue() throws IOException, GeneralSecurityException, UnexpectedException, FogbowRasException {
		// setup		
		String secret = "secret";
		String assertionUrlExpected = "http://localhost";
		String identityProviderExpected = "idp_one";
		String eduPrincipalNameExpected = "fulano@ufcg.br";
		String commonNameExpected = "fulano";
		// TODO improve it!
		String samlAttributesStrExpected = "[\"key\": \"valeu\"]";
		String shibToken = createShibToken(secret, assertionUrlExpected, identityProviderExpected, eduPrincipalNameExpected, commonNameExpected, samlAttributesStrExpected);
		String shibTokenSignature = sign(shibToken);
		
		String expirationTokenExpected = "2136557867856";
		Mockito.doReturn(expirationTokenExpected).when(this.shibbolethTokenGenerator).generateExpirationTime();
		
		Map<String, String> userCredentials = new HashMap<String, String>();
		userCredentials.put(ShibbolethTokenGenerator.TOKEN_CREDENTIAL, shibToken);
		userCredentials.put(ShibbolethTokenGenerator.TOKEN_SIGNATURE_CREDENTIAL, shibTokenSignature);
		
		// exercise
		String tokenValue = this.shibbolethTokenGenerator.createTokenValue(userCredentials);
		
		//verify
		String[] tokenValueSlices = tokenValue.split(ShibbolethTokenGenerator.SHIBBOLETH_SEPARETOR);
		String assertionUrl = tokenValueSlices[0];
		String identityProvider = tokenValueSlices[1];
		String eduPrincipalName = tokenValueSlices[2];
		String commonName = tokenValueSlices[3];
		String samlAttributesStr = tokenValueSlices[4];
		String expirationTime = tokenValueSlices[5];
		String createRawRasToken = createRawRasToken(assertionUrl, identityProvider, 
				eduPrincipalName, commonName, samlAttributesStr, expirationTime);
		
		String signature = tokenValueSlices[6];		
		Assert.assertTrue(verify(createRawRasToken, signature));
		
		Assert.assertEquals(assertionUrlExpected, assertionUrl);
		Assert.assertEquals(eduPrincipalNameExpected, eduPrincipalName);
		Assert.assertEquals(samlAttributesStrExpected, samlAttributesStr);
		Assert.assertEquals(assertionUrlExpected, assertionUrl);
		Assert.assertEquals(expirationTokenExpected, expirationTime);
	}

	// test case: success case - secret is valid
	@Test
	public void testVerifySecretShibToken() throws UnauthenticatedUserException {
		// set up		
		String[] tokenShibParameters = new String[1];
		tokenShibParameters[ShibbolethTokenGenerator.SECREC_ATTR_SHIB_INDEX] = "secretOne";
				
		// exercise
		this.shibbolethTokenGenerator.verifySecretShibToken(tokenShibParameters);
	}
	
	// test case: secret invalid 
	@Test
	public void testVerifySecretShibTokenSecretInvalid() throws UnauthenticatedUserException {
		// set up 
		String[] tokenShibParameters = new String[1];
		tokenShibParameters[ShibbolethTokenGenerator.SECREC_ATTR_SHIB_INDEX] = "secretOne";
			
		// exercise
		this.shibbolethTokenGenerator.verifySecretShibToken(tokenShibParameters);
		
		try {
			
			this.shibbolethTokenGenerator.verifySecretShibToken(tokenShibParameters);
			
			// verify
			Assert.fail();
		} catch (UnauthenticatedUserException e) {}
	}	
	
	// test case: success case
	@Test
	public void testDecryptTokenShib() throws UnauthenticatedUserException, IOException, GeneralSecurityException {
		// set up
		String shibTokenRepresentation = "any_message";
		String shibTokenEncrypted = encrypt(shibTokenRepresentation);
		
		// exercise
		String shibTokenDecrypted = this.shibbolethTokenGenerator.decryptTokenShib(shibTokenEncrypted);
		
		// verify
		Assert.assertEquals(shibTokenRepresentation, shibTokenDecrypted);
	}
	
	// test case: when is not possible decrypt the message
	@Test(expected=UnauthenticatedUserException.class)
	public void testDecryptTokenShibException() throws UnauthenticatedUserException, IOException, GeneralSecurityException {
		// set up
		String wrongShibTokenRepresentation = "any_wrong_message";
		
		// exercise
		String shibTokenDecrypted = this.shibbolethTokenGenerator.decryptTokenShib(wrongShibTokenRepresentation);
		
		// verify
		Assert.assertEquals(wrongShibTokenRepresentation, shibTokenDecrypted);
	}
	
	private String createRawRasToken(String assertionUrl, String identityProvider, 
			String eduPrincipalName, String commonName, String samlAttributesStr, String expirationTime) {
		
		String[] parameters = new String[] {
				assertionUrl,
				identityProvider,
				eduPrincipalName,
				commonName,
				samlAttributesStr,
				expirationTime
			};
		String rawRasToken = StringUtils.join(parameters, ShibbolethTokenGenerator.SHIBBOLETH_SEPARETOR);
		return rawRasToken;
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
	
	private String encrypt(String message) throws IOException, GeneralSecurityException {
		return RSAUtil.encrypt(message, this.publicKey);
	}	
	
	private boolean verify(String message, String signature) throws IOException, GeneralSecurityException {
		return RSAUtil.verify(this.publicKey, message, signature);
	}	
	
	private String getResourceFilePath(String filename) throws FileNotFoundException {
		ClassLoader classLoader = getClass().getClassLoader();
		File file = new File(classLoader.getResource(filename).getFile());
		return file.getAbsolutePath();
	}
}
