package cloud.fogbow.ras.core.plugins.interoperability.opennebula.image.v5_4;

import java.util.*;

import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
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

	protected static final String FORMAT_IMAGE_TYPE_PATH = "//IMAGE[%s]/TYPE";
	protected static final String IMAGE_SIZE_PATH = "/IMAGE/SIZE";
	protected static final String OPERATIONAL_SYSTEM_IMAGE_TYPE = "0";

	private String endpoint;
	
	public OpenNebulaImagePlugin(String confFilePath) throws FatalErrorException {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.endpoint = properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
	}
	
	@Override
	public List<ImageSummary> getAllImages(CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		return getImageSummaryList(imagePool);
	}

	@Override
	public ImageInstance getImage(String imageId, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		org.opennebula.client.image.Image image = OpenNebulaClientUtil.getImage(client, imageId);
		return mount(image);
	}

	protected List<ImageSummary> getImageSummaryList(ImagePool imagePool) {
		String type;
		int index = 1;
		List<ImageSummary> images = new ArrayList<>();
		for (org.opennebula.client.image.Image image : imagePool) {
			type = image.xpath(String.format(FORMAT_IMAGE_TYPE_PATH, index));
			if (type != null && type.equals(OPERATIONAL_SYSTEM_IMAGE_TYPE)) {
				ImageSummary imageSummary = new ImageSummary(image.getId(), image.getName());
				images.add(imageSummary);
			}
			index++;
		}
		return images;
	}
	
	protected ImageInstance mount(org.opennebula.client.image.Image image) {
		String id = image.getId();
		String name = image.getName();
		String imageSize = image.xpath(IMAGE_SIZE_PATH);
		int size = convertToInteger(imageSize);
		int minDisk = -1;
		int minRam = -1;
		int state = image.state();
		InstanceState instanceState = OpenNebulaStateMapper.map(ResourceType.IMAGE, state);
		String status = instanceState.getValue();
		return new ImageInstance(id, name, size, minDisk, minRam, status);
	}
	
	protected int convertToInteger(String number) {
	    int size = 0;
		try {
			size =  Integer.parseInt(number);
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_TO_INTEGER), e);
		}
		return size;
	}

}
