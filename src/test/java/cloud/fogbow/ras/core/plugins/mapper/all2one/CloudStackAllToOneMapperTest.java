package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.federationidentity.plugins.cloudstack.CloudStackFederationIdentityProviderPlugin;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.FederationUserUtil;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
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
@PrepareForTest({CloudStackFederationIdentityProviderPlugin.class})
public class CloudStackAllToOneMapperTest {
    private static final String FAKE_LOGIN1 = "fake-login1";
    private static final String FAKE_LOGIN2 = "fake-login2";
    private static final String FAKE_PASSWORD_KEY = "password";
    private static final String FAKE_PASSWORD = "fake-password";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME_KEY = "username";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";

    private String memberId;
    private CloudStackAllToOneMapper mapper;
    private CloudStackFederationIdentityProviderPlugin cloudStackTokenGenerator;

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "cloudstack" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.cloudStackTokenGenerator = Mockito.spy(CloudStackFederationIdentityProviderPlugin.class);
        this.mapper = new CloudStackAllToOneMapper(mapperConfPath);
        this.mapper.setFederationIdentityProviderPlugin(this.cloudStackTokenGenerator);
    }

    //test case: two different Federation Tokens should be mapped to the same Local Token
    @Test
    public void testCreate2Tokens() throws FogbowException {
        //set up
        FederationUser user1 = new FederationUser(this.memberId, FAKE_LOGIN1, FAKE_LOGIN1, null, new HashMap<>());
        FederationUser user2 = new FederationUser(this.memberId, FAKE_LOGIN2, FAKE_LOGIN2, null, new HashMap<>());

        FederationUser federationUser = new FederationUser(this.memberId, FAKE_USER_ID, FAKE_USER_NAME, FAKE_TOKEN_VALUE, new HashMap<>());

        Mockito.doReturn(federationUser).when(this.cloudStackTokenGenerator).getFederationUser(Mockito.anyMap());

        //exercise
        CloudToken mappedToken1 = this.mapper.map(user1);
        CloudToken mappedToken2 = this.mapper.map(user2);

        //verify
        Assert.assertNotEquals(user1, user2);
        Assert.assertEquals(mappedToken1.getUserId(), mappedToken2.getUserId());
        Assert.assertEquals(mappedToken1.getTokenValue(), mappedToken2.getTokenValue());
    }
}
