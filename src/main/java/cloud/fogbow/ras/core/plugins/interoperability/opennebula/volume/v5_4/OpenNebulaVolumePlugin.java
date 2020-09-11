package cloud.fogbow.ras.core.plugins.interoperability.opennebula.volume.v5_4;

import java.util.UUID;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.util.BinaryUnit;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.sdk.v5_4.volume.model.CreateVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.sdk.v5_4.volume.model.VolumeImage;
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

public class OpenNebulaVolumePlugin implements VolumePlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaVolumePlugin.class);

	@VisibleForTesting
    static final String BLOCK_DISK_TYPE = "BLOCK";
	@VisibleForTesting
    static final String DATABLOCK_IMAGE_TYPE = "DATABLOCK";
	@VisibleForTesting
    static final String DEFAULT_DATASTORE_DEVICE_PREFIX = "vd";
	@VisibleForTesting
    static final String DRIVER_RAW = "raw";
	@VisibleForTesting
    static final String PERSISTENT_DISK_CONFIRMATION = "YES";

	@VisibleForTesting
    static final String DATASTORE_FREE_PATH_FORMAT = "//DATASTORE[%s]/FREE_MB";
	@VisibleForTesting
    static final String IMAGE_TYPE = "IMAGE";

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
		LOGGER.info(String.format(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());

		String volumeName = volumeOrder.getName();
		String name = volumeName == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + this.getRandomUUID() : volumeName;
		String imagePersistent = PERSISTENT_DISK_CONFIRMATION;
		String imageType = DATABLOCK_IMAGE_TYPE;
		String driver = DRIVER_RAW;
		String diskType = BLOCK_DISK_TYPE;
		String devicePrefix = DEFAULT_DATASTORE_DEVICE_PREFIX;
		long sizeInGB = volumeOrder.getVolumeSize();
		long sizeInMB = (long) BinaryUnit.gigabytes(sizeInGB).asMegabytes();

		CreateVolumeRequest request = new CreateVolumeRequest.Builder()
				.name(name)
				.size(sizeInMB)
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
		String instanceId = volumeOrder.getInstanceId();
		LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		Image image = this.doGetInstance(client, volumeOrder.getInstanceId());
		int imageSize = this.getImageSize(image.xpath(OpenNebulaConstants.SIZE));

		return new VolumeInstance(instanceId, image.stateString(), image.getName(), imageSize);
	}

	@Override
	public void deleteInstance(VolumeOrder volumeOrder, CloudUser cloudUser) throws FogbowException {
		String instanceId = volumeOrder.getInstanceId();
		LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));

		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		Image image = this.doGetInstance(client, instanceId);

		OneResponse response = image.delete();
		if (response.isError()) {
			String message = String.format(
					Messages.Log.ERROR_WHILE_REMOVING_VOLUME_IMAGE_S_S, instanceId, response.getMessage());
			LOGGER.error(message);
			throw new InternalServerErrorException(message);
		}
	}

	@VisibleForTesting
    String doRequestInstance(CreateVolumeRequest createVolumeRequest, Client client)
			throws UnacceptableOperationException, InternalServerErrorException, InvalidParameterException {
		VolumeImage volumeImage = createVolumeRequest.getVolumeImage();

		String template = volumeImage.marshalTemplate();
		Integer datastoreId = this.getDataStoreId(client, volumeImage.getSize());
		if (datastoreId == null) {
			throw new UnacceptableOperationException();
		}

		return OpenNebulaClientUtil.allocateImage(client, template, datastoreId);
	}

	@VisibleForTesting
    Image doGetInstance(Client client, String instanceId) throws InternalServerErrorException, InstanceNotFoundException {
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		Image image = imagePool.getById(Integer.parseInt(instanceId));
		if (image == null) {
			throw new InstanceNotFoundException();
		}

		return image;
	}

	@VisibleForTesting
    Integer getDataStoreId(Client client, long diskSize) throws InternalServerErrorException {
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

	@VisibleForTesting
    int getImageSize(String size) {
		int sizeInGB = 0;
		try {
			int sizeInMB = Integer.parseInt(size);
			sizeInGB = (int) BinaryUnit.megabytes(sizeInMB).asGigabytes();
		} catch (NumberFormatException e) {
			LOGGER.error(e.getMessage());
		}

		return sizeInGB;
	}

	@VisibleForTesting
    String getRandomUUID() {
		return UUID.randomUUID().toString();
	}
}
