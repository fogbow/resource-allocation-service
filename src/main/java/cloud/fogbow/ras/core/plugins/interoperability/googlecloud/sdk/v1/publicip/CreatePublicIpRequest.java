package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.publicip;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import cloud.fogbow.ras.api.parameters.SecurityRule.EtherType;

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
        private EtherType etherType;

        public PublicIp(Builder builder) {
            this.projectId = builder.projectId;
            this.etherType = builder.etherType;
        }
    }

    public static class Builder {
        private String projectId;
        private EtherType etherType;

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public CreatePublicIpRequest build() {
            PublicIp publicIp = new PublicIp(this);
            return new CreatePublicIpRequest(publicIp);
        }

        public Builder etherType(EtherType etherType) {
            this.etherType = etherType;
            return this;
        }
    }




}
