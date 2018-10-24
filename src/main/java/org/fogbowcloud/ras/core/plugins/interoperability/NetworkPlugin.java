package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.NetworkInstance;
import org.fogbowcloud.ras.core.models.orders.NetworkOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;

public interface NetworkPlugin<T extends Token> {

    public String requestInstance(NetworkOrder networkOrder, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public NetworkInstance getInstance(String networkInstanceId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public void deleteInstance(String networkInstanceId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;
}
