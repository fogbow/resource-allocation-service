package org.fogbowcloud.manager.core.manager.plugins.attachment;

import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.instances.VolumeAttachment;
import org.fogbowcloud.manager.core.models.token.Token;

public interface AttachmentPlugin {

	public String attachVolume(Token localToken, AttachmentOrder attachmentOrder) throws RequestException;

    public void detachVolume(Token localToken, String instanceId, String attachmentId) throws RequestException;
    
    public VolumeAttachment getInstance(Token localToken, String instanceId, String attachmentId) throws RequestException;
}
