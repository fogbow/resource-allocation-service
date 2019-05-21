package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

public class ImageList {
    @ApiModelProperty(position = 0, example = ApiDocumentation.Model.IMAGE_LIST, notes = ApiDocumentation.Model.IMAGE_LIST_NOTE)
    public List<ImageSummary> images;

    public ImageList(List<ImageSummary> images) {
        this.images = images;
    }

    public List<ImageSummary> getImages() {
        return images;
    }

    public void setImages(List<ImageSummary> images) {
        this.images = images;
    }
}
