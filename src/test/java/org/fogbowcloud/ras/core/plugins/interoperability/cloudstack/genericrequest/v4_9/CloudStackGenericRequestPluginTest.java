package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.genericrequest.v4_9;

import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.cloudstack.CloudStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestHttpResponse;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

public class CloudStackGenericRequestPluginTest {

    public static final String CLOUDSTACK_SEPARATOR = CloudStackTokenGeneratorPlugin.CLOUDSTACK_TOKEN_VALUE_SEPARATOR;

    public static final String FAKE_TOKEN_VALUE = "foo" + CLOUDSTACK_SEPARATOR + "bar";
    public static final String FAKE_PROVIDER = "fake-provider";
    public static final String FAKE_USER_ID = "fake-user-id";
    public static final String FAKE_USER_NAME = "fake-user-name";
    public static final String FAKE_SIGNATURE = "fake-signature";

    private CloudStackToken fakeToken;
    private CloudStackGenericRequestPlugin plugin;
    private HttpRequestClientUtil client;

    @Before
    public void setUp() {
        this.fakeToken = new CloudStackToken(FAKE_PROVIDER, FAKE_TOKEN_VALUE, FAKE_USER_ID, FAKE_USER_NAME, FAKE_SIGNATURE);
        this.client = Mockito.mock(HttpRequestClientUtil.class);

        this.plugin = new CloudStackGenericRequestPlugin();
        this.plugin.setClient(client);
    }

    @Test
    public void testGenericRequest() throws FogbowRasException, URISyntaxException, HttpResponseException {
        // set up
        GenericRequest request = new GenericRequest("GET", "https://www.foo.bar", Collections.emptyMap(), Collections.emptyMap());

        GenericRequestHttpResponse response = new GenericRequestHttpResponse("fake-content", HttpStatus.OK.value());
        Mockito.when(this.client.doGenericRequest(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(response);

        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.any(Token.class))).thenReturn(response.getContent());

        // exercise
        plugin.redirectGenericRequest(request, this.fakeToken);

        // verify
        URIBuilder uriBuilder = new URIBuilder(request.getUrl());
        CloudStackUrlUtil.sign(uriBuilder, fakeToken.getTokenValue());
        String expectedUrl = uriBuilder.toString();

        Mockito.verify(this.client).doGetRequest(Mockito.eq(expectedUrl), Mockito.eq(this.fakeToken));
    }

}
