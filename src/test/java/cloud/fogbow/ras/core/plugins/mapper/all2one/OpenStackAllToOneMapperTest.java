package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.models.OpenStackV3SystemUser;
import cloud.fogbow.as.core.systemidp.plugins.openstack.v3.OpenStackSystemIdentityProviderPlugin;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.plugins.cloudidp.openstack.v3.OpenStackIdentityProviderPlugin;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenStackSystemIdentityProviderPlugin.class})
public class OpenStackAllToOneMapperTest {
    private static final String FAKE_LOGIN1 = "fake-login1";
    private static final String FAKE_LOGIN2 = "fake-login2";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";

    private String providerId;
    private OpenStackIdentityProviderPlugin openStackIdentityProviderPlugin;
    private OpenStackAllToOneMapper mapper;

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.providerId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);
        this.openStackIdentityProviderPlugin = Mockito.spy(OpenStackIdentityProviderPlugin.class);
        this.mapper = new OpenStackAllToOneMapper(mapperConfPath);
        this.mapper.setIdentityProviderPlugin(this.openStackIdentityProviderPlugin);
    }

    //test case: two different SystemUser objects should be mapped to the same OpenStackV3User object
    @Test
    public void testCreate2Tokens() throws FogbowException {
        //set up
        OpenStackV3User openStackV3User1 = new OpenStackV3User(FAKE_LOGIN1, FAKE_USER_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);
        OpenStackV3SystemUser user1 = new OpenStackV3SystemUser(this.providerId, openStackV3User1);
        OpenStackV3User openStackV3User2 = new OpenStackV3User(FAKE_LOGIN2, FAKE_USER_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);
        OpenStackV3SystemUser user2 = new OpenStackV3SystemUser(this.providerId, openStackV3User2);

        OpenStackV3User expectedUser = new OpenStackV3User(FAKE_USER_ID, FAKE_USER_NAME, this.providerId, FAKE_TOKEN_VALUE);
        Mockito.doReturn(expectedUser).when(this.openStackIdentityProviderPlugin).getCloudUser(Mockito.anyMap());

        //exercise
        OpenStackV3User mappedToken1 = (OpenStackV3User) this.mapper.map(user1);
        OpenStackV3User mappedToken2 = (OpenStackV3User) this.mapper.map(user2);

        //verify
        Assert.assertNotEquals(user1, user2);
        Assert.assertEquals(mappedToken1.getId(), mappedToken2.getId());
        Assert.assertEquals(mappedToken1.getToken(), mappedToken2.getToken());
    }
}
