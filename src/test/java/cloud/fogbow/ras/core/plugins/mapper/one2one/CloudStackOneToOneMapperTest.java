package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.models.CloudStackSystemUser;
import cloud.fogbow.as.core.models.OneToOneMappableSystemUser;
import cloud.fogbow.as.core.systemidp.plugins.cloudstack.CloudStackSystemIdentityProviderPlugin;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.plugins.cloudidp.cloudstack.CloudStackIdentityProviderPlugin;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.mapper.all2one.CloudStackAllToOneMapper;
import org.apache.http.client.HttpResponseException;
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
public class CloudStackOneToOneMapperTest {
    private static final String FAKE_ID1 = "fake-id1";
    private static final String FAKE_ID2 = "fake-id2";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME1 = "fake-name1";
    private static final String FAKE_NAME2 = "fake-name2";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_TOKEN1 = "fake-token1";
    private static final String FAKE_TOKEN2 = "fake-token2";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final String FAKE_MEMBER_ID1 = "fake-member-id1";
    private static final String FAKE_MEMBER_ID2 = "fake-member-id2";

    private CloudStackOneToOneMapper mapper;
    private CloudStackAllToOneMapper allToOneMapper;
    private CloudStackIdentityProviderPlugin cloudStackIdentityProviderPlugin;
    private String memberId;

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "cloudstack" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.cloudStackIdentityProviderPlugin = Mockito.spy(CloudStackIdentityProviderPlugin.class);
        this.allToOneMapper = new CloudStackAllToOneMapper(mapperConfPath);
        this.allToOneMapper.setIdentityProviderPlugin(this.cloudStackIdentityProviderPlugin);
        this.mapper = new CloudStackOneToOneMapper(mapperConfPath);
        this.mapper.setRemoteMapper(this.allToOneMapper);
    }

    //test case: two different SystemUser objects should be mapped to two different CloudUser objects
    @Test
    public void testCreate2TokensLocal() throws FogbowException {
        //set up
        CloudStackUser cloudUser1 = new CloudStackUser(FAKE_ID1, FAKE_NAME1, FAKE_TOKEN1);
        CloudStackSystemUser systemUser1 = new CloudStackSystemUser(this.memberId, cloudUser1);
        CloudStackUser cloudUser2 = new CloudStackUser(FAKE_ID2, FAKE_NAME2, FAKE_TOKEN2);
        CloudStackSystemUser systemUser2 = new CloudStackSystemUser(this.memberId, cloudUser2);
        //exercise
        CloudStackUser mappedToken1 = (CloudStackUser) this.mapper.map(systemUser1);
        CloudStackUser mappedToken2 = (CloudStackUser) this.mapper.map(systemUser2);

        //verify
        Assert.assertNotEquals(systemUser1.getToken(), systemUser2.getToken());
        Assert.assertEquals(mappedToken1.getToken(), systemUser1.getToken());
        Assert.assertEquals(mappedToken2.getToken(), systemUser2.getToken());
    }

    //test case: two different SystemUser objects should be mapped to two different CloudUser objects
    @Test(expected = InvalidParameterException.class)
    public void testCreateCloudUserWithTypeMismatch() throws FogbowException {
        //set up
        OneToOneMappableSystemUser systemUser1 = new OneToOneMappableSystemUser(FAKE_ID1, FAKE_NAME1, this.memberId, FAKE_TOKEN1);

        //exercise
        CloudStackUser mappedToken1 = (CloudStackUser) this.mapper.map(systemUser1);
    }

    //test case: two different remote SystemUser objects should be mapped to the same CloudUser object
    @Test
    public void testCreate2TokensRemote() throws FogbowException, HttpResponseException {
        //set up
        OneToOneMappableSystemUser systemUser1 = new OneToOneMappableSystemUser(FAKE_ID1, FAKE_NAME1, FAKE_MEMBER_ID1, FAKE_TOKEN1);
        OneToOneMappableSystemUser systemUser2 = new OneToOneMappableSystemUser(FAKE_ID2, FAKE_NAME2, FAKE_MEMBER_ID2, FAKE_TOKEN2);

        CloudStackUser cloudUser = new CloudStackUser(FAKE_USER_ID, FAKE_USERNAME, FAKE_TOKEN_VALUE);
        Mockito.doReturn(cloudUser).when(this.cloudStackIdentityProviderPlugin).getCloudUser(Mockito.anyMap());

        //exercise
        CloudStackUser mappedToken1 = (CloudStackUser) this.mapper.map(systemUser1);
        CloudStackUser mappedToken2 = (CloudStackUser) this.mapper.map(systemUser2);

        //verify
        Assert.assertNotEquals(systemUser1.getToken(), systemUser2.getToken());
        Assert.assertEquals(mappedToken1.getId(), mappedToken2.getId());
        Assert.assertEquals(mappedToken1.getToken(), mappedToken2.getToken());
    }
}
