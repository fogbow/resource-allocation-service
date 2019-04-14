package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;

public interface AttachmentPlugin<S extends CloudUser> extends OrderPlugin<AttachmentInstance, AttachmentOrder, S> {

    public String requestInstance(AttachmentOrder attachmentOrder, S cloudUser) throws FogbowException;

    public void deleteInstance(String attachmentInstanceId, S cloudUser) throws FogbowException;

    public AttachmentInstance getInstance(String attachmentInstanceId, S cloudUser) throws FogbowException;
}
