package org.fogbowcloud.manager.core.plugins.serialization.openstack.computev2;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

public class GetResponse {

    @SerializedName("server")
    private Server server;

    public class Server {

        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("addresses")
        private Addresses addresses;

        @SerializedName("flavor")
        private Flavor flavor;

        @SerializedName("status")
        private String status;

        public class Addresses {

            @SerializedName("provider")
            private Address[] providerAddresses;

            public Address[] getProviderAddresses() {
                return providerAddresses;
            }

            public class Address {

                @SerializedName("addr")
                private String address;

                public String getAddress() {
                    return address;
                }

            }

        }

        public class Flavor {

            @SerializedName("id")
            private String id;

            public String getId() {
                return id;
            }

        }

    }

    public String getId() {
        return server.id;
    }

    public String getName() {
        return server.name;
    }

    public Server.Addresses getAddresses() {
        return server.addresses;
    }

    public Server.Flavor getFlavor() {
        return server.flavor;
    }

    public String getStatus() {
        return server.status;
    }

    public static GetResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetResponse.class);
    }

}
