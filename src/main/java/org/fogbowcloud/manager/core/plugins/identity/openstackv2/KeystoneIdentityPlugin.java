package org.fogbowcloud.manager.core.plugins.identity.openstackv2;


import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.models.Credential;
import org.fogbowcloud.manager.core.utils.HttpRequestUtil;
import org.fogbowcloud.manager.core.models.token.Token;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class KeystoneIdentityPlugin implements IdentityPlugin {

	public static final String OPEN_STACK_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	// keystone json data
	public static final String TENANT_NAME_PROP = "tenantName";
	public static final String USERNAME_PROP = "username";
	public static final String PASSWORD_PROP = "password";
	public static final String PASSWORD_CREDENTIALS_PROP = "passwordCredentials";
	public static final String AUTH_PROP = "auth";
	public static final String TOKEN_PROP = "token";
	public static final String ID_PROP = "id";
	public static final String TENANT_PROP = "tenant";
	public static final String TENANTS_PROP = "tenants";
	public static final String ACCESS_PROP = "access";
	public static final String EXPIRES_PROP = "expires";
	public static final String USER_PROP = "user";
	public static final String NAME_PROP = "name";
	
	public static final String AUTH_URL = "authUrl";
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String TENANT_NAME = "tenantName";
	public static final String TENANT_ID = "tenantId";	

	private static final int LAST_SUCCESSFUL_STATUS = 204;
	protected static final String USER_CREDENTIALS_ARE_WRONG = "Missing user credentials. " 
			+ USERNAME + ", " + PASSWORD + ", " + TENANT_NAME + " are required.";
	private final static Logger LOGGER = Logger.getLogger(KeystoneIdentityPlugin.class);
	/*
	 * The json response format can be seen in the following link:
	 * http://developer.openstack.org/api-ref-identity-v2.html
	 */
	public static String V2_TOKENS_ENDPOINT_PATH = "/v2.0/tokens";
	public static String V2_TENANTS_ENDPOINT_PATH = "/v2.0/tenants";

	private String keystoneUrl; 
	private String v2TokensEndpoint;
	private String v2TenantsEndpoint;
	private HttpClient client;
	private Properties properties;

	public KeystoneIdentityPlugin(Properties properties) {		
		this.properties = properties;
		this.keystoneUrl = properties.getProperty("identity_url") == null ? 
				properties.getProperty(AUTH_URL) : properties.getProperty("identity_url");
		this.v2TokensEndpoint = keystoneUrl + V2_TOKENS_ENDPOINT_PATH;
		this.v2TenantsEndpoint = keystoneUrl + V2_TENANTS_ENDPOINT_PATH;
		
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}
	
	@Override
	public Token createToken(Map<String, String> credentials) {
		JSONObject json;
		try {
			json = mountJson(credentials);
		} catch (JSONException e) {
			LOGGER.error("Could not mount JSON while creating token.", e);
			Integer statusResponse = HttpStatus.SC_BAD_REQUEST;
			throw new RuntimeException(statusResponse.toString());
		}

		String authUrl = credentials.get(AUTH_URL);
		String currentTokenEndpoint = v2TokensEndpoint;
		if (authUrl != null && !authUrl.isEmpty()) {
			currentTokenEndpoint = authUrl + V2_TOKENS_ENDPOINT_PATH;
		}
		
		String responseStr = doPostRequest(currentTokenEndpoint, json);
		Token token = getTokenFromJson(responseStr);
		
		return token;
	}
	
	private JSONObject mountJson(Map<String, String> credentials) throws JSONException {
		JSONObject passwordCredentials = new JSONObject();
		passwordCredentials.put(USERNAME_PROP, credentials.get(USERNAME));
		passwordCredentials.put(PASSWORD_PROP, credentials.get(PASSWORD));
		JSONObject auth = new JSONObject();
		auth.put(TENANT_NAME_PROP, credentials.get(TENANT_NAME));
		auth.put(PASSWORD_CREDENTIALS_PROP, passwordCredentials);
		JSONObject root = new JSONObject();
		root.put(AUTH_PROP, auth);
		return root;
	}

	private String doPostRequest(String endpoint, JSONObject json) {
		HttpResponse response = null;
		String responseStr = null;
		try {
			HttpPost request = new HttpPost(endpoint);
			request.addHeader(HttpRequestUtil.CONTENT_TYPE, HttpRequestUtil.JSON_CONTENT_TYPE);
			request.addHeader(HttpRequestUtil.ACCEPT, HttpRequestUtil.JSON_CONTENT_TYPE);
			request.setEntity(new StringEntity(json.toString(), Charsets.UTF_8));
			response = getClient().execute(request);
			responseStr = EntityUtils.toString(response.getEntity(), Charsets.UTF_8);
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
			} catch (Throwable t) {
				// Do nothing
			}
		}
		checkStatusResponse(response);

		return responseStr;
	}

	private HttpClient getClient() {
		if (client == null) {
			client = HttpRequestUtil.createHttpClient();
		}
		return client;
	}

	@Override
	public Token reIssueToken(Token token) {
		JSONObject json;
		try {
			json = mountJson(token);
		} catch (JSONException e) {
			LOGGER.error("Could not mount JSON.", e);
			Integer statusResponse = HttpStatus.SC_BAD_REQUEST;
			throw new RuntimeException(statusResponse.toString());
		}

		String responseStr = doPostRequest(v2TokensEndpoint, json);
		return getTokenFromJson(responseStr);
	}

	private static JSONObject mountJson(Token token) throws JSONException {
		return mountJson(token.getAccessId(), token.get(TENANT_NAME));
	}
	
	private static JSONObject mountJson(String accessId, String tenantName) throws JSONException {
		JSONObject idToken = new JSONObject();
		idToken.put(ID_PROP, accessId);
		JSONObject auth = new JSONObject();
		auth.put(TENANT_NAME_PROP, tenantName);
		auth.put(TOKEN_PROP, idToken);
		JSONObject root = new JSONObject();
		root.put(AUTH_PROP, auth);
		return root;
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

	private Token getTokenFromJson(String responseStr) {
		try {			
			JSONObject root = new JSONObject(responseStr);
			JSONObject tokenKeyStone = root.getJSONObject(ACCESS_PROP).getJSONObject(TOKEN_PROP);
			String accessId = tokenKeyStone.getString(ID_PROP);
			
			Map<String, String> tokenAtt = new HashMap<String, String>();
			String tenantId = null;
			String tenantName = null;
			try {
				tenantId = tokenKeyStone.getJSONObject(TENANT_PROP).getString(ID_PROP);
				tokenAtt.put(TENANT_ID, tenantId);
			} catch (JSONException e) {
				LOGGER.debug("There is no tenantId inside json response.");
			}
			try {
				tenantName = tokenKeyStone.getJSONObject(TENANT_PROP).getString(NAME_PROP);
				tokenAtt.put(TENANT_NAME, tenantName);
			} catch (JSONException e) {
				LOGGER.debug("There is no tenantName inside json response.");
			}
			
			String expirationDateToken = tokenKeyStone.getString(EXPIRES_PROP);
			JSONObject userJsonObject = root.getJSONObject(ACCESS_PROP)
					.getJSONObject(USER_PROP);
			String user = userJsonObject.getString(NAME_PROP);
			String id = userJsonObject.getString(ID_PROP);

			LOGGER.debug("json token: " + accessId + ", user name: " + user 
					+ ", user id: " + id + ", expirationDate: " + expirationDateToken 
					+ "json attributes: " + tokenAtt);
			
			return new Token(accessId, new Token.User(id, user), 
					getDateFromOpenStackFormat(expirationDateToken), tokenAtt);
		} catch (Exception e) {
			LOGGER.error("Exception while getting token from json.", e);
			return null;
		}
	}

	@Override
	public Token getToken(String accessId) {
		String tenantName = null;
		try {
			JSONObject decodedAccessId = new JSONObject(new String(Base64.decodeBase64(
					accessId.getBytes(Charsets.UTF_8)), Charsets.UTF_8));
			accessId = decodedAccessId.optString(ACCESS_PROP);
			tenantName = decodedAccessId.optString(TENANT_NAME);
		} catch (Exception e) {
			tenantName = getTenantName(accessId);
		}

		JSONObject root;
		try {
			root = mountJson(accessId, tenantName);
		} catch (JSONException e) {
			LOGGER.error("Could not mount JSON.", e);
			Integer statusResponse = HttpStatus.SC_BAD_REQUEST;
			throw new RuntimeException(statusResponse.toString());
		}

		String responseStr = doPostRequest(v2TokensEndpoint, root);
		return getTokenFromJson(responseStr);
	}

	private String getTenantName(String accessId) {
		HttpResponse response;
		String responseStr = null;
		try {
			HttpGet httpGet = new HttpGet(this.v2TenantsEndpoint);
			httpGet.addHeader(HttpRequestUtil.X_AUTH_TOKEN, accessId);
			httpGet.addHeader(HttpRequestUtil.CONTENT_TYPE, HttpRequestUtil.JSON_CONTENT_TYPE);
			httpGet.addHeader(HttpRequestUtil.ACCEPT, HttpRequestUtil.JSON_CONTENT_TYPE);

			getClient();
			response = client.execute(httpGet);
			responseStr = EntityUtils
					.toString(response.getEntity(), String.valueOf(Charsets.UTF_8));
		} catch (Exception e) {
			LOGGER.error("Could not get tenant name.", e);
			Integer statusResponse = HttpStatus.SC_BAD_REQUEST;
			throw new RuntimeException(statusResponse.toString());
		}
		checkStatusResponse(response);
		return getTenantNameFromJson(responseStr);
	}

	private String getTenantNameFromJson(String responseStr) {
		try {
			JSONObject root = new JSONObject(responseStr);
			JSONArray tenantsStone = root.getJSONArray(TENANTS_PROP);
			for (int i = 0; i < tenantsStone.length(); i++) {
				String currentTenantName = tenantsStone.getJSONObject(i).getString(NAME_PROP);
				// if tenantName is not the same of fogbow tenant
				if (currentTenantName != null
						&& !currentTenantName.equals(properties
								.getProperty("local_proxy_account_user_name"))) {
					return currentTenantName;
				}

			}	
			return tenantsStone.getJSONObject(0).getString(NAME_PROP); // getting first tenant
		} catch (JSONException e) {
			LOGGER.error("Could not get tenant name from JSON.", e);
			Integer statusResponse = HttpStatus.SC_BAD_REQUEST;
			throw new RuntimeException(statusResponse.toString());
		}
	}
	
	@Override
	public boolean isValid(String accessId) {
		try {
			getToken(accessId);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static String getDateOpenStackFormat(Date date) {
		SimpleDateFormat dateFormatOpenStack = new SimpleDateFormat(OPEN_STACK_DATE_FORMAT,
				Locale.ROOT);
		String expirationDate = dateFormatOpenStack.format(date);
		return expirationDate;

	}

	public static Date getDateFromOpenStackFormat(String expirationDateStr) {
		SimpleDateFormat dateFormatOpenStack = new SimpleDateFormat(OPEN_STACK_DATE_FORMAT,
				Locale.ROOT);
		try {
			return dateFormatOpenStack.parse(expirationDateStr);
		} catch (Exception e) {
			LOGGER.error("Exception while parsing date.", e);
			return null;
		}
	}

	@Override
	public Credential[] getCredentials() {
		return new Credential[] { new Credential(USERNAME, true, null),
				new Credential(PASSWORD, true, null), new Credential(TENANT_NAME, true, null),
				new Credential(AUTH_URL, true, null) };
	}

	@Override
	public String getAuthenticationURI() {
		return "Keystone uri='" + keystoneUrl +"'";
	}

	@Override
	public Token getForwardableToken(Token originalToken) {
		return originalToken;
	}	
 }