package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.api.http.response.Image;

import java.util.Map;

public interface ImagePlugin<T extends CloudToken> {

    Map<String, String> getAllImages(T localUserAttributes) throws FogbowException;

    Image getImage(String imageId, T localUserAttributes) throws FogbowException;
}
