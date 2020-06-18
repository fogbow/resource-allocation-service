package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.network.v4_9;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;

import static cloud.fogbow.common.constants.CloudStackConstants.Network.*;

public class CreateNetworkRequest extends CloudStackRequest {

    private CreateNetworkRequest(Builder builder) throws InternalServerErrorException {
        super(builder.cloudStackUrl);

        addParameter(NAME_KEY_JSON, builder.name);
        addParameter(DISPLAY_TEXT_KEY_JSON, builder.displayText);
        addParameter(NETWORK_OFFERING_ID_KEY_JSON, builder.networkOfferingId);
        addParameter(ZONE_ID_KEY_JSON, builder.zoneId);
        addParameter(STARTING_IP_KEY_JSON, builder.startIp);
        addParameter(ENDING_IP_KEY_JSON, builder.endingIp);
        addParameter(GATEWAY_KEY_JSON, builder.gateway);
        addParameter(NETMASK_KEY_JSON, builder.netmask);
    }

    @Override
    public String getCommand() {
        return CREATE_NETWORK_COMMAND;
    }

    public static class Builder {
        private String cloudStackUrl;
        private String name;
        private String displayText;
        private String networkOfferingId;
        private String zoneId;
        private String startIp;
        private String endingIp;
        private String gateway;
        private String netmask;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder displayText(String displayText) {
            this.displayText = displayText;
            return this;
        }

        public Builder networkOfferingId(String networkOfferingId) {
            this.networkOfferingId = networkOfferingId;
            return this;
        }

        public Builder zoneId(String zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public Builder startIp(String startIp) {
            this.startIp = startIp;
            return this;
        }

        public Builder endingIp(String endingIp) {
            this.endingIp = endingIp;
            return this;
        }

        public Builder gateway(String gateway) {
            this.gateway = gateway;
            return this;
        }

        public Builder netmask(String netmask) {
            this.netmask = netmask;
            return this;
        }

        public CreateNetworkRequest build(String cloudStackUrl) throws InternalServerErrorException {
            this.cloudStackUrl = cloudStackUrl;
            return new CreateNetworkRequest(this);
        }
    }
}
