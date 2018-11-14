package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.CONTEXT;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.CPU;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.DISK;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.GRAPHICS;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.MEMORY;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.NETWORK_INTERFACE_CONNECTED;
import static org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaTagNameConstants.TEMPLATE;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaMarshallerTemplate;

@XmlRootElement(name = TEMPLATE)
public class VirtualMachineTemplate extends OpenNebulaMarshallerTemplate {

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
 