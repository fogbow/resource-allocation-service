package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicStatusLine;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlMatcher;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class})
public class CloudStackTokenGeneratorPluginTest {
    private static final String FAKE_ID = "fake-id";
    private static final String FAKE_FIRST_NAME = "fake-first-name";
    private static final String FAKE_LAST_NAME = "fake-last-name";
    private static final String FAKE_USERNAME = "fake-username";
    private static final String FAKE_FULL_USERNAME = FAKE_FIRST_NAME + " " + FAKE_LAST_NAME;
    private static final String FAKE_PASSWORD = "fake-password";
    private static final String FAKE_DOMAIN = "fake-domain";
    private static final String FAKE_SESSION_KEY = "fake-session-key";
    private static final String FAKE_TIMEOUT = "fake-timeout";

    private static final String COMMAND_KEY = "command";
    private static final String USERNAME_KEY = "username";
    private static final String PASSWORD_KEY = "password";
    private static final String DOMAIN_KEY = "domain";
    private static final String SESSION_KEY_KEY = "sessionkey";
    private static final String CLOUDSTACK_URL = "cloudstack_api_url";

    private static final String FAKE_API_KEY = "fake-api-key";
    private static final String FAKE_SECRET_KEY = "fake-secret-key";
    private static final String FAKE_TOKEN_VALUE = FAKE_API_KEY + ":" + FAKE_SECRET_KEY;

    private HttpRequestClientUtil httpRequestClientUtil;
    private CloudStackTokenGeneratorPlugin cloudStackTokenGenerator;
    private String memberId;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(HttpRequestUtil.class);

        this.httpRequestClientUtil = Mockito.mock(HttpRequestClientUtil.class);
        this.cloudStackTokenGenerator = Mockito.spy(new CloudStackTokenGeneratorPlugin());
        this.cloudStackTokenGenerator.setClient(this.httpRequestClientUtil);
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
    }

    // Test case: when creating a token, two requests should be made: one post request to login the user using their
    // credentials and get a session key to perform the other get request to retrieve the user "token", i.e., info
    // needed to perform requests in cloudstack (namely api key and secret key)
    @Test
    public void testCreateToken() throws FogbowRasException, UnexpectedException, IOException {
        //set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String listAccountsCommand = ListAccountsRequest.LIST_ACCOUNTS_COMMAND;
        String loginCommand = LoginRequest.LOGIN_COMMAND;

        String loginJsonResponse = getLoginResponse(FAKE_SESSION_KEY, FAKE_TIMEOUT);
        String accountJsonResponse = getAccountResponse(FAKE_ID, FAKE_USERNAME, FAKE_FIRST_NAME, FAKE_LAST_NAME,
                FAKE_API_KEY, FAKE_SECRET_KEY);
        String expectedListAccountsRequestUrl = generateExpectedUrl(endpoint, listAccountsCommand,
                                                                    SESSION_KEY_KEY, FAKE_SESSION_KEY);

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, loginCommand);
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
                .thenReturn(accountJsonResponse);

        Map<String, String> userCredentials = new HashMap<String, String>();
        userCredentials.put(CloudStackTokenGeneratorPlugin.USERNAME, FAKE_USERNAME);
        userCredentials.put(CloudStackTokenGeneratorPlugin.PASSWORD, FAKE_PASSWORD);
        userCredentials.put(CloudStackTokenGeneratorPlugin.DOMAIN, FAKE_DOMAIN);

        //exercise
        String tokenValue = this.cloudStackTokenGenerator.createTokenValue(userCredentials);

        //verify
        String split[] = tokenValue.split(CloudStackTokenGeneratorPlugin.TOKEN_STRING_SEPARATOR);
        Assert.assertEquals(split.length, 4);
        Assert.assertEquals(split[0], memberId);
        Assert.assertEquals(split[1], FAKE_TOKEN_VALUE);
        Assert.assertEquals(split[2], FAKE_ID);
        Assert.assertEquals(split[3], FAKE_FULL_USERNAME);
    }

    // Test case: throw expection in case any of the credentials are invalid
    @Test(expected = FogbowRasException.class)
    public void testInvalidCredentials() throws FogbowRasException, UnexpectedException {
        Map<String, String> tokenAttributes = new HashMap<String, String>();
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.USERNAME, null);
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.PASSWORD, "key");
        tokenAttributes.put(CloudStackTokenGeneratorPlugin.DOMAIN, "domain");

        String token = this.cloudStackTokenGenerator.createTokenValue(tokenAttributes);
    }

    // Test case: http request fails on retrieving account details
    @Test(expected = FogbowRasException.class)
    public void testListAccountsFail() throws FogbowRasException, UnexpectedException, IOException {
        //set up
        String endpoint = getBaseEndpointFromCloudStackConf();
        String listAccountsCommand = ListAccountsRequest.LIST_ACCOUNTS_COMMAND;
        String loginCommand = LoginRequest.LOGIN_COMMAND;

        String loginJsonResponse = getLoginResponse(FAKE_SESSION_KEY, FAKE_TIMEOUT);
        String accountJsonResponse = getAccountResponse(FAKE_ID, FAKE_USERNAME, FAKE_FIRST_NAME, FAKE_LAST_NAME,
                FAKE_API_KEY, FAKE_SECRET_KEY);
        String expectedListAccountsRequestUrl = generateExpectedUrl(endpoint, listAccountsCommand,
                SESSION_KEY_KEY, FAKE_SESSION_KEY);

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, loginCommand);
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
                .thenThrow(new HttpResponseException(503, "service unavailable"));

        //exercise
        Map<String, String> userCredentials = new HashMap<String, String>();
        userCredentials.put(CloudStackTokenGeneratorPlugin.USERNAME, FAKE_USERNAME);
        userCredentials.put(CloudStackTokenGeneratorPlugin.PASSWORD, FAKE_PASSWORD);
        userCredentials.put(CloudStackTokenGeneratorPlugin.DOMAIN, FAKE_DOMAIN);

        String tokenValue = this.cloudStackTokenGenerator.createTokenValue(userCredentials);
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
        String response = "{\"account\":[{"
                + "\"user\":[{"
                    + "\"id\": \"%s\","
                    + "\"username\": \"%s\","
                    + "\"firstname\": \"%s\","
                    + "\"lastname\": \"%s\","
                    + "\"apikey\": \"%s\","
                    + "\"secretkey\": \"%s\""
                + "}]}]}";

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
