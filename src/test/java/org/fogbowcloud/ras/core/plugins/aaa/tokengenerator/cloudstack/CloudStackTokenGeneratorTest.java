package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class CloudStackTokenGeneratorTest {

    private static final String VALID_TOKEN_VALUE = "api:key";

    private CloudStackTokenGenerator cloudStackTokenGenerator;

    @Before
    public void setUp() throws Exception {
        this.cloudStackTokenGenerator = Mockito.spy(new CloudStackTokenGenerator());
    }

    @Test
    public void testCreateToken() throws FogbowRasException {
        Map<String, String> tokenAttributes = new HashMap<String, String>();
        tokenAttributes.put(CloudStackTokenGenerator.API_KEY, "api");
        tokenAttributes.put(CloudStackTokenGenerator.SECRET_KEY, "key");
        String token = this.cloudStackTokenGenerator.createTokenValue(tokenAttributes);

        Assert.assertEquals(VALID_TOKEN_VALUE, token);
    }

    @Test(expected = FogbowRasException.class)
    public void testInvalidTokenParameters() throws FogbowRasException {
        Map<String, String> tokenAttributes = new HashMap<String, String>();
        tokenAttributes.put(CloudStackTokenGenerator.API_KEY, null);
        tokenAttributes.put(CloudStackTokenGenerator.SECRET_KEY, "key");

        String token = this.cloudStackTokenGenerator.createTokenValue(tokenAttributes);
    }
}
