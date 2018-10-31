package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.VolumeInstance;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;

public interface VolumePlugin<T extends Token> {

    public String requestInstance(VolumeOrder volumeOrder, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public VolumeInstance getInstance(String volumeInstanceId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public void deleteInstance(String volumeInstanceId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;
}
