package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import java.util.Properties;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;

import com.google.common.annotations.VisibleForTesting;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
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

    protected static final String DEFAULT_TARGET = "hdb";
    protected static final String DEVICE_PATH_SEPARATOR = "/";
    protected static final String IMAGE_ID_PATH_FORMAT = "TEMPLATE/DISK[IMAGE_ID=%s]/DISK_ID";
    protected static final String TARGET_PATH_FORMAT = "TEMPLATE/DISK[IMAGE_ID=%s]/TARGET";

    protected static final int DEVICE_PATH_LENGTH = 3;
    protected static final int TARGET_INDEX = 2;

    private String endpoint;

    public OpenNebulaAttachmentPlugin(@NotBlank String confFilePath) throws FatalErrorException {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
    }

    @Override
    public boolean isReady(@NotBlank String cloudState) {
        return OpenNebulaStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.READY);
    }

    @Override
    public boolean hasFailed(@NotBlank String cloudState) {
        return OpenNebulaStateMapper.map(ResourceType.ATTACHMENT, cloudState).equals(InstanceState.FAILED);
    }

    @Override
    public String requestInstance(@NotNull AttachmentOrder order, @NotNull CloudUser cloudUser) throws FogbowException {
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
    public void deleteInstance(@NotNull AttachmentOrder order, @NotNull CloudUser cloudUser) throws FogbowException {
        if (order == null) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
        Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
        String instanceId = order.getInstanceId();
        String computeId = order.getComputeId();
        doDeleteInstance(client, computeId, instanceId);
    }

    @Override
    public AttachmentInstance getInstance(@NotNull AttachmentOrder order, @NotNull CloudUser cloudUser)
            throws FogbowException {

        if (order == null) {
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);
        }
        Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
        String instanceId = order.getInstanceId();
        String computeId = order.getComputeId();
        String volumeId = order.getVolumeId();
        return doGetInstance(client, instanceId, computeId, volumeId);
    }

    @VisibleForTesting
    String getImageDiskId(
            @NotNull Client client, 
            @NotBlank String virtualMachineId, 
            @NotBlank String imageId) throws FogbowException {

        VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineId);
        String imageIdPath = String.format(IMAGE_ID_PATH_FORMAT, imageId);
        String diskId = virtualMachine.xpath(imageIdPath);
        return diskId;
    }

    @VisibleForTesting
    void doRequestInstance(
            @NotNull Client client, 
            @NotBlank String virtualMachineId, 
            @NotBlank String imageId,
            @NotBlank String template) throws FogbowException {

        VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, virtualMachineId);
        OneResponse response = virtualMachine.diskAttach(template);
        if (response.isError()) {
            String message = String.format(Messages.Error.ERROR_WHILE_ATTACHING_VOLUME, imageId, response.getMessage());
            LOGGER.error(message);
            throw new InvalidParameterException(message);
        }
    }

    @VisibleForTesting
    String normalizeDeviceTarget(@NotNull AttachmentOrder order) {
        // Expecting a default target device such as "/dev/sdb".
        String[] pathSplited = order.getDevice().split(DEVICE_PATH_SEPARATOR);
        String target = pathSplited.length == DEVICE_PATH_LENGTH 
                ? pathSplited[TARGET_INDEX]
                : DEFAULT_TARGET;

        return target;
    }

    @VisibleForTesting
    void doDeleteInstance(
            @NotNull Client client, 
            @NotBlank String computeId, 
            @NotBlank String instanceId) throws FogbowException {

        VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeId);
        int diskId = Integer.parseInt(instanceId);
        OneResponse response = virtualMachine.diskDetach(diskId);
        if (response.isError()) {
            String message = String.format(Messages.Error.ERROR_WHILE_DETACHING_VOLUME, diskId, response.getMessage());
            LOGGER.error(message);
            throw new UnexpectedException(message);
        }
    }

    @VisibleForTesting
    AttachmentInstance doGetInstance(
            @NotNull Client client, 
            @NotBlank String instanceId, 
            @NotBlank String computeId,
            @NotBlank String volumeId) throws FogbowException {

        VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeId);
        String deviceTargetPath = String.format(TARGET_PATH_FORMAT, volumeId);
        String device = virtualMachine.xpath(deviceTargetPath);
        String state = getImageState(client, volumeId);

        return new AttachmentInstance(instanceId, state, computeId, volumeId, device);
    }

    @VisibleForTesting
    String getImageState(@NotNull Client client, @NotBlank String volumeId) throws FogbowException {
        ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
        Image image = imagePool.getById(Integer.parseInt(volumeId));
        if (image == null)
            throw new InstanceNotFoundException(Messages.Exception.INSTANCE_NOT_FOUND);

        return image.shortStateStr();
    }

}
