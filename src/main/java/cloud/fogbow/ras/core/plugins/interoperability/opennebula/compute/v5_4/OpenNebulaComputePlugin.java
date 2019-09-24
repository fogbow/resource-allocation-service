package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.*;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.ras.api.http.response.NetworkSummary;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.*;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.template.Template;
import org.opennebula.client.template.TemplatePool;
import org.opennebula.client.vm.VirtualMachine;

import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;

import javax.annotation.Nullable;

public class OpenNebulaComputePlugin implements ComputePlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaComputePlugin.class);

	private static final int MB_CONVERT = 1024;
	protected static final int DEFAULT_NUMBER_OF_INSTANCES = 1;

	protected static final String DEFAULT_ARCHITECTURE = "x86_64";
	protected static final String DEFAULT_GRAPHIC_ADDRESS = "0.0.0.0";
	protected static final String DEFAULT_GRAPHIC_TYPE = "vnc";
	protected static final String NETWORK_CONFIRMATION_CONTEXT = "YES";
	protected static final String NIC_IP_EXPRESSION = "//NIC/IP";

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
		this.flavors = new TreeSet<>();
		this.launchCommandGenerator = new OpenNebulaLaunchCommandGenerator();
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
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		CreateComputeRequest request = this.getCreateComputeRequest(client, computeOrder);
		VirtualMachineTemplate virtualMachine = request.getVirtualMachine();

		synchronized (computeOrder) {
			ComputeAllocation actualAllocation = new ComputeAllocation(
					Integer.parseInt(virtualMachine.getCpu()),
					Integer.parseInt(virtualMachine.getMemory()),
					DEFAULT_NUMBER_OF_INSTANCES,
					Integer.parseInt(virtualMachine.getDisk().getSize()));
			computeOrder.setActualAllocation(actualAllocation);
		}

		return this.doRequestInstance(client, request);
	}

	@Override
	public ComputeInstance getInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeOrder.getInstanceId());
		return this.doGetInstance(virtualMachine);
	}

	@Override
	public void deleteInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeOrder.getInstanceId());
		OneResponse response = virtualMachine.terminate(SHUTS_DOWN_HARD);
		if (response.isError()) {
		    String message = String.format(Messages.Error.ERROR_WHILE_REMOVING_VM, computeOrder.getInstanceId(),
					response.getMessage());
		    throw new UnexpectedException(message);
		}
	}

	protected String doRequestInstance(Client client, CreateComputeRequest request)
			throws InvalidParameterException, NoAvailableResourcesException, QuotaExceededException {

		String template = request.getVirtualMachine().marshalTemplate();
		String instanceId = OpenNebulaClientUtil.allocateVirtualMachine(client, template);

		return instanceId;
	}

	protected ComputeInstance doGetInstance(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();

		String id = virtualMachine.getId();
		String name = virtualMachine.getName();
		String state = virtualMachine.lcmStateStr();

		int cpu = Integer.parseInt(virtualMachine.xpath(TEMPLATE_CPU_PATH));
		int memory = Integer.parseInt(virtualMachine.xpath(TEMPLATE_MEMORY_PATH));
		int disk = Integer.parseInt(virtualMachine.xpath(TEMPLATE_DISK_SIZE_PATH)) / MB_CONVERT;

		String xml = response.getMessage();
		XmlUnmarshaller xmlUnmarshaller = new XmlUnmarshaller(xml);
		List<String> ipAddresses = xmlUnmarshaller.getContextListOf(NIC_IP_EXPRESSION);

		ComputeInstance computeInstance = new ComputeInstance(id, state, name, cpu, memory, disk, ipAddresses);
		this.setComputeInstanceNetworks(computeInstance);

		return computeInstance;
	}

	protected CreateComputeRequest getCreateComputeRequest(Client client, ComputeOrder computeOrder)
			throws UnexpectedException, NoAvailableResourcesException {
		String userName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.SSH_COMMON_USER_KEY,
				ConfigurationPropertyDefaults.SSH_COMMON_USER);

		String hasNetwork = NETWORK_CONFIRMATION_CONTEXT;
		String graphicsAddress = DEFAULT_GRAPHIC_ADDRESS;
		String graphicsType = DEFAULT_GRAPHIC_TYPE;
		String architecture = DEFAULT_ARCHITECTURE;

		String name = computeOrder.getName();
		String publicKey = computeOrder.getPublicKey();
		String imageId = computeOrder.getImageId();

		List<String> networks = this.getNetworkIds(computeOrder.getNetworkIds());
		String startScriptBase64 = this.launchCommandGenerator.createLaunchCommand(computeOrder);

		HardwareRequirements foundFlavor = this.findSmallestFlavor(client, computeOrder);
		String cpu = String.valueOf(foundFlavor.getCpu());
		String memory = String.valueOf(foundFlavor.getMemory());
		String disk = String.valueOf(foundFlavor.getDisk());

		return this.createComputeRequest(name, hasNetwork, publicKey, userName, startScriptBase64,
				cpu, graphicsAddress, graphicsType, imageId, disk, memory, networks, architecture);
	}

	protected List<String> getNetworkIds(List<String> networkIds) {
		String defaultNetworkId = this.properties.getProperty(OpenNebulaConfigurationPropertyKeys.DEFAULT_NETWORK_ID_KEY);
		List<String>  networks = new ArrayList<>();
		networks.add(defaultNetworkId);
		if (!networkIds.isEmpty()) {
			networks.addAll(networkIds);
		}

		return networks;
	}

	protected CreateComputeRequest createComputeRequest(String name, String hasNetwork, String publicKey, String userName,
			String startScriptBase64, String cpu, String graphicsAddress, String graphicsType, String imageId, String disk,
			String memory, List<String> networks, String architecture) {

		CreateComputeRequest request = new CreateComputeRequest.Builder()
				.name(name)
				.contextNetwork(hasNetwork)
				.publicKey(publicKey)
				.userName(userName)
				.startScriptBase64(startScriptBase64)
				.cpu(cpu)
				.graphicsAddress(graphicsAddress)
				.graphicsType(graphicsType)
				.imageId(imageId)
				.diskSize(disk)
				.memory(memory)
				.networks(networks)
				.architecture(architecture)
				.build();

		return request;
	}

	protected HardwareRequirements findSmallestFlavor(Client client, ComputeOrder computeOrder)
			throws NoAvailableResourcesException, UnexpectedException {

		HardwareRequirements bestFlavor = this.getBestFlavor(client, computeOrder);
		if (bestFlavor == null) {
			throw new NoAvailableResourcesException();
		}
		return bestFlavor;
	}

	@Nullable
	protected HardwareRequirements getBestFlavor(Client client, ComputeOrder computeOrder) throws UnexpectedException {
		this.updateHardwareRequirements(client);

		for (HardwareRequirements hardwareRequirements : this.getFlavors()) {
			if (hardwareRequirements.getCpu() >= computeOrder.getvCPU()
					&& hardwareRequirements.getMemory() >= computeOrder.getMemory()
					&& hardwareRequirements.getDisk() >= this.convertDiskSizeToMb(computeOrder.getDisk())) {
				return hardwareRequirements;
			}
		}
		return null;
	}

	protected void updateHardwareRequirements(Client client) throws UnexpectedException {
		Map<String, String> imagesSizeMap = this.getImagesSizes(client);
		TemplatePool templatePool = OpenNebulaClientUtil.getTemplatePool(client);
		List<HardwareRequirements> flavorsTemplate = new ArrayList<>();

		if (templatePool != null) {
			HardwareRequirements flavor;
			for (Template template : templatePool) {
				String id = template.getId();
				String name = template.getName();
				int cpu = this.convertToInteger(template.xpath(TEMPLATE_CPU_PATH));
				int memory = this.convertToInteger(template.xpath(TEMPLATE_MEMORY_PATH));

				int disk = this.convertToInteger(template.xpath(TEMPLATE_DISK_SIZE_PATH));
				// NOTE(pauloewerton): template disk size is not set, so fallback to image disk size
				if (disk == 0) {
					String imageId = template.xpath(TEMPLATE_IMAGE_ID_PATH);
					disk = this.getDiskSizeFromImageSizeMap(imagesSizeMap, imageId);
				}

				if (cpu != 0 && memory != 0 && disk != 0) {
					flavor = new HardwareRequirements(name, id, cpu, memory, disk);
					if (!this.containsFlavor(flavor)) {
						flavorsTemplate.add(flavor);
					}
				}
			}
		}

		if (!flavorsTemplate.isEmpty()) {
			this.flavors.addAll(flavorsTemplate);
		}
	}

	protected TreeSet<HardwareRequirements> getFlavors() {
		synchronized (this.flavors) {
			return this.flavors;
		}
	}

	protected long convertDiskSizeToMb(int diskSizeInGb) {
		return diskSizeInGb * MB_CONVERT;
	}

	protected Map<String, String> getImagesSizes(Client client) throws UnexpectedException {
		Map<String, String> imagesSizeMap = new HashMap<>();
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		for (Image image : imagePool) {
			String imageSize = image.xpath(IMAGE_SIZE_PATH);
			imagesSizeMap.put(image.getId(), imageSize);
		}
		return imagesSizeMap;
	}

	protected int convertToInteger(String number) {
		try {
			return Integer.parseInt(number);
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_TO_INTEGER), e);
			return 0;
		}
	}

	protected int getDiskSizeFromImageSizeMap(Map<String, String> imageSizeMap, String imageId) {
		if (imageSizeMap != null && !imageSizeMap.isEmpty() && imageId != null) {
			String diskSize = imageSizeMap.get(imageId);
			return this.convertToInteger(diskSize);
		} else {
			LOGGER.error(Messages.Error.ERROR_WHILE_GETTING_DISK_SIZE);
			return 0;
		}
	}

	protected boolean containsFlavor(HardwareRequirements flavor) {
		List<HardwareRequirements> list = new ArrayList<>(this.getFlavors());
		for (HardwareRequirements item : list) {
			if (item.getName().equals(flavor.getName())) {
				return true;
			}
		}
		return false;
	}

	protected void setComputeInstanceNetworks(ComputeInstance computeInstance) {
		// The default network is always included in the order by the OpenNebula plugin, thus it should be added
		// in the map of networks in the ComputeInstance by the plugin. The remaining networks passed by the user
		// are appended by the LocalCloudConnector.
		String defaultNetworkId = this.properties.getProperty(OpenNebulaConfigurationPropertyKeys.DEFAULT_NETWORK_ID_KEY);
		List<NetworkSummary> computeNetworks = new ArrayList<>();
		computeNetworks.add(new NetworkSummary(defaultNetworkId, SystemConstants.DEFAULT_NETWORK_NAME));
		computeInstance.setNetworks(computeNetworks);
	}

	protected void setFlavors(TreeSet<HardwareRequirements> flavors) {
		synchronized (this.flavors) {
			this.flavors = flavors;
		}
	}

	protected void setLaunchCommandGenerator(LaunchCommandGenerator launchCommandGenerator) {
		this.launchCommandGenerator = launchCommandGenerator;
	}
}