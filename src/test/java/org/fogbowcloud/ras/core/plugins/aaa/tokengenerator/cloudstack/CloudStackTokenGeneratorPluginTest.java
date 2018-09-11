package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
    @Ignore
    public void testCreateToken() throws FogbowRasException, UnexpectedException {
        Map<String, String> tokenAttributes = new HashMap<String, String>();
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.USERNAME, "api");
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.PASSWORD, "key");
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.DOMAIN, "domain");
        String token = this.cloudStackTokenGeneratorPlugin.createTokenValue(tokenAttributes);

        Assert.assertEquals(VALID_TOKEN_VALUE, token);
    }

    @Test(expected = FogbowRasException.class)
    @Ignore
    public void testInvalidTokenParameters() throws FogbowRasException, UnexpectedException {
        Map<String, String> tokenAttributes = new HashMap<String, String>();
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.USERNAME, null);
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.PASSWORD, "key");
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.DOMAIN, "domain");

        String token = this.cloudStackTokenGeneratorPlugin.createTokenValue(tokenAttributes);
    }
}
