package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.HardwareRequirements;
import org.fogbowcloud.ras.core.models.instances.ComputeInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.ComputePlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.template.Template;
import org.opennebula.client.template.TemplatePool;
import org.opennebula.client.vm.VirtualMachine;

public class OpenNebulaComputePlugin implements ComputePlugin<Token>{

	private static final Logger LOGGER = Logger.getLogger(OpenNebulaComputePlugin.class);
	
	private static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
	private static final String DEFAULT_GRAPHIC_ADDRESS = "0.0.0.0";
	private static final String DEFAULT_GRAPHIC_TYPE = "vnc";
	private static final String DEFAULT_VOLUME_TYPE = "fs";
	private static final String IMAGE_SIZE_PATH = "SIZE";
	private static final String ONE_VM_FAILURE_STATE = "Failure";
	private static final String ONE_VM_RUNNING_STATE = "Running";
	private static final String ONE_VM_SUSPENDED_STATE = "Suspended";
	private static final String NETWORK_CONFIRMATION_CONTEXT = "YES";
	private static final String TEMPLATE_CPU_PATH = "TEMPLATE/CPU";
	private static final String TEMPLATE_DISK_IMAGE_INDEX = "TEMPLATE/DISK[%s]/IMAGE";
	private static final String TEMPLATE_DISK_INDEX = "TEMPLATE/DISK[%s]";
	private static final String TEMPLATE_DISK_SIZE_INDEX = "TEMPLATE/DISK[%s]/SIZE";
	private static final String TEMPLATE_DISK_SIZE_PATH = "TEMPLATE/DISK/SIZE";
	private static final String TEMPLATE_MEMORY_PATH = "TEMPLATE/MEMORY";
	private static final String TEMPLATE_NAME_PATH = "NAME";
	private static final String TEMPLATE_NIC_IP_PATH = "TEMPLATE/NIC/IP";
	private static final String USERDATA_ENCODING_CONTEXT = "base64";

	private static final int FIRST_AVAILABLE_ID = 0;

	private TreeSet<HardwareRequirements> hardwareRequirements;
	private OpenNebulaClientFactory factory;
	private Properties properties;
	
	public OpenNebulaComputePlugin() {
		this.properties = PropertiesUtil
				.readProperties(HomeDir.getPath() + DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME);
		this.hardwareRequirements = new TreeSet<HardwareRequirements>();
	}

	@Override
	public String requestInstance(ComputeOrder computeOrder, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.info(String.format(Messages.Info.REQUESTING_INSTANCE, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		
		String userData = computeOrder.getUserData().toString(); // TODO verify this is correct
		String networkId = resolveNetworksId(computeOrder);
		
		// TODO implements flavors
		
		CreateComputeRequest request = new CreateComputeRequest.Builder()
				.contextEncoding(USERDATA_ENCODING_CONTEXT)
				.contextUserdata(userData)
				.contextNetwork(NETWORK_CONFIRMATION_CONTEXT)
				.cpu(String.valueOf(computeOrder.getvCPU()))
				.graphicsListen(DEFAULT_GRAPHIC_ADDRESS)
				.graphicsType(DEFAULT_GRAPHIC_TYPE)
				.imageId(computeOrder.getImageId())
				.volumeSize(String.valueOf(computeOrder.getDisk()))
				.volumeType(DEFAULT_VOLUME_TYPE)
				.memory(String.valueOf(computeOrder.getMemory()))
				.networkId(networkId)
				.build();
		
		String template = request.getVirtualMachine().generateTemplate();
		return this.factory.allocateVirtualMachine(client, template);
	}

	@Override
	public ComputeInstance getInstance(String computeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.info(String.format(Messages.Info.GETTING_INSTANCE, computeInstanceId, localUserAttributes.getTokenValue()));
		if (this.hardwareRequirements == null || this.hardwareRequirements.isEmpty() ) {
			updateHardwareRequirements(localUserAttributes);
		}
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, computeInstanceId);
		return createVirtualMachineInstance(virtualMachine);
	}

	@Override
	public void deleteInstance(String computeInstanceId, Token localUserAttributes)
			throws FogbowRasException, UnexpectedException {
		
		LOGGER.info(String.format(Messages.Info.DELETING_INSTANCE, computeInstanceId, localUserAttributes.getTokenValue()));
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		VirtualMachine virtualMachine = this.factory.createVirtualMachine(client, computeInstanceId);
		OneResponse response = virtualMachine.terminate();
		if (response.isError()) {			
			LOGGER.error(String.format(Messages.Error.ERROR_WHILE_REMOVING_VM, response.getMessage()));
		}
	}

	private String resolveNetworksId(ComputeOrder computeOrder) {
		List<String> requestedNetworksId = new ArrayList<>();
        String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
        if (!computeOrder.getNetworksId().isEmpty()) {
        	requestedNetworksId.addAll(computeOrder.getNetworksId());
        }
        requestedNetworksId.add(defaultNetworkId);        
        computeOrder.setNetworksId(requestedNetworksId);
		return requestedNetworksId.get(FIRST_AVAILABLE_ID);
	}
	
	private void updateHardwareRequirements(Token localUserAttributes) throws UnexpectedException {
		Client client = this.factory.createClient(localUserAttributes.getTokenValue());
		Map<String, String> imageSizeMap = getImageSizes(client);
		List<HardwareRequirements> flavors = new ArrayList<>();
		List<HardwareRequirements> flavorsTemplate = new ArrayList<>();
		
		TemplatePool templatePool = this.factory.createTemplatePool(client);
		HardwareRequirements hardwareRequirements;
		for (Template template : templatePool) {
			String name = template.xpath(TEMPLATE_NAME_PATH);
			int memory = Integer.parseInt(template.xpath(TEMPLATE_MEMORY_PATH));
			int cpu = Integer.parseInt(template.xpath(TEMPLATE_CPU_PATH));
			int disk = 0; 
			
			hardwareRequirements = new HardwareRequirements(name, null, cpu, memory, disk); // TODO verify field null
			flavorsTemplate.add(hardwareRequirements);
			if (containsFlavor(hardwareRequirements)) {
				int size = loadImageSizeDisk(imageSizeMap, template);
				hardwareRequirements = new HardwareRequirements(name, null, cpu, memory, size); // TODO verify field null
				flavors.add(hardwareRequirements);
			}
		}
		
		if (flavors != null) {
			this.hardwareRequirements.addAll(flavors);			
		}
		removeInvalidFlavors(flavorsTemplate); // TODO implements this method
	}

	private int loadImageSizeDisk(Map<String, String> map, Template template) {
		int index = 1;
		int size = 0;
		while (true) {
			String imageDiskName = template.xpath(String.format(TEMPLATE_DISK_IMAGE_INDEX, index));
			String volatileDiskSize = template.xpath(String.format(TEMPLATE_DISK_SIZE_INDEX, index));
			if (volatileDiskSize != null && !volatileDiskSize.isEmpty()) {
				try {
					size += Integer.parseInt(volatileDiskSize);
				} catch (Exception e) {
					LOGGER.error(e);
				}
			} else if (imageDiskName != null && !imageDiskName.isEmpty()){
				try {
					size += Integer.parseInt(map.get(imageDiskName));
				} catch (Exception e) {
					LOGGER.error(e);
				}
			}
			index++;
			String templateDiskIndex = String.format(TEMPLATE_DISK_INDEX, index);
			if (template.xpath(templateDiskIndex) == null || template.xpath(templateDiskIndex).isEmpty()) {
				break;
			}
		}
		return size;
	}

	private Map<String, String> getImageSizes(Client client) {
		Map<String, String> imageSizeMap = new HashMap<String, String>();
		ImagePool imagePool = this.factory.createImagePool(client);
		for (Image image : imagePool) {
			imageSizeMap.put(image.getName(), image.xpath(IMAGE_SIZE_PATH));
		}
		return imageSizeMap;
	}

	private boolean containsFlavor(HardwareRequirements hardwareRequirements) {
		boolean containsFlavor = false;
		List<HardwareRequirements> flavors = new ArrayList<>(this.hardwareRequirements);
		for (HardwareRequirements flavor : flavors) {
			if (hardwareRequirements.getName().equals(flavor.getName())) {
				containsFlavor = true;
				break;
			}
		}
		return containsFlavor;
	}

	private void removeInvalidFlavors(List<HardwareRequirements> hardwareRequirementsList) {
		// TODO Auto-generated method stub
	}

	private ComputeInstance createVirtualMachineInstance(VirtualMachine virtualMachine) {
		String id = virtualMachine.getId();
		String hostName = null; // FIXME
		int cpu = Integer.parseInt(virtualMachine.xpath(TEMPLATE_CPU_PATH));
		int disk = Integer.parseInt(virtualMachine.xpath(TEMPLATE_DISK_SIZE_PATH));
		int memory = Integer.parseInt(virtualMachine.xpath(TEMPLATE_MEMORY_PATH));
		String privateIp = virtualMachine.xpath(TEMPLATE_NIC_IP_PATH);

		String state = virtualMachine.lcmStateStr();
		InstanceState instanceState = getInstanceState(state);

		LOGGER.info(String.format(Messages.Info.MOUNTING_INSTANCE, id));
		ComputeInstance computeInstance = new ComputeInstance(
				id, 
				instanceState, 
				hostName,
				cpu, 
				memory, 
				disk, 
				privateIp);
		
		return computeInstance;
	}
	
	private InstanceState getInstanceState(String state) {
		switch (state) {
		case ONE_VM_RUNNING_STATE:
			return InstanceState.READY;
		case ONE_VM_SUSPENDED_STATE:
			return InstanceState.INACTIVE;
		case ONE_VM_FAILURE_STATE:
			return InstanceState.FAILED;
		}
		return InstanceState.UNAVAILABLE;
	}
	
}