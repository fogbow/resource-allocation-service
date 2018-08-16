package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.compute.v4_9;

import com.google.gson.annotations.SerializedName;
import org.fogbowcloud.manager.util.GsonHolder;

import static org.fogbowcloud.manager.core.plugins.cloud.cloudstack.CloudStackRestApiConstants.Compute.*;


/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listVirtualMachines.html
 *
 * Response example:
 * {
 * 	"listvirtualmachinesresponse": {
 * 		"count": 6,
 * 		"virtualmachine": [{
 * 			"id": "97637962-2244-4159-b72c-120834757514",
 * 			"name": "PreprocessingProducao",
 * 			"displayname": "PreprocessingProducao",
 * 			"account": "fogbow-rec",
 * 			"userid": "2501e9ee-f5b0-4034-9be2-793ef5e45437",
 * 			"username": "fogbow-rec",
 * 			"domainid": "0dbcf598-4d56-4b8a-8a6f-343fd0956392",
 * 			"domain": "FOGBOW",
 * 			"created": "2017-11-20T12:23:15-0200",
 * 			"state": "Running",
 * 			"haenable": true,
 * 			"zoneid": "0d89768b-bdf5-455e-b4fd-91881fa07375",
 * 			"zonename": "CDC-REC01",
 * 			"templateid": "d022c1ae-2ca1-4cbb-a54b-611b0c7f0f53",
 * 			"templatename": "FogbowTemplate",
 * 			"templatedisplaytext": "Template with fogbow user",
 * 			"passwordenabled": true,
 * 			"serviceofferingid": "5a21d3b3-9496-4bc1-a84b-d5247783b48c",
 * 			"serviceofferingname": "Grande",
 * 			"cpunumber": 4,
 * 			"cpuspeed": 2300,
 * 			"memory": 6144,
 * 			"cpuused": "0.05%",
 * 			"networkkbsread": 2077,
 * 			"networkkbswrite": 187,
 * 			"diskkbsread": 1,
 * 			"diskkbswrite": 134000,
 * 			"memorykbs": 6291480,
 * 			"memoryintfreekbs": 1040,
 * 			"memorytargetkbs": 6291456,
 * 			"diskioread": 0,
 * 			"diskiowrite": 0,
 * 			"guestosid": "1579d5c2-38b0-11e5-b8fd-3ea9d8d6392b",
 * 			"rootdeviceid": 0,
 * 			"rootdevicetype": "ROOT",
 * 			"securitygroup": [],
 * 			"nic": [{
 * 				"id": "c7d6b5f5-3f81-4ee6-98bd-cce814b1f1dc",
 * 				"networkid": "a7821628-0020-4865-9733-0d5fcfa43d58",
 * 				"networkname": "fogbow-rec",
 * 				"netmask": "255.255.255.0",
 * 				"gateway": "10.1.1.1",
 * 				"ipaddress": "10.1.1.146",
 * 				"isolationuri": "vlan://2916",
 * 				"broadcasturi": "vlan://2916",
 * 				"traffictype": "Guest",
 * 				"type": "Isolated",
 * 				"isdefault": true,
 * 				"macaddress": "02:00:4b:7b:03:dd",
 * 				"secondaryip": []
 *                        }],
 * 			"hypervisor": "XenServer",
 * 			"details": {
 * 				"hypervisortoolsversion": "xenserver61"
 *            },
 * 			"affinitygroup": [],
 * 			"isdynamicallyscalable": true,
 * 			"ostypeid": 254,
 * 			"tags": []* 		},
 */
public class GetComputeResponse {

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

    public static GetComputeResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetComputeResponse.class);
    }

    public class Flavor {
        @SerializedName(ID_KEY_JSON)
        private String id;

        public String getId() {
            return id;
        }

    }

    public class Addresses {
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
