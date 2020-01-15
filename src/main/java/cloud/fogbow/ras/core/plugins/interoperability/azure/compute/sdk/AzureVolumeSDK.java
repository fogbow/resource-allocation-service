package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import com.microsoft.azure.management.Azure;
import rx.Completable;

public class AzureVolumeSDK {

    static Completable buildDeleteDiskCompletable(Azure azure, String diskId) {
        return azure.disks().deleteByIdAsync(diskId);
    }

}
