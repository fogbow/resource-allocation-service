package org.fogbowcloud.manager.core.manager.plugins.cloud.attachment;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface AttachmentPlugin {

	public String requestInstance(AttachmentOrder attachmentOrder, Token localToken) throws RequestException;

    public void deleteInstance(String attachmentInstanceId, Token localToken) throws RequestException;
    
    public AttachmentInstance getInstance(String attachmentInstanceId, Token localToken) throws RequestException;
}
