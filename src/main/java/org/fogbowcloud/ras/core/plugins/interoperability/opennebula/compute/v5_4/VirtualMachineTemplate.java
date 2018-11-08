package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.CONTEXT;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.CPU;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.DISK;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.GRAPHICS;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.MEMORY;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.NETWORK_INTERFACE_CONNECTED;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.TEMPLATE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaRequestTemplate;

@XmlRootElement(name = TEMPLATE)
public class VirtualMachineTemplate extends OpenNebulaRequestTemplate {

	private VirtualMachineContext context;
	private String cpu;
	private VirtualMachineGraphics graphics;
	private VirtualMachineImageDisk imageDisk;
	private VirtualMachineVolumeDisk volumeDisk;
	private String memory;
	private VirtualMachineNic networkInterfaceConnected;

	@XmlElement(name = CONTEXT)
	public VirtualMachineContext getContext() {
		return context;
	}

	public void setContext(VirtualMachineContext context) {
		this.context = context;
	}
	
	@XmlElement(name = CPU)
	public String getCpu() {
		return cpu;
	}

	public void setCpu(String cpu) {
		this.cpu = cpu;
	}

	@XmlElement(name = GRAPHICS)
	public VirtualMachineGraphics getGraphics() {
		return graphics;
	}

	public void setGraphics(VirtualMachineGraphics graphics) {
		this.graphics = graphics;
	}

	@XmlElement(name = DISK)
	public VirtualMachineImageDisk getImageDisk() {
		return imageDisk;
	}

	public void setImageDisk(VirtualMachineImageDisk imageDisk) {
		this.imageDisk = imageDisk;
	}

	@XmlElement(name = DISK)
	public VirtualMachineVolumeDisk getVolumeDisk() {
		return volumeDisk;
	}

	public void setVolumeDisk(VirtualMachineVolumeDisk volumeDisk) {
		this.volumeDisk = volumeDisk;
	}

	@XmlElement(name = MEMORY)
	public String getMemory() {
		return memory;
	}

	public void setMemory(String memory) {
		this.memory = memory;
	}

	@XmlElement(name = NETWORK_INTERFACE_CONNECTED)
	public VirtualMachineNic getNetworkInterfaceConnected() {
		return networkInterfaceConnected;
	}

	public void setNetworkInterfaceConnected(VirtualMachineNic networkInterfaceConnected) {
		this.networkInterfaceConnected = networkInterfaceConnected;
	}

}
 