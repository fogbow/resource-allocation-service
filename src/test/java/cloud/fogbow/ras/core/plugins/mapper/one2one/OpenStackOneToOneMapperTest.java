package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.tokengenerator.plugins.AttributeJoiner;
import cloud.fogbow.as.core.tokengenerator.plugins.openstack.v3.OpenStackTokenGeneratorPlugin;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
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
@PrepareForTest({OpenStackTokenGeneratorPlugin.class})
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
    private static final String FAKE_PROJECT = "fake-project";
    private static final String FAKE_TOKEN1 = "fake-token1";
    private static final String FAKE_TOKEN2 = "fake-token2";
    private static final String FAKE_MEMBER_ID1 = "fake-member-id1";
    private static final String FAKE_MEMBER_ID2 = "fake-member-id2";
    private static final String FAKE_TOKEN_VALUE = "fake-token";

    private static final String ID_KEY = "id";
    private static final String PROVIDER_KEY = "provider";
    private static final String NAME_KEY = "name";
    private static final String PROJECT_KEY = "project";
    private static final String TOKEN_KEY = "token";

    private OpenStackOneToOneMapper mapper;
    private OpenStackAllToOneMapper allToOneMapper;
    private OpenStackTokenGeneratorPlugin keystoneV3TokenGenerator;
    private String memberId;

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "cloudstack" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.keystoneV3TokenGenerator = Mockito.spy(OpenStackTokenGeneratorPlugin.class);
        this.allToOneMapper = new OpenStackAllToOneMapper(mapperConfPath);
        this.allToOneMapper.setTokenGeneratorPlugin(this.keystoneV3TokenGenerator);
        this.mapper = new OpenStackOneToOneMapper(mapperConfPath);
        this.mapper.setRemoteMapper(this.allToOneMapper);
    }

    //test case: two different local Federation Tokens should be mapped to two different local Cloud Tokens
    @Test
    public void testCreate2TokensLocal() throws FogbowException {
        //set up
        Map<String, String> attributes1 = new HashMap();
        attributes1.put(PROVIDER_KEY, this.memberId);
        attributes1.put(ID_KEY, FAKE_USER_ID1);
        attributes1.put(NAME_KEY, FAKE_USERNAME1);
        attributes1.put(PROJECT_KEY, FAKE_PROJECT1);
        attributes1.put(TOKEN_KEY, FAKE_TOKEN1);
        String tokenValue1  = AttributeJoiner.join(attributes1);

        Map<String, String> userCredentials1 = new HashMap<String, String>();
        userCredentials1.put(ID_KEY, FAKE_ID1);
        userCredentials1.put(PROVIDER_KEY, this.memberId);
        userCredentials1.put(PROJECT_KEY, FAKE_PROJECT1);
        userCredentials1.put(TOKEN_KEY, tokenValue1);
        FederationUser token1 = new FederationUser(userCredentials1);

        Map<String, String> attributes2 = new HashMap();
        attributes2.put(PROVIDER_KEY, this.memberId);
        attributes2.put(ID_KEY, FAKE_USER_ID2);
        attributes2.put(NAME_KEY, FAKE_USERNAME2);
        attributes2.put(PROJECT_KEY, FAKE_PROJECT2);
        attributes2.put(TOKEN_KEY, FAKE_TOKEN2);
        String tokenValue2  = AttributeJoiner.join(attributes2);

        Map<String, String> userCredentials2 = new HashMap<String, String>();
        userCredentials2.put(ID_KEY, FAKE_ID2);
        userCredentials2.put(PROVIDER_KEY, this.memberId);
        userCredentials2.put(PROJECT_KEY, FAKE_PROJECT2);
        userCredentials2.put(TOKEN_KEY, tokenValue2);
        FederationUser token2 = new FederationUser(userCredentials2);

        //exercise
        OpenStackV3Token mappedToken1 = this.mapper.map(token1);
        OpenStackV3Token mappedToken2 = this.mapper.map(token2);

        //verify
        Assert.assertNotEquals(token1.getTokenValue(), token2.getTokenValue());
        Assert.assertEquals(mappedToken1.getTokenValue(), token1.getTokenValue());
        Assert.assertEquals(mappedToken2.getTokenValue(), token2.getTokenValue());
    }

    @Test
    public void testCreate2TokensRemote() throws FogbowException {
        //set up
        Map<String, String> attributes1 = new HashMap();
        attributes1.put(PROVIDER_KEY, FAKE_MEMBER_ID1);
        attributes1.put(ID_KEY, FAKE_USER_ID1);
        attributes1.put(NAME_KEY, FAKE_USERNAME1);
        attributes1.put(PROJECT_KEY, FAKE_PROJECT1);
        attributes1.put(TOKEN_KEY, FAKE_TOKEN1);
        String tokenValue1  = AttributeJoiner.join(attributes1);

        Map<String, String> userCredentials1 = new HashMap<String, String>();
        userCredentials1.put(ID_KEY, FAKE_ID1);
        userCredentials1.put(PROVIDER_KEY, FAKE_MEMBER_ID1);
        userCredentials1.put(PROJECT_KEY, FAKE_PROJECT1);
        userCredentials1.put(TOKEN_KEY, tokenValue1);
        FederationUser token1 = new FederationUser(userCredentials1);

        Map<String, String> attributes2 = new HashMap();
        attributes2.put(PROVIDER_KEY, FAKE_MEMBER_ID2);
        attributes2.put(ID_KEY, FAKE_USER_ID2);
        attributes2.put(NAME_KEY, FAKE_USERNAME2);
        attributes2.put(PROJECT_KEY, FAKE_PROJECT2);
        attributes2.put(TOKEN_KEY, FAKE_TOKEN2);
        String tokenValue2  = AttributeJoiner.join(attributes2);

        Map<String, String> userCredentials2 = new HashMap<String, String>();
        userCredentials2.put(ID_KEY, FAKE_ID2);
        userCredentials2.put(PROVIDER_KEY, FAKE_MEMBER_ID2);
        userCredentials2.put(PROJECT_KEY, FAKE_PROJECT2);
        userCredentials2.put(TOKEN_KEY, tokenValue2);
        FederationUser token2 = new FederationUser(userCredentials2);

        Map<String, String> attributes = new HashMap();
        attributes.put(ID_KEY, FAKE_USER_ID);
        attributes.put(PROVIDER_KEY, this.memberId);
        attributes.put(PROJECT_KEY, FAKE_PROJECT);
        attributes.put(TOKEN_KEY, FAKE_TOKEN_VALUE);
        String tokenValue = AttributeJoiner.join(attributes);

        Mockito.doReturn(tokenValue).when(this.keystoneV3TokenGenerator).createTokenValue(Mockito.anyMap());

        //exercise
        OpenStackV3Token mappedToken1 = this.mapper.map(token1);
        OpenStackV3Token mappedToken2 = this.mapper.map(token2);

        //verify
        Assert.assertNotEquals(token1.getTokenValue(), token2.getTokenValue());
        Assert.assertEquals(mappedToken1.getProjectId(), mappedToken2.getProjectId());
        Assert.assertEquals(mappedToken1.getTokenValue(), mappedToken2.getTokenValue());
    }
}
