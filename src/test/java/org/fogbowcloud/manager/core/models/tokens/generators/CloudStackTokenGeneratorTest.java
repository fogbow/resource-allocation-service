package org.fogbowcloud.manager.core.models.tokens.generators;

import java.util.HashMap;
import java.util.Map;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.models.tokens.generators.cloudstack.CloudStackTokenGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CloudStackTokenGeneratorTest {

    private static final String VALID_TOKEN_VALUE = "api:key";
    
    private CloudStackTokenGenerator cloudStackTokenGenerator;
    
    @Before
    public void setUp() throws Exception {
        HomeDir.getInstance().setPath("src/test/resources/private");
        this.cloudStackTokenGenerator = Mockito.spy(new CloudStackTokenGenerator());
    }
    
    @Test
    public void testCreateToken() throws FogbowManagerException {
        Map<String, String> tokenAttributes = new HashMap<String, String>();
        tokenAttributes.put(CloudStackTokenGenerator.API_KEY, "api");
        tokenAttributes.put(CloudStackTokenGenerator.SECRET_KEY, "key");
        Token token = this.cloudStackTokenGenerator.createToken(tokenAttributes);

        Assert.assertEquals(VALID_TOKEN_VALUE, token.getTokenValue());
    }

    @Test (expected = FogbowManagerException.class)
    public void testInvalidTokenParameters() throws FogbowManagerException {
        Map<String, String> tokenAttributes = new HashMap<String, String>();
        tokenAttributes.put(CloudStackTokenGenerator.API_KEY, null);
        tokenAttributes.put(CloudStackTokenGenerator.SECRET_KEY, "key");

        Token token = this.cloudStackTokenGenerator.createToken(tokenAttributes);
    }
}
