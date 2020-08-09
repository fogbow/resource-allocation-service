package cloud.fogbow.ras.core.plugins.interoperability.openstack.image.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetAllImagesResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetImageResponse;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

public class OpenStackImagePlugin implements ImagePlugin<OpenStackV3User> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackImagePlugin.class);

    private Properties properties;
    private OpenStackHttpClient client;

    public OpenStackImagePlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.initClient();
    }

    @Override
    public List<ImageSummary> getAllImages(OpenStackV3User cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_GET_ALL_FROM_PROVIDER);
        List<ImageSummary> availableImages = getAvailableImages(cloudUser);
        return availableImages;
    }

    @Override
    public ImageInstance getImage(String imageId, OpenStackV3User cloudUser) throws FogbowException {
        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);
        GetImageResponse getImageResponse = getImageResponse(imageId, cloudUser);
        String status = getImageResponse.getStatus();

        ImageInstance imageInstance = null;

        if (status.equals(OpenStackConstants.ACTIVE_STATE)) {
            imageInstance = buildImageInstance(getImageResponse);
        }

        return imageInstance;
    }

    @VisibleForTesting
    ImageInstance buildImageInstance(GetImageResponse getImageResponse) {
        String id = getImageResponse.getId();
        String status = getImageResponse.getStatus();
        String name = getImageResponse.getName();
        Long size = getImageResponse.getSize();
        Long minDisk = getImageResponse.getMinDisk();
        Long minRam = getImageResponse.getMinRam();
        return new ImageInstance(id, name, size, minDisk, minRam, status);
    }

    @VisibleForTesting
    GetImageResponse getImageResponse(String imageId, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = this.properties.getProperty(OpenStackPluginUtils.IMAGE_GLANCE_URL_KEY)
                    + OpenStackConstants.GLANCE_V2_API_ENDPOINT + OpenStackConstants.ENDPOINT_SEPARATOR +
                    OpenStackConstants.IMAGE_ENDPOINT + File.separator + imageId;
        String jsonResponse = this.client.doGetRequest(endpoint, cloudUser);

        return GetImageResponse.fromJson(jsonResponse);
    }

    @VisibleForTesting
    List<GetImageResponse> getImagesResponse(OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = this.properties.getProperty(OpenStackPluginUtils.IMAGE_GLANCE_URL_KEY)
                    + OpenStackConstants.GLANCE_V2_API_ENDPOINT + OpenStackConstants.ENDPOINT_SEPARATOR +
                    OpenStackConstants.IMAGE_ENDPOINT + OpenStackConstants.QUERY_ACTIVE_IMAGES;
        String jsonResponse = this.client.doGetRequest(endpoint, cloudUser);
        GetAllImagesResponse getAllImagesResponse = getAllImagesResponse(jsonResponse);

        List<GetImageResponse> getImageResponses = new ArrayList<GetImageResponse>();
        getImageResponses.addAll(getAllImagesResponse.getImages());
        getNextImageListResponseByPagination(cloudUser, getAllImagesResponse, getImageResponses);
        return getImageResponses;
    }

    @VisibleForTesting
    void getNextImageListResponseByPagination(OpenStackV3User cloudUser, GetAllImagesResponse getAllImagesResponse,
        List<GetImageResponse> imagesJson) throws FogbowException {
        String next = getAllImagesResponse.getNext();
        if (next != null && !next.isEmpty()) {
            String endpoint = this.properties.getProperty(OpenStackPluginUtils.IMAGE_GLANCE_URL_KEY) + next;
            String jsonResponse = this.client.doGetRequest(endpoint, cloudUser);
           getAllImagesResponse = getAllImagesResponse(jsonResponse);
            imagesJson.addAll(getAllImagesResponse.getImages());
            getNextImageListResponseByPagination(cloudUser, getAllImagesResponse, imagesJson);
        }
    }

    @VisibleForTesting
    List<GetImageResponse> getPublicImagesResponse(List<GetImageResponse> imagesResponse) {
        List<GetImageResponse> publicImagesResponse = new ArrayList<GetImageResponse>();
        for (GetImageResponse getImageResponse : imagesResponse) {
            if (getImageResponse.getVisibility().equals(OpenStackConstants.PUBLIC_VISIBILITY)) {
                publicImagesResponse.add(getImageResponse);
            }
        }
        return publicImagesResponse;
    }

    @VisibleForTesting
    List<GetImageResponse> getPrivateImagesResponse(List<GetImageResponse> imagesResponse, String tenantId) {
        List<GetImageResponse> privateImagesResponse = new ArrayList<GetImageResponse>();
        for (GetImageResponse getImageResponse : imagesResponse) {
            if (getImageResponse.getOwner().equals(tenantId)
                    && getImageResponse.getVisibility().equals(OpenStackConstants.PRIVATE_VISIBILITY)) {
                privateImagesResponse.add(getImageResponse);
            }
        }
        return privateImagesResponse;
    }

    @VisibleForTesting
    List<ImageSummary> getAvailableImages(OpenStackV3User cloudUser) throws FogbowException {
        List<ImageSummary> availableImages = new ArrayList<>();

        List<GetImageResponse> allImagesResponse = getImagesResponse(cloudUser);
        List<GetImageResponse> filteredImagesResponse = filterImagesResponse(OpenStackPluginUtils.getProjectIdFrom(cloudUser), allImagesResponse);

        for (GetImageResponse getImageResponse : filteredImagesResponse) {
            ImageSummary imageSummary = new ImageSummary(getImageResponse.getId(), getImageResponse.getName());
            availableImages.add(imageSummary);
        }
        return availableImages;
    }

    private List<GetImageResponse> filterImagesResponse(String tenantId, List<GetImageResponse> allImagesResponse) {
        List<GetImageResponse> filteredImages = new ArrayList<GetImageResponse>();
        filteredImages.addAll(getPublicImagesResponse(allImagesResponse));
        filteredImages.addAll(getPrivateImagesResponse(allImagesResponse, tenantId));
        return filteredImages;
    }

    @VisibleForTesting
    void setClient(OpenStackHttpClient client) {
        this.client = client;
    }

    @VisibleForTesting
    GetAllImagesResponse getAllImagesResponse(String json) {
        return GetAllImagesResponse.fromJson(json);
    }

    @VisibleForTesting
    void setProperties(Properties properties) {
        this.properties = properties;
    }

    private void initClient() {
        this.client = new OpenStackHttpClient();
    }
}
