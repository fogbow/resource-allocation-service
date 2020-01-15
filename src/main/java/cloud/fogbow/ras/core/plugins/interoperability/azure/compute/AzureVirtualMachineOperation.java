package cloud.fogbow.ras.core.plugins.interoperability.azure.compute;

import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AzureUser;

public interface AzureVirtualMachineOperation {

    AzureGetVirtualMachineRef doGetInstance(String azureInstanceId, AzureUser azureUser)
            throws UnauthenticatedUserException, UnexpectedException,
            NoAvailableResourcesException, InstanceNotFoundException;

}

