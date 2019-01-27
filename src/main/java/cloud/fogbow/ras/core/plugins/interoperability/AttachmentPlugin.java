package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.models.instances.AttachmentInstance;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;

public interface AttachmentPlugin<T extends CloudToken> {

    public String requestInstance(AttachmentOrder attachmentOrder, CloudToken localUserAttributes) throws FogbowException;

    public void deleteInstance(String attachmentInstanceId, CloudToken localUserAttributes) throws FogbowException;

    public AttachmentInstance getInstance(String attachmentInstanceId, CloudToken localUserAttributes) throws FogbowException;
}
