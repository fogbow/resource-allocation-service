package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import java.util.Properties;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.sdk.v5_4.attachment.model.CreateAttachmentRequest;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;

public class OpenNebulaAttachmentPlugin implements AttachmentPlugin<CloudUser> {

    private static final Logger LOGGER = Logger.getLogger(OpenNebulaAttachmentPlugin.class);

    @VisibleForTesting
    static final String DEFAULT_TARGET = "hdb";
    @VisibleForTesting
    static final String DEVICE_PATH_SEPARATOR = "/";
    @VisibleForTesting
    static final String DISK_ID_PATH_FORMAT = "TEMPLATE/DISK[IMAGE_ID=%s]/DISK_ID";
    @VisibleForTesting
    static final String TARGET_PATH_FORMAT = "TEMPLATE/DISK[IMAGE_ID=%s]/TARGET";

    @VisibleForTesting
    static final int DEVICE_PATH_LENGTH = 3;
    @VisibleForTesting
    static final int TARGET_INDEX = 2;

    private String endpoint;

    public OpenNebulaAttachmentPlugin(String confFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
    }

    @Override
    public boolean isReady(String cloudState) {
        return OpenNebulaStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(String cloudState) {
        return OpenNebulaStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(AttachmentOrder order, CloudUser cloudUser) throws FogbowException {
        LOGGER.info(String.format(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER));
        Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
        String virtualMachineId = order.getComputeId();
        String imageId = order.getVolumeId();
        String target = normalizeDeviceTarget(order);

        CreateAttachmentRequest request = new CreateAttachmentRequest.Builder()
                .imageId(imageId)
                .target(target)
                .build();

        String template = request.getAttachDisk().marshalTemplate();
        doRequestInstance(client, virtualMachineId, imageId, template);
        return getImageDiskId(client, virtualMachineId, imageId);
    }

    @Override
    public void deleteInstance(AttachmentOrder order, CloudUser cloudUser) throws FogbowException {
        if (order == null) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }

        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));

        Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
        String computeId = order.getComputeId();
        doDeleteInstance(client, computeId, instanceId);
    }

    @Override
    public AttachmentInstance getInstance(AttachmentOrder order, CloudUser cloudUser)
            throws FogbowException {
        if (order == null) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }

        String instanceId = order.getInstanceId();
        LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));

        Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
        String computeId = order.getComputeId();
        String volumeId = order.getVolumeId();
        return doGetInstance(client, instanceId, computeId, volumeId);
    }

    @VisibleForTesting
    String getImageDiskId(
            Client client, 
            String virtualMachineId, 
            String imageId) throws FogbowException {

        VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineId);
        String imageIdPath = String.format(DISK_ID_PATH_FORMAT, imageId);
        String diskId = virtualMachine.xpath(imageIdPath);
        return diskId;
    }

    @VisibleForTesting
    void doRequestInstance(
            Client client, 
            String virtualMachineId, 
            String imageId,
            String template) throws FogbowException {

        VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineId);
        OneResponse response = virtualMachine.diskAttach(template);
        if (response.isError()) {
            String message = String.format(Messages.Log.ERROR_WHILE_ATTACHING_VOLUME_S_WITH_RESPONSE_S, imageId, response.getMessage());
            LOGGER.error(message);
            throw new InvalidParameterException(message);
        }
    }

    @VisibleForTesting
    String normalizeDeviceTarget(AttachmentOrder order) {
        // Expecting a default target device such as "/dev/sdb".
        String[] splitPath = order.getDevice().split(DEVICE_PATH_SEPARATOR);
        String target = splitPath.length == DEVICE_PATH_LENGTH
                ? splitPath[TARGET_INDEX]
                : DEFAULT_TARGET;

        return target;
    }

    @VisibleForTesting
    void doDeleteInstance(
            Client client, 
            String computeId, 
            String instanceId) throws FogbowException {

        VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeId);
        int diskId = Integer.parseInt(instanceId);
        OneResponse response = virtualMachine.diskDetach(diskId);
        if (response.isError()) {
            String message = String.format(Messages.Log.ERROR_WHILE_DETACHING_VOLUME_S, diskId, response.getMessage());
            LOGGER.error(message);
            throw new InternalServerErrorException(message);
        }
    }

    @VisibleForTesting
    AttachmentInstance doGetInstance(
            Client client, 
            String instanceId, 
            String computeId,
            String volumeId) throws FogbowException {

        VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeId);
        String deviceTargetPath = String.format(TARGET_PATH_FORMAT, volumeId);
        String device = virtualMachine.xpath(deviceTargetPath);
        String state = getImageState(client, volumeId);

        return new AttachmentInstance(instanceId, state, computeId, volumeId, device);
    }

    @VisibleForTesting
    String getImageState(Client client, String volumeId) throws FogbowException {
        ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
        Image image = imagePool.getById(Integer.parseInt(volumeId));
        if (image == null)
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);

        return image.shortStateStr();
    }

}
