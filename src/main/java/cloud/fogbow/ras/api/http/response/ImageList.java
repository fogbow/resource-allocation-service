package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;

public class ImageList {
    @ApiModelProperty(example = ApiDocumentation.Model.IMAGE_LIST, notes = ApiDocumentation.Model.IMAGE_LIST_NOTE)
    private Map<String, String> images;

    public ImageList(Map<String, String> images) {
        this.images = images;
    }

    public Map<String, String> getImages() {
        return images;
    }

    public void setImages(Map<String, String> images) {
        this.images = images;
    }
}
