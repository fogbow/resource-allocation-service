package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.image.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CloudStackImagePlugin implements ImagePlugin<CloudStackUser> {
    private static final Logger LOGGER = Logger.getLogger(CloudStackImagePlugin.class);

    protected static final int DEFAULT_MIN_DISK_VALUE = -1;
    protected static final int DEFAULT_MIN_RAM_VALUE = -1;
    protected static final String DEFAULT_STATUS_VALUE = null;

    private String cloudStackUrl;
    private CloudStackHttpClient client;

    public CloudStackImagePlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.client = new CloudStackHttpClient();
    }

    @Override
    public List<ImageSummary> getAllImages(@NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(Messages.Log.REQUESTING_GET_ALL_FROM_PROVIDER);

        GetAllImagesRequest request = new GetAllImagesRequest.Builder()
                .build(this.cloudStackUrl);

        return buildImagesSummary(request, cloudStackUser);
    }

    @Override
    public ImageInstance getImage(String imageId, @VisibleForTesting CloudStackUser cloudStackUser)
            throws FogbowException {

        LOGGER.info(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER);

        GetAllImagesRequest request = new GetAllImagesRequest.Builder()
                .id(imageId)
                .build(this.cloudStackUrl);

        return buildImageInstance(request, cloudStackUser);
    }

    @NotNull
    @VisibleForTesting
    ImageInstance buildImageInstance(@NotNull GetAllImagesRequest request,
                                     @NotNull CloudStackUser cloudStackUser) throws FogbowException {

        GetAllImagesResponse response = doDescribeImagesRequest(request, cloudStackUser);
        List<GetAllImagesResponse.Image> images = response.getImages();
        if (images.isEmpty()) {
            throw new InstanceNotFoundException();
        }

        // since an id was specified, there should be no more than one network in the getNetworkResponse
        GetAllImagesResponse.Image image = images.listIterator().next();
        return new ImageInstance(image.getId(), image.getName(), image.getSize(),
                DEFAULT_MIN_DISK_VALUE, DEFAULT_MIN_RAM_VALUE, DEFAULT_STATUS_VALUE);
    }

    @NotNull
    @VisibleForTesting
    List<ImageSummary> buildImagesSummary(@NotNull GetAllImagesRequest request,
                                          @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        GetAllImagesResponse response = doDescribeImagesRequest(request, cloudStackUser);
        List<GetAllImagesResponse.Image> images = response.getImages();

        List<ImageSummary> idToImageNames = new ArrayList<>();
        for (GetAllImagesResponse.Image image : images) {
            ImageSummary imageSummary = new ImageSummary(image.getId(), image.getName());
            idToImageNames.add(imageSummary);
        }

        return idToImageNames;
    }

    @NotNull
    @VisibleForTesting
    GetAllImagesResponse doDescribeImagesRequest(@NotNull GetAllImagesRequest request,
                                                 @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        String jsonResponse = CloudStackCloudUtils.doRequest(this.client, uriRequest.toString(), cloudStackUser);
        return GetAllImagesResponse.fromJson(jsonResponse);
    }

    @VisibleForTesting
    void setClient(CloudStackHttpClient client) {
        this.client = client;
    }
}
