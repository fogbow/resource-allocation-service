package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.api.http.response.NetworkSummary;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.HardwareRequirements;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.*;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;

import java.util.*;

public class OpenNebulaComputePlugin implements ComputePlugin<CloudUser> {

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaComputePlugin.class);

	protected static final int ONE_GIGABYTE_IN_MEGABYTES = 1024;
	protected static final int DEFAULT_NUMBER_OF_INSTANCES = 1;

	protected static final String DEFAULT_ARCHITECTURE = "x86_64";
	protected static final String DEFAULT_GRAPHIC_ADDRESS = "0.0.0.0";
	protected static final String DEFAULT_GRAPHIC_TYPE = "vnc";
	protected static final String NETWORK_CONFIRMATION_CONTEXT = "YES";
	protected static final String NIC_IP_EXPRESSION = "//NIC/IP";

	protected static final boolean SHUTS_DOWN_HARD = true;
	private static final int DEFAULT_DISK_VALUE_UNKNOWN = 0;
	protected static final int VALUE_NOT_DEFINED_BY_USER = 0;
	protected static final int MINIMUM_VCPU_VALUE = 1;
	private static final int MINIMUM_MEMORY_VALUE = 1;

	protected static final String IMAGE_SIZE_PATH = "SIZE";
	protected static final String TEMPLATE_CPU_PATH = "TEMPLATE/CPU";
	protected static final String TEMPLATE_DISK_SIZE_PATH = "TEMPLATE/DISK/SIZE";
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

		String instanceId = this.doRequestInstance(client, request);
		this.setOrderAllocation(computeOrder, virtualMachine);

		return instanceId;
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
			throw new UnexpectedException(String.format(Messages.Error.ERROR_WHILE_REMOVING_VM, computeOrder.getInstanceId(),
					response.getMessage()));
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
		int disk = Integer.parseInt(virtualMachine.xpath(TEMPLATE_DISK_SIZE_PATH)) / ONE_GIGABYTE_IN_MEGABYTES;

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

		HardwareRequirements foundFlavor = this.getFlavor(client, computeOrder);
		String cpu = String.valueOf(foundFlavor.getCpu());
		String memory = String.valueOf(foundFlavor.getMemory());
		String disk = String.valueOf(foundFlavor.getDisk());

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

	protected synchronized void setOrderAllocation(ComputeOrder computeOrder, VirtualMachineTemplate virtualMachine) {
		int sizeInMegabytes = Integer.parseInt(virtualMachine.getDisk().getSize());
		int size = convertDiskSizeToGb(sizeInMegabytes);
		ComputeAllocation actualAllocation = new ComputeAllocation(
				Integer.parseInt(virtualMachine.getCpu()),
				Integer.parseInt(virtualMachine.getMemory()),
				DEFAULT_NUMBER_OF_INSTANCES,
				size);
		computeOrder.setActualAllocation(actualAllocation);
	}

	@VisibleForTesting
	int convertDiskSizeToGb(int sizeInMegabytes) {
		return sizeInMegabytes / ONE_GIGABYTE_IN_MEGABYTES;
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

	@VisibleForTesting
	HardwareRequirements getFlavor(Client client, ComputeOrder computeOrder)
			throws NoAvailableResourcesException, UnexpectedException {

		int disk = getFlavorDisk(client, computeOrder);
		int cpu = getFlavorVcpu(computeOrder);
		int memory = getFlavorMemory(computeOrder);

		return new HardwareRequirements.Opennebula(cpu, memory, disk);
	}

	// TODO(chico) - Implement tests
	@VisibleForTesting
	int getFlavorMemory(ComputeOrder computeOrder) {
		return computeOrder.getMemory() != VALUE_NOT_DEFINED_BY_USER ?
				computeOrder.getMemory() : MINIMUM_MEMORY_VALUE;
	}

	// TODO(chico) - Implement tests
	@VisibleForTesting
	int getFlavorVcpu(ComputeOrder computeOrder) {
		return computeOrder.getvCPU() != VALUE_NOT_DEFINED_BY_USER ?
				computeOrder.getvCPU() : MINIMUM_VCPU_VALUE;
	}

	@VisibleForTesting
	int getFlavorDisk(Client client, ComputeOrder computeOrder) throws UnexpectedException, NoAvailableResourcesException {
		String imageId = computeOrder.getImageId();
		int minimumImageSize = getMinimumImageSize(client, imageId);
		int diskInGb = computeOrder.getDisk();
		if (diskInGb == VALUE_NOT_DEFINED_BY_USER) {
			return minimumImageSize;
		}

		int disk = convertDiskSizeToMb(diskInGb);
		if (disk < minimumImageSize) {
			throw new NoAvailableResourcesException();
		}
		return disk;
	}

	@VisibleForTesting
	int getMinimumImageSize(Client client, String imageId)
			throws UnexpectedException, NoAvailableResourcesException {

		Map<String, String> imagesSizeMap = this.getImagesSizes(client);
		String minimumImageSizeStr = Optional.ofNullable(imagesSizeMap.get(imageId))
				.orElseThrow(() -> new NoAvailableResourcesException(Messages.Exception.IMAGE_NOT_FOUND));

		return Integer.parseInt(minimumImageSizeStr);
	}

	protected TreeSet<HardwareRequirements> getFlavors() {
		synchronized (this.flavors) {
			return this.flavors;
		}
	}

	protected int convertDiskSizeToMb(int diskSizeInGb) {
		return diskSizeInGb * ONE_GIGABYTE_IN_MEGABYTES;
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

	// TODO(chico) - It's deprecated; Remove it
	protected int convertToInteger(String number) {
		try {
			return Integer.parseInt(number);
		} catch (NumberFormatException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_CONVERTING_TO_INTEGER), e);
			return 0;
		}
	}

	// TODO(chico) - It's deprecated; Remove it
	protected int getDiskSizeFromImageSizeMap(Map<String, String> imageSizeMap, String imageId) {
		if (imageSizeMap != null && !imageSizeMap.isEmpty() && imageId != null) {
			String diskSize = imageSizeMap.get(imageId);
			return this.convertToInteger(diskSize);
		} else {
			LOGGER.error(Messages.Error.ERROR_WHILE_GETTING_DISK_SIZE);
			return DEFAULT_DISK_VALUE_UNKNOWN;
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