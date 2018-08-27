package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.AttachmentPlugin;

public class CloudStackAttachmentPlugin implements AttachmentPlugin<CloudStackToken>{

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder,
            CloudStackToken localUserAttributes) throws FogbowRasException, UnexpectedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteInstance(String attachmentInstanceId, CloudStackToken localUserAttributes)
            throws FogbowRasException, UnexpectedException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public AttachmentInstance getInstance(String attachmentInstanceId,
            CloudStackToken localUserAttributes) throws FogbowRasException, UnexpectedException {
        // TODO Auto-generated method stub
        return null;
    }

}
