package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.quota.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackErrorResponse;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Documentation: https://cloudstack.apache.org/api/apidocs-4.9/apis/listPublicIpAddresses.html
 *
 * Response Example:
 * {
 *    "listpublicipaddressesresponse":{
 *       "count":1,
 *       "publicipaddress":[
 *          {
 *             "id":"d23b97e7-e476-421a-a622-49f8ad213c2a",
 *             "ipaddress":"200.139.35.14"
 *          }
*       ]
 * }
 */
public class ListPublicIpAddressResponse {
    private static final String LIST_PUBLIC_IP_ADDRESSES_JSON_KEY = "listpublicipaddressesresponse";
    private static final String PUBLIC_IP_ADDRESSES_JSON_KEY = "publicipaddress";

    @SerializedName(LIST_PUBLIC_IP_ADDRESSES_JSON_KEY)
    private ListPublicIpAddressesResponse response;

    public static ListPublicIpAddressResponse fromJson(String json) throws FogbowException {
        ListPublicIpAddressResponse listPublicIpAddressResponse = GsonHolder.getInstance().fromJson(json, ListPublicIpAddressResponse.class);
        listPublicIpAddressResponse.response.checkErrorExistence();
        return listPublicIpAddressResponse;
    }

    public List<PublicIpAddress> getPublicIpAddresses() {
        return this.response.publicIpAddresses;
    }

    public class ListPublicIpAddressesResponse extends CloudStackErrorResponse {
        @SerializedName(PUBLIC_IP_ADDRESSES_JSON_KEY)
        private List<PublicIpAddress> publicIpAddresses;
    }

    public class PublicIpAddress {
        private String id;
        private String ipAddress;

        public String getId() {
            return id;
        }

        public String getIpAddress() {
            return ipAddress;
        }
    }
}
