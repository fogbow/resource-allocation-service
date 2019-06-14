package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

public class ImageSummary {
    @ApiModelProperty(position = 0, example = ApiDocumentation.Model.IMAGE_ID, notes = ApiDocumentation.Model.IMAGE_ID_NOTE)
    private String id;
    @ApiModelProperty(position = 1, example = ApiDocumentation.Model.IMAGE_NAME, notes = ApiDocumentation.Model.IMAGE_NAME_NOTE)
    private String name;

    public ImageSummary(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageSummary)) return false;
        ImageSummary that = (ImageSummary) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
