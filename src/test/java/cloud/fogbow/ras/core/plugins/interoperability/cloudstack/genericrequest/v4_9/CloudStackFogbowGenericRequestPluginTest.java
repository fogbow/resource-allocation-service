package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.genericrequest.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.HttpRequest;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

public class CloudStackFogbowGenericRequestPluginTest {

    public static final String CLOUDSTACK_SEPARATOR = CloudStackConstants.KEY_VALUE_SEPARATOR;

    public static final String FAKE_TOKEN_VALUE = "foo" + CLOUDSTACK_SEPARATOR + "bar";
    public static final String FAKE_USER_ID = "fake-user-id";
    public static final String FAKE_NAME = "fake-name";
    public static final String FAKE_DOMAIN = "fake-domain";
    private static final HashMap<String, String> FAKE_COOKIE_HEADER = new HashMap<>();

    private CloudStackUser fakeToken;
    private CloudStackGenericRequestPlugin plugin;
    private CloudStackHttpClient client;

    @Before
    public void setUp() {
        this.fakeToken = new CloudStackUser(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_DOMAIN, FAKE_COOKIE_HEADER);
        this.client = Mockito.mock(CloudStackHttpClient.class);

        this.plugin = new CloudStackGenericRequestPlugin();
        this.plugin.setClient(client);
    }

    @Test
    public void testGenericRequest() throws FogbowException, URISyntaxException, HttpResponseException {
        // set up
        HashMap<String, String> headers = new HashMap<>();
        HashMap<String, String> body = new HashMap<>();
        HashMap<String, List<String>> responseHeaders = new HashMap<>();
        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, "https://www.foo.bar", body, headers);
        String serializedHttpRequest = GsonHolder.getInstance().toJson(httpRequest);

        HttpResponse response = new HttpResponse("fake-content", HttpStatus.SC_OK, responseHeaders);
        Mockito.when(this.client.doGenericRequest(Mockito.any(HttpMethod.class), Mockito.anyString(), Mockito.any(HashMap.class), Mockito.any(HashMap.class), Mockito.any(CloudStackUser.class))).thenReturn(response);

        // exercise
        plugin.redirectGenericRequest(serializedHttpRequest, this.fakeToken);

        // verify
        URIBuilder uriBuilder = new URIBuilder(httpRequest.getUrl());
        CloudStackUrlUtil.sign(uriBuilder, fakeToken.getToken());
        String expectedUrl = uriBuilder.toString();

        Mockito.verify(this.client).doGenericRequest(Mockito.eq(HttpMethod.GET), Mockito.eq(expectedUrl),
                Mockito.any(HashMap.class), Mockito.any(HashMap.class), Mockito.any(CloudStackUser.class));
    }

}
