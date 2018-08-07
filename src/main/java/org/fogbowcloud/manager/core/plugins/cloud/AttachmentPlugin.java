package org.fogbowcloud.manager.core.plugins.cloud;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;

public interface AttachmentPlugin<T extends LocalUserAttributes> {

	public String requestInstance(AttachmentOrder attachmentOrder, T localUserAttributes) throws FogbowManagerException, UnexpectedException;

    public void deleteInstance(String attachmentInstanceId, T localUserAttributes) throws FogbowManagerException, UnexpectedException;
    
    public AttachmentInstance getInstance(String attachmentInstanceId, T localUserAttributes) throws FogbowManagerException, UnexpectedException;
}
