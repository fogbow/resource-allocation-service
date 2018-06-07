package org.fogbowcloud.manager.core.plugins.cloud.image.openstack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.constants.OpenStackConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.ImageException;
import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.cloud.image.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.utils.HttpRequestClientUtil;
import org.fogbowcloud.manager.utils.JSONUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.JsonObject;

public class OpenStackImagePlugin implements ImagePlugin {
	
	private static final String SUFFIX = "images";
	private static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
	private static final String TENANT_ID = "tenantId";
	
	private Properties properties;
	private HttpRequestClientUtil client;
	
	public OpenStackImagePlugin(Properties properties) {
		this.properties = properties;
		this.client = new HttpRequestClientUtil(this.properties);
	}
	
	@Override
	public Map<String, String> getImages(Token localToken) throws ImageException {
		Map<String, String> allAvailableImageNameIdMap = getImageNameAndIdMapFromAllAvailableImages(
				localToken,
				localToken.getAttributes().get(TENANT_ID));
		return allAvailableImageNameIdMap;
	}

	@Override
	public Image getImage(Token localToken, String id) {
		JSONObject imageJsonObject = getJsonObjectImage(localToken, id);
		// TODO converte jsonObject to image
		return null;
	}
	
	private JSONObject getJsonObjectImage(Token localToken, String id) throws ImageException {
		String endpoint = 
				this.properties.getProperty(OpenStackConfigurationConstants.COMPUTE_NOVAV2_URL_KEY)
                + COMPUTE_V2_API_ENDPOINT
                + SUFFIX
                + "?id="
                + id;
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
			if (image.getString("owner").equals(tenantId) && image.getString("owner").equals(tenantId)) {
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
