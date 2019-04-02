package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.models.OpenStackV3ScopedSystemUser;
import cloud.fogbow.as.core.models.OpenStackV3SystemUser;
import cloud.fogbow.as.core.systemidp.plugins.openstack.v3.OpenStackSystemIdentityProviderPlugin;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.plugins.cloudidp.openstack.v3.OpenStackIdentityProviderPlugin;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.mapper.all2one.OpenStackAllToOneMapper;
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
public class OpenStackOneToOneMapperTest {
    private static final String FAKE_ID1 = "fake-id1";
    private static final String FAKE_ID2 = "fake-id2";
    private static final String FAKE_USER_ID1 = "fake-user-id1";
    private static final String FAKE_USER_ID2 = "fake-user-id2";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USERNAME1 = "fake-username1";
    private static final String FAKE_USERNAME2 = "fake-username2";
    private static final String FAKE_PROJECT1 = "fake-project1";
    private static final String FAKE_PROJECT2 = "fake-project2";
    private static final String FAKE_MEMBER_ID1 = "fake-member-id1";
    private static final String FAKE_MEMBER_ID2 = "fake-member-id2";
    private static final String FAKE_TOKEN1 = "fake-token1";
    private static final String FAKE_TOKEN2 = "fake-token2";
    private static final String FAKE_TOKEN_VALUE = "fake-token";

    private OpenStackOneToOneMapper mapper;
    private OpenStackAllToOneMapper allToOneMapper;
    private OpenStackIdentityProviderPlugin openStackIdentityProviderPlugin;
    private String memberId;

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "cloudstack" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.openStackIdentityProviderPlugin = Mockito.spy(OpenStackIdentityProviderPlugin.class);
        this.allToOneMapper = new OpenStackAllToOneMapper(mapperConfPath);
        this.allToOneMapper.setIdentityProviderPlugin(this.openStackIdentityProviderPlugin);
        this.mapper = new OpenStackOneToOneMapper(mapperConfPath);
        this.mapper.setRemoteMapper(this.allToOneMapper);
    }

    //test case: two different OpenStackV3ScopedSystemUser objects should be mapped to two different OpenStackV3User objects
    @Test
    public void testCreate2TokensLocal() throws FogbowException {
        //set up
        OpenStackV3User cloudUser1 = new OpenStackV3User(FAKE_USER_ID1, FAKE_USERNAME1, FAKE_TOKEN1, FAKE_PROJECT1);
        OpenStackV3ScopedSystemUser systemUser1 = new OpenStackV3ScopedSystemUser(this.memberId, cloudUser1);
        OpenStackV3User cloudUser2 = new OpenStackV3User(FAKE_USER_ID2, FAKE_USERNAME2, FAKE_TOKEN2, FAKE_PROJECT2);
        OpenStackV3ScopedSystemUser systemUser2 = new OpenStackV3ScopedSystemUser(this.memberId, cloudUser2);

        //exercise
        OpenStackV3User mappedToken1 = (OpenStackV3User) this.mapper.map(systemUser1);
        OpenStackV3User mappedToken2 = (OpenStackV3User) this.mapper.map(systemUser2);

        //verify
        Assert.assertNotEquals(systemUser1.getToken(), systemUser2.getToken());
        Assert.assertNotEquals(mappedToken1.getToken(), mappedToken2.getToken());
        Assert.assertEquals(mappedToken1.getToken(), systemUser1.getToken());
        Assert.assertEquals(mappedToken2.getToken(), systemUser2.getToken());
    }

    @Test
    public void testCreate2TokensRemote() throws FogbowException {
        //set up
        OpenStackV3User cloudUser1 = new OpenStackV3User(FAKE_USER_ID1, FAKE_USERNAME1, FAKE_TOKEN1, FAKE_PROJECT1);
        OpenStackV3ScopedSystemUser token1 = new OpenStackV3ScopedSystemUser(FAKE_MEMBER_ID1, cloudUser1);
        OpenStackV3User cloudUser2 = new OpenStackV3User(FAKE_USER_ID2, FAKE_USERNAME2, FAKE_TOKEN2, FAKE_PROJECT2);
        OpenStackV3ScopedSystemUser token2 = new OpenStackV3ScopedSystemUser(FAKE_MEMBER_ID2, cloudUser2);

        OpenStackV3User cloudUser = new OpenStackV3User(FAKE_USER_ID, FAKE_USERNAME1, FAKE_TOKEN_VALUE, FAKE_PROJECT1);
        Mockito.doReturn(cloudUser).when(this.openStackIdentityProviderPlugin).getCloudUser(Mockito.anyMap());

        //exercise
        OpenStackV3User mappedToken1 = (OpenStackV3User) this.mapper.map(token1);
        OpenStackV3User mappedToken2 = (OpenStackV3User) this.mapper.map(token2);

        //verify
        Assert.assertNotEquals(token1.getToken(), token2.getToken());
        Assert.assertEquals(mappedToken1.getProjectId(), mappedToken2.getProjectId());
        Assert.assertEquals(mappedToken1.getToken(), mappedToken2.getToken());
    }
}
