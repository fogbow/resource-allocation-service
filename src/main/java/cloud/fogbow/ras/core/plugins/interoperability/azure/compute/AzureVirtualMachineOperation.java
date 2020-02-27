package cloud.fogbow.ras.core.plugins.interoperability.azure.compute;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.microsoft.azure.management.compute.VirtualMachineSize;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetVirtualMachineRef;

public interface AzureVirtualMachineOperation {

    void doCreateInstance(
            @NotNull AzureCreateVirtualMachineRef azureCreateVirtualMachineRef,
            @NotNull AzureUser azureCloudUser) throws FogbowException;

    VirtualMachineSize findVirtualMachineSize(
            int memoryRequired, 
            int vCpuRequired,
            @NotBlank String regionName, 
            @NotNull AzureUser azureCloudUser) throws FogbowException;

    AzureGetVirtualMachineRef doGetInstance(
            @NotBlank String azureInstanceId, 
            @NotNull AzureUser azureUser) throws FogbowException;

    void doDeleteInstance(
            @NotBlank String azureInstanceId, 
            @NotNull AzureUser azureCloudUser) throws FogbowException;

}

