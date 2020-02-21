package cloud.fogbow.ras.core.plugins.interoperability.azure.compute;

import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureCreateVirtualMachineRef;

public interface AzureVirtualMachineOperation {

    void doCreateInstance(AzureCreateVirtualMachineRef azureCreateVirtualMachineRef,
                          AzureUser azureCloudUser)
            throws UnauthenticatedUserException, UnexpectedException, InstanceNotFoundException;

    String findVirtualMachineSizeName(int memoryRequired, int vCpuRequired,
                                      String regionName, AzureUser azureCloudUser)
            throws UnauthenticatedUserException, NoAvailableResourcesException, UnexpectedException;

    AzureGetVirtualMachineRef doGetInstance(String azureInstanceId, AzureUser azureUser)
            throws UnauthenticatedUserException, UnexpectedException,
            NoAvailableResourcesException, InstanceNotFoundException;

    void doDeleteInstance(String azureInstanceId, AzureUser azureCloudUser)
            throws UnauthenticatedUserException, UnexpectedException, InstanceNotFoundException;

}

