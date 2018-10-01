package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaXmlTagsConstants.VirtualMachine.*;

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
	
	public VirtualMachineTemplate(VirtualMachineContext context, String cpu, VirtualMachineGraphics graphics,
			VirtualMachineImageDisk imageDisk, VirtualMachineVolumeDisk volumeDisk, String memory,
			VirtualMachineNic networkInterfaceConnected) {
		
		super();
		this.context = context;
		this.cpu = cpu;
		this.graphics = graphics;
		this.imageDisk = imageDisk;
		this.volumeDisk = volumeDisk;
		this.memory = memory;
		this.networkInterfaceConnected = networkInterfaceConnected;
	}

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
 