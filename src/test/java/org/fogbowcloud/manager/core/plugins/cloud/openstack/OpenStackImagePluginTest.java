package org.fogbowcloud.manager.core.plugins.cloud.openstack;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.meta.When;

import org.apache.commons.httpclient.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.matchers.Equals;

import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import com.google.gson.Gson;

public class OpenStackImagePluginTest {

    private static final String FAKE_IMAGE_ID = "fake-image-id";
    private static final String FAKE_IMAGE_NAME = "fake-image-name";
    private static final long FAKE_SIZE = 10l;
    private static final long FAKE_MIN_DISK = 1l;
    private static final long FAKE_MIN_RAM = 2l;
    private static final String STATUS = "status";
    
    private OpenStackImagePlugin plugin;
    private Token token;
    private HttpRequestClientUtil client;
    private Properties properties;
    
    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
        this.plugin = Mockito.spy(new OpenStackImagePlugin());
        this.token = Mockito.mock(Token.class);
        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.properties = Mockito.mock(Properties.class);
    	this.plugin.setProperties(this.properties);
    	this.plugin.setClient(this.client);
    }
//    
//    @Test
//    public void getAllImagesTest() throws FogbowManagerException, UnexpectedException, HttpResponseException {
//        Map<String, String> allImages = new HashMap<>();
//        allImages.put("fake-image-id", "fake-image-name");
//        Mockito.doReturn(allImages).when(this.plugin).getImageNameAndIdMapFromAllAvailableImages(Mockito.any(Token.class), Mockito.anyString());
//        Assert.assertEquals(allImages, this.plugin.getAllImages(this.token));
//    }
    
    @Test (expected = UnexpectedException.class)
    public void getAllImagesTestThrowUnexpectedException() throws FogbowManagerException, UnexpectedException {
        Mockito.doThrow(UnexpectedException.class).when(this.plugin).getAllImages(Mockito.any(Token.class));
        this.plugin.getAllImages(this.token);
    }
    
    @Test (expected = FogbowManagerException.class)
    public void getAllImagesTestThrowFogbowManagerException() throws FogbowManagerException, UnexpectedException {
        Mockito.doThrow(FogbowManagerException.class).when(this.plugin).getAllImages(Mockito.any(Token.class));
        this.plugin.getAllImages(this.token);
    }
    
    @Test
    public void getImageTest() throws FogbowManagerException, UnexpectedException {
        Image image = new Image(FAKE_IMAGE_ID, FAKE_IMAGE_NAME, FAKE_SIZE, FAKE_MIN_DISK, FAKE_MIN_RAM, STATUS);
        Mockito.doReturn(image).when(this.plugin).getImage(Mockito.anyString(), Mockito.any(Token.class));
        Assert.assertEquals(image, this.plugin.getImage(FAKE_IMAGE_ID, this.token));        
    }
    
    @Test (expected = UnexpectedException.class)
    public void getImageThrowUnexpectedExceptionTest() throws FogbowManagerException, UnexpectedException {
        Mockito.doThrow(UnexpectedException.class).when(this.plugin).getImage(Mockito.anyString(), Mockito.any(Token.class));
        this.plugin.getImage(FAKE_IMAGE_ID, this.token);
    }
    
    @Test (expected = FogbowManagerException.class)
    public void getImageThrowFogbowManagerExceptionTest() throws FogbowManagerException, UnexpectedException {
        Mockito.doThrow(FogbowManagerException.class).when(this.plugin).getImage(Mockito.anyString(), Mockito.any(Token.class));
        this.plugin.getImage(FAKE_IMAGE_ID, this.token);
    }

//    public void getJsonObjectImageTest() throws HttpResponseException, UnavailableProviderException {
//    	String imageId = "12345";
//    	String jsonResponse = "{}";
//    	String imageGlancev2UrlKey = "key";
//    	
//    	Mockito.when(this.properties.getProperty(OpenStackImagePlugin.IMAGE_GLANCEV2_URL_KEY)).thenReturn(imageGlancev2UrlKey);
//    	
//    	String endpoint = 
//				imageGlancev2UrlKey
//                + OpenStackImagePlugin.COMPUTE_V2_API_ENDPOINT
//                + OpenStackImagePlugin.SUFFIX
//                + OpenStackImagePlugin.QUERY_ACTIVE_IMAGES;
//    	
//    	Mockito.when(this.client.doGetRequest(imageId, Mockito.any())).thenReturn(jsonResponse);
//    	equals(plugin.get)
//    }
    
    @Test
    public void getAllImagesTest() throws FogbowManagerException, UnexpectedException, HttpResponseException {
    	
    	String tenantId = "tenant-id";
    	String imageGlancev2UrlKey = "image-url-key";
    	String endpoint = 
    				imageGlancev2UrlKey
	                + OpenStackImagePlugin.COMPUTE_V2_API_ENDPOINT
	                + OpenStackImagePlugin.SUFFIX
	                + OpenStackImagePlugin.QUERY_ACTIVE_IMAGES;
    	List<Map<String, String>> generatedImages = generateImages(tenantId);
    	
    	String jsonResponse = getImagesJson(generatedImages);
    	
    	Mockito.when(this.token.getAttributes().get(OpenStackImagePlugin.TENANT_ID)).thenReturn(tenantId);
    	Mockito.when(this.properties.getProperty(OpenStackImagePlugin.IMAGE_GLANCEV2_URL_KEY)).thenReturn(imageGlancev2UrlKey);
    	Mockito.when(this.client.doGetRequest(endpoint, token)).thenReturn(jsonResponse);
    	
    	Map<String, String> expectedOutput = getPublicImages(generatedImages);
    	expectedOutput.putAll(getPrivateImagesFromProject(generatedImages, tenantId));
    	System.out.println(expectedOutput.toString());
    	Assert.assertEquals(expectedOutput, this.plugin.getAllImages(token));
    }
    
    private String getImagesJson(List<Map<String, String>> imagesList) {
    	Map <String, Object> jsonMap= new HashMap<String, Object>();
    	jsonMap.put("images", imagesList);
    	Gson gson = new Gson();
    	return gson.toJson(jsonMap);
    }
    
    private List<Map<String, String>> generateImages(String tenantId){
    	
    	int qtdImages = 5;
    	String tenantId2 = tenantId + "2";
    	List<Map<String, String>> myList = new ArrayList<Map<String, String>>();
    	
    	for (int i = 0; i < qtdImages; i++) {
    		Map <String, String> image = new HashMap<String, String>();
    		image.put("visibility", i % 2 == 0? "public" : "private");
    		image.put("owner", i < qtdImages / 2 ? tenantId2 : tenantId);
    		image.put("id", "id" + Integer.toString(i));
    		image.put("name", "name" + Integer.toString(i));
    		myList.add(image);
    	}
    	
    	for (int i = 0; i < qtdImages; i++) {
    		Map <String, String> image = new HashMap<String, String>();
    		image.put("visibility", i % 2 == 0? "community" : "shared");
    		image.put("owner", i < qtdImages / 2 ? tenantId2 : tenantId);
    		image.put("id", "id" + Integer.toString(qtdImages + i));
    		image.put("name", "name" + Integer.toString(qtdImages + i));
    		myList.add(image);
    	}
    	
    	return myList;
    }
    
    private Map<String, String> getPublicImages(List<Map<String, String>> arrayList) {
    	Map<String, String> imageMap = new HashMap<String, String>();
    	for (Map<String, String> image: arrayList) {
    		if (image.get("visibility").equals("public")) {
    			imageMap.put(image.get("id"), image.get("name"));
    		}
    	}
    	return imageMap;
    }
    
    private Map<String, String> getPrivateImagesFromProject(List<Map<String, String>> arrayList, String tenantId) {
    	Map<String, String> imageMap = new HashMap<String, String>();
    	for (Map<String, String> image: arrayList) {
    		System.out.printf ("%s %s %s\n", image.get("id"),  image.get("owner"), image.get("visibility"));
    		if (image.get("visibility").equals("private") && image.get("owner").equals(tenantId)) {
    			System.out.println("entrou");
    			imageMap.put(image.get("id"), image.get("name"));
    		}
    	}
    	return imageMap;
    }
    
}
