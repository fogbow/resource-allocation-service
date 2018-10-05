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
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.util.RSAUtil;
import org.json.JSONObject;
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
	private String tokenProviderId;
	
	@Before
	public void setUp() throws IOException, GeneralSecurityException {
		this.tokenProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
		
		String privateKeyPath = getResourceFilePath(PRIVATE_KEY_SUFIX_PATH);
		String publicKeyPath = getResourceFilePath(PUBLIC_KEY_SUFIX_PATH);
		
		this.privateKey = RSAUtil.getPrivateKey(privateKeyPath);
		this.publicKey = RSAUtil.getPublicKey(publicKeyPath);
		
		this.shibbolethTokenGenerator = Mockito.spy(new ShibbolethTokenGenerator());		
	}

	// test case: success case
	@Test
	public void testCreateTokenValue() throws IOException, GeneralSecurityException, UnexpectedException, FogbowRasException {
		// setup		
		String secret = "secret";
		String assertionUrlExpected = "http://localhost";
		String eduPrincipalNameExpected = "fulano@ufcg.br";
		String commonNameExpected = "fulano";
		String samlAttributesStrExpected = createSamlAttributes();
		String shibAppToken = createShibToken(secret, assertionUrlExpected, eduPrincipalNameExpected, commonNameExpected, samlAttributesStrExpected);
		String shibAppTokenSignature = sign(shibAppToken);
		
		String expirationTokenExpected = "2136557867856";
		Mockito.doReturn(expirationTokenExpected).when(this.shibbolethTokenGenerator).generateExpirationTime();
		
		Map<String, String> userCredentials = new HashMap<String, String>();
		userCredentials.put(ShibbolethTokenGenerator.TOKEN_CREDENTIAL, shibAppToken);
		userCredentials.put(ShibbolethTokenGenerator.TOKEN_SIGNATURE_CREDENTIAL, shibAppTokenSignature);
		
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
		Assert.assertEquals(this.tokenProviderId, identityProvider);
		Assert.assertEquals(eduPrincipalNameExpected, eduPrincipalName);
		Assert.assertEquals(samlAttributesStrExpected, samlAttributesStr);
		Assert.assertEquals(assertionUrlExpected, assertionUrl);
		Assert.assertEquals(expirationTokenExpected, expirationTime);
	}

	private String createSamlAttributes() {
		Map<String, String> attributes = new HashMap<>();
		attributes.put("keyOne", "valueOne");
		attributes.put("keyTwo", "valueTwo");
		return new JSONObject(attributes).toString();
	}

	// test case: success case - secret is valid
	@Test
	public void testVerifySecretShibToken() throws UnauthenticatedUserException {
		// set up		
		String[] tokenShibParameters = new String[1];
		tokenShibParameters[ShibbolethTokenGenerator.SECREC_ATTR_SHIB_INDEX] = "secretOne";
				
		// exercise
		this.shibbolethTokenGenerator.verifySecretShibAppToken(tokenShibParameters);
	}
	
	// test case: secret invalid 
	@Test
	public void testVerifySecretShibTokenSecretInvalid() throws UnauthenticatedUserException {
		// set up 
		String[] tokenShibParameters = new String[1];
		tokenShibParameters[ShibbolethTokenGenerator.SECREC_ATTR_SHIB_INDEX] = "secretOne";
			
		// exercise
		this.shibbolethTokenGenerator.verifySecretShibAppToken(tokenShibParameters);
		
		try {
			this.shibbolethTokenGenerator.verifySecretShibAppToken(tokenShibParameters);
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
	
	// test case: Shibboleth App token is not in a correct format
	@Test(expected=UnauthenticatedUserException.class)
	public void testCheckTokenFormat() throws UnauthenticatedUserException {
		// set up
		String[] tokenShibParameters = new String[1];
		
		// exercise e verify
		this.shibbolethTokenGenerator.checkTokenFormat(tokenShibParameters);
	}

	// test case: Signature of Shibboleth App is worng
	@Test(expected=UnauthenticatedUserException.class)
	public void testVerifyShibAppTokenAuthenticity() throws UnauthenticatedUserException {
		// set up
		String tokenShibApp = "anything";
		String tokenSignature = "anything";
		
		// exercise e verify
		this.shibbolethTokenGenerator.verifyShibAppTokenAuthenticity(tokenSignature, tokenShibApp);
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
	
	private String createShibToken(String secret, String assertionUrl, String eduPrincipalName,
			String commonName, String samlAttributesStr) throws IOException, GeneralSecurityException {
		
		String[] parameters = new String[] {
				secret,
				assertionUrl,
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
