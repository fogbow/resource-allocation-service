package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.attachment;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud.EmulatedCloudUtils;
import org.apache.log4j.Logger;

import java.util.Properties;

public class EmulatedCloudAttachmentPlugin implements AttachmentPlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(EmulatedCloudAttachmentPlugin.class);

    private Properties properties;

    public EmulatedCloudAttachmentPlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public void deleteInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
        String attachmentId = attachmentOrder.getId();
        String attachmentPath = EmulatedCloudUtils.getResourcePath(this.properties, attachmentId);

        EmulatedCloudUtils.deleteFile(attachmentPath);
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder attachmentOrder, CloudUser cloudUser) throws FogbowException {
        return null;
    }

    @Override
    public boolean isReady(String instanceState) {
        return true;
    }

    @Override
    public boolean hasFailed(String instanceState) {
        return false;
    }
}
