package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.models.tokens.OpenStackToken;
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.image.v2.GetImageResponse;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.image.v2.GetAllImagesResponse;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;

public class OpenStackImagePlugin implements ImagePlugin<OpenStackToken> {

	private static final Logger LOGGER = Logger.getLogger(OpenStackImagePlugin.class);

	public static final String IMAGE_GLANCEV2_URL_KEY = "openstack_glance_v2_url";
	
	public static final String ACTIVE_STATE = "active";
	public static final String PUBLIC_VISIBILITY = "public";
	private static final String PRIVATE_VISIBILITY = "private";
	
	public static final String QUERY_ACTIVE_IMAGES = "?status=" + ACTIVE_STATE;
	public static final String IMAGE_V2_API_SUFFIX = "images";
	public static final String IMAGE_V2_API_ENDPOINT = "/v2/";
	
	public static final String TENANT_ID = "tenantId";

	private Properties properties;
	private HttpRequestClientUtil client;
	
	public OpenStackImagePlugin() throws FatalErrorException {
		HomeDir homeDir = HomeDir.getInstance();
        this.properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
                + DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
		this.client = new HttpRequestClientUtil();
	}
	
	@Override
	public Map<String, String> getAllImages(OpenStackToken openStackToken) throws FogbowManagerException, UnexpectedException {
		Map<String, String> availableImages = getAvailableImages(
				openStackToken, openStackToken.getTenantId());
		return availableImages;
	}

	@Override
	public Image getImage(String imageId, OpenStackToken openStackToken) throws FogbowManagerException, UnexpectedException {
		GetImageResponse getImageResponse = getImageResponse(imageId, openStackToken);
		String id = getImageResponse.getId();
		String status = getImageResponse.getStatus();
		LOGGER.debug("The image " + id + " status is " + status);
		if (status.equals(ACTIVE_STATE)) {
			Image image = new Image(id,
					getImageResponse.getName(),
					getImageResponse.getSize(),
					getImageResponse.getMinDisk(),
					getImageResponse.getMinRam(),
					status
			);
			return image;
		}
		
		return null;
	}
	
	private GetImageResponse getImageResponse(String imageId, Token token)
			throws FogbowManagerException, UnexpectedException {
		String jsonResponse = null;
		try {
			String endpoint = this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY) 
					+ IMAGE_V2_API_ENDPOINT + IMAGE_V2_API_SUFFIX + File.separator + imageId;
			jsonResponse = this.client.doGetRequest(endpoint, token);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
		
		return GetImageResponse.fromJson(jsonResponse);
	}
	
	private List<GetImageResponse> getImagesResponse(Token token) throws FogbowManagerException, UnexpectedException {
		String jsonResponse = null;
		try {
			String endpoint = this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY) 
					+ IMAGE_V2_API_ENDPOINT + IMAGE_V2_API_SUFFIX + QUERY_ACTIVE_IMAGES;
			jsonResponse = this.client.doGetRequest(endpoint, token);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
		GetAllImagesResponse getAllImagesResponse = getAllImagesResponse(jsonResponse);
		
		List<GetImageResponse> getImageResponses = new ArrayList<GetImageResponse>();
		getImageResponses.addAll(getAllImagesResponse.getImages());
		getNextImageListResponseByPagination(token, getAllImagesResponse, getImageResponses);
		return getImageResponses;
	}

	private void getNextImageListResponseByPagination(Token token, GetAllImagesResponse getAllImagesResponse, List<GetImageResponse> imagesJson)
			throws FogbowManagerException, UnexpectedException {
		
		String next = getAllImagesResponse.getNext();
		if (next != null && !next.isEmpty()) {
			String endpoint = this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY) + next;
			String jsonResponse = null;
			try {
				jsonResponse = this.client.doGetRequest(endpoint, token);
			} catch (HttpResponseException e) {
				OpenStackHttpToFogbowManagerExceptionMapper.map(e);
			}
			getAllImagesResponse = getAllImagesResponse(jsonResponse);
			
			imagesJson.addAll(getAllImagesResponse.getImages());
			getNextImageListResponseByPagination(token, getAllImagesResponse, imagesJson);
		}	
	}
	
	
	private List<GetImageResponse> getPublicImagesResponse(List<GetImageResponse> imagesResponse){
		List<GetImageResponse> publicImagesResponse = new ArrayList<GetImageResponse>();
		for (GetImageResponse getImageResponse : imagesResponse) {
			if (getImageResponse.getVisibility().equals(PUBLIC_VISIBILITY)) {
				publicImagesResponse.add(getImageResponse);
			}
		}
		return publicImagesResponse;
	}
	
	private List<GetImageResponse> getPrivateImagesResponse(List<GetImageResponse> imagesResponse, String tenantId){
		List<GetImageResponse> privateImagesResponse = new ArrayList<GetImageResponse>();
		for (GetImageResponse getImageResponse : imagesResponse) {
			if (getImageResponse.getOwner().equals(tenantId)
					&& getImageResponse.getVisibility().equals(PRIVATE_VISIBILITY)) {
				privateImagesResponse.add(getImageResponse);
			}
		}
		return privateImagesResponse;
	}
	
	private Map<String, String> getAvailableImages(Token token, String tenantId)
			throws FogbowManagerException, UnexpectedException {
		Map<String, String> availableImages = new HashMap<String, String>();
		
		List<GetImageResponse> allImagesResponse = getImagesResponse(token);
		
		List<GetImageResponse> filteredImagesResponse = filterImagesResponse(tenantId, allImagesResponse);
		
		for (GetImageResponse getImageResponse : filteredImagesResponse) {
			availableImages.put(getImageResponse.getId(), getImageResponse.getName());
		}
		
		return availableImages;
	}

	private List<GetImageResponse> filterImagesResponse(String tenantId, List<GetImageResponse> allImagesResponse) {
		List<GetImageResponse> filteredImages = new ArrayList<GetImageResponse>();
		filteredImages.addAll(getPublicImagesResponse(allImagesResponse));
		filteredImages.addAll(getPrivateImagesResponse(allImagesResponse, tenantId));
		return filteredImages;
	}
	
	protected void setClient(HttpRequestClientUtil client) {
		this.client = client;
	}
	
	private GetAllImagesResponse getAllImagesResponse(String json) {
		return GetAllImagesResponse.fromJson(json);
	}
	
	protected void setProperties(Properties properties) {
		this.properties = properties;
	}
}
