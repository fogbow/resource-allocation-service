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
import org.fogbowcloud.manager.core.plugins.cloud.ImagePlugin;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.image.v2.ImageResponse;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.image.v2.ImageListResponse;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;

public class OpenStackImagePlugin implements ImagePlugin {

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
	public Map<String, String> getAllImages(Token localToken) throws FogbowManagerException, UnexpectedException {
		Map<String, String> allAvailableImageNameIdMap = getImageNameAndIdMapFromAllAvailableImages(
				localToken, localToken.getAttributes().get(TENANT_ID));
		return allAvailableImageNameIdMap;
	}

	@Override
	public Image getImage(String imageId, Token localToken) throws FogbowManagerException, UnexpectedException {
		ImageResponse imageResponse = getImageResponse(imageId, localToken);
		String id = imageResponse.getId();
		String status = imageResponse.getStatus();
		LOGGER.debug("The image " + id + " status is " + status);
		if (status.equals(ACTIVE_STATE)) {
			Image image = new Image(id,
					imageResponse.getName(),
					imageResponse.getSize(),
					imageResponse.getMinDisk(),
					imageResponse.getMinRam(),
					status
			);
			return image;
		}
		
		return null;
	}
	
	private ImageResponse getImageResponse(String imageId, Token localToken)
			throws FogbowManagerException, UnexpectedException {
		String jsonResponse = null;
		try {
			String endpoint = this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY) 
					+ IMAGE_V2_API_ENDPOINT + IMAGE_V2_API_SUFFIX + File.separator + imageId;
			jsonResponse = this.client.doGetRequest(endpoint, localToken);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
		
		return ImageResponse.fromJson(jsonResponse);
	}
	
	private List<ImageResponse> getImagesResponse(Token localToken) throws FogbowManagerException, UnexpectedException {
		String jsonResponse = null;
		try {
			String endpoint = this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY) 
					+ IMAGE_V2_API_ENDPOINT + IMAGE_V2_API_SUFFIX + QUERY_ACTIVE_IMAGES;
			jsonResponse = this.client.doGetRequest(endpoint, localToken);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
		ImageListResponse imagesResponse = getImageListResponse(jsonResponse);
		
		List<ImageResponse> imagesJson = new ArrayList<ImageResponse>();
		imagesJson.addAll(imagesResponse.getImageResponseList());
		getNextImageListResponseByPagination(localToken, imagesResponse, imagesJson);
		return imagesJson;
	}

	private void getNextImageListResponseByPagination(Token localToken, ImageListResponse imageListResponse, List<ImageResponse> imagesJson)
			throws FogbowManagerException, UnexpectedException {
		
		String next = imageListResponse.getNext();
		if (next != null && !next.isEmpty()) {
			String endpoint = this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY) + next;
			String jsonResponse = null;
			try {
				jsonResponse = this.client.doGetRequest(endpoint, localToken);
			} catch (HttpResponseException e) {
				OpenStackHttpToFogbowManagerExceptionMapper.map(e);
			}
			imageListResponse = getImageListResponse(jsonResponse);
			
			imagesJson.addAll(imageListResponse.getImageResponseList());
			getNextImageListResponseByPagination(localToken, imageListResponse, imagesJson);
		}	
	}
	
	
	private List<ImageResponse> getPublicImagesResponse(List<ImageResponse> imagesResponse){
		List<ImageResponse> publicImagesResponse = new ArrayList<ImageResponse>();
		for (ImageResponse imageResponse: imagesResponse) {
			if (imageResponse.getVisibility().equals(PUBLIC_VISIBILITY)) {
				publicImagesResponse.add(imageResponse);
			}
		}
		return publicImagesResponse;
	}
	
	private List<ImageResponse> getPrivateImagesResponse(List<ImageResponse> imagesResponse, String tenantId){
		List<ImageResponse> privateImagesResponse = new ArrayList<ImageResponse>();
		for (ImageResponse imageResponse: imagesResponse) {
			if (imageResponse.getOwner().equals(tenantId) 
					&& imageResponse.getVisibility().equals(PRIVATE_VISIBILITY)) {
				privateImagesResponse.add(imageResponse);
			}
		}
		return privateImagesResponse;
	}
	
	private Map<String, String> getImageNameAndIdMapFromAllAvailableImages(Token localToken, String tenantId)
			throws FogbowManagerException, UnexpectedException {
		Map<String, String> imageNameIdMap = new HashMap<String, String>();
		
		List<ImageResponse> allImagesResponse = getImagesResponse(localToken);
		
		List<ImageResponse> filteredImagesResponse = filterImagesResponse(tenantId, allImagesResponse);
		
		for (ImageResponse imageResponse: filteredImagesResponse) {
			imageNameIdMap.put(imageResponse.getId(), imageResponse.getName()); 
		}
		
		return imageNameIdMap;
	}

	private List<ImageResponse> filterImagesResponse(String tenantId, List<ImageResponse> allImagesResponse) {
		List<ImageResponse> filteredImages = new ArrayList<ImageResponse>();
		filteredImages.addAll(getPublicImagesResponse(allImagesResponse));
		filteredImages.addAll(getPrivateImagesResponse(allImagesResponse, tenantId));
		return filteredImages;
	}
	
	protected void setClient(HttpRequestClientUtil client) {
		this.client = client;
	}
	
	private ImageListResponse getImageListResponse(String json) {
		return ImageListResponse.fromJson(json);
	}
	
	protected void setProperties(Properties properties) {
		this.properties = properties;
	}
}
