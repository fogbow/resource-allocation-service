package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.image.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import org.apache.http.client.HttpResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CloudStackImagePlugin implements ImagePlugin<CloudStackUser> {

    public static final String CLOUDSTACK_URL = "cloudstack_api_url";

    private String cloudStackUrl;
    private CloudStackHttpClient client;
    private Properties properties;

    public CloudStackImagePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL);
        this.client = new CloudStackHttpClient();
    }

    @Override
    public Map<String, String> getAllImages(CloudStackUser cloudUser) throws FogbowException {
        GetAllImagesRequest request = new GetAllImagesRequest.Builder().build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudUser.getToken());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudUser);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        GetAllImagesResponse response = GetAllImagesResponse.fromJson(jsonResponse);
        List<GetAllImagesResponse.Image> images = response.getImages();

        Map<String, String> idToImageNames = new HashMap<>();
        for (GetAllImagesResponse.Image image : images) {
            idToImageNames.put(image.getId(), image.getName());
        }

        return idToImageNames;
    }

    @Override
    public ImageInstance getImage(String imageId, CloudStackUser cloudUser) throws FogbowException {
        GetAllImagesRequest request = new GetAllImagesRequest.Builder()
                .id(imageId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudUser.getToken());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudUser);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        GetAllImagesResponse response = GetAllImagesResponse.fromJson(jsonResponse);
        List<GetAllImagesResponse.Image> images = response.getImages();

        if (images != null && images.size() > 0) {
            GetAllImagesResponse.Image image = images.get(0);
            return new ImageInstance(image.getId(), image.getName(), image.getSize(), -1, -1, null);
        } else {
            throw new InstanceNotFoundException();
        }
    }

    protected void setClient(CloudStackHttpClient client) {
        this.client = client;
    }
}
