package org.fogbowcloud.manager.core.plugins.cloud.image.openstack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.constants.OpenStackConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.ImageException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.cloud.image.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.utils.HttpRequestClientUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class OpenStackImagePlugin implements ImagePlugin {
	
	private static final String SUFFIX = "images";
	private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
	private static final String TENANT_ID = "tenantId";
	
	private static final String ID_JSON = "id";
	private static final String NAME_JSON = "name";
	private static final String SIZE_JSON = "size";
	private static final String MIN_DISK_JSON = "min_disk";
	private static final String MIN_RAM_JSON = "min_ram";
	private static final String STATUS = "status";
	
	private Properties properties;
	private HttpRequestClientUtil client;
	
	public OpenStackImagePlugin(Properties properties) {
		this.properties = properties;
		this.client = new HttpRequestClientUtil(this.properties);
	}
	
	@Override
	public Map<String, String> getAllImages(Token localToken) throws ImageException {
		Map<String, String> allAvailableImageNameIdMap = getImageNameAndIdMapFromAllAvailableImages(
				localToken,
				localToken.getAttributes().get(TENANT_ID));
		return allAvailableImageNameIdMap;
	}

	@Override
	public Image getImage(String imageId, Token localToken) throws ImageException {
		JSONObject imageJsonObject = getJsonObjectImage(imageId, localToken);
		Image image = new Image(
				imageJsonObject.getString(ID_JSON),
				imageJsonObject.getString(NAME_JSON),
				imageJsonObject.getInt(SIZE_JSON),
				imageJsonObject.getInt(MIN_DISK_JSON),
				imageJsonObject.getInt(MIN_RAM_JSON),
				imageJsonObject.getString(STATUS)
				);
		return image;
	}
	
	private JSONObject getJsonObjectImage(String imageId, Token localToken) throws ImageException {
		String endpoint = 
				this.properties.getProperty(OpenStackConfigurationConstants.COMPUTE_NOVAV2_URL_KEY)
                + COMPUTE_V2_API_ENDPOINT
                + SUFFIX
                + "?id="
                + imageId;
		try {
			String jsonResponse = this.client.doGetRequest(endpoint, localToken);
			JSONObject image = new JSONObject(jsonResponse);
			return image;
		} catch (RequestException e) {
			throw new ImageException("Could not make GET request.", e);
		}
	}
	
	private List<JSONObject> getAllImagesJson(Token localToken) throws ImageException  {
		String endpoint = 
				this.properties.getProperty(OpenStackConfigurationConstants.COMPUTE_NOVAV2_URL_KEY)
                + COMPUTE_V2_API_ENDPOINT
                + SUFFIX;
		try {
			String jsonResponse = this.client.doGetRequest(endpoint, localToken);
			List<JSONObject> imagesJson = new ArrayList<JSONObject>();
			imagesJson.addAll(getImagesFromJson(jsonResponse));
			getNextJsonByPagination(localToken, jsonResponse, imagesJson);
			return imagesJson;
		} catch (RequestException e) {
			throw new ImageException("Could not make GET request.", e);
		}
	}
	
	private void getNextJsonByPagination(Token localToken, String currentJson, List<JSONObject> imagesJson) throws RequestException{
		JSONObject jsonObject = new JSONObject (currentJson);
		if (jsonObject.has("next")) {
			String next = jsonObject.getString("next");
			String endpoint = 
					this.properties.getProperty(OpenStackConfigurationConstants.COMPUTE_NOVAV2_URL_KEY)
	                + COMPUTE_V2_API_ENDPOINT
	                + SUFFIX
	                + "?marker="
	                + next;
			String jsonResponse = this.client.doGetRequest(endpoint, localToken);
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
	
	private Map<String, String> getImageNameAndIdMapFromAllAvailableImages(Token localToken, String tenantId) throws ImageException{
		Map<String, String> imageNameIdMap = new HashMap<String, String>();
		List<JSONObject> allImages = getAllImagesJson(localToken);
		List<JSONObject> filteredImages = new ArrayList<JSONObject>();
		filteredImages.addAll(getPublicImages(allImages));
		filteredImages.addAll(getPrivateImagesByTenantId(allImages, tenantId));
		for (JSONObject image: filteredImages) {
			imageNameIdMap.put(image.getString("name"), image.getString("id")); 
		}
		return imageNameIdMap;
	}
}
