package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.attachment.v5_4;


import org.bouncycastle.crypto.engines.CamelliaLightEngine;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.AttachmentPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;

public class OpenNebulaAttachmentPlugin implements AttachmentPlugin<Token> {

    private final String SEPARATOR_ID= " ";

    private OpenNebulaClientFactory factory;

    public OpenNebulaAttachmentPlugin() {
        this.factory = new OpenNebulaClientFactory();
    }

    @Override
    public String requestInstance(AttachmentOrder attachmentOrder, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        String serverId = attachmentOrder.getComputeId();
        String volumeId = attachmentOrder.getVolumeId();

        Client client = this.factory.createClient(localUserAttributes.getTokenValue());
        String diskTemplate = generateDiskTemplate(volumeId);
        OneResponse attachResponse = VirtualMachine.diskAttach(client, Integer.valueOf(serverId), diskTemplate);

        if (attachResponse.isError()) {
            throw new UnexpectedException(attachResponse.getMessage());
        }

        return serverId + SEPARATOR_ID + volumeId;
    }

    @Override
    public void deleteInstance(String attachmentInstanceId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        String[] separatorInstanceId = attachmentInstanceId.split(SEPARATOR_ID);
        int serverId = Integer.parseInt(separatorInstanceId[0]);
        int volumeId = Integer.parseInt(separatorInstanceId[1]);

        Client client = factory.createClient(localUserAttributes.getTokenValue());

        OneResponse detachResponse = VirtualMachine.diskDetach(client, serverId, volumeId);

        if(detachResponse.isError()){
            throw new UnexpectedException(detachResponse.getMessage());
        }
    }

    @Override
    public AttachmentInstance getInstance(String attachmentInstanceId, Token localUserAttributes) throws FogbowRasException, UnexpectedException {
        String[] separatorInstanceId = attachmentInstanceId.split(SEPARATOR_ID);
        String serverId = separatorInstanceId[0];
        String volumeId = separatorInstanceId[1];

        Client client = factory.createClient(localUserAttributes.getTokenValue());

        ImagePool imagePool = this.factory.createImagePool(client);
        Image oneImage = imagePool.getById(Integer.parseInt(volumeId));

        InstanceState instanceState = OpenNebulaStateMapper.map(ResourceType.ATTACHMENT, oneImage.stateString());

        String attachmentDevice = oneImage.xpath(OpenNebulaXmlTagsConstants.VirtualMachine.DEFAULT_DEVICE_PREFIX);

        return new AttachmentInstance(attachmentInstanceId, instanceState, serverId, volumeId, attachmentDevice);
    }

    private String generateDiskTemplate(String volumeId) {
        return "DISK=[IMAGE_ID=" + volumeId + "]";
    }
}
