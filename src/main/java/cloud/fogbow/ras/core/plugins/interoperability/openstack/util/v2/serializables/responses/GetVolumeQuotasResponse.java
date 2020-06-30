package cloud.fogbow.ras.core.plugins.interoperability.openstack.util.v2.serializables.responses;

import static cloud.fogbow.common.constants.OpenStackConstants.Quota.ABSOLUTE_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Quota.LIMITS_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Quota.MAX_TOTAL_VOLUMES_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Quota.MAX_TOTAL_VOLUME_GIGABYTES_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Quota.TOTAL_GIGABYTES_USED_KEY_JSON;
import static cloud.fogbow.common.constants.OpenStackConstants.Quota.TOTAL_VOLUMES_USED_KEY_JSON;

import com.google.gson.annotations.SerializedName;

import cloud.fogbow.common.util.GsonHolder;

/**
 * Documentation: 
 * https://docs.openstack.org/api-ref/block-storage/v3/index.html?expanded=#limits-limits
 *
 * Response Example: 
 * {
 *     "limits": {
 *         "absolute": {
 *             "maxTotalVolumeGigabytes": 1000,
 *             "maxTotalVolumes": 10,
 *             "totalVolumesUsed": 0,
 *             "totalGigabytesUsed": 0
 *         }
 *     }
 * }
 */
public class GetVolumeQuotasResponse {

    @SerializedName(LIMITS_KEY_JSON)
    private Limits limits;
    
    public static GetVolumeQuotasResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetVolumeQuotasResponse.class);
    }
    
    public class Limits {
        
        @SerializedName(ABSOLUTE_KEY_JSON)
        private Absolute absolute;
        
        public class Absolute {
            
            @SerializedName(MAX_TOTAL_VOLUME_GIGABYTES_KEY_JSON)
            private int maxTotalVolumeGigabytes;
            
            @SerializedName(MAX_TOTAL_VOLUMES_KEY_JSON)
            private int maxTotalVolumes;
            
            @SerializedName(TOTAL_VOLUMES_USED_KEY_JSON)
            private int totalVolumesUsed;
            
            @SerializedName(TOTAL_GIGABYTES_USED_KEY_JSON)
            private int totalGigabytesUsed;
            
        }
        
    }
    
    public int getMaxTotalVolumeGigabytes() {
        return limits.absolute.maxTotalVolumeGigabytes;
    }

    public int getMaxTotalVolumes() {
        return limits.absolute.maxTotalVolumes;
    }

    public int getTotalVolumesUsed() {
        return limits.absolute.totalVolumesUsed;
    }

    public int getTotalGigabytesUsed() {
        return limits.absolute.totalGigabytesUsed;
    }
    
}
