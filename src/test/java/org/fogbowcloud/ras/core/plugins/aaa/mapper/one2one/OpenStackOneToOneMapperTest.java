package org.fogbowcloud.ras.core.plugins.aaa.mapper.one2one;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.aaa.identity.openstack.v3.OpenStackIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3.OpenStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class OpenStackOneToOneMapperTest {
    private static final org.apache.log4j.Logger LOGGER = Logger.getLogger(OpenStackOneToOneMapperTest.class);

    private static final String FAKE_USER_ID1 = "fake-user-id1";
    private static final String FAKE_USER_ID2 = "fake-user-id2";
    private static final String FAKE_USER_NAME = "fake-user-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_PROJECT_NAME = "fake-project-name";
    private static final String FAKE_TOKEN_VALUE1 = "fake-token-value1";
    private static final String FAKE_TOKEN_VALUE2 = "fake-token-value2";
    private static final String UTF_8 = "UTF-8";

    private OpenStackOneToOneMapper mapper;
    private HttpClient client;
    private HttpRequestClientUtil httpRequestClientUtil;
    private OpenStackTokenGeneratorPlugin keystoneV3TokenGenerator;
    private OpenStackIdentityPlugin openStackIdentityPlugin;
    private String memberId;

    @Before
    public void setUp() {
        this.mapper = new OpenStackOneToOneMapper();
        this.client = Mockito.spy(HttpClient.class);
        this.httpRequestClientUtil = Mockito.spy(new HttpRequestClientUtil(this.client));
        this.keystoneV3TokenGenerator = Mockito.spy(new OpenStackTokenGeneratorPlugin());
        this.keystoneV3TokenGenerator.setClient(this.httpRequestClientUtil);
        this.openStackIdentityPlugin = new OpenStackIdentityPlugin();
        this.memberId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
    }

    //test case: two different Federation Tokens should be mapped to two different Tokens
    @Test
    public void testCreate2Tokens() throws java.io.IOException, UnexpectedException, FogbowRasException {
        //set up
        String jsonResponse1 = "{\"token\":{\"user\":{\"id\":\"" + FAKE_USER_ID1 + "\",\"name\": \"" + FAKE_USER_NAME +
                "\"}, \"project\":{\"id\": \"" + FAKE_PROJECT_ID + "\", \"name\": \"" + FAKE_PROJECT_NAME +
                "\"}}}";
        HttpResponse httpResponse1 = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity1 = Mockito.mock(HttpEntity.class);
        InputStream contentInputStream1 = new ByteArrayInputStream(jsonResponse1.getBytes(UTF_8));
        Mockito.when(httpEntity1.getContent()).thenReturn(contentInputStream1);
        Mockito.when(httpResponse1.getEntity()).thenReturn(httpEntity1);
        BasicStatusLine basicStatus1 = new BasicStatusLine(new ProtocolVersion("", 0, 0),
                HttpStatus.SC_OK, "");
        Mockito.when(httpResponse1.getStatusLine()).thenReturn(basicStatus1);
        Header[] headers1 = new BasicHeader[1];
        headers1[0] = new BasicHeader(OpenStackTokenGeneratorPlugin.X_SUBJECT_TOKEN, FAKE_TOKEN_VALUE1);
        Mockito.when(httpResponse1.getAllHeaders()).thenReturn(headers1);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse1);

        Map<String, String> userCredentials1 = new HashMap<String, String>();
        userCredentials1.put(OpenStackTokenGeneratorPlugin.PASSWORD, "any password");
        userCredentials1.put(OpenStackTokenGeneratorPlugin.PROJECT_NAME, FAKE_PROJECT_ID);
        String tokenValue1 = this.keystoneV3TokenGenerator.createTokenValue(userCredentials1);
        OpenStackV3Token token1 = this.openStackIdentityPlugin.createToken(tokenValue1);

        String jsonResponse2 = "{\"token\":{\"user\":{\"id\":\"" + FAKE_USER_ID2 + "\",\"name\": \"" + FAKE_USER_NAME +
                "\"}, \"project\":{\"id\": \"" + FAKE_PROJECT_ID + "\", \"name\": \"" + FAKE_PROJECT_NAME +
                "\"}}}";
        HttpResponse httpResponse2 = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity2 = Mockito.mock(HttpEntity.class);
        InputStream contentInputStream2 = new ByteArrayInputStream(jsonResponse2.getBytes(UTF_8));
        Mockito.when(httpEntity2.getContent()).thenReturn(contentInputStream2);
        Mockito.when(httpResponse2.getEntity()).thenReturn(httpEntity2);
        BasicStatusLine basicStatus2 = new BasicStatusLine(new ProtocolVersion("", 0, 0),
                HttpStatus.SC_OK, "");
        Mockito.when(httpResponse2.getStatusLine()).thenReturn(basicStatus2);
        Header[] headers2 = new BasicHeader[1];
        headers2[0] = new BasicHeader(OpenStackTokenGeneratorPlugin.X_SUBJECT_TOKEN, FAKE_TOKEN_VALUE2);
        Mockito.when(httpResponse2.getAllHeaders()).thenReturn(headers2);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse2);

        Map<String, String> userCredentials2 = new HashMap<String, String>();
        userCredentials2.put(OpenStackTokenGeneratorPlugin.PASSWORD, "any password");
        userCredentials2.put(OpenStackTokenGeneratorPlugin.PROJECT_NAME, FAKE_PROJECT_ID);
        String tokenValue2 = this.keystoneV3TokenGenerator.createTokenValue(userCredentials2);
        OpenStackV3Token token2 = this.openStackIdentityPlugin.createToken(tokenValue2);

        //exercise
        OpenStackV3Token mappedToken1 = (OpenStackV3Token) this.mapper.map(token1);
        OpenStackV3Token mappedToken2 = (OpenStackV3Token) this.mapper.map(token2);

        //verify
        Assert.assertEquals(token1, mappedToken1);
        Assert.assertEquals(token2, mappedToken2);
        Assert.assertNotEquals(mappedToken1, mappedToken2);
    }

}
