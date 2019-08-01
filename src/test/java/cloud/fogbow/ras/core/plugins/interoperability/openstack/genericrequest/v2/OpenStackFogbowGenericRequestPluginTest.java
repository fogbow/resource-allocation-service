package cloud.fogbow.ras.core.plugins.interoperability.openstack.genericrequest.v2;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.connectivity.HttpRequest;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.models.OpenStackV3User;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class OpenStackFogbowGenericRequestPluginTest {

    public static final String FAKE_VALUE = "fake-value";
    public static final String FAKE_KEY = "fake-key";
    public static final String FAKE_URL = "fake-url";

    private OpenStackGenericRequestPlugin plugin;
    private OpenStackHttpClient openStackHttpClient;
    private HttpRequest genericRequest;

    @Before
    public void setUp() {
        openStackHttpClient = Mockito.mock(OpenStackHttpClient.class);
        plugin = Mockito.spy(new OpenStackGenericRequestPlugin());
        plugin.setClient(openStackHttpClient);
        genericRequest = createGenericRequest();
    }

    // test case: A generic request is invalid, if its header contains 'X-Auth-Token' key.
    @Test
    public void testGenericRequestWithWrongHeader() throws FogbowException {
        // set up
        Map<String, String> header = genericRequest.getHeaders();
        header.put(OpenStackConstants.X_AUTH_TOKEN_KEY, FAKE_VALUE);
        genericRequest.setHeaders(header);
        String serializedGenericRequest = GsonHolder.getInstance().toJson(genericRequest);

        // exercise
        try {
            plugin.redirectGenericRequest(serializedGenericRequest, Mockito.mock(OpenStackV3User.class));
            // verify
            Assert.fail();
        } catch (InvalidParameterException e) {
        }
    }

    private HttpRequest createGenericRequest() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put(FAKE_KEY, FAKE_VALUE);

        HashMap<String, String> body = new HashMap<>();
        body.put(FAKE_KEY, FAKE_VALUE);

        HttpRequest genericRequest = new HttpRequest(HttpMethod.GET, FAKE_URL, body, headers);
        return genericRequest;
    }
}
