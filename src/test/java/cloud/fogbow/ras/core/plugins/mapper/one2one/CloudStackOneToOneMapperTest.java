package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.as.core.tokengenerator.plugins.AttributeJoiner;
import cloud.fogbow.as.core.tokengenerator.plugins.cloudstack.CloudStackTokenGeneratorPlugin;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
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
@PrepareForTest({CloudStackTokenGeneratorPlugin.class})
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

    private static final String ID_KEY = "id";
    private static final String PROVIDER_KEY = "provider";
    private static final String NAME_KEY = "name";
    private static final String TOKEN_KEY = "token";

    private CloudStackOneToOneMapper mapper;
    private CloudStackAllToOneMapper allToOneMapper;
    private CloudStackTokenGeneratorPlugin cloudStackTokenGenerator;
    private String memberId;

    @Before
    public void setUp() {
        String path = HomeDir.getPath();
        String mapperConfPath = path + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "cloudstack" + File.separator + SystemConstants.MAPPER_CONF_FILE_NAME;

        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_MEMBER_ID_KEY);
        this.cloudStackTokenGenerator = Mockito.spy(CloudStackTokenGeneratorPlugin.class);
        this.allToOneMapper = new CloudStackAllToOneMapper(mapperConfPath);
        this.allToOneMapper.setTokenGeneratorPlugin(this.cloudStackTokenGenerator);
        this.mapper = new CloudStackOneToOneMapper(mapperConfPath);
        this.mapper.setRemoteMapper(this.allToOneMapper);
    }

    //test case: two different local Federation Tokens should be mapped to two different local Cloud Tokens
    @Test
    public void testCreate2TokensLocal() throws FogbowException, HttpResponseException {
        //set up
        Map<String, String> fedUser1Attrs = new HashMap();
        fedUser1Attrs.put(PROVIDER_KEY, this.memberId);
        fedUser1Attrs.put(ID_KEY, FAKE_ID1);
        fedUser1Attrs.put(NAME_KEY, FAKE_NAME1);
        fedUser1Attrs.put(TOKEN_KEY, FAKE_TOKEN1);
        String tokenValue1 = AttributeJoiner.join(fedUser1Attrs);

        Map<String, String> userCredentials1 = new HashMap<String, String>();
        userCredentials1.put(ID_KEY, FAKE_ID1);
        userCredentials1.put(PROVIDER_KEY, this.memberId);
        userCredentials1.put(TOKEN_KEY, tokenValue1);
        FederationUser token1 = new FederationUser(userCredentials1);

        Map<String, String> fedUser2Attrs = new HashMap();
        fedUser2Attrs.put(PROVIDER_KEY, this.memberId);
        fedUser2Attrs.put(ID_KEY, FAKE_ID2);
        fedUser2Attrs.put(NAME_KEY, FAKE_NAME2);
        fedUser2Attrs.put(TOKEN_KEY, FAKE_TOKEN2);
        String tokenValue2 = AttributeJoiner.join(fedUser2Attrs);

        Map<String, String> userCredentials2 = new HashMap<String, String>();
        userCredentials2.put(ID_KEY, FAKE_ID2);
        userCredentials2.put(PROVIDER_KEY, this.memberId);
        userCredentials2.put(TOKEN_KEY, tokenValue2);
        FederationUser token2 = new FederationUser(userCredentials2);

        //exercise
        CloudToken mappedToken1 = this.mapper.map(token1);
        CloudToken mappedToken2 = this.mapper.map(token2);

        //verify
        Assert.assertNotEquals(token1.getTokenValue(), token2.getTokenValue());
        Assert.assertEquals(mappedToken1.getTokenValue(), token1.getTokenValue());
        Assert.assertEquals(mappedToken2.getTokenValue(), token2.getTokenValue());
    }

    //test case: two different remote Federation Tokens should be mapped to one local Cloud Token
    @Test
    public void testCreate2TokensRemote() throws FogbowException, HttpResponseException {
        //set up
        Map<String, String> fedUser1Attrs = new HashMap();
        fedUser1Attrs.put(PROVIDER_KEY, FAKE_MEMBER_ID1);
        fedUser1Attrs.put(ID_KEY, FAKE_ID1);
        fedUser1Attrs.put(NAME_KEY, FAKE_NAME1);
        fedUser1Attrs.put(TOKEN_KEY, FAKE_TOKEN1);
        String tokenValue1 = AttributeJoiner.join(fedUser1Attrs);

        Map<String, String> userCredentials1 = new HashMap<String, String>();
        userCredentials1.put(ID_KEY, FAKE_ID1);
        userCredentials1.put(PROVIDER_KEY, FAKE_MEMBER_ID1);
        userCredentials1.put(TOKEN_KEY, tokenValue1);
        FederationUser token1 = new FederationUser(userCredentials1);

        Map<String, String> fedUser2Attrs = new HashMap();
        fedUser2Attrs.put(PROVIDER_KEY, FAKE_MEMBER_ID2);
        fedUser2Attrs.put(ID_KEY, FAKE_ID2);
        fedUser2Attrs.put(NAME_KEY, FAKE_NAME2);
        fedUser2Attrs.put(TOKEN_KEY, FAKE_TOKEN2);
        String tokenValue2 = AttributeJoiner.join(fedUser2Attrs);

        Map<String, String> userCredentials2 = new HashMap<String, String>();
        userCredentials2.put(ID_KEY, FAKE_ID2);
        userCredentials2.put(PROVIDER_KEY, FAKE_MEMBER_ID2);
        userCredentials2.put(TOKEN_KEY, tokenValue2);
        FederationUser token2 = new FederationUser(userCredentials2);

        Map<String, String> attributes = new HashMap();
        attributes.put(PROVIDER_KEY, this.memberId);
        attributes.put(ID_KEY, FAKE_USER_ID);
        attributes.put(NAME_KEY, FAKE_USERNAME);
        attributes.put(TOKEN_KEY, FAKE_TOKEN_VALUE);
        String tokenValue = AttributeJoiner.join(attributes);

        Mockito.doReturn(tokenValue).when(this.cloudStackTokenGenerator).createTokenValue(Mockito.anyMap());

        //exercise
        CloudToken mappedToken1 = this.mapper.map(token1);
        CloudToken mappedToken2 = this.mapper.map(token2);

        //verify
        Assert.assertNotEquals(token1.getTokenValue(), token2.getTokenValue());
        Assert.assertEquals(mappedToken1.getUserId(), mappedToken2.getUserId());
        Assert.assertEquals(mappedToken1.getTokenValue(), mappedToken2.getTokenValue());
    }
}
