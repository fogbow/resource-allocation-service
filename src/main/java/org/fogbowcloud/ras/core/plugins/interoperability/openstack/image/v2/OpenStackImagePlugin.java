package org.fogbowcloud.ras.core.plugins.interoperability.openstack.image.v2;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.images.Image;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ImagePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.io.File;
import java.util.*;

public class OpenStackImagePlugin implements ImagePlugin<OpenStackV3Token> {
    private static final Logger LOGGER = Logger.getLogger(OpenStackImagePlugin.class);

    public static final String IMAGE_GLANCEV2_URL_KEY = "openstack_glance_v2_url";
    public static final String ACTIVE_STATE = "active";
    public static final String PUBLIC_VISIBILITY = "public";
    private static final String PRIVATE_VISIBILITY = "private";
    public static final String QUERY_ACTIVE_IMAGES = "?status=" + ACTIVE_STATE;
    public static final String IMAGE_V2_API_SUFFIX = "images";
    public static final String IMAGE_V2_API_ENDPOINT = "/v2/";
    private Properties properties;
    private HttpRequestClientUtil client;

    public OpenStackImagePlugin() throws FatalErrorException {
        this.properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
        this.client = new HttpRequestClientUtil();
    }

    @Override
    public Map<String, String> getAllImages(OpenStackV3Token openStackV3Token) throws FogbowRasException,
            UnexpectedException {
        Map<String, String> availableImages = getAvailableImages(
                openStackV3Token, openStackV3Token.getProjectId());
        return availableImages;
    }

    @Override
    public Image getImage(String imageId, OpenStackV3Token openStackV3Token) throws FogbowRasException,
            UnexpectedException {
        GetImageResponse getImageResponse = getImageResponse(imageId, openStackV3Token);
        String id = getImageResponse.getId();
        String status = getImageResponse.getStatus();
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
            throws FogbowRasException, UnexpectedException {
        String jsonResponse = null;
        try {
            String endpoint = this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY)
                    + IMAGE_V2_API_ENDPOINT + IMAGE_V2_API_SUFFIX + File.separator + imageId;
            jsonResponse = this.client.doGetRequest(endpoint, token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }

        return GetImageResponse.fromJson(jsonResponse);
    }

    private List<GetImageResponse> getImagesResponse(Token token) throws FogbowRasException, UnexpectedException {
        String jsonResponse = null;
        try {
            String endpoint = this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY)
                    + IMAGE_V2_API_ENDPOINT + IMAGE_V2_API_SUFFIX + QUERY_ACTIVE_IMAGES;
            jsonResponse = this.client.doGetRequest(endpoint, token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
        GetAllImagesResponse getAllImagesResponse = getAllImagesResponse(jsonResponse);

        List<GetImageResponse> getImageResponses = new ArrayList<GetImageResponse>();
        getImageResponses.addAll(getAllImagesResponse.getImages());
        getNextImageListResponseByPagination(token, getAllImagesResponse, getImageResponses);
        return getImageResponses;
    }

    private void getNextImageListResponseByPagination(Token token, GetAllImagesResponse getAllImagesResponse,
                                                      List<GetImageResponse> imagesJson) throws FogbowRasException,
                                                        UnexpectedException {

        String next = getAllImagesResponse.getNext();
        if (next != null && !next.isEmpty()) {
            String endpoint = this.properties.getProperty(IMAGE_GLANCEV2_URL_KEY) + next;
            String jsonResponse = null;
            try {
                jsonResponse = this.client.doGetRequest(endpoint, token);
            } catch (HttpResponseException e) {
                OpenStackHttpToFogbowRasExceptionMapper.map(e);
            }
            getAllImagesResponse = getAllImagesResponse(jsonResponse);

            imagesJson.addAll(getAllImagesResponse.getImages());
            getNextImageListResponseByPagination(token, getAllImagesResponse, imagesJson);
        }
    }

    private List<GetImageResponse> getPublicImagesResponse(List<GetImageResponse> imagesResponse) {
        List<GetImageResponse> publicImagesResponse = new ArrayList<GetImageResponse>();
        for (GetImageResponse getImageResponse : imagesResponse) {
            if (getImageResponse.getVisibility().equals(PUBLIC_VISIBILITY)) {
                publicImagesResponse.add(getImageResponse);
            }
        }
        return publicImagesResponse;
    }

    private List<GetImageResponse> getPrivateImagesResponse(List<GetImageResponse> imagesResponse, String tenantId) {
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
            throws FogbowRasException, UnexpectedException {
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
