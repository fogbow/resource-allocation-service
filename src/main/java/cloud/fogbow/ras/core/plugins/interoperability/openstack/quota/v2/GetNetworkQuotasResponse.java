package cloud.fogbow.ras.core.plugins.interoperability.openstack.quota.v2;

import static cloud.fogbow.common.constants.OpenStackConstants.Quota.FLOATING_IP_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Quota.LIMIT_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Quota.NETWORK_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Quota.QUOTA_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Quota.RESERVED_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Quota.USED_KEY_JSON;

import com.google.gson.annotations.SerializedName;

import cloud.fogbow.common.util.GsonHolder;

/**
 * Documentation: 
 * https://docs.openstack.org/api-ref/network/v2/?expanded=#quotas-details-extension-quota-details
 *
 * Response Example: 
 * {
 *     "quota": {
 *         "floatingip": {
 *             "limit": 2,
 *             "reserved": 0,
 *             "used": 0
 *         },
 *         "network" :{
 *             "limit": 10,
 *             "reserved": 0,
 *             "used": 1,
 *         }
 *     }
 * }
 */
public class GetNetworkQuotasResponse {

    @SerializedName(QUOTA_KEY_JSON)
    private Quota quota;
    
    public static GetNetworkQuotasResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetNetworkQuotasResponse.class);
    }
    
    public class Quota {
        
        @SerializedName(FLOATING_IP_KEY_JSON)
        private FloatingIp floatingIp;
        
        @SerializedName(NETWORK_KEY_JSON)
        private Network network;
        
        public class QuotaDetails {
            
            @SerializedName(LIMIT_KEY_JSON)
            int limit;
            
            @SerializedName(RESERVED_KEY_JSON)
            int reserved;
            
            @SerializedName(USED_KEY_JSON)
            int used;
            
        }
        
        public class FloatingIp extends QuotaDetails {}
        
        public class Network extends QuotaDetails {}
        
    }
    
    public int getFloatingIpLimit() {
        return quota.floatingIp.limit;
    }

    public int getFloatingIpReserved() {
        return quota.floatingIp.reserved;
    }

    public int getFloatingIpUsed() {
        return quota.floatingIp.used;
    }
    
    public int getNetworkLimit() {
        return quota.network.limit;
    }

    public int getNetworkReserved() {
        return quota.network.reserved;
    }

    public int getNetworkUsed() {
        return quota.network.used;
    }

}
