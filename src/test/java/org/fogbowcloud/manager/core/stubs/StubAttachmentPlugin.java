package org.fogbowcloud.manager.core.stubs;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.AttachmentPlugin;

/**
 * This class is a stub for the AttachmentPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubAttachmentPlugin implements AttachmentPlugin {

    public StubAttachmentPlugin() {}
    
    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

    @Override
    public void deleteInstance(String attachmentInstanceId, Token localToken)
            throws FogbowManagerException, UnexpectedException {
    }

    @Override
    public AttachmentInstance getInstance(String attachmentInstanceId, Token localToken)
            throws FogbowManagerException, UnexpectedException {
        return null;
    }

}
