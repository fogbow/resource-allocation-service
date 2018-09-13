package org.fogbowcloud.ras.core.plugins.aaa.mapper.all2one;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.LdapToken;
import org.fogbowcloud.ras.core.plugins.aaa.identity.cloudstack.CloudStackIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.identity.ldap.LdapFederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap.LdapTokenGeneratorPlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class CloudStackAllToOneMapperTest {
    private static final String FAKE_NAME1 = "fake-name1";
    private static final String FAKE_LOGIN1 = "fake-login1";
    private static final String FAKE_NAME2 = "fake-name2";
    private static final String FAKE_LOGIN2 = "fake-login2";
    private static final String FAKE_PASSWORD = "fake-password";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";

    private String memberId;
    private CloudStackAllToOneMapper mapper;
    private LdapTokenGeneratorPlugin ldapTokenGenerator;
    private LdapFederationIdentityPlugin ldapIdentityPlugin;
    private CloudStackTokenGeneratorPlugin cloudStackTokenGenerator;

    @Before
    public void setUp() {
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
        this.ldapTokenGenerator = Mockito.spy(new LdapTokenGeneratorPlugin());
        this.ldapIdentityPlugin = new LdapFederationIdentityPlugin();
        this.cloudStackTokenGenerator = Mockito.spy(new CloudStackTokenGeneratorPlugin());
        GenericAllToOneFederationToLocalMapper genericMapper = new
                GenericAllToOneFederationToLocalMapper(cloudStackTokenGenerator, new CloudStackIdentityPlugin(),
                "cloudstack-mapper.conf");
        this.mapper = new CloudStackAllToOneMapper();
        this.mapper.setGenericMapper(genericMapper);
    }

    //test case: two different Federation Tokens should be mapped to the same Local Token
    @Test
    public void testCreate2Tokens() throws UnexpectedException, FogbowRasException {
        //set up
        Map<String, String> userCredentials1 = new HashMap<String, String>();
        userCredentials1.put(LdapTokenGeneratorPlugin.CRED_USERNAME, FAKE_LOGIN1);
        userCredentials1.put(LdapTokenGeneratorPlugin.CRED_PASSWORD, FAKE_PASSWORD);
        Mockito.doReturn(FAKE_NAME1).when(this.ldapTokenGenerator).
                ldapAuthenticate(Mockito.eq(FAKE_LOGIN1), Mockito.eq(FAKE_PASSWORD));
        String tokenValue1 = this.ldapTokenGenerator.createTokenValue(userCredentials1);
        LdapToken token1 = this.ldapIdentityPlugin.createToken(tokenValue1);

        Map<String, String> userCredentials2 = new HashMap<String, String>();
        userCredentials2.put(LdapTokenGeneratorPlugin.CRED_USERNAME, FAKE_LOGIN2);
        userCredentials2.put(LdapTokenGeneratorPlugin.CRED_PASSWORD, FAKE_PASSWORD);
        Mockito.doReturn(FAKE_NAME2).when(ldapTokenGenerator).
                ldapAuthenticate(Mockito.eq(FAKE_LOGIN2), Mockito.eq(FAKE_PASSWORD));
        String tokenValue2 = this.ldapTokenGenerator.createTokenValue(userCredentials2);
        LdapToken token2 = this.ldapIdentityPlugin.createToken(tokenValue2);

        String tokenValue = this.memberId + CloudStackTokenGeneratorPlugin.TOKEN_STRING_SEPARATOR +
                FAKE_TOKEN_VALUE + CloudStackTokenGeneratorPlugin.TOKEN_STRING_SEPARATOR +
                FAKE_USER_ID + CloudStackTokenGeneratorPlugin.TOKEN_STRING_SEPARATOR +
                FAKE_USER_NAME + CloudStackTokenGeneratorPlugin.TOKEN_STRING_SEPARATOR;

        Mockito.doReturn(tokenValue).when(this.cloudStackTokenGenerator).createTokenValue(Mockito.anyMap());

        //exercise
        CloudStackToken mappedToken1 = (CloudStackToken) this.mapper.map(token1);
        CloudStackToken mappedToken2 = (CloudStackToken) this.mapper.map(token2);

        //verify
        Assert.assertNotEquals(token1, token2);
        Assert.assertEquals(mappedToken1.getUserId(), mappedToken2.getUserId());
        Assert.assertEquals(mappedToken1.getUserName(), mappedToken2.getUserName());
    }
}
