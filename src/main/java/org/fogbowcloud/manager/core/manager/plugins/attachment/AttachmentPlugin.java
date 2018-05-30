package org.fogbowcloud.manager.core.manager.plugins.attachment;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface AttachmentPlugin {

	public String requestInstance(AttachmentOrder attachmentOrder, Token localToken) throws RequestException;

    public void deleteInstance(Token localToken, String instanceId) throws RequestException;
    
    public AttachmentInstance getInstance(Token localToken, String instanceId) throws RequestException;
}
