package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

import static org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRestApiConstants.PublicIp.*;

/**
 * Documentation : https://cloudstack.apache.org/api/apidocs-4.9/apis/enableStaticNat.html
 * <p>
 * Request Example:
 */
public class EnableStaticNatRequest extends CloudStackRequest {

    protected EnableStaticNatRequest(Builder builder) throws InvalidParameterException {
        addParameter(VM_ID_KEY_JSON, builder.virtualMachineId);
        addParameter(IP_ADDRESS_ID_KEY_JSON, builder.ipAddressId);
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public String getCommand() {
        return ENABLE_STATIC_NAT_COMMAND;
    }

    public static class Builder {
        private String virtualMachineId;
        private String ipAddressId;

        public Builder virtualMachineId(String virtualMachineId) {
            this.virtualMachineId = virtualMachineId;
            return this;
        }

        public Builder ipAddressId(String ipAddressId) {
            this.ipAddressId = ipAddressId;
            return this;
        }

        public EnableStaticNatRequest build() throws InvalidParameterException {
            return new EnableStaticNatRequest(this);
        }
    }

}
