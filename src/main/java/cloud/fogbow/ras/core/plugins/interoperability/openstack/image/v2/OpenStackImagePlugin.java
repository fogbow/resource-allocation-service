package cloud.fogbow.ras.core.plugins.interoperability.openstack.image.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.OpenStackPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetAllImagesResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses.GetImageResponse;

import java.io.File;
import java.util.*;

public class OpenStackImagePlugin implements ImagePlugin<OpenStackV3User> {
    private Properties properties;
    private OpenStackHttpClient client;

    public OpenStackImagePlugin(String confFilePath) throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.client = new OpenStackHttpClient();
    }

    @Override
    public List<ImageSummary> getAllImages(OpenStackV3User cloudUser) throws FogbowException {
        List<ImageSummary> availableImages = getAvailableImages(cloudUser);
        return availableImages;
    }

    @Override
    public ImageInstance getImage(String imageId, OpenStackV3User cloudUser) throws FogbowException {
        GetImageResponse getImageResponse = getImageResponse(imageId, cloudUser);
        String id = getImageResponse.getId();
        String status = getImageResponse.getStatus();
        if (status.equals(OpenStackConstants.ACTIVE_STATE)) {
            ImageInstance imageInstance = new ImageInstance(id,
                    getImageResponse.getName(),
                    getImageResponse.getSize(),
                    getImageResponse.getMinDisk(),
                    getImageResponse.getMinRam(),
                    status
            );
            return imageInstance;
        }

        return null;
    }

    protected GetImageResponse getImageResponse(String imageId, OpenStackV3User cloudUser) throws FogbowException {
        String endpoint = this.properties.getProperty(OpenStackPluginUtils.IMAGE_GLANCE_URL_KEY)
                    + OpenStackConstants.GLANCE_V2_API_ENDPOINT + OpenStackConstants.ENDPOINT_SEPARATOR +
                    OpenStackConstants.IMAGE_ENDPOINT + File.separator + imageId;
        String jsonResponse = this.client.doGetRequest(endpoint, cloudUser);

        return GetImageResponse.fromJson(jsonResponse);
    }

    protected List<GetImageResponse> getImagesResponse(OpenStackV3User cloudUser) throws FogbowException {
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

    protected void getNextImageListResponseByPagination(OpenStackV3User cloudUser, GetAllImagesResponse getAllImagesResponse,
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

    protected List<GetImageResponse> getPublicImagesResponse(List<GetImageResponse> imagesResponse) {
        List<GetImageResponse> publicImagesResponse = new ArrayList<GetImageResponse>();
        for (GetImageResponse getImageResponse : imagesResponse) {
            if (getImageResponse.getVisibility().equals(OpenStackConstants.PUBLIC_VISIBILITY)) {
                publicImagesResponse.add(getImageResponse);
            }
        }
        return publicImagesResponse;
    }

    protected List<GetImageResponse> getPrivateImagesResponse(List<GetImageResponse> imagesResponse, String tenantId) {
        List<GetImageResponse> privateImagesResponse = new ArrayList<GetImageResponse>();
        for (GetImageResponse getImageResponse : imagesResponse) {
            if (getImageResponse.getOwner().equals(tenantId)
                    && getImageResponse.getVisibility().equals(OpenStackConstants.PRIVATE_VISIBILITY)) {
                privateImagesResponse.add(getImageResponse);
            }
        }
        return privateImagesResponse;
    }

    protected List<ImageSummary> getAvailableImages(OpenStackV3User cloudUser) throws FogbowException {
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

    protected void setClient(OpenStackHttpClient client) {
        this.client = client;
    }

    protected GetAllImagesResponse getAllImagesResponse(String json) {
        return GetAllImagesResponse.fromJson(json);
    }

    protected void setProperties(Properties properties) {
        this.properties = properties;
    }
}
