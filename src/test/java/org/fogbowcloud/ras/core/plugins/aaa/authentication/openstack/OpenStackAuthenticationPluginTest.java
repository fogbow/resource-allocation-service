package org.fogbowcloud.ras.core.plugins.aaa.authentication.openstack;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicStatusLine;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OpenStackAuthenticationPluginTest {
    private OpenStackAuthenticationPlugin authenticationPlugin;
    private HttpRequestClientUtil httpRequestClientUtil;
    private HttpClient client;

    private static final String UTF_8 = "UTF-8";

    private String userId;
    private String projectId;
    private String providerId;

    @Before
    public void setUp() {
        this.client = Mockito.spy(HttpClient.class);
        this.httpRequestClientUtil = Mockito.spy(new HttpRequestClientUtil(this.client));

        this.userId = "userId";
        this.projectId = "projectId";
        this.providerId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);

        this.authenticationPlugin = Mockito.spy(new OpenStackAuthenticationPlugin());
        this.authenticationPlugin.setClient(this.httpRequestClientUtil);
    }

    //test case: check if isAuthentic returns true when the tokenValue is valid.
    @Test
    public void testGetTokenValidTokenValue() throws IOException, UnavailableProviderException {
        //set up
        FederationUserToken token = new OpenStackV3Token(this.providerId, "fake-token",
                this.userId, "fake-name", this.projectId, "fake-project-name");
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        String content = "any content";
        InputStream contentInputStream = new ByteArrayInputStream(content.getBytes(UTF_8));
        Mockito.when(httpEntity.getContent()).thenReturn(contentInputStream);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus = new BasicStatusLine(new ProtocolVersion("", 0, 0),
                HttpStatus.SC_OK, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);

        //exercise

        //verify
        Assert.assertTrue(this.authenticationPlugin.isAuthentic(token));
    }

    //test case: check if isAuthentic returns false when the tokenValue is not valid or keystone service is not available.
    @Test
    public void testGetTokenError() throws Exception {
        //set up
        FederationUserToken token = new OpenStackV3Token(this.providerId, "fake-token",
                this.userId, "fake-name", this.projectId, "fake-project-name");
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        String content = "any content";
        InputStream contentInputStream = new ByteArrayInputStream(content.getBytes(UTF_8));
        Mockito.when(httpEntity.getContent()).thenReturn(contentInputStream);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus = new BasicStatusLine(new ProtocolVersion("", 0, 0),
                HttpStatus.SC_BAD_GATEWAY, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).
                thenThrow(new HttpResponseException(HttpStatus.SC_BAD_GATEWAY, ""));
        //exercise

        //verify
        Assert.assertFalse(this.authenticationPlugin.isAuthentic(token));
    }
}

