package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.compute.VirtualMachineSizes;
import com.microsoft.azure.management.compute.VirtualMachines;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.resources.fluentcore.model.Indexable;
import rx.Completable;
import rx.Observable;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AzureVirtualMachineSDK {

    static Observable<Indexable> buildVirtualMachineObservable(Azure azure, String resourceName, Region region,
                                                               String resourceGroupName, Network network, String subnetName,
                                                               String imagePublished, String imageOffer, String imageSku,
                                                               String osUserName, String osUserPassword, String osComputeName,
                                                               String userData, int diskSize, String size, Map tags) {

        VirtualMachines virtualMachine = getVirtualMachinesSDK(azure);

        VirtualMachine.DefinitionStages.WithOS osChoosen = virtualMachine
                .define(resourceName)
                .withRegion(region)
                .withExistingResourceGroup(resourceGroupName)
                .withExistingPrimaryNetwork(network)
                .withSubnet(subnetName)
                .withPrimaryPrivateIPAddressDynamic()
                .withoutPrimaryPublicIPAddress();

        VirtualMachine.DefinitionStages.WithFromImageCreateOptionsManaged optionsManaged;
        if (isWindowsImage(imageOffer, imageSku)) {
            optionsManaged = osChoosen.withLatestWindowsImage(imagePublished, imageOffer, imageSku)
                    .withAdminUsername(osUserName)
                    .withAdminPassword(osUserPassword)
                    .withComputerName(osComputeName);
        } else {
            optionsManaged = osChoosen.withLatestLinuxImage(imagePublished, imageOffer, imageSku)
                    .withRootUsername(osUserName)
                    .withRootPassword(osUserPassword)
                    .withComputerName(osComputeName);
        }
        return optionsManaged
                .withCustomData(userData)
                .withOSDiskSizeInGB(diskSize)
                .withSize(size)
                .withTags(tags)
                .createAsync();
    }

    public static Optional<VirtualMachine> getVirtualMachine(Azure azure, String virtualMachineId)
            throws InternalServerErrorException {

        try {
            VirtualMachines virtualMachinesObject = getVirtualMachinesSDK(azure);
            return Optional.ofNullable(virtualMachinesObject.getById(virtualMachineId));
        } catch (RuntimeException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    static PagedList<VirtualMachineSize> getVirtualMachineSizes(Azure azure, Region region)
            throws InternalServerErrorException {

        try {
            VirtualMachines virtualMachinesObject = getVirtualMachinesSDK(azure);
            VirtualMachineSizes sizes = virtualMachinesObject.sizes();
            return sizes.listByRegion(region);
        } catch (RuntimeException e) {
            throw new InternalServerErrorException(e.getMessage());
        }
    }

    static Completable buildDeleteVirtualMachineCompletable(Azure azure, String virtualMachineId) {
        return azure.virtualMachines().deleteByIdAsync(virtualMachineId);
    }

    @VisibleForTesting
    static boolean isWindowsImage(String imageOffer, String imageSku) {
        return containsWindownsOn(imageOffer) || containsWindownsOn(imageSku);
    }

    @VisibleForTesting
    static boolean containsWindownsOn(String text) {
        String regex = ".*windows.*";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcherOffer = pattern.matcher(text);
        return matcherOffer.find();
    }

    // This class is used only for test proposes.
    // It is necessary because was not possible mock the Azure(final class)
    @VisibleForTesting
    static VirtualMachines getVirtualMachinesSDK(Azure azure) {
        return azure.virtualMachines();
    }

}
