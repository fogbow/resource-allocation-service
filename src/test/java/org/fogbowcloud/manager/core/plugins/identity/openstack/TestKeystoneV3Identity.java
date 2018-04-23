package org.fogbowcloud.manager.core.plugins.identity.openstack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpStatus;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.utils.HttpRequestUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.fogbowcloud.manager.core.models.token.Token.User;

public class TestKeystoneV3Identity {
	
	private static final String IDENTITY_URL_KEY = "identity_url";
	private final String KEYSTONE_URL = "http://localhost:" + "0000";
	private KeystoneV3IdentityPlugin keystoneV3Identity;
	private HttpClient client;

	private static final String UTF_8 = "UTF-8";

	@Before
	public void setUp() throws Exception {
		Properties properties = new Properties();
		properties.put(IDENTITY_URL_KEY, KEYSTONE_URL);
		client = Mockito.mock(HttpClient.class);
		this.keystoneV3Identity = new KeystoneV3IdentityPlugin(properties, client);

	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testRequestMountJson() throws JSONException {

		String userId = "userID";
		String userPass = "UserPass";
		String tenantId = "tenantID";
		String projectId = "projectID";

		Map<String, String> credentials = new HashMap<String, String>();
		credentials.put(KeystoneV3IdentityPlugin.USER_ID, userId);
		credentials.put(KeystoneV3IdentityPlugin.PASSWORD, userPass);
		credentials.put(KeystoneV3IdentityPlugin.TENANT_ID, tenantId);
		credentials.put(KeystoneV3IdentityPlugin.PROJECT_ID, projectId);

		JSONObject json = keystoneV3Identity.mountJson(credentials);

		assertNotNull(json);
		JSONObject auth = json.getJSONObject("auth");
		assertNotNull(auth);

		JSONObject identity = auth.getJSONObject("identity");
		assertNotNull(identity);
		JSONArray methods = identity.getJSONArray("methods");
		assertNotNull(methods);
		assertEquals(1, methods.length());
		assertEquals("password", methods.getString(0));
		JSONObject password = identity.getJSONObject("password");
		assertNotNull(password);

		JSONObject user = password.getJSONObject("user");
		assertNotNull(user);
		assertEquals(userId, user.getString("id"));
		assertEquals(userPass, user.getString("password"));

		JSONObject scope = auth.getJSONObject("scope");
		assertNotNull(scope);
		JSONObject project = scope.getJSONObject("project");
		assertNotNull(project);
		assertEquals(projectId, project.getString("id"));

	}

	@Test
	public void testDoRequest() throws JSONException, ClientProtocolException, IOException {

		String userId = "3e57892203271c195f5d473fc84f484b8062103275ce6ad6e7bcd1baedf70d5c";
		String userName = "fogbow";
		String userPass = "UserPass";

		String method = "password";
		String roleId = "9fe2ff9ee4384b1894a90878d3e92bab";
		String roleName = "Member";
		Date expireDate = new Date();
		String domainId = "2a73b8f597c04551a0fdc8e95544be8a";
		String domainName = "LSD";
		String projectId = "3324431f606d4a74a060cf78c16fcb21";
		String projectName = "naf-lsd-site";
		
		String endpoint = "http://localhost:60423/v3/auth/tokens";

		Map<String, String> credentials = new HashMap<String, String>();
		credentials.put(KeystoneV3IdentityPlugin.USER_ID, userId);
		credentials.put(KeystoneV3IdentityPlugin.PASSWORD, userPass);
		credentials.put(KeystoneV3IdentityPlugin.TENANT_ID, projectName);
		credentials.put(KeystoneV3IdentityPlugin.PROJECT_ID, projectId);

		JSONObject json = keystoneV3Identity.mountJson(credentials);

		HttpPost request = new HttpPost(endpoint);
		request.addHeader(HttpRequestUtil.CONTENT_TYPE, HttpRequestUtil.JSON_CONTENT_TYPE);
		request.addHeader(HttpRequestUtil.ACCEPT, HttpRequestUtil.JSON_CONTENT_TYPE);
		request.setEntity(new StringEntity(json.toString(), StandardCharsets.UTF_8));
		// response = getClient().execute(request);

		HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
		HttpEntity httpEntity = Mockito.mock(HttpEntity.class);

		JSONObject returnJson = createJsonReturn(method, roleId, roleName, expireDate, domainId, domainName, projectId,
				projectName, userId, userName);

		String content = returnJson.toString();

		InputStream contentInputStream = new ByteArrayInputStream(content.getBytes(UTF_8));
		Mockito.when(httpEntity.getContent()).thenReturn(contentInputStream);
		Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
		BasicStatusLine basicStatus = new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_OK, "");
		Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
		Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
		//POST http://localhost:60565/v3/auth/tokens HTTP/1.1
		Mockito.when(client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);

		Token token = keystoneV3Identity.createToken(credentials);

		assertNotNull(token);

	}

	@Test
	public void testForwardableToken() {
		
		String accessId = "access_ID";
		User user = new User("user_ID", "user_name");
		Date expirationTime = new Date();
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("propA", "propriety_name");
		
		Token token = new Token(accessId, user, expirationTime, attributes);
		
		Token tokenReturned = this.keystoneV3Identity.getForwardableToken(token);
		
		assertNotNull(tokenReturned);
		assertEquals(token, tokenReturned);
		assertEquals(accessId, tokenReturned.getAccessId());
		assertEquals(user, tokenReturned.getUser());
		assertEquals(expirationTime, tokenReturned.getExpirationDate());
		assertEquals(attributes, tokenReturned.getAttributes());
	}

	@Test (expected = RuntimeException.class)
	public void testCreateToken() throws JSONException, ClientProtocolException, IOException {

		doReturn(null).when(client).execute(Mockito.any(HttpPost.class));

		Map<String, String> credentialsB = new HashMap<String, String>();
		credentialsB.put(KeystoneV3IdentityPlugin.USER_ID, "user");
		credentialsB.put(KeystoneV3IdentityPlugin.PASSWORD, "wrongpass");
		credentialsB.put(KeystoneV3IdentityPlugin.TENANT_ID, "project");
		credentialsB.put(KeystoneV3IdentityPlugin.PROJECT_ID, "projectid");
		
		Token token = keystoneV3Identity.createToken(credentialsB);

		assertNotNull(token);
	}
	

	private JSONObject createJsonReturn(String method, String roleId, String roleName, Date expireDate, String domainId,
			String domainName, String projectId, String projectName, String userId, String userName)
					throws JSONException {

		JSONObject returnJson = new JSONObject();
		JSONObject tokenJson = new JSONObject();

		// Add property "methods"
		JSONArray methodsJson = new JSONArray();
		methodsJson.put(method);
		tokenJson.put("methods", methodsJson);

		// Add property "roles"
		JSONArray rolesJson = new JSONArray();
		JSONObject roleJson = new JSONObject();
		roleJson.put("id", roleId);
		roleJson.put("name", roleName);
		rolesJson.put(roleJson);
		tokenJson.put("roles", rolesJson);

		// Add property "expires_at"
		tokenJson.put("expires_at", expireDate);

		// Domain Json
		JSONObject domainJson = new JSONObject();
		domainJson.put("id", domainId);
		domainJson.put("name", domainName);

		// Add property "project"
		JSONObject projectJson = new JSONObject();
		projectJson.put("domain", domainJson);
		projectJson.put("id", projectId);
		projectJson.put("name", projectName);
		tokenJson.put("project", projectJson);

		// Add property "user"
		JSONObject userJson = new JSONObject();
		userJson.put("domain", domainJson);
		userJson.put("id", "");
		userJson.put("name", "");
		tokenJson.put("user", userJson);

		returnJson.put("token", tokenJson);

		return returnJson;
	}
	
}
