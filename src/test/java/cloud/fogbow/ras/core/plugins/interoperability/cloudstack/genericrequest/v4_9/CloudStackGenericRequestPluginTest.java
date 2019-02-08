package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.genericrequest.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestHttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import java.net.URISyntaxException;
import java.util.Collections;

public class CloudStackGenericRequestPluginTest {

    public static final String CLOUDSTACK_SEPARATOR = CloudStackConstants.KEY_VALUE_SEPARATOR;

    public static final String FAKE_TOKEN_VALUE = "foo" + CLOUDSTACK_SEPARATOR + "bar";
    public static final String FAKE_PROVIDER = "fake-provider";
    public static final String FAKE_USER_ID = "fake-user-id";
    public static final String FAKE_USER_NAME = "fake-user-name";
    public static final String FAKE_SIGNATURE = "fake-signature";

    private CloudToken fakeToken;
    private CloudStackGenericRequestPlugin plugin;
    private AuditableHttpRequestClient client;

    @Before
    public void setUp() {
        this.fakeToken = new CloudToken(FAKE_PROVIDER, FAKE_USER_ID, FAKE_TOKEN_VALUE);
        this.client = Mockito.mock(AuditableHttpRequestClient.class);

        this.plugin = new CloudStackGenericRequestPlugin();
        this.plugin.setClient(client);
    }

    @Test
    public void testGenericRequest() throws FogbowException, URISyntaxException, HttpResponseException {
        // set up
        GenericRequest request = new GenericRequest("GET", "https://www.foo.bar", Collections.emptyMap(), Collections.emptyMap());

        GenericRequestHttpResponse response = new GenericRequestHttpResponse("fake-content", HttpStatus.OK.value());
        Mockito.when(this.client.doGenericRequest(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap(), Mockito.any(CloudToken.class))).thenReturn(response);

        // exercise
        plugin.redirectGenericRequest(request, this.fakeToken);

        // verify
        URIBuilder uriBuilder = new URIBuilder(request.getUrl());
        CloudStackUrlUtil.sign(uriBuilder, fakeToken.getTokenValue());
        String expectedUrl = uriBuilder.toString();

        Mockito.verify(this.client).doGenericRequest(Mockito.eq("GET"), Mockito.eq(expectedUrl),
                Mockito.eq(Collections.emptyMap()), Mockito.eq(Collections.emptyMap()), Mockito.any(CloudToken.class));
    }

}
