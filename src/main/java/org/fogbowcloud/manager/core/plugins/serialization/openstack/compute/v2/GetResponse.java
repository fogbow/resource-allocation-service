package org.fogbowcloud.manager.core.plugins.serialization.openstack.compute.v2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import static org.fogbowcloud.manager.core.plugins.serialization.openstack.OpenstackRestApiConstants.Compute.*;

public class GetResponse {

    @SerializedName(SERVER_KEY_JSON)
    private Server server;

    public class Server {

        @SerializedName(ID_KEY_JSON)
        private String id;

        @SerializedName(NAME_KEY_JSON)
        private String name;

        @SerializedName(ADDRESSES_KEY_JSON)
        private Addresses addresses;

        @SerializedName(FLAVOR_KEY_JSON)
        private Flavor flavor;

        @SerializedName(STATUS_KEY_JSON)
        private String status;

    }

    public String getId() {
        return server.id;
    }

    public String getName() {
        return server.name;
    }

    public Addresses getAddresses() {
        return server.addresses;
    }

    public Flavor getFlavor() {
        return server.flavor;
    }

    public String getStatus() {
        return server.status;
    }

    public static GetResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetResponse.class);
    }

    public class Flavor {

        @SerializedName(ID_KEY_JSON)
        private String id;

        public String getId() {
            return id;
        }

    }

    public class Addresses {

        @SerializedName(PROVIDER_KEY_JSON)
        private Address[] providerAddresses;

        public Address[] getProviderAddresses() {
            return providerAddresses;
        }

    }

    public class Address {

        @SerializedName(ADDRESS_KEY_JSON)
        private String address;

        public String getAddress() {
            return address;
        }

    }
}
