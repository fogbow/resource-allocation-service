package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;

/**
 * This class is a stub for the ComputePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubComputePlugin implements ComputePlugin<CloudUser> {

    public StubComputePlugin(String confFilePath) {
    }

    @Override
    public String requestInstance(ComputeOrder computeOrder, CloudUser cloudUser) {
        return null;
    }

    @Override
    public ComputeInstance getInstance(ComputeOrder computeOrder, CloudUser cloudUser) {
        return null;
    }

    @Override
    public void deleteInstance(ComputeOrder computeOrder, CloudUser cloudUser) {
    }

    @Override
    public void takeSnapshot(ComputeOrder computeOrder, String name, CloudUser cloudUser) throws FogbowException {
    }

    @Override
    public void pauseInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
    }

    @Override
    public void hibernateInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
    }

    @Override
    public void stopInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
        
    }
    
    @Override
    public void resumeInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {

    }

    @Override
    public boolean isPaused(String cloudState) {
        return false;
    }

    @Override
    public boolean isHibernated(String cloudState) {
        return false;
    }

    @Override
    public boolean isStopped(String cloudState) throws FogbowException {
        return false;
    }
    
    @Override
    public boolean isReady(String cloudState) {
        return true;
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return false;
    }
}
