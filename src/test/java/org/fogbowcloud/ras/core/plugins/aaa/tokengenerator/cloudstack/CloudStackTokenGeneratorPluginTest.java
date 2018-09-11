package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class CloudStackTokenGeneratorPluginTest {

    private static final String VALID_TOKEN_VALUE = "api:key";

    private CloudStackTokenGeneratorPlugin cloudStackTokenGeneratorPlugin;

    @Before
    public void setUp() throws Exception {
        this.cloudStackTokenGeneratorPlugin = Mockito.spy(new CloudStackTokenGeneratorPlugin());
    }

    @Test
    public void testCreateToken() throws FogbowRasException {
        Map<String, String> tokenAttributes = new HashMap<String, String>();
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.API_KEY, "api");
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.SECRET_KEY, "key");
        String token = this.cloudStackTokenGeneratorPlugin.createTokenValue(tokenAttributes);

        Assert.assertEquals(VALID_TOKEN_VALUE, token);
    }

    @Test(expected = FogbowRasException.class)
    public void testInvalidTokenParameters() throws FogbowRasException {
        Map<String, String> tokenAttributes = new HashMap<String, String>();
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.API_KEY, null);
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.SECRET_KEY, "key");

        String token = this.cloudStackTokenGeneratorPlugin.createTokenValue(tokenAttributes);
    }
}
