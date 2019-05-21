package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;

import java.util.List;

public interface ImagePlugin<T extends CloudUser> {

    List<ImageSummary> getAllImages(T cloudUser) throws FogbowException;

    ImageInstance getImage(String imageId, T cloudUser) throws FogbowException;
}
