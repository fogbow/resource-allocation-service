package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.image;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.ImagePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudStackImagePlugin implements ImagePlugin<CloudStackToken> {

    private HttpRequestClientUtil client;

    public CloudStackImagePlugin() {
        this.client = new HttpRequestClientUtil();
    }

    @Override
    public Map<String, String> getAllImages(CloudStackToken cloudStackToken) throws FogbowRasException, UnexpectedException {
        GetAllImagesRequest request = new GetAllImagesRequest.Builder().build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        GetAllImagesResponse response = GetAllImagesResponse.fromJson(jsonResponse);
        List<GetAllImagesResponse.Image> images = response.getImages();

        Map<String, String> idToImageNames = new HashMap<>();
        for (GetAllImagesResponse.Image image : images){
            idToImageNames.put(image.getId(), image.getName());
        }

        return idToImageNames;
    }

    @Override
    public Image getImage(String imageId, CloudStackToken cloudStackToken) throws FogbowRasException, UnexpectedException {
        GetAllImagesRequest request = new GetAllImagesRequest.Builder()
                .id(imageId)
                .build();

        CloudStackUrlUtil.sign(request.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(request.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
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

    protected void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }
}
