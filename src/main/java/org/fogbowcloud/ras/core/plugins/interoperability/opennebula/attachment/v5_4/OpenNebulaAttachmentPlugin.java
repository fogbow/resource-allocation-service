package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.attachment.v5_4;


import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.AttachmentPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.vm.VirtualMachine;

public class OpenNebulaAttachmentPlugin implements AttachmentPlugin<Token> {

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        String serverId = attachmentOrder.getSource();
        String volumeId = attachmentOrder.getTarget();

        OpenNebulaClientFactory factory = new OpenNebulaClientFactory();
        Client client = factory.createClient(localUserAttributes.getTokenValue());
        String diskTemplate = generateDiskTemplate(volumeId);
        OneResponse attachResponse = VirtualMachine.diskAttach(client, Integer.valueOf(serverId), diskTemplate);

        if(attachResponse.isError()){
            throw new UnexpectedException(attachResponse.getMessage());
        }


    }

    @Override
    public void deleteInstance(String attachmentInstanceId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {

    }

    @Override
    public AttachmentInstance getInstance(String attachmentInstanceId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        return null;
    }

    private String generateDiskTemplate(String volumeId) {
        return "DISK=[IMAGE_ID=" + volumeId + "]";
    }
}
