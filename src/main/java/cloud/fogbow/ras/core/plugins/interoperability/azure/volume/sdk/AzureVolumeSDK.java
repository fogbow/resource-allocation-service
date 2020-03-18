package cloud.fogbow.ras.core.plugins.interoperability.azure.volume.sdk;

import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.Disks;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.constants.Messages;
import rx.Completable;
import rx.Observable;

public class AzureVolumeSDK {

    public static Observable<Indexable> buildCreateDiskObservable(Creatable<Disk> diskCreatable) {
        return diskCreatable.createAsync();
    }
    
    public static Completable buildDeleteDiskCompletable(Azure azure, String resourceId) 
            throws UnexpectedException {
        try {
            Disks disks = getDisksSDK(azure);
            return disks.deleteByIdAsync(resourceId);
        } catch (Exception e) {
            String message = String.format(Messages.Exception.GENERIC_EXCEPTION, e);
            throw new UnexpectedException(message, e);
        }
    }
    
    public static Optional<Disk> getDisk(Azure azure, String resourceId) 
            throws UnexpectedException {
        try {
            Disks disks = getDisksSDK(azure);
            return Optional.ofNullable(disks.getById(resourceId));
        } catch (Exception e) {
            String message = String.format(Messages.Exception.GENERIC_EXCEPTION, e);
            throw new UnexpectedException(message, e);
        }
    }

    // This class is used only for test proposes.
    // It is necessary because was not possible mock the Azure(final class)
    @VisibleForTesting
    static Disks getDisksSDK(Azure azure) {
        return azure.disks();
    }

}
