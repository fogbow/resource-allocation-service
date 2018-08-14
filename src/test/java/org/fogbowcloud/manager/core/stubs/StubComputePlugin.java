package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;

/**
 * This class is a stub for the ComputePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubComputePlugin implements ComputePlugin<Token> {
    
    public StubComputePlugin() {}

    @Override
    public String requestInstance(ComputeOrder computeOrder, Token token)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

    @Override
    public ComputeInstance getInstance(String computeInstanceId, Token token)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteInstance(String computeInstanceId, Token token)
            throws FogbowManagerException, UnexpectedException {
    }

}
