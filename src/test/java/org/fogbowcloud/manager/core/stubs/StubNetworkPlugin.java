package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;

/**
 * This class is a stub for the NetworkPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubNetworkPlugin implements NetworkPlugin {

    public StubNetworkPlugin() {}
    
    @Override
    public String requestInstance(NetworkOrder networkOrder, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

    @Override
    public NetworkInstance getInstance(String networkInstanceId, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteInstance(String networkInstanceId, Token localToken)
            throws FogbowManagerException, UnexpectedException {
    }

}
