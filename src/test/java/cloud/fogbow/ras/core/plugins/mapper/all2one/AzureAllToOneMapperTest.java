package cloud.fogbow.ras.core.plugins.mapper.all2one;

import cloud.fogbow.as.core.models.AzureSystemUser;
import cloud.fogbow.as.core.systemidp.plugins.azure.AzureSystemIdentityProviderPlugin;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.plugins.cloudidp.azure.AzureIdentityProviderPlugin;
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
@PrepareForTest({AzureSystemIdentityProviderPlugin.class})
public class AzureAllToOneMapperTest {
    private String providerId;
    private AzureIdentityProviderPlugin identityProviderPlugin;
    private AzureAllToOneMapper mapper;

    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_USER_NAME = "fake-user-name";

    private static final String FAKE_TENANT_ID = "fake-tenant-id";
    private static final String FAKE_CLIENT_KEY = "fake-client-key";
    private static final String FAKE_SUBSCRIPTION_ID = "fake-subscription-id";
    private static final String FAKE_RESOURCE_GROUP_NAME = "fake-resource-group-name";
    private static final String FAKE_REGION_NAME = "fake-region-name";

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.providerId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
        this.identityProviderPlugin = Mockito.spy(AzureIdentityProviderPlugin.class);
        this.mapper = new AzureAllToOneMapper(mapperConfPath);
        this.mapper.setIdentityProviderPlugin(this.identityProviderPlugin);
    }

    //test case: two different SystemUser objects should be mapped to the same AzureUser object
    @Test
    public void testCreate2Tokens() throws FogbowException {
        //set up
        AzureSystemUser user1 = Mockito.mock(AzureSystemUser.class);
        AzureSystemUser user2 = Mockito.mock(AzureSystemUser.class);

        AzureUser expectedUser = new AzureUser(FAKE_USER_ID, FAKE_USER_NAME, this.providerId,
                FAKE_TENANT_ID, FAKE_CLIENT_KEY, FAKE_SUBSCRIPTION_ID, FAKE_RESOURCE_GROUP_NAME, FAKE_REGION_NAME);
        Mockito.doReturn(expectedUser).when(this.identityProviderPlugin).getCloudUser(Mockito.anyMap());

        //exercise
        AzureUser mappedToken1 = this.mapper.map(user1);
        AzureUser mappedToken2 = this.mapper.map(user2);

        //verify
        Assert.assertNotEquals(user1, user2);
        Assert.assertEquals(expectedUser.getId(), mappedToken1.getId());
        Assert.assertEquals(expectedUser.getToken(), mappedToken1.getToken());

        Assert.assertEquals(expectedUser.getId(), mappedToken2.getId());
        Assert.assertEquals(expectedUser.getToken(), mappedToken2.getToken());
    }
}
