package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.images.Image;

import java.util.Map;

public interface ImagePlugin {

    Map<String, String> getAllImages(CloudToken localUserAttributes) throws FogbowException;

    Image getImage(String imageId, CloudToken localUserAttributes) throws FogbowException;
}
