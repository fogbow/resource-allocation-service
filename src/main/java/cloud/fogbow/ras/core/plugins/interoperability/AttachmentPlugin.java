package cloud.fogbow.ras.core.plugins.interoperability;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;

public interface AttachmentPlugin<T extends CloudUser> {

    public String requestInstance(AttachmentOrder attachmentOrder, T cloudUser) throws FogbowException;

    public void deleteInstance(String attachmentInstanceId, T cloudUser) throws FogbowException;

    public AttachmentInstance getInstance(String attachmentInstanceId, T cloudUser) throws FogbowException;
}
