package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.VolumeInstance;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.VolumePlugin;

/**
 * This class is a stub for the VolumePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubVolumePlugin implements VolumePlugin<Token> {

    public StubVolumePlugin() {
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, Token token)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

    @Override
    public VolumeInstance getInstance(String volumeInstanceId, Token token)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteInstance(String volumeInstanceId, Token token)
            throws FogbowManagerException, UnexpectedException {
    }

}
