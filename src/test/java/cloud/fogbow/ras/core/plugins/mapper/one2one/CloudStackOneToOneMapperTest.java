package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.federationidentity.plugins.cloudstack.CloudStackFederationIdentityProviderPlugin;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.FederationUserUtil;
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
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackFederationIdentityProviderPlugin.class})
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
    private CloudStackFederationIdentityProviderPlugin cloudStackTokenGenerator;
    private String memberId;

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "cloudstack" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.cloudStackTokenGenerator = Mockito.spy(CloudStackFederationIdentityProviderPlugin.class);
        this.allToOneMapper = new CloudStackAllToOneMapper(mapperConfPath);
        this.allToOneMapper.setFederationIdentityProviderPlugin(this.cloudStackTokenGenerator);
        this.mapper = new CloudStackOneToOneMapper(mapperConfPath);
        this.mapper.setRemoteMapper(this.allToOneMapper);
    }

    //test case: two different local Federation Tokens should be mapped to two different local Cloud Tokens
    @Test
    public void testCreate2TokensLocal() throws FogbowException, HttpResponseException {
        //set up
        FederationUser federationUser1 = new FederationUser(this.memberId, FAKE_ID1, FAKE_NAME1, FAKE_TOKEN1, new HashMap<>());
        FederationUser federationUser2 = new FederationUser(this.memberId, FAKE_ID2, FAKE_NAME2, FAKE_TOKEN2, new HashMap<>());

        //exercise
        CloudToken mappedToken1 = this.mapper.map(federationUser1);
        CloudToken mappedToken2 = this.mapper.map(federationUser2);

        //verify
        Assert.assertNotEquals(federationUser1.getTokenValue(), federationUser2.getTokenValue());
        Assert.assertEquals(mappedToken1.getTokenValue(), federationUser1.getTokenValue());
        Assert.assertEquals(mappedToken2.getTokenValue(), federationUser2.getTokenValue());
    }

    //test case: two different remote Federation Tokens should be mapped to one local Cloud Token
    @Test
    public void testCreate2TokensRemote() throws FogbowException, HttpResponseException {
        //set up
        FederationUser federationUser1 = new FederationUser(FAKE_MEMBER_ID1, FAKE_ID1, FAKE_NAME1, FAKE_TOKEN1, new HashMap<>());
        FederationUser federationUser2 = new FederationUser(FAKE_MEMBER_ID2, FAKE_ID2, FAKE_NAME2, FAKE_TOKEN2, new HashMap<>());

        FederationUser federationUser = new FederationUser(this.memberId, FAKE_USER_ID, FAKE_USERNAME, FAKE_TOKEN_VALUE, new HashMap<>());
        Mockito.doReturn(federationUser).when(this.cloudStackTokenGenerator).getFederationUser(Mockito.anyMap());

        //exercise
        CloudToken mappedToken1 = this.mapper.map(federationUser1);
        CloudToken mappedToken2 = this.mapper.map(federationUser2);

        //verify
        Assert.assertNotEquals(federationUser1.getTokenValue(), federationUser2.getTokenValue());
        Assert.assertEquals(mappedToken1.getUserId(), mappedToken2.getUserId());
        Assert.assertEquals(mappedToken1.getTokenValue(), mappedToken2.getTokenValue());
    }
}
