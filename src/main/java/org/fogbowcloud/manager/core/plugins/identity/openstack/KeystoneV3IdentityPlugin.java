package org.fogbowcloud.manager.core.plugins.identity.openstack;

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
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.utils.HttpRequestUtil;
import org.fogbowcloud.manager.core.models.Credential;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class KeystoneV3IdentityPlugin implements IdentityPlugin {

	private final static Logger LOGGER = Logger.getLogger(KeystoneV3IdentityPlugin.class);
	private static final String IDENTITY_URL = "identity_url";
	private static final String X_SUBJECT_TOKEN = "X-Subject-Token";
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

	private static final int LAST_SUCCESSFUL_STATUS = 204;
	public static String V3_TOKENS_ENDPOINT_PATH = "/v3/auth/tokens";
	public static final String AUTH_URL = "authUrl";

	private String keystoneUrl;
	private String v3TokensEndpoint;
	private HttpClient client;

	protected KeystoneV3IdentityPlugin(Properties properties, HttpClient client) {
		this(properties);
		if (client == null) {
			this.client = HttpRequestUtil.createHttpClient();
		} else {
			this.client = client;
		}
	}

	public KeystoneV3IdentityPlugin(Properties properties) {
		
		if (properties.getProperty(IDENTITY_URL) == null) {
			this.keystoneUrl = properties.getProperty(AUTH_URL);
		} else {
			this.keystoneUrl = properties.getProperty(IDENTITY_URL);
		}
		
		this.v3TokensEndpoint = keystoneUrl + V3_TOKENS_ENDPOINT_PATH;
	}

	@Override
	public Token createToken(Map<String, String> credentials) {

		LOGGER.debug("Creating new Token");

		JSONObject json;
		try {
			json = mountJson(credentials);
		} catch (JSONException e) {
			LOGGER.error("Could not mount JSON while creating token.", e);

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

	@Override
	public Token reIssueToken(Token token) {
		return token;
	}

	@Override
	public Token getToken(String accessId) {
		// TODO Implement this method?
		return null;
	}

	@Override
	public boolean isValid(String accessId) {
		// TODO Implement this method?
		return true;
	}

	@Override
	public Credential[] getCredentials() {
		return new Credential[] { new Credential(PROJECT_ID, true, null), new Credential(PASSWORD, true, null),
				new Credential(USER_ID, true, null), new Credential(AUTH_URL, true, null) };
	}

	@Override
	public String getAuthenticationURI() {

		StringBuilder sb = new StringBuilder();
		sb.append("Keystone uri='");
		sb.append(keystoneUrl);
		sb.append("'");

		return sb.toString();
	}

	@Override
	public Token getForwardableToken(Token originalToken) {
		return originalToken;
	}

	private Response doPostRequest(String endpoint, JSONObject json) {
		HttpResponse response = null;
		String responseStr = null;
		try {
			HttpPost request = new HttpPost(endpoint);
			request.addHeader(HttpRequestUtil.CONTENT_TYPE_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
			request.addHeader(HttpRequestUtil.ACCEPT_KEY, HttpRequestUtil.JSON_CONTENT_TYPE_KEY);
			request.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
			response = getClient().execute(request);
			responseStr = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
		} catch (UnknownHostException e) {
			LOGGER.error("Could not do post request, unknown host.", e);
			Integer statusResponse = HttpStatus.SC_BAD_REQUEST;
			throw new RuntimeException(statusResponse.toString());
		} catch (Exception e) {
			LOGGER.error("Could not do post request.", e);
			Integer statusResponse = HttpStatus.SC_BAD_REQUEST;
			throw new RuntimeException(statusResponse.toString());
		} finally {
			try {
				EntityUtils.consume(response.getEntity());
			} catch (Exception e) {
				LOGGER.error("Could not release HTTPEntity resources.", e);
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
		identity.put(METHODS_PROP, new JSONArray(new String[] { PASSWORD_PROP }));
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
				LOGGER.debug("There is no tenantId inside json response.");
			}
			try {
				tenantName = token.getJSONObject(PROJECT_PROP).getString(NAME_PROP);
				tokenAtt.put(TENANT_NAME, tenantName);
			} catch (JSONException e) {
				LOGGER.debug("There is no tenantName inside json response.");
			}

			return new Token(accessId, new Token.User(userId, userName), new Date(), tokenAtt);
		} catch (Exception e) {
			LOGGER.error("Exception while getting token from json.", e);
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
