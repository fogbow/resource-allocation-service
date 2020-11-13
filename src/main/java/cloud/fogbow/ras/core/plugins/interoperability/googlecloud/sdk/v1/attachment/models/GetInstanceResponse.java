package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.attachment.models;


import cloud.fogbow.common.util.GsonHolder;
import com.google.gson.annotations.SerializedName;
import static cloud.fogbow.common.constants.GoogleCloudConstants.Attachment.*;
import static cloud.fogbow.common.constants.GoogleCloudConstants.ENDPOINT_SEPARATOR;

import java.util.List;

/**
 * Documentation: https://cloud.google.com/compute/docs/reference/rest/v1/instances/attachDisk/
 * <p>
 * Response example:
 *   {
 *       "name": "instance-1",
 *       "disks": [
 *          {
 *              "source": "https://www.googleapis.com/compute/v1/projects/diesel-talon-291703/zones/southamerica-east1-b/disks/diskAttach1",
 *              "deviceName": "disk1"
 *          },
 *          {
 *              "source": "https://www.googleapis.com/compute/v1/projects/diesel-talon-291703/zones/southamerica-east1-b/disks/diskAttach2",
 *              "deviceName": "disk2"
 *          }
 *       ]
 *  }
 */
public class GetInstanceResponse {

    private static final Integer ZERO_INDEXED = -1;
    @SerializedName(INSTANCE_NAME_KEY_JSON)
    private String name;
    @SerializedName(DISKS_KEY_JSON)
    private List<Disk> disks;

    public String getName() { return name; }
    public List<Disk> getDisks() { return disks; };

    public static GetInstanceResponse fromJson(String json) {
        return GsonHolder.getInstance().fromJson(json, GetInstanceResponse.class);
    }

    public class Disk {
        @SerializedName(DEVICE_NAME_KEY_JSON)
        private String deviceName;

        @SerializedName(VOLUME_SOURCE_KEY_JSON)
        private String source;

        public String getDeviceName() { return deviceName; }
        public String getSource() { return source; }
        public String getVolumeName() {
            String[] sourceSplit = getSource().split(ENDPOINT_SEPARATOR);
            Integer size = sourceSplit.length;
            return sourceSplit[size+ZERO_INDEXED];
        }
    }
}