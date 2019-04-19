package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import cloud.fogbow.ras.constants.SystemConstants;
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
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaConfigurationPropertyKeys;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.XmlUnmarshaller;
import cloud.fogbow.ras.core.plugins.interoperability.util.DefaultLaunchCommandGenerator;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;

public class OpenNebulaComputePlugin implements ComputePlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaComputePlugin.class);
	
	private static final String DEFAULT_ARCHITECTURE = "x86_64";
	private static final String DEFAULT_DISK_FORMAT = "ext3";
	private static final String DEFAULT_DISK_TYPE = "fs";
	private static final String DEFAULT_GRAPHIC_ADDRESS = "0.0.0.0";
	private static final String DEFAULT_GRAPHIC_TYPE = "vnc";
	protected static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
	private static final String NETWORK_CONFIRMATION_CONTEXT = "YES";
	private static final String NIC_IP_EXPRESSION = "//NIC/IP";
	private static final String USERDATA_ENCODING_CONTEXT = "base64";
	
	protected static final boolean SHUTS_DOWN_HARD = true;

	protected static final String IMAGE_SIZE_PATH = "SIZE";
	protected static final String TEMPLATE_CPU_PATH = "TEMPLATE/CPU";
	protected static final String TEMPLATE_DISK_SIZE_PATH = "TEMPLATE/DISK/SIZE";
	protected static final String TEMPLATE_IMAGE_ID_PATH = "TEMPLATE/DISK/IMAGE_ID";
	protected static final String TEMPLATE_MEMORY_PATH = "TEMPLATE/MEMORY";

	private String endpoint;
	private TreeSet<HardwareRequirements> flavors;
	private LaunchCommandGenerator launchCommandGenerator;
	private Properties properties;

	public OpenNebulaComputePlugin(String confFilePath) throws FatalErrorException {
		this.properties = PropertiesUtil.readProperties(confFilePath);
		this.endpoint = this.properties.getProperty(OpenNebulaConfigurationPropertyKeys.OPENNEBULA_RPC_ENDPOINT_KEY);
		this.flavors = new TreeSet<HardwareRequirements>();
		this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
	}

	@Override
	public boolean isReady(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.READY);
	}

	@Override
	public boolean hasFailed(String cloudState) {
		return OpenNebulaStateMapper.map(ResourceType.COMPUTE, cloudState).equals(InstanceState.FAILED);
	}

	@Override
	public String requestInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());

		String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);
		String encoding = USERDATA_ENCODING_CONTEXT;
		String hasNetwork = NETWORK_CONFIRMATION_CONTEXT;
		String graphicsAddress = DEFAULT_GRAPHIC_ADDRESS;
		String graphicsType = DEFAULT_GRAPHIC_TYPE;
		String architecture = DEFAULT_ARCHITECTURE;

		List<String> networks = new ArrayList<>();
		String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
		networks.add(defaultNetworkId);
		List<String> userDefinedNetworks = computeOrder.getNetworkIds();
		if (!userDefinedNetworks.isEmpty()) {
			networks.addAll(userDefinedNetworks);
		}

		HardwareRequirements foundFlavor = findSmallestFlavor(computeOrder, cloudUser);
		String cpu = String.valueOf(foundFlavor.getCpu());
		String memory = String.valueOf(foundFlavor.getMemory());
		
		int disk = foundFlavor.getDisk();
		String diskSize = null;
		String diskType = null;
		String diskFormat = null;
		String diskImageId = null;
		if (computeOrder.getDisk() > 0 && computeOrder.getDisk() < disk) {
			diskSize = String.valueOf(computeOrder.getDisk());
			diskType = DEFAULT_DISK_TYPE;
			diskFormat = DEFAULT_DISK_FORMAT;
		} else {
			diskImageId = computeOrder.getImageId();
		}
		
		CreateComputeRequest request = new CreateComputeRequest.Builder()
				.contextEncoding(encoding)
				.contextUserdata(userData)
				.contextNetwork(hasNetwork)
				.cpu(cpu)
				.graphicsAddress(graphicsAddress)
				.graphicsType(graphicsType)
				.diskImageId(diskImageId)
				.diskType(diskType)
				.diskSize(diskSize)
				.diskFormat(diskFormat)
				.memory(memory)
				.networks(networks)
				.architecture(architecture)
				.build();
		
		String template = request.getVirtualMachine().marshalTemplate();
		String instanceId = OpenNebulaClientUtil.allocateVirtualMachine(client, template);
		return instanceId;
	}

	@Override
	public ComputeInstance getInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, computeOrder.getInstanceId(), cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeOrder.getInstanceId());
		return getComputeInstance(virtualMachine);
	}

	@Override
	public void deleteInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, computeOrder.getInstanceId(), cloudUser.getToken()));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeOrder.getInstanceId());
		OneResponse response = virtualMachine.terminate(SHUTS_DOWN_HARD);
		if (response.isError()) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_VM, computeOrder.getInstanceId(),
					response.getMessage()));
		}
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
		for (HardwareRequirements hardwareRequirements : getFlavors()) {
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
		Map<String, String> imagesSizeMap = getImagesSize(client);
		List<HardwareRequirements> flavorsTemplate = new ArrayList<>();

		TemplatePool templatePool = OpenNebulaClientUtil.getTemplatePool(client);
		if (templatePool != null) {
			HardwareRequirements flavor;
			for (Template template : templatePool) {
				String id = template.getId();
				String name = template.getName();
				int cpu = convertToInteger(template.xpath(TEMPLATE_CPU_PATH));
				int memory = convertToInteger(template.xpath(TEMPLATE_MEMORY_PATH));
				String imageId = template.xpath(TEMPLATE_IMAGE_ID_PATH);
				int disk = getDiskSizeFromImages(imagesSizeMap, imageId);
				if (cpu != 0 && memory != 0 && disk != 0) {
					flavor = new HardwareRequirements(name, id, cpu, memory, disk);
					if (!containsFlavor(flavor, getFlavors())) {
						flavorsTemplate.add(flavor);
					}
				}
			}
		}

		if (!flavorsTemplate.isEmpty()) {
			this.flavors.addAll(flavorsTemplate);
		}
	}
	
	protected int getDiskSizeFromImages(Map<String, String> imageSizeMap, String imageId) {
		if (imageSizeMap != null && imageId != null) {
			String diskSize = imageSizeMap.get(imageId);
			return convertToInteger(diskSize);
		} else {
			LOGGER.error(Messages.Error.ERROR_WHILE_GETTING_DISK_SIZE);
			return 0;
		}
	}

	protected int convertToInteger(String number) {
		try {
			return Integer.parseInt(number);
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_TO_INTEGER), e);
			return 0;
		}
	}

	protected Map<String, String> getImagesSize(Client client) throws UnexpectedException {
		Map<String, String> imagesSizeMap = new HashMap<String, String>();
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		for (Image image : imagePool) {
			String imageSize = image.xpath(IMAGE_SIZE_PATH);
			imagesSizeMap.put(image.getId(), imageSize);
		}
		return imagesSizeMap;
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

	protected ComputeInstance getComputeInstance(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();
		String xml = response.getMessage();
		String id = virtualMachine.getId();
		String name = virtualMachine.getName();
		int cpu = Integer.parseInt(virtualMachine.xpath(TEMPLATE_CPU_PATH));
		int memory = Integer.parseInt(virtualMachine.xpath(TEMPLATE_MEMORY_PATH));
		int disk = Integer.parseInt(virtualMachine.xpath(TEMPLATE_DISK_SIZE_PATH));

		String state = virtualMachine.lcmStateStr();
		XmlUnmarshaller xmlUnmarshaller = new XmlUnmarshaller(xml);
		List<String> ipAddresses = xmlUnmarshaller.getContextListOf(NIC_IP_EXPRESSION);

		LOGGER.info(String.format(Messages.Info.MOUNTING_INSTANCE, id));
		ComputeInstance computeInstance = new ComputeInstance(id, state, name, cpu, memory, disk, ipAddresses);
		// The default network is always included in the order by the OpenNebula plugin, thus it should be added
		// in the map of networks in the ComputeInstance by the plugin. The remaining networks passed by the user
		// are appended by the LocalCloudConnector.
		String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
		Map<String, String> computeNetworks = new HashMap<>();
		computeNetworks.put(defaultNetworkId, SystemConstants.DEFAULT_NETWORK_NAME);
		computeInstance.setNetworks(computeNetworks);
		return computeInstance;
	}

	protected TreeSet<HardwareRequirements> getFlavors() {
        synchronized (this.flavors) {
            return this.flavors;
        }
    }

    protected void setFlavors(TreeSet<HardwareRequirements> flavors) {
        synchronized (this.flavors) {
            this.flavors = flavors;
        }
    }
	
	public void setLaunchCommandGenerator(LaunchCommandGenerator launchCommandGenerator) {
		this.launchCommandGenerator = launchCommandGenerator;
	}
	
}