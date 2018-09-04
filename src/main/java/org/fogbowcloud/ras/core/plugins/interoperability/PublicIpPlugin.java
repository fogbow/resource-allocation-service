package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;

public interface PublicIpPlugin<T extends Token> {

    String requestInstance(PublicIpOrder publicIpOrder, T token) throws FogbowRasException, UnexpectedException;

    void deleteInstance(String publicIpOrderId, T token) throws FogbowRasException, UnexpectedException;

    PublicIpInstance getInstance(String publicIpOrderId, T token) throws FogbowRasException, UnexpectedException;

}
