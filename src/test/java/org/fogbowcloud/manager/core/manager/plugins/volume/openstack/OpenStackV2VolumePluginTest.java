package org.fogbowcloud.manager.core.manager.plugins.volume.openstack;

import com.google.common.base.Charsets;
import org.apache.http.*;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.manager.constants.OpenStackConfigurationConstants;
import org.fogbowcloud.manager.core.models.orders.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.instances.StorageOrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;
import org.junit.Before;
import org.junit.Test;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
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

    // TODO move to appropriate class
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String X_AUTH_TOKEN = "X-Auth-Token";
    private static final String ACCEPT = "Accept";
    private static final String JSON_CONTENT_TYPE = "application/json";

    private static final String STORAGE_URL = "http://localhost:0000";
    private static final String SIZE = "2";
    private static final String ACCESS_ID = "accessId";
    private static final String TENANT_ID = "tenantId";

    private HttpClient client;
    private HttpUriRequestMatcher expectedRequest;
    private OpenStackV2VolumePlugin openStackV2VolumePlugin;
    private Token tokenDefault;

    @Before
    public void setUp() throws Exception {
        Properties properties = new Properties();
        properties.put(OpenStackV2VolumePlugin.VOLUME_NOVAV2_URL_KEY, STORAGE_URL);
        this.openStackV2VolumePlugin = new OpenStackV2VolumePlugin(properties);

        this.client = Mockito.mock(HttpClient.class);
        HttpResponseFactory factory = new DefaultHttpResponseFactory();
        HttpResponse response = factory.newHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1,
                HttpStatus.SC_NO_CONTENT, "Return Irrelevant"), null);
        Mockito.when(this.client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        this.openStackV2VolumePlugin.setClient(this.client);

        Map<String, String> attributes = new HashMap<>();
        attributes.put(OpenStackConfigurationConstants.TENANT_ID, TENANT_ID);
        Token.User tokenUser = new Token.User("user", "user");
        this.tokenDefault = new Token(ACCESS_ID, tokenUser, new Date(), attributes);
    }

    @Test
    public void requestInstance() throws IOException, RequestException {
        String url = STORAGE_URL + OpenStackConfigurationConstants.V2_API_ENDPOINT + TENANT_ID
                + OpenStackV2VolumePlugin.SUFIX_ENDPOINT_VOLUMES;
        HttpUriRequest request = new HttpPost(url);
        request.addHeader(CONTENT_TYPE, JSON_CONTENT_TYPE);
        request.addHeader(ACCEPT, JSON_CONTENT_TYPE);
        request.addHeader(X_AUTH_TOKEN, this.tokenDefault.getAccessId());

        this.expectedRequest = new HttpUriRequestMatcher(request, this.openStackV2VolumePlugin
                .generateJsonEntityToCreateInstance(SIZE).toString());

        String id = "fake-id";
        String name = "fake-name";
        InstanceState state = InstanceState.ACTIVE;
        int size = 2;
        StorageOrderInstance storageOrderInstance = new StorageOrderInstance(id, name, state, size);

        this.openStackV2VolumePlugin.requestInstance(tokenDefault, storageOrderInstance);

        Mockito.verify(this.client).execute(Mockito.argThat(this.expectedRequest));
    }

    @Test
    public void getInstance() {
    }

    @Test
    public void removeInstance() {
    }

    @Test
    public void getInstanceFromJson() {
    }

    @Test
    public void getInstanceState() {
    }

    @Test
    public void generateJsonEntityToCreateInstance() {
    }

    private class HttpUriRequestMatcher extends ArgumentMatcher<HttpUriRequest> {

        private HttpUriRequest request;
        private String entityStrCompare;
        private boolean doNotCheck;

        public HttpUriRequestMatcher() {
            this.doNotCheck = true;
        }

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
            if (object instanceof HttpPost && entityStrCompare != null) {
                try {
                    HttpEntityEnclosingRequestBase httpEntityEnclosingRequestBase = (HttpEntityEnclosingRequestBase) object;
                    String entityStr = EntityUtils.toString(httpEntityEnclosingRequestBase.getEntity(), Charsets.UTF_8);
                    if (!entityStrCompare.equals(entityStr)) {
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
                    if (header.getName().equals(X_AUTH_TOKEN)) {
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