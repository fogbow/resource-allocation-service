package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.template.Template;
import org.opennebula.client.template.TemplatePool;
import org.opennebula.client.vm.VirtualMachine;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.XmlUnmarshaller;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;

public class OpenNebulaComputePlugin implements ComputePlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaComputePlugin.class);
	
	private static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
	private static final String DEFAULT_GRAPHIC_ADDRESS = "0.0.0.0";
	private static final String DEFAULT_GRAPHIC_TYPE = "vnc";
	private static final String DEFAULT_VOLUME_TYPE = "fs";
	private static final String IMAGE_SIZE_PATH = "SIZE";
	private static final String NETWORK_CONFIRMATION_CONTEXT = "YES";
	private static final String NIC_IP_EXPRESSION = "//NIC/IP";
	private static final String TEMPLATE_CPU_PATH = "TEMPLATE/CPU";
	private static final String TEMPLATE_DISK_IMAGE_INDEX = "TEMPLATE/DISK[%s]/IMAGE";
	private static final String TEMPLATE_DISK_INDEX = "TEMPLATE/DISK[%s]";
	private static final String TEMPLATE_DISK_SIZE_INDEX = "TEMPLATE/DISK[%s]/SIZE";
	private static final String TEMPLATE_DISK_SIZE_PATH = "TEMPLATE/DISK/SIZE";
	private static final String TEMPLATE_MEMORY_PATH = "TEMPLATE/MEMORY";
	private static final String TEMPLATE_NAME_PATH = "TEMPLATE/NAME";
	private static final String USERDATA_ENCODING_CONTEXT = "base64";
	
	protected static final String FIELD_RESPONSE_LIMIT = "limit";
	protected static final String FIELD_RESPONSE_QUOTA = "quota";
	protected static final String RESPONSE_NOT_ENOUGH_FREE_MEMORY = "Not enough free memory";
	
	private TreeSet<HardwareRequirements> flavors;
	private LaunchCommandGenerator launchCommandGenerator;
	private Properties properties;
	private String endpoint;

	public OpenNebulaComputePlugin(String confFilePath) throws FatalErrorException {
		this.properties = PropertiesUtil.readProperties(confFilePath);
		this.endpoint = this.properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
		this.flavors = new TreeSet<HardwareRequirements>();
		this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
	}
	
	@Override
	public String requestInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());

		String encoding = USERDATA_ENCODING_CONTEXT;
		String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);
		String hasNetwork = NETWORK_CONFIRMATION_CONTEXT;
		String address = DEFAULT_GRAPHIC_ADDRESS;
		String graphicsType = DEFAULT_GRAPHIC_TYPE;
		String imageId = computeOrder.getImageId();
		String volumeType = DEFAULT_VOLUME_TYPE;
		List<String> networks = resolveNetworkIds(computeOrder);

		HardwareRequirements foundFlavor = findSmallestFlavor(computeOrder, cloudUser);
		String cpu = String.valueOf(foundFlavor.getCpu());
		String ram = String.valueOf(foundFlavor.getMemory());
		String disk = String.valueOf(foundFlavor.getDisk());

		CreateComputeRequest request = new CreateComputeRequest.Builder()
				.contextEncoding(encoding)
				.contextUserdata(userData)
				.contextNetwork(hasNetwork)
				.cpu(cpu)
				.graphicsListen(address)
				.graphicsType(graphicsType)
				.imageId(imageId)
				.volumeSize(disk)
				.volumeType(volumeType)
				.memory(ram)
				.networks(networks)
				.build();

		String template = request.getVirtualMachine().marshalTemplate();
		String instanceId = OpenNebulaClientUtil.allocateVirtualMachine(client, template);
		return instanceId;
	}

	@Override
	public ComputeInstance getInstance(String computeInstanceId, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, computeInstanceId, cloudUser.getToken()));
		if (this.flavors == null || this.flavors.isEmpty()) {
			updateHardwareRequirements(cloudUser);
		}
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeInstanceId);
		return getComputeInstance(virtualMachine);
	}

	@Override
	public void deleteInstance(String computeInstanceId, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, computeInstanceId, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeInstanceId);
		OneResponse response = virtualMachine.terminate();
		if (response.isError()) {
			LOGGER.error(
					String.format(Messages.Error.ERROR_WHILE_REMOVING_VM, computeInstanceId, response.getMessage()));
		}
	}

	protected List<String> resolveNetworkIds(ComputeOrder computeOrder) {
		List<String> requestedNetworkIds = new ArrayList<>();
		String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
		requestedNetworkIds.add(defaultNetworkId);
		if (!computeOrder.getNetworkIds().isEmpty()) {
			requestedNetworkIds.addAll(computeOrder.getNetworkIds());
		}
		computeOrder.setNetworkIds(requestedNetworkIds);
		return requestedNetworkIds;
	}
	
	protected HardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, CloudUser token)
			throws NoAvailableResourcesException, UnexpectedException {

		HardwareRequirements bestFlavor = getBestFlavor(computeOrder, token);
		if (bestFlavor == null) {
			throw new NoAvailableResourcesException();
		}
		return bestFlavor;
	}
	
	protected HardwareRequirements getBestFlavor(ComputeOrder computeOrder, CloudUser cloudUser)
			throws UnexpectedException {

		updateHardwareRequirements(cloudUser);
		for (HardwareRequirements hardwareRequirements : this.flavors) {
			if (hardwareRequirements.getCpu() >= computeOrder.getvCPU()
					&& hardwareRequirements.getMemory() >= computeOrder.getMemory()
					&& hardwareRequirements.getDisk() >= computeOrder.getDisk()) {
				return hardwareRequirements;
			}
		}
		return null;
	}

	protected void updateHardwareRequirements(CloudUser cloudUser) throws UnexpectedException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		Map<String, String> imageSizeMap = getImageSizes(client);
		List<HardwareRequirements> flavors = new ArrayList<>();
		List<HardwareRequirements> flavorsTemplate = new ArrayList<>();

		TemplatePool templatePool = OpenNebulaClientUtil.getTemplatePool(client);
		if (templatePool != null) {
			HardwareRequirements flavor;
			for (Template template : templatePool) {
				String id = template.getId();
				String name = template.getName();
				int cpu;
				try {
					cpu = Integer.parseInt(template.xpath(TEMPLATE_CPU_PATH));
				} catch (NumberFormatException e) {
					continue;
				}
				int memory = Integer.parseInt(template.xpath(TEMPLATE_MEMORY_PATH));
				int disk = 0;
				flavor = new HardwareRequirements(name, id, cpu, memory, disk);
				flavorsTemplate.add(flavor);
				if (containsFlavor(flavor, this.flavors)) {
					int size = loadImageSizeDisk(imageSizeMap, template);
					flavor = new HardwareRequirements(name, id, cpu, memory, size);
					flavors.add(flavor);
				}
			}
		}

		if (flavors != null) {
			this.flavors.addAll(flavors);
		}
		removeInvalidFlavors(flavorsTemplate);
	}

	protected int loadImageSizeDisk(Map<String, String> map, Template template) {
		int index = 1;
		int size = 0;
		while (true) {
			String imageDiskName = template.xpath(String.format(TEMPLATE_DISK_IMAGE_INDEX, index));
			String volatileDiskSize = template.xpath(String.format(TEMPLATE_DISK_SIZE_INDEX, index));
			if (volatileDiskSize != null && !volatileDiskSize.isEmpty()) {
				size += Integer.parseInt(volatileDiskSize);
			} else if (imageDiskName != null && !imageDiskName.isEmpty()){
				size += Integer.parseInt(map.get(imageDiskName));
			}
			index++;
			String templateDiskIndex = String.format(TEMPLATE_DISK_INDEX, index);
			if (template.xpath(templateDiskIndex) == null || template.xpath(templateDiskIndex).isEmpty()) {
				break;
			}
		}
		return size;
	}

	protected Map<String, String> getImageSizes(Client client) throws UnexpectedException {
		Map<String, String> imageSizeMap = new HashMap<String, String>();
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		for (Image image : imagePool) {
			imageSizeMap.put(image.getName(), image.xpath(IMAGE_SIZE_PATH));
		}
		return imageSizeMap;
	}

	protected boolean containsFlavor(HardwareRequirements flavor, Collection<HardwareRequirements> flavors) {
		List<HardwareRequirements> list = new ArrayList<>(flavors);
		for (HardwareRequirements item : list) {
			if (item.getName().equals(flavor.getName())) {
				return true;
			}
		}
		return false;
	}

	protected void removeInvalidFlavors(List<HardwareRequirements> flavors) {
		ArrayList<HardwareRequirements> copyFlavors = new ArrayList<>(this.flavors);
		for (HardwareRequirements flavor : copyFlavors) {
			if (!containsFlavor(flavor, flavors) && copyFlavors.size() != 0) {
				this.flavors.remove(flavor);
			}
		}
	}
	
	protected ComputeInstance getComputeInstance(VirtualMachine virtualMachine) {
		String xml = getTemplateResponse(virtualMachine);
		String id = virtualMachine.getId();
		String hostName = virtualMachine.xpath(TEMPLATE_NAME_PATH);
		int cpu = Integer.parseInt(virtualMachine.xpath(TEMPLATE_CPU_PATH));
		int memory = Integer.parseInt(virtualMachine.xpath(TEMPLATE_MEMORY_PATH));
		int disk = Integer.parseInt(virtualMachine.xpath(TEMPLATE_DISK_SIZE_PATH));

		String state = virtualMachine.lcmStateStr();
		InstanceState instanceState = OpenNebulaStateMapper.map(ResourceType.COMPUTE, state);
		
		XmlUnmarshaller xmlUnmarshaller = new XmlUnmarshaller(xml);
		List<String> ipAddresses = xmlUnmarshaller.getContextListOf(NIC_IP_EXPRESSION);

		LOGGER.info(String.format(Messages.Info.MOUNTING_INSTANCE, id));
		ComputeInstance computeInstance = new ComputeInstance(
				id, 
				instanceState, 
				hostName,
				cpu, 
				memory, 
				disk, 
				ipAddresses);
		
		return computeInstance;
	}

	protected String getTemplateResponse(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();
		return response.getMessage();
	}
	
	protected void setFlavors(TreeSet<HardwareRequirements> flavors) {
		this.flavors = flavors;
	}
	
	public void setLaunchCommandGenerator(LaunchCommandGenerator launchCommandGenerator) {
		this.launchCommandGenerator = launchCommandGenerator;
	}
	
}