package org.fogbowcloud.manager.core.manager.plugins.attachment;

import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public interface AttachmentPlugin {

	public String attachVolume(Token localToken, AttachmentOrder volumeAttachment);

    public String detachVolume(Token localToken, AttachmentOrder volumeAttachment);
    
    public AttachmentInstance getAttachmentInstance(Token localToken, String instanceId);
}
