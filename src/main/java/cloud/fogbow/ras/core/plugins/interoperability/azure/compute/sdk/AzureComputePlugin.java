package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.AzureGetVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureIdBuilder;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import java.util.List;

public class AzureComputePlugin implements ComputePlugin<AzureUser> {

    private static final Logger LOGGER = Logger.getLogger(AzureComputePlugin.class);

    private AzureVirtualMachineOperation azureVirtualMachineOperation;

    @Override
    public boolean isReady(String instanceState) {
        return false;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, AzureUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, AzureUser azureUser)
            throws FogbowException {

        LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE_S, computeOrder.getInstanceId()));
        String azureVirtualMachineId = computeOrder.getInstanceId();

        AzureGetVirtualMachineRef azureGetVirtualMachineRef = this.azureVirtualMachineOperation
                .doGetInstance(azureVirtualMachineId, azureUser);

        return buildComputeInstance(azureGetVirtualMachineRef, azureUser);
    }

    @VisibleForTesting
    ComputeInstance buildComputeInstance(AzureGetVirtualMachineRef azureGetVirtualMachineRef, AzureUser azureUser) {
        String virtualMachineName = azureGetVirtualMachineRef.getName();
        String virtualMachineId = AzureIdBuilder
                .configure(azureUser)
                .buildVirtualMachineId(virtualMachineName);
        String cloudState = azureGetVirtualMachineRef.getCloudState();
        int vCPU = azureGetVirtualMachineRef.getvCPU();
        int memory = azureGetVirtualMachineRef.getMemory();
        int disk = azureGetVirtualMachineRef.getDisk();
        List<String> ipAddresses = azureGetVirtualMachineRef.getIpAddresses();

        return new ComputeInstance(virtualMachineId, cloudState, virtualMachineName, vCPU, memory, disk, ipAddresses);
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, AzureUser cloudUser) throws FogbowException {

    }

}
