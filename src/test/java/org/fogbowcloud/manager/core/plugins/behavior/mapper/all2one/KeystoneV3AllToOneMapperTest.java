package org.fogbowcloud.manager.core.plugins.behavior.mapper.all2one;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.LdapToken;
import org.fogbowcloud.manager.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.manager.core.models.tokens.generators.ldap.LdapTokenGeneratorPlugin;
import org.fogbowcloud.manager.core.models.tokens.generators.openstack.v3.KeystoneV3TokenGeneratorPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.identity.ldap.LdapFederationIdentityPlugin;
import org.fogbowcloud.manager.core.plugins.behavior.identity.openstack.KeystoneV3IdentityPlugin;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class KeystoneV3AllToOneMapperTest {
    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(KeystoneV3AllToOneMapperTest.class);

    private static final String FAKE_NAME1 = "fake-name1";
    private static final String FAKE_LOGIN1 = "fake-login1";
    private static final String FAKE_NAME2 = "fake-name2";
    private static final String FAKE_LOGIN2 = "fake-login2";
    private static final String FAKE_PASSWORD = "fake-password";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_PROJECT_NAME = "fake-project-name";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";

    private String memberId;
    private HttpClient client;
    private HttpRequestClientUtil httpRequestClientUtil;
    private KeystoneV3AllToOneMapper mapper;
    private LdapTokenGeneratorPlugin ldapTokenGenerator;
    private LdapFederationIdentityPlugin ldapIdentityPlugin;
    private KeystoneV3TokenGeneratorPlugin keystoneV3TokenGenerator;
    private KeystoneV3IdentityPlugin keystoneV3IdentityPlugin;

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        this.ldapTokenGenerator = Mockito.spy(new LdapTokenGeneratorPlugin());
        this.ldapIdentityPlugin = new LdapFederationIdentityPlugin();
        this.client = Mockito.spy(HttpClient.class);
        this.httpRequestClientUtil = Mockito.spy(new HttpRequestClientUtil(this.client));
        this.keystoneV3TokenGenerator = Mockito.spy(new KeystoneV3TokenGeneratorPlugin());
        this.keystoneV3TokenGenerator.setClient(this.httpRequestClientUtil);
        this.keystoneV3IdentityPlugin = new KeystoneV3IdentityPlugin();
        GenericAllToOneFederationToLocalMapper genericMapper = new
                GenericAllToOneFederationToLocalMapper(keystoneV3TokenGenerator, keystoneV3IdentityPlugin,
                "keystone-v3-mapper.conf");
        this.mapper = new KeystoneV3AllToOneMapper();
        this.mapper.setGenericMapper(genericMapper);
    }

    //test case: two different Federation Tokens should be mapped to the same Local Token
    @Test
    public void testCreate2Tokens() throws java.io.IOException, UnexpectedException, FogbowManagerException {
        //set up
        Map<String, String> userCredentials1 = new HashMap<String, String>();
        userCredentials1.put(LdapTokenGeneratorPlugin.CRED_USERNAME, FAKE_LOGIN1);
        userCredentials1.put(LdapTokenGeneratorPlugin.CRED_PASSWORD, FAKE_PASSWORD);
        Mockito.doReturn(FAKE_NAME1).when(this.ldapTokenGenerator).
                ldapAuthenticate(Mockito.eq(FAKE_LOGIN1), Mockito.eq(FAKE_PASSWORD));
        String tokenValue1 = this.ldapTokenGenerator.createTokenValue(userCredentials1);
        LdapToken token1 = this.ldapIdentityPlugin.createToken(tokenValue1);
        LOGGER.debug("token1: " + token1.getTokenValue() + " token value1: " + tokenValue1);

        Map<String, String> userCredentials2 = new HashMap<String, String>();
        userCredentials2.put(LdapTokenGeneratorPlugin.CRED_USERNAME, FAKE_LOGIN2);
        userCredentials2.put(LdapTokenGeneratorPlugin.CRED_PASSWORD, FAKE_PASSWORD);
        Mockito.doReturn(FAKE_NAME2).when(ldapTokenGenerator).
                ldapAuthenticate(Mockito.eq(FAKE_LOGIN2), Mockito.eq(FAKE_PASSWORD));
        String tokenValue2 = this.ldapTokenGenerator.createTokenValue(userCredentials2);
        LdapToken token2 = this.ldapIdentityPlugin.createToken(tokenValue2);
        LOGGER.debug("token2: " + token2.getTokenValue() + " token value2: " + tokenValue2);

        String tokenValue = this.memberId + KeystoneV3TokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR +
                FAKE_TOKEN_VALUE + KeystoneV3TokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR +
                FAKE_USER_ID + KeystoneV3TokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR +
                FAKE_USER_NAME + KeystoneV3TokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR +
                FAKE_PROJECT_ID + KeystoneV3TokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR + FAKE_PROJECT_NAME;

        Mockito.doReturn(tokenValue).when(this.keystoneV3TokenGenerator).createTokenValue(Mockito.anyMap());

        //exercise
        OpenStackV3Token mappedToken1 = (OpenStackV3Token) this.mapper.map(token1);
        LOGGER.debug("token1: " + token1.getTokenValue() + " mapped: " + mappedToken1.getTokenValue());
        OpenStackV3Token mappedToken2 = (OpenStackV3Token) this.mapper.map(token2);
        LOGGER.debug("token2: " + token2.getTokenValue() + " mapped: " + mappedToken2.getTokenValue());

        //verify
        Assert.assertNotEquals(token1, token2);
        Assert.assertEquals(mappedToken1.getUserId(), mappedToken2.getUserId());
        Assert.assertEquals(mappedToken1.getUserName(), mappedToken2.getUserName());
    }
}
