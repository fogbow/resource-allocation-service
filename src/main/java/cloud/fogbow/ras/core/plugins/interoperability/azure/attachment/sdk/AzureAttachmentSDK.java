package cloud.fogbow.ras.core.plugins.interoperability.azure.attachment.sdk;

import com.microsoft.azure.management.compute.Disk;
import com.microsoft.azure.management.compute.VirtualMachine;

import rx.Observable;

public class AzureAttachmentSDK {

    public static Observable<VirtualMachine> attachDisk(VirtualMachine virtualMachine, Disk disk) {
        return virtualMachine.update().withExistingDataDisk(disk).applyAsync();
    }
    
    public static Observable<VirtualMachine> detachDisk(VirtualMachine virtualMachine, int lun) {
        return virtualMachine.update().withoutDataDisk(lun).applyAsync();
    }
    
}
