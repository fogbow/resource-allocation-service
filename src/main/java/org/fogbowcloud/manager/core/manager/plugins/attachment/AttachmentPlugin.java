package org.fogbowcloud.manager.core.manager.plugins.attachment;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.models.orders.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface AttachmentPlugin {

	public String attachVolume(Token localToken, AttachmentOrder attachmentOrder) throws RequestException;

    public void detachVolume(Token localToken, Order order) throws RequestException;
    
    public AttachmentInstance getAttachment(Token localToken, Order order) throws RequestException;
}
