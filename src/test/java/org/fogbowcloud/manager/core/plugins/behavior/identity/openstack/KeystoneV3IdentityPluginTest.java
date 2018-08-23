package org.fogbowcloud.manager.core.plugins.behavior.identity.openstack;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.manager.core.models.tokens.generators.openstack.v3.KeystoneV3TokenGenerator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class KeystoneV3IdentityPluginTest {
    private static final String FAKE_PROVIDER = "fake-provider";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_PROJECT_NAME = "fake-project-name";

    private KeystoneV3IdentityPlugin identityPlugin;
    private KeystoneV3TokenGenerator tokenGenerator;

    @Before
    public void setUp() {
        this.identityPlugin = new KeystoneV3IdentityPlugin();
        this.tokenGenerator = Mockito.spy(new KeystoneV3TokenGenerator());
    }

    //test case: check if the token value information is correct when creating a token with the correct user credentials.
    @Test
    public void testCreateToken() throws Exception {
        //set up
        String fakeTokenValue = FAKE_PROVIDER + KeystoneV3TokenGenerator.TOKEN_VALUE_SEPARATOR + FAKE_TOKEN_VALUE +
                KeystoneV3TokenGenerator.TOKEN_VALUE_SEPARATOR + FAKE_USER_ID +
                KeystoneV3TokenGenerator.TOKEN_VALUE_SEPARATOR + FAKE_NAME +
                KeystoneV3TokenGenerator.TOKEN_VALUE_SEPARATOR + FAKE_PROJECT_ID +
                KeystoneV3TokenGenerator.TOKEN_VALUE_SEPARATOR + FAKE_PROJECT_NAME;
        Mockito.doReturn(fakeTokenValue).when(this.tokenGenerator).createTokenValue(Mockito.anyMap());
        Map<String, String> userCredentials = new HashMap<String, String>();
        userCredentials = new HashMap<String, String>();
        userCredentials.put(KeystoneV3TokenGenerator.USER_ID, "userId");
        userCredentials.put(KeystoneV3TokenGenerator.PASSWORD, "userPass");
        userCredentials.put(KeystoneV3TokenGenerator.PROJECT_ID, "projectId");
        userCredentials.put(KeystoneV3TokenGenerator.AUTH_URL, "http://localhost");

        //exercise
        String federationTokenValue = this.tokenGenerator.createTokenValue(userCredentials);
        OpenStackV3Token token = this.identityPlugin.createToken(federationTokenValue);

        //verify
        String split[] = federationTokenValue.split(KeystoneV3TokenGenerator.TOKEN_VALUE_SEPARATOR);
        Assert.assertEquals(split[0], FAKE_PROVIDER);
        Assert.assertEquals(split[1], FAKE_TOKEN_VALUE);
        Assert.assertEquals(split[2], FAKE_USER_ID);
        Assert.assertEquals(split[3], FAKE_NAME);
        Assert.assertEquals(split[4], FAKE_PROJECT_ID);
        Assert.assertEquals(split[5], FAKE_PROJECT_NAME);
    }

    //test case: check if createFederationTokenValue throws UnauthenticatedUserException when the user credentials
    // are invalid.
    @Test (expected = InvalidParameterException.class)
    public void testCreateTokenFail() throws Exception {
        //exercise/verify
        this.identityPlugin.createToken("anything");
    }
}
