package org.fogbowcloud.manager.core.plugins.cloud.localidentity.openstack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicStatusLine;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.KeystoneV3IdentityPlugin;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.models.token.Token;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class KeystoneV3IdentityTest {

    @SuppressWarnings("unused")
    private final String KEYSTONE_URL = "http://localhost:0000";
    private KeystoneV3IdentityPlugin keystoneV3Identity;
    private HttpClient client;

    private static final String UTF_8 = "UTF-8";

    @Before
    public void setUp() throws Exception {
        HomeDir.getInstance().setPath("src/test/resources/private");
        client = Mockito.mock(HttpClient.class);
        this.keystoneV3Identity = Mockito.spy(new KeystoneV3IdentityPlugin(client));
    }

    @Test
    public void testCreateToken() throws JSONException, ClientProtocolException, IOException {

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

        Map<String, String> credentials = new HashMap<String, String>();
        credentials.put(KeystoneV3IdentityPlugin.USER_ID, userId);
        credentials.put(KeystoneV3IdentityPlugin.PASSWORD, userPass);
        credentials.put(KeystoneV3IdentityPlugin.TENANT_ID, projectName);
        credentials.put(KeystoneV3IdentityPlugin.PROJECT_ID, projectId);

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);

        JSONObject returnJson =
                createJsonResponse(
                        method,
                        roleId,
                        roleName,
                        expireDate,
                        domainId,
                        domainName,
                        projectId,
                        projectName,
                        userId,
                        userName);

        String content = returnJson.toString();

        InputStream contentInputStream = new ByteArrayInputStream(content.getBytes(UTF_8));
        Mockito.when(httpEntity.getContent()).thenReturn(contentInputStream);

        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus =
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_OK, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);

        Mockito.when(client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);

        Token token = keystoneV3Identity.createToken(credentials);

        assertNotNull(token);

        assertEquals(userName, token.getUser().getName());
        assertEquals(userId, token.getUser().getId());
        assertEquals(token.get("tenantName"), projectName);
        assertEquals(token.get("tenantId"), projectId);
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
    public void testMissingTokenParameters()
            throws JSONException, ClientProtocolException, IOException {

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

        Map<String, String> credentials = new HashMap<String, String>();
        credentials.put(KeystoneV3IdentityPlugin.USER_ID, userId);
        credentials.put(KeystoneV3IdentityPlugin.PASSWORD, userPass);
        credentials.put(KeystoneV3IdentityPlugin.TENANT_ID, projectName);
        credentials.put(KeystoneV3IdentityPlugin.PROJECT_ID, projectId);

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);

        JSONObject returnJson =
                createJsonResponse(
                        method,
                        roleId,
                        roleName,
                        expireDate,
                        domainId,
                        domainName,
                        projectId,
                        projectName,
                        userId,
                        userName);

        returnJson.remove("token");

        String content = returnJson.toString();

        InputStream contentInputStream = new ByteArrayInputStream(content.getBytes(UTF_8));
        Mockito.when(httpEntity.getContent()).thenReturn(contentInputStream);

        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus =
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_OK, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);

        Mockito.when(client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);

        Token token = keystoneV3Identity.createToken(credentials);

        assertNull(token);
    }

    private JSONObject createJsonResponse(
            String method,
            String roleId,
            String roleName,
            Date expireDate,
            String domainId,
            String domainName,
            String projectId,
            String projectName,
            String userId,
            String userName)
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
        userJson.put("id", userId);
        userJson.put("name", userName);
        tokenJson.put("user", userJson);

        returnJson.put("token", tokenJson);

        return returnJson;
    }

    @Test
    public void testCheckStatusResponseWhenUnauthorized()
            throws ClientProtocolException, IOException {

        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        Mockito.doReturn("").when(credentials).get(Mockito.any());
        Mockito.doReturn(new JSONObject()).when(keystoneV3Identity).mountJson(Mockito.any());

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus =
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_UNAUTHORIZED, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);

        try {
            this.keystoneV3Identity.createToken(credentials);
            Assert.fail();
        } catch (RuntimeException runtimeException) {
            Integer expectedStatusResponse = HttpStatus.SC_UNAUTHORIZED;
            Assert.assertEquals(expectedStatusResponse.toString(), runtimeException.getMessage());
        }
    }

    @Test
    public void testCheckStatusResponseWhenSCNotFound()
            throws ClientProtocolException, IOException {

        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        Mockito.doReturn("").when(credentials).get(Mockito.any());
        Mockito.doReturn(new JSONObject()).when(keystoneV3Identity).mountJson(Mockito.any());

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus =
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_NOT_FOUND, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);

        try {
            this.keystoneV3Identity.createToken(credentials);
            Assert.fail();
        } catch (RuntimeException runtimeException) {
            Integer expectedStatusResponse = HttpStatus.SC_NOT_FOUND;
            Assert.assertEquals(expectedStatusResponse.toString(), runtimeException.getMessage());
        }
    }

    @Test
    public void testCheckStatusResponseWhenBadRequest()
            throws ClientProtocolException, IOException {

        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        Mockito.doReturn("").when(credentials).get(Mockito.any());
        Mockito.doReturn(new JSONObject()).when(keystoneV3Identity).mountJson(Mockito.any());

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus =
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_BAD_REQUEST, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);

        try {
            this.keystoneV3Identity.createToken(credentials);
            Assert.fail();
        } catch (RuntimeException runtimeException) {
            Integer expectedStatusResponse = HttpStatus.SC_BAD_REQUEST;
            Assert.assertEquals(expectedStatusResponse.toString(), runtimeException.getMessage());
        }
    }

    @Test
    public void testDoPostRequestOnUnknownHostException()
            throws ClientProtocolException, IOException {
        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        Mockito.doReturn("").when(credentials).get(Mockito.any());
        Mockito.doReturn(new JSONObject()).when(keystoneV3Identity).mountJson(Mockito.any());

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus =
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_BAD_REQUEST, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(client.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new UnknownHostException());
        try {
            keystoneV3Identity.createToken(credentials);
        } catch (RuntimeException runtimeException) {
            Integer expectedStatusResponse = HttpStatus.SC_BAD_REQUEST;
            Assert.assertEquals(expectedStatusResponse.toString(), runtimeException.getMessage());
        }
    }

    @Test
    public void testDoPostRequestOnRuntimeException() throws ClientProtocolException, IOException {
        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        Mockito.doReturn("").when(credentials).get(Mockito.any());
        Mockito.doReturn(new JSONObject()).when(keystoneV3Identity).mountJson(Mockito.any());

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus =
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_BAD_REQUEST, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(client.execute(Mockito.any(HttpPost.class))).thenThrow(new RuntimeException());
        try {
            keystoneV3Identity.createToken(credentials);
        } catch (Exception exception) {
            Integer expectedStatusResponse = HttpStatus.SC_BAD_REQUEST;
            Assert.assertEquals(expectedStatusResponse.toString(), exception.getMessage());
        }
    }

    @Test
    public void testCreateTokenOnJsonException() {
        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        Mockito.doThrow(new JSONException("")).when(keystoneV3Identity).mountJson(Mockito.any());

        try {
            keystoneV3Identity.createToken(credentials);
        } catch (IllegalArgumentException illegalArgumentException) {
            Integer expectedStatusResponse = HttpStatus.SC_BAD_REQUEST;
            Assert.assertEquals(
                    expectedStatusResponse.toString(), illegalArgumentException.getMessage());
        }
    }

    @Test
    public void testCreateTokenWhenAuthUrlIsNotEmpty() throws ClientProtocolException, IOException {
        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        String authUrl = "auth-url";

        Mockito.doReturn(authUrl).when(credentials).get(Mockito.any());
        Mockito.doReturn(new JSONObject()).when(keystoneV3Identity).mountJson(Mockito.any());

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus =
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_OK, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);

        try {
            this.keystoneV3Identity.createToken(credentials);
        } catch (RuntimeException runtimeException) {
            Assert.fail();
        }
    }
}
