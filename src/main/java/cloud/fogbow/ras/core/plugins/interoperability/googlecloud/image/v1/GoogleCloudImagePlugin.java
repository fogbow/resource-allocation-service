package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.image.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;


import cloud.fogbow.common.constants.GoogleCloudConstants;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.image.models.CreateImageResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.image.models.CreateImagesResponse;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;

public class GoogleCloudImagePlugin implements ImagePlugin<GoogleCloudUser> {

	private Properties properties;
	private GoogleCloudHttpClient client;
	private static final Integer NO_VALUE_FLAG = -1;
	private static final String URL = "https://compute.googleapis.com/compute/v1";

	public GoogleCloudImagePlugin(String confFilePath) {
		this.properties = PropertiesUtil.readProperties(confFilePath);
		this.client = new GoogleCloudHttpClient();
	}

	@Override
	public List<ImageSummary> getAllImages(GoogleCloudUser cloudUser) throws FogbowException {
		List<CreateImageResponse> imageList = new ArrayList<>();
		imageList = this.getImagesResponse(cloudUser, imageList);
		return buildImagesInstance(imageList);
	}

	@Override
	public ImageInstance getImage(String imageId, GoogleCloudUser cloudUser) throws FogbowException {
		CreateImageResponse imageResponse = getImageResponse(imageId, cloudUser);
		ImageInstance image = buildImageInstance(imageResponse);
		return image;
	}
	
	@VisibleForTesting
	CreateImageResponse getImageResponse(String imageId, GoogleCloudUser cloudUser) throws FogbowException {
		String endPoint = this.getPrefixEndPoint(cloudUser) + GoogleCloudConstants.ENDPOINT_SEPARATOR + imageId;
		String response = this.client.doGetRequest(endPoint, cloudUser);
		System.out.println(response);
		return CreateImageResponse.fromJson(response);
    }
	
	@VisibleForTesting
	List<CreateImageResponse> getImagesResponse(GoogleCloudUser cloudUser, List<CreateImageResponse> imageList) throws FogbowException {
		String endPoint = this.getPrefixEndPoint(cloudUser);
		String response = this.client.doGetRequest(endPoint, cloudUser);
		System.out.println(response);
		CreateImagesResponse imagesResponse = CreateImagesResponse.fromJson(response);
		imageList = getNextImageListResponse(cloudUser, imagesResponse, imageList);
		return imageList;
    }
	
	@VisibleForTesting
	List<CreateImageResponse> getNextImageListResponse(GoogleCloudUser cloudUser, CreateImagesResponse imagesResponse, 
    		List<CreateImageResponse> imageList) throws FogbowException {
		imageList.addAll(imagesResponse.getImages());
		if (imagesResponse.getNextToken() != null) {
			String newEndPoint = this.getNextPageEndPoint(cloudUser, imagesResponse.getNextToken());
			String response = this.client.doGetRequest(newEndPoint, cloudUser);
			CreateImagesResponse nextImagesResponse = CreateImagesResponse.fromJson(response);
			imageList = this.getNextImageListResponse(cloudUser, nextImagesResponse, imageList);
		}
		return imageList;
    }
	
	@VisibleForTesting
	String getNextPageEndPoint(GoogleCloudUser cloudUser, String token) throws InvalidParameterException {
		String endPoint = this.getPrefixEndPoint(cloudUser) + 
				GoogleCloudConstants.Image.INTERROGATION_QUERY + 
				GoogleCloudConstants.Image.NEXT_TOKEN_KEY_QUERY + 
				GoogleCloudConstants.Image.EQUAL_QUERY + token;
		return endPoint;
	}
	
	String getPrefixEndPoint(GoogleCloudUser cloudUser) throws InvalidParameterException {
		return URL + GoogleCloudConstants.PROJECT_ENDPOINT 
				+ GoogleCloudConstants.ENDPOINT_SEPARATOR
				+ GoogleCloudPluginUtils.getProjectIdFrom(cloudUser) 
				+ GoogleCloudConstants.GLOBAL_IMAGES_ENDPOINT;
	}
	
	@VisibleForTesting
	ImageInstance buildImageInstance(CreateImageResponse imageResponse) {
		ImageInstance image = null;
		String status = imageResponse.getStatus();
		if (status != null && status.equals(GoogleCloudConstants.Image.IMAGE_STATUS_READY )) {
			String id = imageResponse.getId();
			String name = imageResponse.getName();
			Long size = Long.parseLong(imageResponse.getDiskSize());
			image = new ImageInstance(id, name, size, NO_VALUE_FLAG, NO_VALUE_FLAG, status);
		}
		return image;
	}
	
	@VisibleForTesting
	List<ImageSummary> buildImagesInstance(List<CreateImageResponse> imagesList) {
		List<ImageSummary> newListImages = new ArrayList<>();
		for (CreateImageResponse image : imagesList) {
			ImageSummary finalImage = new ImageSummary(image.getId(), image.getName());
			newListImages.add(finalImage);
		}
		return newListImages;
	}
}
