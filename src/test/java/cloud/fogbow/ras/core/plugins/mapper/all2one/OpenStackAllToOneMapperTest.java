package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.federationidentity.plugins.openstack.v3.OpenStackFederationIdentityProviderPlugin;
import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.FederationUserUtil;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenStackFederationIdentityProviderPlugin.class})
public class OpenStackAllToOneMapperTest {
    private static final String FAKE_LOGIN1 = "fake-login1";
    private static final String FAKE_LOGIN2 = "fake-login2";
    private static final String FAKE_PASSWORD = "fake-password";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";

    private static final String FAKE_USER_NAME_KEY = "username";
    private static final String FAKE_PASSWORD_KEY = "password";

    private String memberId;
    private OpenStackFederationIdentityProviderPlugin keystoneV3TokenGenerator;
    private OpenStackAllToOneMapper mapper;

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.keystoneV3TokenGenerator = Mockito.spy(OpenStackFederationIdentityProviderPlugin.class);
        this.mapper = new OpenStackAllToOneMapper(mapperConfPath);
        this.mapper.setFederationIdentityProviderPlugin(this.keystoneV3TokenGenerator);
    }

    //test case: two different Federation Tokens should be mapped to the same Local Token
    @Test
    public void testCreate2Tokens() throws FogbowException {
        //set up
        Map<String, String> userCredentials1 = new HashMap<String, String>();
        userCredentials1.put(FAKE_USER_NAME_KEY, FAKE_LOGIN1);
        userCredentials1.put(FAKE_PASSWORD_KEY, FAKE_PASSWORD);
        FederationUser token1 = new FederationUser("fake-token-provider", FAKE_LOGIN1,
                FAKE_LOGIN1, "fake-federation-token-value", new HashMap<>());;

        Map<String, String> userCredentials2 = new HashMap<String, String>();
        userCredentials2.put(FAKE_USER_NAME_KEY, FAKE_LOGIN2);
        userCredentials2.put(FAKE_PASSWORD_KEY, FAKE_PASSWORD);
        FederationUser token2 = new FederationUser("fake-token-provider", FAKE_LOGIN2,
                FAKE_LOGIN2, "fake-federation-token-value", new HashMap<>());;

        Map<String, String> extraAttributes = new HashMap();
        extraAttributes.put(OpenStackConstants.Identity.PROJECT_KEY_JSON, FAKE_PROJECT_ID);

        FederationUser expectedUser = new FederationUser(this.memberId, FAKE_USER_ID, FAKE_USER_NAME, FAKE_TOKEN_VALUE, extraAttributes);
        Mockito.doReturn(expectedUser).when(this.keystoneV3TokenGenerator).getFederationUser(Mockito.anyMap());

        //exercise
        OpenStackV3Token mappedToken1 = (OpenStackV3Token) this.mapper.map(token1);
        OpenStackV3Token mappedToken2 = (OpenStackV3Token) this.mapper.map(token2);

        //verify
        Assert.assertNotEquals(token1.getExtraAttributes(), token2.getExtraAttributes());
        Assert.assertEquals(mappedToken1.getUserId(), mappedToken2.getUserId());
        Assert.assertEquals(mappedToken1.getTokenValue(), mappedToken2.getTokenValue());
    }
}
