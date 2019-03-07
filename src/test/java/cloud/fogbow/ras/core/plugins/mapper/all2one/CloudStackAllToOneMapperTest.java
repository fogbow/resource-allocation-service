package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.systemidp.plugins.cloudstack.CloudStackSystemIdentityProviderPlugin;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.cloudidp.cloudstack.CloudStackIdentityProviderPlugin;
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
@PrepareForTest({CloudStackSystemIdentityProviderPlugin.class})
public class CloudStackAllToOneMapperTest {
    private static final String FAKE_LOGIN1 = "fake-login1";
    private static final String FAKE_LOGIN2 = "fake-login2";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";

    private String memberId;
    private CloudStackAllToOneMapper mapper;
    private CloudStackIdentityProviderPlugin cloudStackIdentityProviderPlugin;

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "cloudstack" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.cloudStackIdentityProviderPlugin = Mockito.spy(CloudStackIdentityProviderPlugin.class);
        this.mapper = new CloudStackAllToOneMapper(mapperConfPath);
        this.mapper.setIdentityProviderPlugin(this.cloudStackIdentityProviderPlugin);
    }

    //test case: two different SystemUser should be mapped to the same CloudStackUser
    @Test
    public void testCreate2Tokens() throws FogbowException {
        //set up
        SystemUser user1 = new SystemUser(FAKE_LOGIN1, FAKE_LOGIN1, this.memberId);
        SystemUser user2 = new SystemUser(FAKE_LOGIN2, FAKE_LOGIN2, this.memberId);

        CloudStackUser systemUser = new CloudStackUser(FAKE_USER_ID, FAKE_USER_NAME, FAKE_TOKEN_VALUE);

        Mockito.doReturn(systemUser).when(this.cloudStackIdentityProviderPlugin).getCloudUser(Mockito.anyMap());

        //exercise
        CloudUser mappedToken1 = this.mapper.map(user1);
        CloudUser mappedToken2 = this.mapper.map(user2);

        //verify
        Assert.assertNotEquals(user1, user2);
        Assert.assertEquals(mappedToken1.getId(), mappedToken2.getId());
        Assert.assertEquals(mappedToken1.getToken(), mappedToken2.getToken());
    }
}
