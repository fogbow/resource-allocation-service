package org.fogbowcloud.manager.core.manager.plugins.volume.openstack;

import com.google.common.base.Charsets;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.constants.OpenStackConstants;
import org.fogbowcloud.manager.core.models.RequestHeaders;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.StorageOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.message.BasicStatusLine;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class OpenStackV2VolumePluginTest {

    private final String FAKE_STORAGE_URL = "http://localhost:0000";
    private final String FAKE_SIZE = "2";
    private final String FAKE_ACCESS_ID = "access-id";
    private final String FAKE_TENANT_ID = "tenant-id";
    private final String FAKE_INSTANCE_ID = "instance-id";

    private HttpClient client;
    private HttpUriRequestMatcher expectedRequest;
    private OpenStackV2VolumePlugin openStackV2VolumePlugin;
    private Token tokenDefault;

    @Before
    public void setUp() throws Exception {
        Properties properties = new Properties();
        properties.put(OpenStackV2VolumePlugin.VOLUME_NOVAV2_URL_KEY, FAKE_STORAGE_URL);
        this.openStackV2VolumePlugin = new OpenStackV2VolumePlugin(properties);

        this.client = Mockito.mock(HttpClient.class);
        HttpResponseFactory factory = new DefaultHttpResponseFactory();
        HttpResponse response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_NO_CONTENT, "Return Irrelevant"), null);
        Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        this.openStackV2VolumePlugin.setClient(this.client);

        Map<String, String> attributes = new HashMap<>();
        attributes.put(OpenStackConstants.TENANT_ID, FAKE_TENANT_ID);
        Token.User tokenUser = new Token.User("user", "user");
        this.tokenDefault = new Token(FAKE_ACCESS_ID, tokenUser, new Date(), attributes);
    }

    @Test
    public void testRequestInstance() throws IOException, RequestException {
        String url = FAKE_STORAGE_URL + OpenStackConstants.V2_API_ENDPOINT + FAKE_TENANT_ID
                + OpenStackV2VolumePlugin.SUFIX_ENDPOINT_VOLUMES;
        HttpUriRequest request = new HttpPost(url);
        addHeaders(request);

        this.expectedRequest = new HttpUriRequestMatcher(request, this.openStackV2VolumePlugin
                .generateJsonEntityToCreateInstance(FAKE_SIZE).toString());

        String id = "fake-id";
        String name = "fake-name";
        InstanceState state = InstanceState.ACTIVE;
        int size = 2;
        StorageOrderInstance storageOrderInstance = new StorageOrderInstance(id, name, state, size);

        try {
            this.openStackV2VolumePlugin.requestInstance(this.tokenDefault, storageOrderInstance);
        } catch (Exception e) {

        }

        Mockito.verify(this.client).execute(Mockito.argThat(this.expectedRequest));
    }

    @Test(expected=RequestException.class)
    public void testRequestInstanceWithoutTenantId() throws RequestException {
        this.tokenDefault.getAttributes().clear();
        StorageOrderInstance storageOrderInstance = null;
        this.openStackV2VolumePlugin.requestInstance(this.tokenDefault, storageOrderInstance);
    }

    @Test
    public void testGenerateJsonEntityToCreateInstance() {
        JSONObject jsonEntity = this.openStackV2VolumePlugin.generateJsonEntityToCreateInstance(FAKE_SIZE);
        Assert.assertEquals(FAKE_SIZE, jsonEntity.getJSONObject(OpenStackV2VolumePlugin.KEY_JSON_VOLUME)
                .getString(OpenStackV2VolumePlugin.KEY_JSON_SIZE));
    }

    @Test
    public void testGetInstanceFromJson() throws RequestException  {
        StorageOrderInstance instance = this.openStackV2VolumePlugin.getInstanceFromJson(
                generateInstanceJsonResponse(FAKE_INSTANCE_ID).toString());
        Assert.assertEquals(FAKE_INSTANCE_ID, instance.getId());
    }

    @Test
    public void testGetInstance() throws IOException {
        String url = FAKE_STORAGE_URL + OpenStackConstants.V2_API_ENDPOINT + FAKE_TENANT_ID
                + OpenStackV2VolumePlugin.SUFIX_ENDPOINT_VOLUMES + "/" + FAKE_INSTANCE_ID;
        HttpUriRequest request = new HttpGet(url);
        addHeaders(request);

        this.expectedRequest = new HttpUriRequestMatcher(request, null);

        try {
            this.openStackV2VolumePlugin.getInstance(this.tokenDefault, FAKE_INSTANCE_ID);
        } catch (Exception e) {
        }

        Mockito.verify(this.client).execute(Mockito.argThat(this.expectedRequest));
    }

    @Test(expected=RequestException.class)
    public void testGetInstancesWithoutTenantId() throws Exception {
        this.tokenDefault.getAttributes().clear();
        this.openStackV2VolumePlugin.getInstance(this.tokenDefault, FAKE_ACCESS_ID);
    }

    @Test
    public void removeInstance() throws IOException {
        String url = FAKE_STORAGE_URL + OpenStackConstants.V2_API_ENDPOINT + FAKE_TENANT_ID
                + OpenStackV2VolumePlugin.SUFIX_ENDPOINT_VOLUMES + "/" + FAKE_INSTANCE_ID;
        HttpUriRequest request = new HttpDelete(url);
        addHeaders(request);

        this.expectedRequest = new HttpUriRequestMatcher(request, null);

        try {
            this.openStackV2VolumePlugin.removeInstance(this.tokenDefault, FAKE_INSTANCE_ID);
        } catch (Exception e) {
        }

        Mockito.verify(client).execute(Mockito.argThat(this.expectedRequest));
    }

    @Test(expected=RequestException.class)
    public void testRemoveInstanceWithoutTenantId() throws Exception {
        this.tokenDefault.getAttributes().clear();
        this.openStackV2VolumePlugin.removeInstance(tokenDefault, FAKE_INSTANCE_ID);
    }

    @Ignore
    @Test
    public void getInstanceState() {
        // TODO
    }

    private void addHeaders(HttpUriRequest request) {
        request.addHeader(RequestHeaders.CONTENT_TYPE.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
        request.addHeader(RequestHeaders.ACCEPT.getValue(), RequestHeaders.JSON_CONTENT_TYPE.getValue());
        request.addHeader(RequestHeaders.X_AUTH_TOKEN.getValue(), this.tokenDefault.getAccessId());
    }

    private JSONObject generateInstanceJsonResponse(String instanceId) throws JSONException {
        JSONObject jsonId = new JSONObject();
        jsonId.put(OpenStackV2VolumePlugin.KEY_JSON_ID, instanceId);
        jsonId.put(OpenStackV2VolumePlugin.KEY_JSON_SIZE, FAKE_SIZE);
        JSONObject jsonStorage = new JSONObject();
        jsonStorage.put(OpenStackV2VolumePlugin.KEY_JSON_VOLUME, jsonId);
        return jsonStorage;
    }

    private class HttpUriRequestMatcher extends ArgumentMatcher<HttpUriRequest> {

        private HttpUriRequest request;
        private String entityStrCompare;
        private boolean doNotCheck;

        public HttpUriRequestMatcher(Object request, String entityStrCompare) {
            this.request = (HttpUriRequest) request;
            this.entityStrCompare = entityStrCompare;
        }

        public boolean matches(Object object) {
            if (this.doNotCheck) {
                return true;
            }

            HttpUriRequest comparedRequest = null;
            comparedRequest = (HttpUriRequest) object;
            if (object instanceof HttpPost && this.entityStrCompare != null) {
                try {
                    HttpEntityEnclosingRequestBase httpEntityEnclosingRequestBase = (HttpEntityEnclosingRequestBase) object;
                    String entityStr = EntityUtils.toString(httpEntityEnclosingRequestBase.getEntity(), Charsets.UTF_8);
                    if (!this.entityStrCompare.equals(entityStr)) {
                        return false;
                    }
                } catch (Exception e) {}
            }
            if (!this.request.getURI().equals(comparedRequest.getURI())) {
                return false;
            }
            if (!checkHeaders(comparedRequest.getAllHeaders())) {
                return false;
            }
            if (!this.request.getMethod().equals(comparedRequest.getMethod())) {
                return false;
            }
            return true;
        }

        public boolean checkHeaders(Header[] comparedHeaders) {
            for (Header comparedHeader : comparedHeaders) {
                boolean headerEquals = false;
                for (Header header : this.request.getAllHeaders()) {
                    if (header.getName().equals(RequestHeaders.X_AUTH_TOKEN.getValue())) {
                        if (header.getName().equals(comparedHeader.getName())) {
                            headerEquals = true;
                            break;
                        }
                    } else
                    if (header.getName().equals(comparedHeader.getName())
                            && header.getValue().equals(comparedHeader.getValue())) {
                        headerEquals = true;
                        continue;
                    }
                }
                if (!headerEquals) {
                    return false;
                }
            }
            return true;
        }
    }

}