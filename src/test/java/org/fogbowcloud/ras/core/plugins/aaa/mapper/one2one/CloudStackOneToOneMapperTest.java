package org.fogbowcloud.ras.core.plugins.aaa.mapper.one2one;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.aaa.identity.cloudstack.CloudStackIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.ListAccountsRequest;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.LoginRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlMatcher;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class})
public class CloudStackOneToOneMapperTest {
    private static final String FAKE_ID1 = "fake-id1";
    private static final String FAKE_ID2 = "fake-id2";
    private static final String FAKE_FIRST_NAME = "fake-first-name";
    private static final String FAKE_LAST_NAME = "fake-last-name";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_PASSWORD = "fake-password";
    private static final String FAKE_DOMAIN = "fake-domain";
    private static final String FAKE_SESSION_KEY = "fake-session-key";
    private static final String FAKE_TIMEOUT = "fake-timeout";
    private static final String JSON = "json";

    private static final String COMMAND_KEY = "command";
    private static final String RESPONSE_KEY = "response";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";
    private static final String DOMAIN_KEY = "domain";
    private static final String SESSION_KEY_KEY = "sessionkey";
    private static final String CLOUDSTACK_URL = "cloudstack_api_url";

    private static final String FAKE_API_KEY = "fake-api-key";
    private static final String FAKE_SECRET_KEY = "fake-secret-key";

    private CloudStackOneToOneMapper mapper;
    private HttpRequestClientUtil httpRequestClientUtil;
    private CloudStackTokenGeneratorPlugin cloudStackTokenGeneratorPlugin;
    private CloudStackIdentityPlugin cloudStackIdentityPlugin;
    private String memberId;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(HttpRequestUtil.class);

        this.mapper = new CloudStackOneToOneMapper();
        this.httpRequestClientUtil = Mockito.mock(HttpRequestClientUtil.class);
        this.cloudStackTokenGeneratorPlugin = Mockito.spy(new CloudStackTokenGeneratorPlugin());
        this.cloudStackTokenGeneratorPlugin.setClient(this.httpRequestClientUtil);
        this.cloudStackIdentityPlugin = new CloudStackIdentityPlugin();
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
    }

    //test case: two different Federation Tokens should be mapped to two different Tokens
    @Test
    public void testCreate2Tokens() throws UnexpectedException, FogbowRasException, UnexpectedException, HttpResponseException {
        //set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String listAccountsCommand = ListAccountsRequest.LIST_ACCOUNTS_COMMAND;
        String loginCommand = LoginRequest.LOGIN_COMMAND;

        String loginJsonResponse = getLoginResponse(FAKE_SESSION_KEY, FAKE_TIMEOUT);
        String accountJsonResponse1 = getAccountResponse(FAKE_ID1, FAKE_USERNAME, FAKE_FIRST_NAME, FAKE_LAST_NAME,
                FAKE_API_KEY, FAKE_SECRET_KEY);
        String accountJsonResponse2 = getAccountResponse(FAKE_ID2, FAKE_USERNAME, FAKE_FIRST_NAME, FAKE_LAST_NAME,
                FAKE_API_KEY, FAKE_SECRET_KEY);
        String expectedListAccountsRequestUrl = generateExpectedUrl(endpoint, listAccountsCommand,
                RESPONSE_KEY, JSON,
                SESSION_KEY_KEY, FAKE_SESSION_KEY);

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, loginCommand);
        expectedParams.put(RESPONSE_KEY, JSON);
        expectedParams.put(USERNAME_KEY, FAKE_USERNAME);
        expectedParams.put(PASSWORD_KEY, FAKE_PASSWORD);
        expectedParams.put(DOMAIN_KEY, FAKE_DOMAIN);
        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams);

        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString())).thenCallRealMethod();

        HttpRequestClientUtil.Response httpResponse = Mockito.mock(HttpRequestClientUtil.Response.class);
        Mockito.when(httpResponse.getContent()).thenReturn(loginJsonResponse);
        Mockito.when(this.httpRequestClientUtil.doPostRequest(Mockito.argThat(urlMatcher), Mockito.anyString()))
                .thenReturn(httpResponse);
        Mockito.when(this.httpRequestClientUtil.doGetRequest(Mockito.eq(expectedListAccountsRequestUrl), Mockito.any()))
                .thenReturn(accountJsonResponse1)
                .thenReturn(accountJsonResponse2);

        Map<String, String> userCredentials = new HashMap<String, String>();
        userCredentials.put(CloudStackTokenGeneratorPlugin.USERNAME, FAKE_USERNAME);
        userCredentials.put(CloudStackTokenGeneratorPlugin.PASSWORD, FAKE_PASSWORD);
        userCredentials.put(CloudStackTokenGeneratorPlugin.DOMAIN, FAKE_DOMAIN);

        String tokenValue1 = this.cloudStackTokenGeneratorPlugin.createTokenValue(userCredentials);
        String tokenValue2 = this.cloudStackTokenGeneratorPlugin.createTokenValue(userCredentials);
        CloudStackToken token1 = this.cloudStackIdentityPlugin.createToken(tokenValue1);
        CloudStackToken token2 = this.cloudStackIdentityPlugin.createToken(tokenValue2);

        //exercise
        CloudStackToken mappedToken1 = (CloudStackToken) this.mapper.map(token1);
        CloudStackToken mappedToken2 = (CloudStackToken) this.mapper.map(token2);

        //verify
        Assert.assertEquals(token1, mappedToken1);
        Assert.assertEquals(token2, mappedToken2);
        Assert.assertNotEquals(mappedToken1, mappedToken2);
    }

    private String getLoginResponse(String sessionKey, String timeout) {
        String response = "{\"loginresponse\":{"
                + "\"sessionkey\": \"%s\","
                + "\"timeout\": \"%s\""
                + "}}";

        return String.format(response, sessionKey, timeout);
    }

    private String getAccountResponse(String id, String username, String firstName, String lastName, String apiKey,
                                      String secretKey) {
        String response = "{\"listaccountsresponse\":{"
                + "\"account\":[{"
                + "\"user\":[{"
                + "\"id\": \"%s\","
                + "\"username\": \"%s\","
                + "\"firstname\": \"%s\","
                + "\"lastname\": \"%s\","
                + "\"apikey\": \"%s\","
                + "\"secretkey\": \"%s\""
                + "}]}]}}";

        return String.format(response, id, username, firstName, lastName, apiKey, secretKey);
    }

    private String generateExpectedUrl(String endpoint, String command, String... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            // there should be one value for each key
            return null;
        }

        String url = String.format("%s?command=%s", endpoint, command);
        for (int i = 0; i < keysAndValues.length; i += 2) {
            String key = keysAndValues[i];
            String value = keysAndValues[i + 1];
            url += String.format("&%s=%s", key, value);
        }

        return url;
    }

    private String getBaseEndpointFromCloudStackConf() {
        String filePath = HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;
        Properties properties = PropertiesUtil.readProperties(filePath);
        return properties.getProperty(CLOUDSTACK_URL);
    }
}
