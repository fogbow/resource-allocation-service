package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.cloud.VolumePlugin;

/**
 * This class is a stub for the VolumePlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubVolumePlugin implements VolumePlugin<Token> {

    public StubVolumePlugin() {
    }

    @Override
    public String requestInstance(VolumeOrder volumeOrder, Token token)
            throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public VolumeInstance getInstance(String volumeInstanceId, Token token)
            throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteInstance(String volumeInstanceId, Token token)
            throws FogbowRasException, UnexpectedException {
    }

}
