package org.fogbowcloud.manager.core.models.tokens.generators;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;
import org.fogbowcloud.manager.core.models.tokens.OpenStackUserAttributes;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.OpenStackHttpToFogbowManagerExceptionMapper;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class KeystoneV3TokenGenerator implements LocalTokenGenerator<OpenStackUserAttributes> {

    private static final Logger LOGGER = Logger.getLogger(KeystoneV3TokenGenerator.class);

    private static final String OPENSTACK_KEYSTONE_V3_URL = "openstack_keystone_v3_url";
    private static final String X_SUBJECT_TOKEN = "X-Subject-LocalUserAttributes";
    private static final String PASSWORD_PROP = "password";
    private static final String IDENTITY_PROP = "identity";
    private static final String PROJECT_PROP = "project";
    private static final String METHODS_PROP = "methods";
    private static final String TOKEN_PROP = "token";
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
    public static final String AUTH_URL = "authUrl";
    public static String V3_TOKENS_ENDPOINT_PATH = "/auth/tokens";

    private String keystoneUrl;
    private String v3TokensEndpoint;
    private HttpRequestClientUtil client;

    public KeystoneV3TokenGenerator() throws FatalErrorException {
        HomeDir homeDir = HomeDir.getInstance();
        Properties properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);

        String identityUrl = properties.getProperty(OPENSTACK_KEYSTONE_V3_URL);
        if (isUrlValid(identityUrl)) {
            this.keystoneUrl = identityUrl;
            this.v3TokensEndpoint = keystoneUrl + V3_TOKENS_ENDPOINT_PATH;
        }
        this.client = new HttpRequestClientUtil();
    }

    private boolean isUrlValid(String url) throws FatalErrorException {
        if (url == null || url.trim().isEmpty()) {
            throw new FatalErrorException("Invalid Keystone_V3_URL " + OPENSTACK_KEYSTONE_V3_URL);
        }
        return true;
    }

    @Override
    public OpenStackUserAttributes createToken(Map<String, String> credentials) throws FogbowManagerException,
            UnexpectedException {
        LOGGER.debug("Creating new LocalUserAttributes");

        JSONObject json;
        try {
            json = mountJson(credentials);
        } catch (JSONException e) {
            String errorMsg = "An error occurred when generating json.";
            LOGGER.error(errorMsg, e);
            throw new InvalidParameterException(errorMsg, e);
        }

        String authUrl = credentials.get(AUTH_URL);
        String currentTokenEndpoint = v3TokensEndpoint;
        if (authUrl != null && !authUrl.isEmpty()) {
            currentTokenEndpoint = authUrl + V3_TOKENS_ENDPOINT_PATH;
        }

        HttpRequestClientUtil.Response response = null;
        try {
            response = this.client.doPostRequest(currentTokenEndpoint, json);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowManagerExceptionMapper.map(e);
        }

        OpenStackUserAttributes localUserAttributes = getTokenFromJson(response);
        return localUserAttributes;
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

    private OpenStackUserAttributes getTokenFromJson(HttpRequestClientUtil.Response response) throws UnexpectedException {
        String tokenValue = null;
        Header[] headers = response.getHeaders();
        for (Header header : headers) {
            if (header.getName().equals(X_SUBJECT_TOKEN)) {
                tokenValue = header.getValue();
            }
        }

        try {
            JSONObject root = new JSONObject(response.getContent());
            JSONObject token = root.getJSONObject(TOKEN_PROP);

            String tenantId = token.getJSONObject(PROJECT_PROP).getString(ID_PROP);
            return new OpenStackUserAttributes(tokenValue, tenantId);
        } catch (Exception e) {
            LOGGER.error("Exception while getting tokens from json", e);
            throw new UnexpectedException("Exception while getting tokens from json", e);
        }
    }
    
    protected void setClient (HttpRequestClientUtil client) {
    	this.client = client;
    }
}
