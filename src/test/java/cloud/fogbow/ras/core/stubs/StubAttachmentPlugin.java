package cloud.fogbow.ras.core.stubs;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;

/**
 * This class is a stub for the AttachmentPlugin interface used for tests only.
 * Should not have a proper implementation.
 */
public class StubAttachmentPlugin implements AttachmentPlugin<CloudUser> {

    public StubAttachmentPlugin(String confFilePath) {
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) {
        return null;
    }

    @Override
    public void deleteInstance(String attachmentInstanceId, CloudUser cloudUser) {
    }

    @Override
    public AttachmentInstance getInstance(String attachmentInstanceId, CloudUser cloudUser) {
        return null;
    }
}
