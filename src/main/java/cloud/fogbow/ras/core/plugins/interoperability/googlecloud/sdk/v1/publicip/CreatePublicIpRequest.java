package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.publicip;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;

public class CreatePublicIpRequest implements JsonSerializable {

    private PublicIp publicIp;

    public CreatePublicIpRequest(PublicIp publicIp) {
        this.publicIp = publicIp;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    private static class PublicIp {

        private String projectId;

        public PublicIp(Builder builder) {
            this.projectId = builder.projectId;
        }
    }

    public static class Builder {
        private String projectId;

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public CreatePublicIpRequest build() {
            PublicIp publicIp = new PublicIp(this);
            return new CreatePublicIpRequest(publicIp);
        }
    }




}
