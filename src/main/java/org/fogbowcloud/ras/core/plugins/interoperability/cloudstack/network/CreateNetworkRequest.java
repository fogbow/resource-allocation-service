package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.network;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class CreateNetworkRequest extends CloudStackRequest {
    public static final String CREATE_NETWORK_COMMAND = "createNetwork";
    public static final String NAME_KEY = "name";
    public static final String NETWORK_OFFERING_ID_KEY = "networkofferingid";
    public static final String ZONE_ID_KEY = "zoneid";
    public static final String STARTING_IP_KEY = "startip";
    public static final String ENDING_IP_KEY = "endip";
    public static final String GATEWAY_KEY = "gateway";
    public static final String NETMASK_KEY = "netmask";
    public static final String DISPLAY_TEXT_KEY = "displaytext";

    private CreateNetworkRequest(Builder builder) throws InvalidParameterException {
        addParameter(NAME_KEY, builder.name);
        addParameter(DISPLAY_TEXT_KEY, builder.displayText);
        addParameter(NETWORK_OFFERING_ID_KEY, builder.networkOfferingId);
        addParameter(ZONE_ID_KEY, builder.zoneId);
        addParameter(STARTING_IP_KEY, builder.startIp);
        addParameter(ENDING_IP_KEY, builder.endingIp);
        addParameter(GATEWAY_KEY, builder.gateway);
        addParameter(NETMASK_KEY, builder.netmask);
    }

    @Override
    public String getCommand() {
        return CREATE_NETWORK_COMMAND;
    }

    public static class Builder {
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

        public CreateNetworkRequest build() throws InvalidParameterException {
            return new CreateNetworkRequest(this);
        }
    }
}
