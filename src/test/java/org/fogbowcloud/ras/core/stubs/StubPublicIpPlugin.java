package org.fogbowcloud.ras.core.stubs;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;

/**
 * This class is a stub for the PublicIpPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubPublicIpPlugin implements PublicIpPlugin {
    @Override
    public String requestInstance(PublicIpOrder publicIpOrder, String computeInstanceId, Token token) throws FogbowRasException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteInstance(String publicIpInstanceId, Token token) throws FogbowRasException, UnexpectedException {

    }

    @Override
    public PublicIpInstance getInstance(String publicIpInstanceId, Token token) throws FogbowRasException, UnexpectedException {
        return null;
    }
}
