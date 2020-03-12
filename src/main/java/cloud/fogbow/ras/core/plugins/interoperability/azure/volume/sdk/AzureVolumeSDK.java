package cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

import rx.Completable;
import rx.Observable;

public class AzureVolumeSDK {

    public static Observable<Indexable> buildCreateDiskObservable(Creatable<Disk> diskCreatable) {
        return diskCreatable.createAsync();
    }
    
    public static Completable buildDeleteDiskCompletable(Azure azure, String resourceId) {
        return azure.disks().deleteByIdAsync(resourceId);
    }

}
