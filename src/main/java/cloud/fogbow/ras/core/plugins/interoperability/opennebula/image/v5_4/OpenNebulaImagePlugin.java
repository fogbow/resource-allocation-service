package cloud.fogbow.ras.core.plugins.interoperability.opennebula.image.v5_4;

import java.util.*;

import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;

public class OpenNebulaImagePlugin implements ImagePlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaImagePlugin.class);

	@VisibleForTesting
    static final String FORMAT_IMAGE_TYPE_PATH = "//IMAGE[%s]/TYPE";
	@VisibleForTesting
    static final String IMAGE_SIZE_PATH = "/IMAGE/SIZE";
	@VisibleForTesting
    static final String OPERATIONAL_SYSTEM_IMAGE_TYPE = "0";
	@VisibleForTesting
	static final int NO_VALUE_FLAG = -1;

	private String endpoint;
	
	public OpenNebulaImagePlugin(String confFilePath) throws FatalErrorException {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
	}
	
	@Override
	public List<ImageSummary> getAllImages(CloudUser cloudUser) throws FogbowException {
		LOGGER.info(Messages.Log.RECEIVING_GET_ALL_IMAGES_REQUEST);
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		return getImageSummaryList(imagePool);
	}

	@Override
	public ImageInstance getImage(String imageId, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Log.RECEIVING_GET_IMAGE_REQUEST_S, imageId));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		Image image = OpenNebulaClientUtil.getImage(client, imageId);
		return buildImageInstance(image);
	}

	@VisibleForTesting
    List<ImageSummary> getImageSummaryList(ImagePool imagePool) {
		String type;
		int index = 1;
		List<ImageSummary> images = new ArrayList<>();
		for (Image image : imagePool) {
			type = image.xpath(String.format(FORMAT_IMAGE_TYPE_PATH, index));
			if (type != null && type.equals(OPERATIONAL_SYSTEM_IMAGE_TYPE)) {
				ImageSummary imageSummary = new ImageSummary(image.getId(), image.getName());
				images.add(imageSummary);
			}
			index++;
		}
		return images;
	}
	
	@VisibleForTesting
    ImageInstance buildImageInstance(Image image) {
		String id = image.getId();
		String name = image.getName();
		String imageSize = image.xpath(IMAGE_SIZE_PATH);
		int size = convertToInteger(imageSize);
		int minDisk = NO_VALUE_FLAG;
		int minRam = NO_VALUE_FLAG;
		int state = image.state();
		InstanceState instanceState = OpenNebulaStateMapper.map(ResourceType.IMAGE, state);
		String status = instanceState.getValue();
		return new ImageInstance(id, name, size, minDisk, minRam, status);
	}
	
	@VisibleForTesting
    int convertToInteger(String number) {
	    int size = 0;
		try {
			size =  Integer.parseInt(number);
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Log.ERROR_WHILE_CONVERTING_TO_INTEGER), e);
		}
		return size;
	}

}
