package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import java.util.UUID;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.constants.SystemConstants;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.datastore.Datastore;
import org.opennebula.client.datastore.DatastorePool;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;

import cloud.fogbow.common.constants.OpenNebulaConstants;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;

import javax.annotation.Nullable;

public class OpenNebulaVolumePlugin implements VolumePlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaVolumePlugin.class);

	protected static final String BLOCK_DISK_TYPE = "BLOCK";
	protected static final String DATABLOCK_IMAGE_TYPE = "DATABLOCK";
	protected static final String DEFAULT_DATASTORE_DEVICE_PREFIX = "vd";
	protected static final String DRIVER_RAW = "raw";
	protected static final String PERSISTENT_DISK_CONFIRMATION = "YES";
	protected static final int CONVERT_DISK = 1024;

	protected static final String DATASTORE_FREE_PATH_FORMAT = "//DATASTORE[%s]/FREE_MB";
	protected static final String IMAGE_TYPE = "IMAGE";

	private String endpoint;

	public OpenNebulaVolumePlugin(String confFilePath) throws FatalErrorException {
		this.endpoint = PropertiesUtil.readProperties(confFilePath)
				.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
	}

	@Override
	public boolean isReady(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.VOLUME, cloudState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.VOLUME, cloudState).equals(InstanceState.FAILED);
	}

	@Override
	public String requestInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());

		String volumeName = volumeOrder.getName();
		String name = volumeName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + this.getRandomUUID() : volumeName;
		String imagePersistent = PERSISTENT_DISK_CONFIRMATION;
		String imageType = DATABLOCK_IMAGE_TYPE;
		String driver = DRIVER_RAW;
		String diskType = BLOCK_DISK_TYPE;
		String devicePrefix = DEFAULT_DATASTORE_DEVICE_PREFIX;
		long size = volumeOrder.getVolumeSize() * CONVERT_DISK;

		CreateVolumeRequest request = new CreateVolumeRequest.Builder()
				.name(name)
				.size(size)
				.imagePersistent(imagePersistent)
				.imageType(imageType)
				.driver(driver)
				.diskType(diskType)
				.devicePrefix(devicePrefix)
				.build();

		String instanceId = this.doRequestInstance(request, client);
		this.setOrderAllocation(volumeOrder, volumeOrder.getVolumeSize());
		return instanceId;
	}

	@VisibleForTesting
	void setOrderAllocation(VolumeOrder order, long size) {
		synchronized (order) {
			VolumeAllocation volumeAllocation = new VolumeAllocation((int) size);
			order.setActualAllocation(volumeAllocation);
		}
	}

	@Override
	public VolumeInstance getInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		Image image = this.doGetInstance(client, volumeOrder.getInstanceId());
		int imageSize = this.getImageSize(image.xpath(OpenNebulaConstants.SIZE));

		return new VolumeInstance(volumeOrder.getInstanceId(), image.stateString(), image.getName(), imageSize);
	}

	@Override
	public void deleteInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		Image image = this.doGetInstance(client, volumeOrder.getInstanceId());

		OneResponse response = image.delete();
		if (response.isError()) {
			LOGGER.error(String.format(
					Messages.Log.ERROR_WHILE_REMOVING_VOLUME_IMAGE_S_S, volumeOrder.getInstanceId(), response.getMessage()));
			throw new InternalServerErrorException(String.format(
					Messages.Exception.ERROR_WHILE_REMOVING_VOLUME_IMAGE_S_S, volumeOrder.getInstanceId(), response.getMessage()));
		}
	}

	protected String doRequestInstance(CreateVolumeRequest createVolumeRequest, Client client)
			throws UnacceptableOperationException, InternalServerErrorException, InvalidParameterException {
		VolumeImage volumeImage = createVolumeRequest.getVolumeImage();

		String template = volumeImage.marshalTemplate();
		Integer datastoreId = this.getDataStoreId(client, volumeImage.getSize());
		if (datastoreId == null) {
			throw new UnacceptableOperationException();
		}

		return OpenNebulaClientUtil.allocateImage(client, template, datastoreId);
	}

	protected Image doGetInstance(Client client, String instanceId) throws InternalServerErrorException, InstanceNotFoundException {
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(instanceId));
		if (image == null) {
			throw new InstanceNotFoundException();
		}

		return image;
	}

	protected Integer getDataStoreId(Client client, long diskSize) throws InternalServerErrorException {
		DatastorePool datastorePool = OpenNebulaClientUtil.getDatastorePool(client);

		int index = 1;
		for (Datastore datastore : datastorePool) {
			Long freeDiskSize = null;
			if (datastore.typeStr().equals(IMAGE_TYPE)) {
				try {
					freeDiskSize = Long.valueOf(datastore.xpath(String.format(DATASTORE_FREE_PATH_FORMAT, index)));
				} catch(NumberFormatException e) {
					LOGGER.error(String.format(Messages.Log.INVALID_NUMBER_FORMAT, e.getMessage()));
					continue;
				}
			}

			if (freeDiskSize != null && freeDiskSize >= diskSize) {
				return datastore.id();
			}

			index++;
		}

		return null;
	}

	protected int getImageSize(String size) {
		int imageSize = 0;
		try {
			imageSize = Integer.parseInt(size) / CONVERT_DISK;
		} catch (NumberFormatException e) {
			LOGGER.error(e.getMessage());
		}

		return imageSize;
	}

	protected String getRandomUUID() {
		return UUID.randomUUID().toString();
	}
}
