package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.emulatedmodels;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;

import com.google.gson.annotations.SerializedName;

import static cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudConstants.Json.*;

public class EmulatedSecurityRule implements JsonSerializable {

    @SerializedName(INSTANCE_ID_KEY_JSON)
    private String id;

    @SerializedName(DIRECTION_KEY_JSON)
    private String direction;

    @SerializedName(PORT_FROM_KEY_JSON)
    private int portFrom;

    @SerializedName(PORT_TO_KEY_JSON)
    private int portTo;

    @SerializedName(CIDR_KEY_JSON)
    private String cidr;

    @SerializedName(ETHER_TYPE_KEY_JSON)
    private String etherType;

    @SerializedName(PROTOCOL_KEY_JSON)
    private String protocol;

    private EmulatedSecurityRule (String id, String direction, int portFrom, int portTo,
                                 String cidr, String etherType, String protocol) {
        this.id = id;
        this.cidr = cidr;
        this.portFrom = portFrom;
        this.portTo = portTo;
        this.direction = direction;
        this.etherType = etherType;
        this.protocol = protocol;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static EmulatedSecurityRule fromJson(String jsonStr) {
        return GsonHolder.getInstance().fromJson(jsonStr, EmulatedSecurityRule.class);
    }

    public static class Builder {
        private String id;
        private String direction;
        private int portFrom;
        private int portTo;
        private String cidr;
        private String etherType;
        private String protocol;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder direction(String direction) {
            this.direction = direction;
            return this;
        }

        public Builder portFrom(int portFrom) {
            this.portFrom = portFrom;
            return this;
        }

        public Builder portTo(int portTo) {
            this.portTo = portTo;
            return this;
        }

        public Builder cidr(String cidr) {
            this.cidr = cidr;
            return this;
        }

        public Builder etherType(String etherType) {
            this.etherType = etherType;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public EmulatedSecurityRule build(){
            return new EmulatedSecurityRule(id, direction, portFrom, portTo, cidr, etherType, protocol);
        }
    }

    public String getId() {
        return id;
    }

    public String getDirection() {
        return direction;
    }

    public int getPortFrom() {
        return portFrom;
    }

    public int getPortTo() {
        return portTo;
    }

    public String getCidr() {
        return cidr;
    }

    public String getEtherType() {
        return etherType;
    }

    public String getProtocol() {
        return protocol;
    }
}
