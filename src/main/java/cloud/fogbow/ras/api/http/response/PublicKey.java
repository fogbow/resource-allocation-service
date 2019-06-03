package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.common.constants.ApiDocumentation;
import io.swagger.annotations.ApiModelProperty;

public class PublicKey {
    @ApiModelProperty(position = 0, example = ApiDocumentation.Model.PUBLIC_KEY)
    private String publicKey;

    public PublicKey() {}

    public PublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
