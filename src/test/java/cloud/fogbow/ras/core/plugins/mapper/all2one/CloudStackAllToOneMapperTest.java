package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.models.CloudStackSystemUser;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.plugins.cloudidp.cloudstack.v4_9.CloudStackIdentityProviderPlugin;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CloudStackAllToOneMapperTest {
    private static final String FAKE_LOGIN1 = "fake-login1";
    private static final String FAKE_LOGIN2 = "fake-login2";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_DOMAIN = "fake-domain";
    private static final String FAKE_TOKEN_VALUE = "fake-api-key:fake-secret-key";
    private static final String FAKE_CLOUDSTACK_URL = "http://localhost:8080";
    private static final Map<String, String> FAKE_COOKIE_HEADER = new HashMap<>();

    private String providerId;
    private CloudStackAllToOneMapper mapper;
    private CloudStackIdentityProviderPlugin cloudStackIdentityProviderPlugin;

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "cloudstack" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.providerId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
        this.cloudStackIdentityProviderPlugin = Mockito.spy(new CloudStackIdentityProviderPlugin(FAKE_CLOUDSTACK_URL));
        this.mapper = new CloudStackAllToOneMapper(mapperConfPath);
        this.mapper.setIdentityProviderPlugin(this.cloudStackIdentityProviderPlugin);
    }

    //test case: two different SystemUser should be mapped to the same CloudStackUser
    @Test
    public void testCreate2Tokens() throws FogbowException {
        //set up
        CloudStackUser cloudStackUser1 = new CloudStackUser(FAKE_LOGIN1, FAKE_USER_NAME, FAKE_TOKEN_VALUE, FAKE_DOMAIN, FAKE_COOKIE_HEADER);
        CloudStackSystemUser user1 = new CloudStackSystemUser(this.providerId, cloudStackUser1);
        CloudStackUser cloudStackUser2 = new CloudStackUser(FAKE_LOGIN2, FAKE_USER_NAME, FAKE_TOKEN_VALUE, FAKE_DOMAIN, FAKE_COOKIE_HEADER);
        CloudStackSystemUser user2 = new CloudStackSystemUser(this.providerId, cloudStackUser2);

        CloudStackUser systemUser = new CloudStackUser(FAKE_USER_ID, FAKE_USER_NAME, FAKE_TOKEN_VALUE, FAKE_DOMAIN, FAKE_COOKIE_HEADER);

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
