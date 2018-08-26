package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;

public interface AttachmentPlugin<T extends Token> {

    public String requestInstance(AttachmentOrder attachmentOrder, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public void deleteInstance(String attachmentInstanceId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;

    public AttachmentInstance getInstance(String attachmentInstanceId, T localUserAttributes)
            throws FogbowRasException, UnexpectedException;
}
