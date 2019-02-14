package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.image;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.images.Image;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackHttpClient;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.apache.http.client.HttpResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CloudStackImagePlugin implements ImagePlugin {

    public static final String CLOUDSTACK_URL = "cloudstack_api_url";

    private String cloudStackUrl;
    private CloudStackHttpClient client;
    private Properties properties;

    public CloudStackImagePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.cloudStackUrl = this.properties.getProperty(CLOUDSTACK_URL);

        Integer timeout = new Integer(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.HTTP_REQUEST_TIMEOUT_KEY,
                ConfigurationPropertyDefaults.XMPP_TIMEOUT));
        HttpRequestClientUtil client = new HttpRequestClientUtil(timeout);
        this.client = new CloudStackHttpClient(client);
    }

    @Override
    public Map<String, String> getAllImages(CloudToken CloudToken) throws FogbowException {
        GetAllImagesRequest request = new GetAllImagesRequest.Builder().build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), CloudToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), CloudToken);
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
    public Image getImage(String imageId, CloudToken CloudToken) throws FogbowException {
        GetAllImagesRequest request = new GetAllImagesRequest.Builder()
                .id(imageId)
                .build(this.cloudStackUrl);

        CloudStackUrlUtil.sign(request.getUriBuilder(), CloudToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), CloudToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        GetAllImagesResponse response = GetAllImagesResponse.fromJson(jsonResponse);
        List<GetAllImagesResponse.Image> images = response.getImages();

        if (images != null && images.size() > 0) {
            GetAllImagesResponse.Image image = images.get(0);
            return new Image(image.getId(), image.getName(), image.getSize(), -1, -1, null);
        } else {
            throw new InstanceNotFoundException();
        }
    }

    protected void setClient(CloudStackHttpClient client) {
        this.client = client;
    }
}
