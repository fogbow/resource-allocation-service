package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.util.BinaryUnit;
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
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.*;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.sdk.v5_4.compute.model.CreateComputeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.sdk.v5_4.compute.model.VirtualMachineTemplate;
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

	@VisibleForTesting
    static final int ONE_GIGABYTE_IN_MEGABYTES = 1024;
	@VisibleForTesting
    static final int DEFAULT_NUMBER_OF_INSTANCES = 1;

	@VisibleForTesting
    static final String DEFAULT_ARCHITECTURE = "x86_64";
	@VisibleForTesting
    static final String DEFAULT_GRAPHIC_ADDRESS = "0.0.0.0";
	@VisibleForTesting
    static final String DEFAULT_GRAPHIC_TYPE = "vnc";
	@VisibleForTesting
    static final String NETWORK_CONFIRMATION_CONTEXT = "YES";
	@VisibleForTesting
    static final String NIC_IP_EXPRESSION = "//NIC/IP";

	@VisibleForTesting
    static final boolean SHUTS_DOWN_HARD = true;
	@VisibleForTesting
    static final int VALUE_NOT_DEFINED_BY_USER = 0;
	@VisibleForTesting
    static final int MINIMUM_VCPU_VALUE = 1;
	@VisibleForTesting
    static final int MINIMUM_RAM_VALUE = 1;

	@VisibleForTesting
    static final String IMAGE_SIZE_PATH = "SIZE";
	@VisibleForTesting
    static final String TEMPLATE_CPU_PATH = "TEMPLATE/CPU";
	@VisibleForTesting
    static final String TEMPLATE_DISK_SIZE_PATH = "TEMPLATE/DISK/SIZE";
	@VisibleForTesting
    static final String TEMPLATE_MEMORY_PATH = "TEMPLATE/MEMORY";

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
		LOGGER.info(String.format(Messages.Log.REQUESTING_INSTANCE_FROM_PROVIDER));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		CreateComputeRequest request = this.getCreateComputeRequest(client, computeOrder);
		VirtualMachineTemplate virtualMachine = request.getVirtualMachine();

		String instanceId = this.doRequestInstance(client, request);
		this.setOrderAllocation(computeOrder, virtualMachine);

		return instanceId;
	}

	@Override
	public ComputeInstance getInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
		String instanceId = computeOrder.getInstanceId();
		LOGGER.info(String.format(Messages.Log.GETTING_INSTANCE_S, instanceId));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, computeOrder.getInstanceId());
		return this.doGetInstance(virtualMachine);
	}

	@Override
	public void deleteInstance(ComputeOrder computeOrder, CloudUser cloudUser) throws FogbowException {
		String instanceId = computeOrder.getInstanceId();
		LOGGER.info(String.format(Messages.Log.DELETING_INSTANCE_S, instanceId));
		Client client = OpenNebulaClientUtil.createClient(this.endpoint, cloudUser.getToken());
		VirtualMachine virtualMachine = OpenNebulaClientUtil.getVirtualMachine(client, instanceId);
		OneResponse response = virtualMachine.terminate(SHUTS_DOWN_HARD);
		if (response.isError()) {
			throw new InternalServerErrorException(String.format(Messages.Exception.ERROR_WHILE_REMOVING_VM_S_S,
					instanceId, response.getMessage()));
		}
	}

    @Override
    public void takeSnapshot(ComputeOrder computeOrder, String name, CloudUser cloudUser) throws FogbowException {
		// ToDo: implement
    }

	@Override
	public void pauseInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
		// ToDo: implement
	}

	@Override
	public void hibernateInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
		// ToDo: implement
	}
	
    @Override
    public void stopInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
        // TODO implement
        throw new NotImplementedOperationException();
    }

	@Override
	public void resumeInstance(ComputeOrder order, CloudUser cloudUser) throws FogbowException {
		// ToDo: implement
	}

	@Override
	public boolean isPaused(String cloudState) throws FogbowException {
		return false;
	}

	@Override
	public boolean isHibernated(String cloudState) throws FogbowException {
		return false;
	}
	
    @Override
    public boolean isStopped(String cloudState) throws FogbowException {
        // TODO implement
        return false;
    }

	@VisibleForTesting
    String doRequestInstance(Client client, CreateComputeRequest request)
			throws InvalidParameterException, UnacceptableOperationException {

		String template = request.getVirtualMachine().marshalTemplate();
		String instanceId = OpenNebulaClientUtil.allocateVirtualMachine(client, template);

		return instanceId;
	}

	@VisibleForTesting
    ComputeInstance doGetInstance(VirtualMachine virtualMachine) {
		OneResponse response = virtualMachine.info();

		String id = virtualMachine.getId();
		String name = virtualMachine.getName();
		String state = virtualMachine.lcmStateStr();

		int cpu = Integer.parseInt(virtualMachine.xpath(TEMPLATE_CPU_PATH));
		int memoryRam = Integer.parseInt(virtualMachine.xpath(TEMPLATE_MEMORY_PATH));
		int disk = Integer.parseInt(virtualMachine.xpath(TEMPLATE_DISK_SIZE_PATH)) / ONE_GIGABYTE_IN_MEGABYTES;

		String xml = response.getMessage();
		XmlUnmarshaller xmlUnmarshaller = new XmlUnmarshaller(xml);
		List<String> ipAddresses = xmlUnmarshaller.getContextListOf(NIC_IP_EXPRESSION);

		ComputeInstance computeInstance = new ComputeInstance(id, state, name, cpu, memoryRam, disk, ipAddresses);
		this.setComputeInstanceNetworks(computeInstance);

		return computeInstance;
	}

	@VisibleForTesting
    CreateComputeRequest getCreateComputeRequest(Client client, ComputeOrder computeOrder)
			throws InternalServerErrorException, UnacceptableOperationException {
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
		String memoryRam = String.valueOf(foundFlavor.getRam());
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
				.memory(memoryRam)
				.networks(networks)
				.architecture(architecture)
				.build();

		return request;
	}

	@VisibleForTesting
    synchronized void setOrderAllocation(ComputeOrder computeOrder, VirtualMachineTemplate virtualMachine) {
		int sizeInMegabytes = Integer.parseInt(virtualMachine.getDisk().getSize());
		int size = (int) BinaryUnit.megabytes(sizeInMegabytes).asGigabytes();
		ComputeAllocation actualAllocation = new ComputeAllocation(
                DEFAULT_NUMBER_OF_INSTANCES, Integer.parseInt(virtualMachine.getCpu()),
				Integer.parseInt(virtualMachine.getMemory()),
                size);
		computeOrder.setActualAllocation(actualAllocation);
	}

	@VisibleForTesting
	int convertDiskSizeToGb(int sizeInMegabytes) {
		return sizeInMegabytes / ONE_GIGABYTE_IN_MEGABYTES;
	}

	@VisibleForTesting
    List<String> getNetworkIds(List<String> networkIds) {
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
			throws UnacceptableOperationException, InternalServerErrorException {

		int disk = getFlavorDisk(client, computeOrder);
		int cpu = getFlavorVcpu(computeOrder);
		int ram = getFlavorRam(computeOrder);
		return new HardwareRequirements.Opennebula(cpu, ram, disk);
	}

	@VisibleForTesting
	int getFlavorRam(ComputeOrder computeOrder) {
		return computeOrder.getRam() != VALUE_NOT_DEFINED_BY_USER ?
				computeOrder.getRam(): MINIMUM_RAM_VALUE;
	}

	@VisibleForTesting
	int getFlavorVcpu(ComputeOrder computeOrder) {
		return computeOrder.getvCPU() != VALUE_NOT_DEFINED_BY_USER ?
				computeOrder.getvCPU() : MINIMUM_VCPU_VALUE;
	}

	@VisibleForTesting
	int getFlavorDisk(Client client, ComputeOrder computeOrder) throws InternalServerErrorException, UnacceptableOperationException {
		String imageId = computeOrder.getImageId();
		int minimumImageSize = getMinimumImageSize(client, imageId);
		int diskInGb = computeOrder.getDisk();
		if (diskInGb == VALUE_NOT_DEFINED_BY_USER) {
			return minimumImageSize;
		}

		int disk = convertDiskSizeToMb(diskInGb);
		if (disk < minimumImageSize) {
			throw new UnacceptableOperationException();
		}
		return disk;
	}

	@VisibleForTesting
	int getMinimumImageSize(Client client, String imageId)
			throws InternalServerErrorException, UnacceptableOperationException {

		Map<String, String> imagesSizeMap = this.getImagesSizes(client);
		String minimumImageSizeStr = Optional.ofNullable(imagesSizeMap.get(imageId))
				.orElseThrow(() -> new UnacceptableOperationException(Messages.Exception.IMAGE_NOT_FOUND));

		return Integer.parseInt(minimumImageSizeStr);
	}

	@VisibleForTesting
    TreeSet<HardwareRequirements> getFlavors() {
		synchronized (this.flavors) {
			return this.flavors;
		}
	}

	@VisibleForTesting
    int convertDiskSizeToMb(int diskSizeInGb) {
		return diskSizeInGb * ONE_GIGABYTE_IN_MEGABYTES;
	}

	@VisibleForTesting
    Map<String, String> getImagesSizes(Client client) throws InternalServerErrorException {
		Map<String, String> imagesSizeMap = new HashMap<>();
		ImagePool imagePool = OpenNebulaClientUtil.getImagePool(client);
		for (Image image : imagePool) {
			String imageSize = image.xpath(IMAGE_SIZE_PATH);
			imagesSizeMap.put(image.getId(), imageSize);
		}
		return imagesSizeMap;
	}

	@VisibleForTesting
    boolean containsFlavor(HardwareRequirements flavor) {
		List<HardwareRequirements> list = new ArrayList<>(this.getFlavors());
		for (HardwareRequirements item : list) {
			if (item.getName().equals(flavor.getName())) {
				return true;
			}
		}
		return false;
	}

	@VisibleForTesting
    void setComputeInstanceNetworks(ComputeInstance computeInstance) {
		// The default network is always included in the order by the OpenNebula plugin, thus it should be added
		// in the map of networks in the ComputeInstance by the plugin. The remaining networks passed by the user
		// are appended by the LocalCloudConnector.
		String defaultNetworkId = this.properties.getProperty(OpenNebulaConfigurationPropertyKeys.DEFAULT_NETWORK_ID_KEY);
		List<NetworkSummary> computeNetworks = new ArrayList<>();
		computeNetworks.add(new NetworkSummary(defaultNetworkId, SystemConstants.DEFAULT_NETWORK_NAME));
		computeInstance.setNetworks(computeNetworks);
	}

	@VisibleForTesting
    void setFlavors(TreeSet<HardwareRequirements> flavors) {
		synchronized (this.flavors) {
			this.flavors = flavors;
		}
	}

	@VisibleForTesting
    void setLaunchCommandGenerator(LaunchCommandGenerator launchCommandGenerator) {
		this.launchCommandGenerator = launchCommandGenerator;
	}
}