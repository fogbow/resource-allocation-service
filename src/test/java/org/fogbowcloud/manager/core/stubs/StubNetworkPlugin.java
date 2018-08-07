package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.NetworkInstance;
import org.fogbowcloud.manager.core.models.orders.NetworkOrder;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;
import org.fogbowcloud.manager.core.plugins.cloud.NetworkPlugin;

/**
 * This class is a stub for the NetworkPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubNetworkPlugin implements NetworkPlugin<LocalUserAttributes> {

    public StubNetworkPlugin() {}
    
    @Override
    public String requestInstance(NetworkOrder networkOrder, LocalUserAttributes localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

    @Override
    public NetworkInstance getInstance(String networkInstanceId, LocalUserAttributes localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteInstance(String networkInstanceId, LocalUserAttributes localUserAttributes)
            throws FogbowManagerException, UnexpectedException {
    }

}
