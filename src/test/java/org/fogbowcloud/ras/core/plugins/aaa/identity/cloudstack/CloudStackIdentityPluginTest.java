package org.fogbowcloud.ras.core.plugins.aaa.identity.cloudstack;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class CloudStackIdentityPluginTest {
    private static final String FAKE_PROVIDER = "fake-provider";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_SIGNATURE = "fake-signature";

    private CloudStackIdentityPlugin identityPlugin;
    private CloudStackTokenGeneratorPlugin tokenGenerator;

    @Before
    public void setUp() {
        this.identityPlugin = new CloudStackIdentityPlugin();
        this.tokenGenerator = Mockito.spy(new CloudStackTokenGeneratorPlugin());
    }

    //test case: check if the token value information is correct when creating a token with the correct user credentials.
    @Test
    public void testCreateToken() throws Exception {
        //set up
        String fakeTokenValue = FAKE_PROVIDER + CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_STRING_SEPARATOR +
                FAKE_TOKEN_VALUE + CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_STRING_SEPARATOR + FAKE_USER_ID +
                CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_STRING_SEPARATOR + FAKE_USERNAME +
                CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_STRING_SEPARATOR + FAKE_SIGNATURE;
        Mockito.doReturn(fakeTokenValue).when(this.tokenGenerator).createTokenValue(Mockito.anyMap());
        Map<String, String> userCredentials = new HashMap<String, String>();
        userCredentials = new HashMap<String, String>();
        userCredentials.put(CloudStackTokenGeneratorPlugin.USERNAME, "username");
        userCredentials.put(CloudStackTokenGeneratorPlugin.PASSWORD, "password");
        userCredentials.put(CloudStackTokenGeneratorPlugin.DOMAIN, "domain");

        //exercise
        String federationTokenValue = this.tokenGenerator.createTokenValue(userCredentials);
        CloudStackToken token = this.identityPlugin.createToken(federationTokenValue);

        //verify
        String split[] = federationTokenValue.split(CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_STRING_SEPARATOR);
        Assert.assertEquals(split[0], FAKE_PROVIDER);
        Assert.assertEquals(split[1], FAKE_TOKEN_VALUE);
        Assert.assertEquals(split[2], FAKE_USER_ID);
        Assert.assertEquals(split[3], FAKE_USERNAME);
        Assert.assertEquals(split[4], FAKE_SIGNATURE);
    }

    //test case: check if createFederationTokenValue throws UnauthenticatedUserException when the user credentials
    // are invalid.
    @Test(expected = InvalidParameterException.class)
    public void testCreateTokenFail() throws Exception {
        //exercise/verify
        this.identityPlugin.createToken("anything");
    }
}
