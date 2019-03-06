package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.Image;

import java.util.Map;

public interface ImagePlugin<T extends CloudUser> {

    Map<String, String> getAllImages(T cloudUser) throws FogbowException;

    Image getImage(String imageId, T cloudUser) throws FogbowException;
}
