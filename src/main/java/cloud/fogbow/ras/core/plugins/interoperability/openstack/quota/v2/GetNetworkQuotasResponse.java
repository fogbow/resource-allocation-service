package cloud.fogbow.ras.core.plugins.interoperability.openstack.quota.v2;

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

    @SerializedName("quota") // FIXME migrate the string to a constant...
    private Quota quota;
    
    public static GetNetworkQuotasResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetNetworkQuotasResponse.class);
    }
    
    public class Quota {
        
        @SerializedName("floatingip") // FIXME migrate the string to a constant...
        private FloatingIp floatingIp;
        
        @SerializedName("network") // FIXME migrate the string to a constant...
        private Network network;
        
        public class QuotaDetails {
            
            @SerializedName("limit") // FIXME migrate the string to a constant...
            int limit;
            
            @SerializedName("reserved") // FIXME migrate the string to a constant...
            int reserved;
            
            @SerializedName("used") // FIXME migrate the string to a constant...
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
