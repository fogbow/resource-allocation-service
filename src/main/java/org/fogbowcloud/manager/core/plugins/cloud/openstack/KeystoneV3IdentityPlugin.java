package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.plugins.cloud.LocalIdentityPlugin;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.util.connectivity.HttpRequestUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class KeystoneV3IdentityPlugin implements LocalIdentityPlugin {

    private static final Logger LOGGER = Logger.getLogger(KeystoneV3IdentityPlugin.class);

    private static final String KEYSTONEV3_PLUGIN_CONF_FILE = "openstack-keystone-identity-plugin.conf";

    private static final String OPENSTACK_KEYSTONE_V3_URL = "openstack_keystone_v3_url";
    private static final String X_SUBJECT_TOKEN = "X-Subject-Token";
    private static final String PASSWORD_PROP = "password";
    private static final String IDENTITY_PROP = "identity";
    private static final String PROJECT_PROP = "project";
    private static final String METHODS_PROP = "methods";
    private static final String TOKEN_PROP = "tokens";
    private static final String SCOPE_PROP = "scope";
    private static final String NAME_PROP = "name";
    private static final String AUTH_PROP = "auth";
    private static final String USER_PROP = "user";
    private static final String ID_PROP = "id";

    public static final String TENANT_NAME = "tenantName";
    public static final String PROJECT_ID = "projectId";
    public static final String TENANT_ID = "tenantId";
    public static final String PASSWORD = "password";
    public static final String USER_ID = "userId";

    public static final int LAST_SUCCESSFUL_STATUS = 204;

    public static String V3_TOKENS_ENDPOINT_PATH = "/auth/tokens";

    public static final String AUTH_URL = "authUrl";

    private String keystoneUrl;
    private String v3TokensEndpoint;
    private HttpClient client;

    public KeystoneV3IdentityPlugin() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.
                readProperties(homeDir.getPath() + File.separator + KEYSTONEV3_PLUGIN_CONF_FILE);

        String identityUrl = properties.getProperty(OPENSTACK_KEYSTONE_V3_URL);
        if (isUrlValid(identityUrl)) {
            this.keystoneUrl = identityUrl;
            this.v3TokensEndpoint = keystoneUrl + V3_TOKENS_ENDPOINT_PATH;
        }
        this.client = HttpRequestUtil.createHttpClient();
    }

    /**
     * Constructor used for testing only
     * @param client
     * @throws FatalErrorException if the properties file cannot be read or a required property was not set
     */
    protected KeystoneV3IdentityPlugin(HttpClient client) throws FatalErrorException {

        HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.
                readProperties(homeDir.getPath() + File.separator + KEYSTONEV3_PLUGIN_CONF_FILE);
        
        String identityUrl = properties.getProperty(OPENSTACK_KEYSTONE_V3_URL);
        isUrlValid(identityUrl);
        this.keystoneUrl = identityUrl;
        this.v3TokensEndpoint = keystoneUrl + V3_TOKENS_ENDPOINT_PATH;
        this.client = client != null ? client : HttpRequestUtil.createHttpClient();
    }

    private boolean isUrlValid(String url) throws FatalErrorException {
        if (url == null || url.trim().isEmpty()) {
            throw new FatalErrorException("Invalid Keystone_V3_URL " + OPENSTACK_KEYSTONE_V3_URL);
        }
        return true;
    }

    @Override
    public Token createToken(Map<String, String> credentials) {
        LOGGER.debug("Creating new Token");

        JSONObject json;
        try {
            json = mountJson(credentials);
        } catch (JSONException e) {
            LOGGER.error("Could not mount JSON while creating tokens", e);

            Integer statusResponse = HttpStatus.SC_BAD_REQUEST;
            throw new IllegalArgumentException(statusResponse.toString());
        }

        String authUrl = credentials.get(AUTH_URL);
        String currentTokenEndpoint = v3TokensEndpoint;
        if (authUrl != null && !authUrl.isEmpty()) {
            currentTokenEndpoint = authUrl + V3_TOKENS_ENDPOINT_PATH;
        }

        Response response = doPostRequest(currentTokenEndpoint, json);
        Token token = getTokenFromJson(response);

        return token;
    }

    private Response doPostRequest(String endpoint, JSONObject json) {
        HttpResponse response = null;
        String responseStr = null;
        try {
            HttpPost request = new HttpPost(endpoint);
            request.addHeader(
                    HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
            request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
            request.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
            response = getClient().execute(request);
            responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (UnknownHostException e) {
            LOGGER.error("Could not do post request, unknown host", e);
            Integer statusResponse = HttpStatus.SC_BAD_REQUEST;
            throw new RuntimeException(statusResponse.toString());
        } catch (Exception e) {
            LOGGER.error("Could not do post request", e);
            Integer statusResponse = HttpStatus.SC_BAD_REQUEST;
            throw new RuntimeException(statusResponse.toString());
        } finally {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (Exception e) {
                LOGGER.error("Could not release HTTPEntity resources", e);
            }
        }
        checkStatusResponse(response);

        return new Response(responseStr, response.getAllHeaders());
    }

    private HttpClient getClient() {
        return client;
    }

    protected JSONObject mountJson(Map<String, String> credentials) throws JSONException {
        JSONObject projectId = new JSONObject();
        projectId.put(ID_PROP, credentials.get(PROJECT_ID));
        JSONObject project = new JSONObject();
        project.put(PROJECT_PROP, projectId);

        JSONObject userProperties = new JSONObject();
        userProperties.put(PASSWORD_PROP, credentials.get(PASSWORD));
        userProperties.put(ID_PROP, credentials.get(USER_ID));
        JSONObject password = new JSONObject();
        password.put(USER_PROP, userProperties);

        JSONObject identity = new JSONObject();
        identity.put(METHODS_PROP, new JSONArray(new String[] {PASSWORD_PROP}));
        identity.put(PASSWORD_PROP, password);

        JSONObject auth = new JSONObject();
        auth.put(SCOPE_PROP, project);
        auth.put(IDENTITY_PROP, identity);

        JSONObject root = new JSONObject();
        root.put(AUTH_PROP, auth);

        return root;
    }

    private Token getTokenFromJson(Response response) {

        String accessId = null;
        Header[] headers = response.getHeaders();
        for (Header header : headers) {
            if (header.getName().equals(X_SUBJECT_TOKEN)) {
                accessId = header.getValue();
            }
        }

        try {
            JSONObject root = new JSONObject(response.getContent());
            JSONObject token = root.getJSONObject(TOKEN_PROP);

            JSONObject user = token.getJSONObject(USER_PROP);
            String userId = user.getString(ID_PROP);
            String userName = user.getString(NAME_PROP);

            Map<String, String> tokenAtt = new HashMap<String, String>();
            String tenantId = null;
            String tenantName = null;
            try {
                tenantId = token.getJSONObject(PROJECT_PROP).getString(ID_PROP);
                tokenAtt.put(TENANT_ID, tenantId);
            } catch (JSONException e) {
                LOGGER.debug("There is no tenantId inside json response");
            }
            try {
                tenantName = token.getJSONObject(PROJECT_PROP).getString(NAME_PROP);
                tokenAtt.put(TENANT_NAME, tenantName);
            } catch (JSONException e) {
                LOGGER.debug("There is no tenantName inside json response");
            }

            return new Token(accessId, new Token.User(userId, userName), new Date(), tokenAtt);
        } catch (Exception e) {
            LOGGER.error("Exception while getting tokens from json", e);
            return null;
        }
    }

    private void checkStatusResponse(HttpResponse response) {

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            Integer statusResponse = HttpStatus.SC_UNAUTHORIZED;
            throw new RuntimeException(statusResponse.toString());
        } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            Integer statusResponse = HttpStatus.SC_NOT_FOUND;
            throw new RuntimeException(statusResponse.toString());
        } else if (response.getStatusLine().getStatusCode() > LAST_SUCCESSFUL_STATUS) {
            Integer statusResponse = HttpStatus.SC_BAD_REQUEST;
            throw new RuntimeException(statusResponse.toString());
        }
    }

    public class Response {

        private String content;
        private Header[] headers;

        public Response(String content, Header[] headers) {
            this.content = content;
            this.headers = headers;
        }

        public String getContent() {
            return content;
        }

        public Header[] getHeaders() {
            return headers;
        }
    }
}
