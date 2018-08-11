package org.fogbowcloud.manager.core.models.tokens.generators.openstack.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.*;
import org.fogbowcloud.manager.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class KeystoneV3TokenGeneratorTest {

    @SuppressWarnings("unused")
    private final String KEYSTONE_URL = "http://localhost:0000";
    private KeystoneV3TokenGenerator keystoneV3TokenGenerator;
    private HttpRequestClientUtil httpRequestClientUtil;
    private HttpClient client;
    
    private static final String UTF_8 = "UTF-8";
    
    private final String userId = "3e57892203271c195f5d473fc84f484b8062103275ce6ad6e7bcd1baedf70d5c";
    private final String userName = "fogbow";
    private final String userPass = "UserPass";
    private final String method = "password";
    private final String roleId = "9fe2ff9ee4384b1894a90878d3e92bab";
    private final String roleName = "Member";
    private final Date expireDate = new Date();
    private final String domainId = "2a73b8f597c04551a0fdc8e95544be8a";
    private final String domainName = "LSD";
    private final String projectId = "3324431f606d4a74a060cf78c16fcb21";
    private final String projectName = "naf-lsd-site";

    @Before
    public void setUp() throws Exception {
        HomeDir.getInstance().setPath("src/test/resources/private");
        this.client = Mockito.spy(HttpClient.class);
        this.httpRequestClientUtil = Mockito.spy(new HttpRequestClientUtil(this.client));
        this.keystoneV3TokenGenerator = Mockito.spy(new KeystoneV3TokenGenerator());
        this.keystoneV3TokenGenerator.setClient(this.httpRequestClientUtil);
    }
    
    //test case: Check if CreateToken is returning the expected token returned by the http response.
    @Test
    public void testCreateToken() throws JSONException, IOException, UnexpectedException, FogbowManagerException {
    	//set up
        Map<String, String> credentials = new HashMap<String, String>();
        credentials.put(KeystoneV3TokenGenerator.USER_ID, userId);
        credentials.put(KeystoneV3TokenGenerator.PASSWORD, userPass);
        credentials.put(KeystoneV3TokenGenerator.PROJECT_ID, projectName);
        credentials.put(KeystoneV3TokenGenerator.PROJECT_ID, projectId);

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
        
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);
        
        // exercise
        OpenStackV3Token token = (OpenStackV3Token) this.keystoneV3TokenGenerator.createToken(credentials);

        //verify
        assertNotNull(token);

        assertEquals(token.getProjectId(), projectId);
    }
    
    //test case: check if mountJson is creating a correct json from credentials.
    @Test
    public void testRequestMountJson() throws JSONException {
    	//set up
        Map<String, String> credentials = new HashMap<String, String>();
        credentials.put(KeystoneV3TokenGenerator.USER_ID, userId);
        credentials.put(KeystoneV3TokenGenerator.PASSWORD, userPass);
        credentials.put(KeystoneV3TokenGenerator.PROJECT_ID, projectId);
        
        //exercise
        String jsonStr = this.keystoneV3TokenGenerator.mountJsonBody(credentials);
        JSONObject json = new JSONObject(jsonStr);

        //verify
        JSONObject auth = json.getJSONObject("auth");
        JSONObject identity = auth.getJSONObject("identity");
        JSONArray methods = identity.getJSONArray("methods");
        JSONObject password = identity.getJSONObject("password");
        JSONObject user = password.getJSONObject("user");
        JSONObject scope = auth.getJSONObject("scope");
        JSONObject project = scope.getJSONObject("project");
        
        assertNotNull(json);
        assertNotNull(auth);
        assertNotNull(identity);
        assertNotNull(methods);
        assertEquals(1, methods.length());
        assertEquals("password", methods.getString(0));
        assertNotNull(password);
        assertNotNull(user);
        assertEquals(userId, user.getString("id"));
        assertEquals(userPass, user.getString("password"));
        assertNotNull(scope);
        assertNotNull(project);
        assertEquals(projectId, project.getString("id"));
    }
    
    //test case: createToken must throw UnauthorizedRequestException when the http request is forbidden (403).
    @Test (expected = UnauthorizedRequestException.class)
    public void testMissingTokenParameters()
            throws JSONException, IOException, UnexpectedException, FogbowManagerException {
    	//set up
        Map<String, String> credentials = new HashMap<String, String>();
        credentials.put(KeystoneV3TokenGenerator.USER_ID, userId);
        credentials.put(KeystoneV3TokenGenerator.PASSWORD, userPass);
        credentials.put(KeystoneV3TokenGenerator.PROJECT_ID, projectId);

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
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_FORBIDDEN, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);

        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);
        
        //exercise/verify
        OpenStackV3Token token = (OpenStackV3Token) this.keystoneV3TokenGenerator.createToken(credentials);
    }
    
    //test case: check if createToken throws UnauthenticatedUserException when the http request is Unauthorized (401).
    @Test (expected = UnauthenticatedUserException.class)
    public void testCheckStatusResponseWhenUnauthorized()
            throws ClientProtocolException, IOException, FogbowManagerException, UnexpectedException {
    	//set up
        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        Mockito.doReturn("").when(credentials).get(Mockito.any());
        Mockito.doReturn("").when(this.keystoneV3TokenGenerator).mountJsonBody(Mockito.any());

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus =
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_UNAUTHORIZED, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);
        
        //exercise/verify
        OpenStackV3Token token = (OpenStackV3Token) this.keystoneV3TokenGenerator.createToken(credentials);
    }
    
    //test case: check if createToken throws InstanceNotFoundException when the http request is Not Found (404).
    @Test (expected = InstanceNotFoundException.class)
    public void testCheckStatusResponseWhenSCNotFound()
            throws ClientProtocolException, IOException, FogbowManagerException, UnexpectedException {
    	//set up
        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        Mockito.doReturn("").when(credentials).get(Mockito.any());
        Mockito.doReturn("").when(this.keystoneV3TokenGenerator).mountJsonBody(Mockito.any());

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus =
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_NOT_FOUND, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);
        
        //exercise/verify
        OpenStackV3Token token = (OpenStackV3Token) this.keystoneV3TokenGenerator.createToken(credentials);
    }
    
    //test case: check if createToken throws InvalidParameterException when the http request is Bad Request (400).
    @Test (expected = InvalidParameterException.class)
    public void testCheckStatusResponseWhenBadRequest()
            throws ClientProtocolException, IOException, FogbowManagerException, UnexpectedException {
    	//set up
        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        Mockito.doReturn("").when(credentials).get(Mockito.any());
        Mockito.doReturn("").when(this.keystoneV3TokenGenerator).mountJsonBody(Mockito.any());

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        BasicStatusLine basicStatus =
                new BasicStatusLine(new ProtocolVersion("", 0, 0), HttpStatus.SC_BAD_REQUEST, "");
        Mockito.when(httpResponse.getStatusLine()).thenReturn(basicStatus);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);
        
        //exercise/verify
        OpenStackV3Token token = (OpenStackV3Token) this.keystoneV3TokenGenerator.createToken(credentials);
    }
    
    //test case: check if createToken throws UnavailableProviderException when the http request host is Unknown.
    @Test (expected = UnavailableProviderException.class)
    public void testDoPostRequestOnUnknownHostException()
            throws ClientProtocolException, IOException, FogbowManagerException, UnexpectedException {
    	//set up
        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        Mockito.doReturn("").when(credentials).get(Mockito.any());
        Mockito.doReturn("").when(this.keystoneV3TokenGenerator).mountJsonBody(Mockito.any());

        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        HttpEntity httpEntity = Mockito.mock(HttpEntity.class);
        Mockito.when(httpResponse.getEntity()).thenReturn(httpEntity);
        Mockito.when(httpResponse.getAllHeaders()).thenReturn(new Header[0]);
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class)))
                .thenThrow(new UnknownHostException());
        
        //exercise/verify
        OpenStackV3Token token = (OpenStackV3Token) this.keystoneV3TokenGenerator.createToken(credentials);
    }
    
    //test case: check if createToken throws InvalidParameterException when the credentials attributes is incomplete.
    @Test (expected = InvalidParameterException.class)
    public void testCreateTokenOnJsonException() throws FogbowManagerException, UnexpectedException {
    	//set up
        Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        Mockito.doThrow(new JSONException("")).when(this.keystoneV3TokenGenerator).mountJsonBody(Mockito.any());
        //exercise/verify
        OpenStackV3Token token = (OpenStackV3Token) this.keystoneV3TokenGenerator.createToken(credentials);
    }
    
    //test case: check if the authUrl in credentials is used in the http request url when the authUrl is not null.
    @Test
    public void testCreateTokenWhenAuthUrlIsNotEmpty() throws ClientProtocolException, IOException, FogbowManagerException, UnexpectedException {
    	//set up
    	Map<String, String> credentials = Mockito.spy(new HashMap<String, String>());
        String authUrl = "auth-url";
        Mockito.doReturn(authUrl).when(credentials).get(Mockito.any());
        String jsonStr = "";
		Mockito.doReturn(jsonStr).when(this.keystoneV3TokenGenerator).mountJsonBody(Mockito.any());
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
        Mockito.when(this.client.execute(Mockito.any(HttpPost.class))).thenReturn(httpResponse);
        String currentTokenEndpoint = authUrl + "/auth/tokens";
        
        //exercise
        OpenStackV3Token token = (OpenStackV3Token) this.keystoneV3TokenGenerator.createToken(credentials);

        //verify
        Mockito.verify(this.httpRequestClientUtil, Mockito.times(1)).doPostRequest(currentTokenEndpoint, jsonStr);
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
}
