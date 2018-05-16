package org.fogbowcloud.manager.core.services;

import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

public class AuthenticationServiceUtilTest {

	private String CREDENTIALS_PREFIX = AuthenticationServiceUtil.LOCAL_TOKEN_CREDENTIALS_PREFIX;
	
	@Test
	public void testGetDefaultLocalTokenCredentials() {
		Properties properties = new Properties();
		String keyOne = "one";
		String keyTwo = "two";
		String keyThree = "Three";
		String valueOne = "valueOne";
		String valueTwo = "valueTwo";
		String valueThree = "valueThree";
		properties.put(CREDENTIALS_PREFIX + keyOne, valueOne);
		properties.put(CREDENTIALS_PREFIX + keyTwo, valueTwo);
		properties.put(keyThree, valueThree);
		
		Map<String, String> defaulLocalTokenCredentials = 
				AuthenticationServiceUtil.getDefaultLocalTokenCredentials(properties);
		Assert.assertEquals(valueOne, defaulLocalTokenCredentials.get(keyOne));
		Assert.assertEquals(valueTwo, defaulLocalTokenCredentials.get(keyTwo));
		Assert.assertNull(defaulLocalTokenCredentials.get(keyThree));
	}
	
	@Test
	public void testGetDefaultLocalTokenCredentialsWithPropertiesNull() {
		Map<String, String> defaulLocalTokenCredentials = 
				AuthenticationServiceUtil.getDefaultLocalTokenCredentials(null);
		Assert.assertTrue(defaulLocalTokenCredentials.isEmpty());
	}
	
}
