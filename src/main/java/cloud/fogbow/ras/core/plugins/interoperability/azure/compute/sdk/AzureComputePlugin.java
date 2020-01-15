package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;

public class AzureComputePlugin implements ComputePlugin<AzureUser> {

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
    public ComputeInstance getInstance(ComputeOrder computeOrder, AzureUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, AzureUser cloudUser) throws FogbowException {

    }
}
