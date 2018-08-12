package org.fogbowcloud.manager.core.plugins.behavior.identity.openstack;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.models.tokens.generators.openstack.v3.KeystoneV3TokenGenerator;
import org.fogbowcloud.manager.core.plugins.behavior.authentication.openstack.KeystoneV3AuthenticationPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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

    private static final String TOKEN_VALUE_SEPARATOR = "!#!";
    private KeystoneV3IdentityPlugin identityPlugin;
    private KeystoneV3AuthenticationPlugin authenticationPlugin;
    private KeystoneV3TokenGenerator tokenGenerator;

    private String providerId;

    Map<String, String> userCredentials = new HashMap<String, String>();

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");

        this.providerId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        this.userCredentials = new HashMap<String, String>();
        this.userCredentials.put(KeystoneV3TokenGenerator.USER_ID, "userId");
        this.userCredentials.put(KeystoneV3TokenGenerator.PASSWORD, "userPass");
        this.userCredentials.put(KeystoneV3TokenGenerator.PROJECT_ID, "projectId");
        this.userCredentials.put(KeystoneV3TokenGenerator.AUTH_URL, "http://localhost");

        this.identityPlugin = new KeystoneV3IdentityPlugin();
        this.tokenGenerator = Mockito.spy(new KeystoneV3TokenGenerator());
        this.identityPlugin.setKeystoneV3TokenGenerator(this.tokenGenerator);
        this.authenticationPlugin = new KeystoneV3AuthenticationPlugin();
    }

    //test case: check if the token value information is correct when creating a token with the correct user credentials.
    @Test
    public void testCreateToken() throws Exception {
        //set up
        Token fakeToken = new OpenStackV3Token(FAKE_PROVIDER, FAKE_TOKEN_VALUE, FAKE_USER_ID, FAKE_NAME,
                FAKE_PROJECT_ID, FAKE_PROJECT_NAME);
        Mockito.doReturn(fakeToken).when(this.tokenGenerator).createToken(Mockito.anyMap());

        //exercise
        String federationTokenValue = this.identityPlugin.createTokenValue(userCredentials);
        OpenStackV3Token token = (OpenStackV3Token) this.identityPlugin.createToken(federationTokenValue);

        String split[] = federationTokenValue.split(TOKEN_VALUE_SEPARATOR);

        //verify
        Assert.assertEquals(split[0], FAKE_PROVIDER);
        Assert.assertEquals(split[1], FAKE_TOKEN_VALUE);
        Assert.assertEquals(split[2], FAKE_USER_ID);
        Assert.assertEquals(split[3], FAKE_NAME);
        Assert.assertEquals(split[4], FAKE_PROJECT_ID);
        Assert.assertEquals(split[5], FAKE_PROJECT_NAME);
        Assert.assertTrue(this.authenticationPlugin.isAuthentic(token));
    }

    //test case: check if createFederationTokenValue throws UnauthenticatedUserException when the token value is invalid
    @Test (expected = UnauthenticatedUserException.class)
    public void testCreateTokenFail() throws Exception {
        //set up

        //exercise/verify
        this.identityPlugin.createToken("anything");
    }
}
