package cloud.fogbow.ras.core.plugins.interoperability.openstack.genericrequest.v2;

import cloud.fogbow.common.constants.HttpConstants;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;

public class OpenStackGenericRequestPluginTest {

    public static final String FAKE_VALUE = "fake-value";
    public static final String FAKE_KEY = "fake-key";
    public static final String FAKE_URL = "fake-url";

    private OpenStackGenericRequestPlugin plugin;
    private OpenStackHttpClient openStackHttpClient;
    private GenericRequest genericRequest;

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
        HashMap<String, String> header = genericRequest.getHeaders();
        header.put(HttpConstants.X_AUTH_TOKEN_KEY, FAKE_VALUE);
        genericRequest.setHeaders(header);

        // exercise
        try {
            plugin.redirectGenericRequest(genericRequest, Mockito.mock(OpenStackV3Token.class));
            // verify
            Assert.fail();
        } catch (InvalidParameterException e) {
        }
    }

    private GenericRequest createGenericRequest() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put(FAKE_KEY, FAKE_VALUE);

        HashMap<String, String> body = new HashMap<>();
        body.put(FAKE_KEY, FAKE_VALUE);

        GenericRequest genericRequest = new GenericRequest(HttpMethod.GET, FAKE_URL, body, headers);
        return genericRequest;
    }
}
