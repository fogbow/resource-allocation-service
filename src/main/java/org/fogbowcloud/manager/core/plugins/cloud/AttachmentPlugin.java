package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.tokens.Token;

public interface AttachmentPlugin {

	public String requestInstance(AttachmentOrder attachmentOrder, Token localToken) throws FogbowManagerException;

    public void deleteInstance(String attachmentInstanceId, Token localToken) throws FogbowManagerException;
    
    public AttachmentInstance getInstance(String attachmentInstanceId, Token localToken) throws FogbowManagerException;
}
