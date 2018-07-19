package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class OpenStackImagePlugin implements ImagePlugin {

	public static final String IMAGE_GLANCEV2_URL_KEY = "openstack_glance_v2_url";

	public static final String SUFFIX = "images";
	public static final String ACTIVE_STATE = "active";
	public static final String QUERY_ACTIVE_IMAGES = "?status=" + ACTIVE_STATE;
	public static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
	public static final String TENANT_ID = "tenantId";
	
	public static final String ID_JSON = "id";
	public static final String NAME_JSON = "name";
	public static final String SIZE_JSON = "size";
	public static final String MIN_DISK_JSON = "min_disk";
	public static final String MIN_RAM_JSON = "min_ram";
	public static final String STATUS = "status";

	private Properties properties;
	private HttpRequestClientUtil client;
	
	public OpenStackImagePlugin() throws FatalErrorException {
		HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
		this.client = new HttpRequestClientUtil();
	}
	
	@Override
	public Map<String, String> getAllImages(Token localToken) throws FogbowManagerException, UnexpectedException {
		Map<String, String> allAvailableImageNameIdMap = getImageNameAndIdMapFromAllAvailableImages(
				localToken, localToken.getAttributes().get(TENANT_ID));
		return allAvailableImageNameIdMap;
	}

	@Override
	public Image getImage(String imageId, Token localToken) throws FogbowManagerException, UnexpectedException {
		JSONObject imageJsonObject = getJsonObjectImage(imageId, localToken);
		String status = imageJsonObject.optString(STATUS);
		if (status.equals(ACTIVE_STATE)) {
			Image image = new Image(
					imageJsonObject.getString(ID_JSON),
					imageJsonObject.getString(NAME_JSON),
					imageJsonObject.getLong(SIZE_JSON),
					imageJsonObject.getLong(MIN_DISK_JSON),
					imageJsonObject.getLong(MIN_RAM_JSON),
					imageJsonObject.getString(STATUS)
			);
			return image;
		}
		return null;
	}
	
	private JSONObject getJsonObjectImage(String imageId, Token localToken)
			throws FogbowManagerException, UnexpectedException {
		String endpoint = 
				this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY)
                + COMPUTE_V2_API_ENDPOINT
                + SUFFIX
                + "/"
                + imageId;
		String jsonResponse = null;
		try {
			jsonResponse = this.client.doGetRequest(endpoint, localToken);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
		JSONObject image = new JSONObject(jsonResponse);
		return image;
	}
	
	private List<JSONObject> getAllImagesJson(Token localToken) throws FogbowManagerException, UnexpectedException {
		String endpoint = 
				this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY)
                + COMPUTE_V2_API_ENDPOINT
                + SUFFIX
                + QUERY_ACTIVE_IMAGES;
		String jsonResponse = null;
		try {
			jsonResponse = this.client.doGetRequest(endpoint, localToken);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
		List<JSONObject> imagesJson = new ArrayList<JSONObject>();
		imagesJson.addAll(getImagesFromJson(jsonResponse));
		getNextJsonByPagination(localToken, jsonResponse, imagesJson);
		return imagesJson;
	}
	
	private void getNextJsonByPagination(Token localToken, String currentJson, List<JSONObject> imagesJson)
			throws FogbowManagerException, UnexpectedException {
		JSONObject jsonObject = new JSONObject (currentJson);
		if (jsonObject.has("next")) {
			String next = jsonObject.getString("next");
			String endpoint = 
					this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY)
	                + next;
			String jsonResponse = null;
			try {
				jsonResponse = this.client.doGetRequest(endpoint, localToken);
			} catch (HttpResponseException e) {
				OpenStackHttpToFogbowManagerExceptionMapper.map(e);
			}
			imagesJson.addAll(getImagesFromJson(jsonResponse));
			getNextJsonByPagination(localToken, jsonResponse, imagesJson);
		}
	}
	
	private List<JSONObject> getImagesFromJson(String json) {
		JSONObject jsonObject = new JSONObject(json);
		JSONArray jsonArray = jsonObject.getJSONArray("images");
		List<JSONObject> jsonList = new ArrayList<JSONObject>();
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonImage = (JSONObject) jsonArray.get(i);
			jsonList.add(jsonImage);
		}
		return jsonList;
	}
	
	private List<JSONObject> getPublicImages(List<JSONObject> images){
		List<JSONObject> publicImages = new ArrayList<JSONObject>();
		for (JSONObject image: images) {
			if (image.getString("visibility").equals("public")) {
				publicImages.add(image);
			}
		}
		return publicImages;
	}
	
	private List<JSONObject> getPrivateImagesByTenantId(List<JSONObject> images, String tenantId){
		List<JSONObject> privateImages = new ArrayList<JSONObject>();
		for (JSONObject image: images) {
			if (image.getString("owner").equals(tenantId) && image.getString("visibility").equals("private")) {
				privateImages.add(image);
			}
		}
		return privateImages;
	}
	
	private Map<String, String> getImageNameAndIdMapFromAllAvailableImages(Token localToken, String tenantId)
			throws FogbowManagerException, UnexpectedException {
		Map<String, String> imageNameIdMap = new HashMap<String, String>();
		
		List<JSONObject> allImages = getAllImagesJson(localToken);
		
		List<JSONObject> filteredImages = new ArrayList<JSONObject>();
		filteredImages.addAll(getPublicImages(allImages));
		filteredImages.addAll(getPrivateImagesByTenantId(allImages, tenantId));
		
		for (JSONObject image: filteredImages) {
			imageNameIdMap.put(image.getString("id"), image.getString("name")); 
		}
		
		return imageNameIdMap;
	}
	
	protected void setClient(HttpRequestClientUtil client) {
		this.client = client;
	}
	
	protected void setProperties(Properties properties) {
		this.properties = properties;
	}
}
