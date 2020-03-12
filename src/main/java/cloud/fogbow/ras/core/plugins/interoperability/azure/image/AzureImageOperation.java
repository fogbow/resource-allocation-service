package cloud.fogbow.ras.core.plugins.interoperability.azure.image;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineOffer;
import com.microsoft.azure.management.compute.VirtualMachinePublisher;
import com.microsoft.azure.management.compute.VirtualMachineSku;

public class AzureImageOperation {
    private final String region;

    public AzureImageOperation(String region) {
        this.region = region;
    }

    public PagedList<VirtualMachineSku> getSkusFrom(VirtualMachineOffer offer) {
        return offer.skus().list();
    }

    public PagedList<VirtualMachineOffer> getOffersFrom(VirtualMachinePublisher publisher) {
        return publisher.offers().list();
    }

    public PagedList<VirtualMachinePublisher> getPublishers(Azure azure) {
        return azure.virtualMachineImages().publishers().listByRegion(this.region);
    }
}
