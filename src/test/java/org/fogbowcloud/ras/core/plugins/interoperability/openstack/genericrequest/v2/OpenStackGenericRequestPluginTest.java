package org.fogbowcloud.ras.core.plugins.interoperability.openstack.genericrequest.v2;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import org.fogbowcloud.ras.util.connectivity.AuditableHttpRequestClient;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class OpenStackGenericRequestPluginTest {

    public static final String FAKE_VALUE = "fake-value";
    public static final String FAKE_KEY = "fake-key";
    public static final String FAKE_URL = "fake-url";

    private OpenStackGenericRequestPlugin plugin;
    private AuditableHttpRequestClient auditableHttpRequestClientMock;
    private GenericRequest genericRequest;

    @Before
    public void setUp() {
        auditableHttpRequestClientMock = Mockito.mock(AuditableHttpRequestClient.class);
        plugin = Mockito.spy(new OpenStackGenericRequestPlugin());
        plugin.setClient(auditableHttpRequestClientMock);
        genericRequest = createGenericRequest();
    }

    // test case: A generic request is invalid, if its header contains 'X-Auth-Token' key.
    @Test
    public void testGenericRequestWithWrongHeader() throws FogbowRasException {
        // set up
        Map<String, String> header = genericRequest.getHeaders();
        header.put(HttpRequestUtil.X_AUTH_TOKEN_KEY, FAKE_VALUE);
        genericRequest.setHeaders(header);

        // exercise
        try {
            plugin.redirectGenericRequest(genericRequest, Mockito.mock(OpenStackV3Token.class));
            // verify
            Assert.fail();
        } catch (InvalidParameterException e) {
        }
    }

    // test case: The header 'X-Auth-Token' must be added before executing actual request.
    @Test
    public void testGenericRequestPlugin() throws FogbowRasException {
        // set up
        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.doReturn(null).when(auditableHttpRequestClientMock).doGenericRequest(Mockito.anyString(),
                Mockito.anyString(), argumentCaptor.capture(), Mockito.anyMap());

        // exercise
        plugin.redirectGenericRequest(genericRequest, Mockito.mock(OpenStackV3Token.class));
        // verify
        Assert.assertTrue(argumentCaptor.getValue().size() == 2);
        Assert.assertTrue(argumentCaptor.getValue().containsKey(HttpRequestUtil.X_AUTH_TOKEN_KEY));
        Mockito.verify(auditableHttpRequestClientMock, Mockito.times(1)).doGenericRequest(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyMap(), Mockito.anyMap());
    }

    private GenericRequest createGenericRequest() {
        Map<String, String> headers = new HashMap<>();
        headers.put(FAKE_KEY, FAKE_VALUE);

        Map<String, String> body = new HashMap<>();
        body.put(FAKE_KEY, FAKE_VALUE);

        String method = "GET";

        GenericRequest genericRequest = new GenericRequest(method, FAKE_URL, headers, body);
        return genericRequest;
    }
}
