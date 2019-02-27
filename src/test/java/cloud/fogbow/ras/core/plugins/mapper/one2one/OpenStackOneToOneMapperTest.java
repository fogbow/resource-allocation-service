package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.federationidentity.plugins.openstack.v3.OpenStackFederationIdentityProviderPlugin;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.FederationUserUtil;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2.OpenStackComputePlugin;
import cloud.fogbow.ras.core.plugins.mapper.all2one.OpenStackAllToOneMapper;
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
    private static final String FAKE_TOKEN1 = "fake-token1";
    private static final String FAKE_TOKEN2 = "fake-token2";
    private static final String FAKE_TOKEN_VALUE = "fake-token";

    private OpenStackOneToOneMapper mapper;
    private OpenStackAllToOneMapper allToOneMapper;
    private OpenStackFederationIdentityProviderPlugin keystoneV3TokenGenerator;
    private String memberId;

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "cloudstack" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.keystoneV3TokenGenerator = Mockito.spy(OpenStackFederationIdentityProviderPlugin.class);
        this.allToOneMapper = new OpenStackAllToOneMapper(mapperConfPath);
        this.allToOneMapper.setFederationIdentityProviderPlugin(this.keystoneV3TokenGenerator);
        this.mapper = new OpenStackOneToOneMapper(mapperConfPath);
        this.mapper.setRemoteMapper(this.allToOneMapper);
    }

    //test case: two different local Federation Tokens should be mapped to two different local Cloud Tokens
    @Test
    public void testCreate2TokensLocal() throws FogbowException {
        //set up
        HashMap<String, String> extraAttributes1 = new HashMap<>();
        extraAttributes1.put(OpenStackComputePlugin.PROJECT_ID, FAKE_PROJECT1);
        FederationUser federationUser1 = new FederationUser(this.memberId, FAKE_USER_ID1, FAKE_USERNAME1, FAKE_TOKEN1, extraAttributes1);

        HashMap<String, String> extraAttributes2 = new HashMap<>();
        extraAttributes2.put(OpenStackComputePlugin.PROJECT_ID, FAKE_PROJECT2);
        FederationUser federationUser2 = new FederationUser(this.memberId, FAKE_USER_ID2, FAKE_USERNAME2, FAKE_TOKEN2, extraAttributes2);

        //exercise
        OpenStackV3Token mappedToken1 = this.mapper.map(federationUser1);
        OpenStackV3Token mappedToken2 = this.mapper.map(federationUser2);

        //verify
        Assert.assertNotEquals(federationUser1.getTokenValue(), federationUser2.getTokenValue());
        Assert.assertEquals(mappedToken1.getTokenValue(), federationUser1.getTokenValue());
        Assert.assertEquals(mappedToken2.getTokenValue(), federationUser2.getTokenValue());
    }

    @Test
    public void testCreate2TokensRemote() throws FogbowException {
        //set up
        HashMap<String, String> extraAttributes1 = new HashMap<>();
        extraAttributes1.put(OpenStackComputePlugin.PROJECT_ID, FAKE_PROJECT1);
        FederationUser token1 = new FederationUser(this.memberId, FAKE_USER_ID1, FAKE_USERNAME1, FAKE_TOKEN1, extraAttributes1);

        HashMap<String, String> extraAttributes2 = new HashMap<>();
        extraAttributes2.put(OpenStackComputePlugin.PROJECT_ID, FAKE_PROJECT2);
        FederationUser token2 = new FederationUser(this.memberId, FAKE_USER_ID2, FAKE_USERNAME2, FAKE_TOKEN2, extraAttributes2);

        FederationUser federationUser = new FederationUser(this.memberId, FAKE_USER_ID, FAKE_USERNAME1, FAKE_TOKEN_VALUE, new HashMap<>());
        Mockito.doReturn(federationUser).when(this.keystoneV3TokenGenerator).getFederationUser(Mockito.anyMap());

        //exercise
        OpenStackV3Token mappedToken1 = this.mapper.map(token1);
        OpenStackV3Token mappedToken2 = this.mapper.map(token2);

        //verify
        Assert.assertNotEquals(token1.getTokenValue(), token2.getTokenValue());
        Assert.assertEquals(mappedToken1.getProjectId(), mappedToken2.getProjectId());
        Assert.assertEquals(mappedToken1.getTokenValue(), mappedToken2.getTokenValue());
    }
}
