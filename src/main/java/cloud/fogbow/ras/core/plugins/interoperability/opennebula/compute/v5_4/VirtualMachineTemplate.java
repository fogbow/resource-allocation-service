package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;


import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaMarshallerTemplate;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import static cloud.fogbow.common.constants.OpenNebulaConstants.*;

@XmlRootElement(name = TEMPLATE)
public class VirtualMachineTemplate extends OpenNebulaMarshallerTemplate {

	private VirtualMachineTemplate.Context context;
	private String cpu;
	private VirtualMachineTemplate.Graphics graphics;
	private VirtualMachineTemplate.ImageDisk imageDisk;
	private VirtualMachineTemplate.VolumeDisk volumeDisk;
	private String memory;
	private List<VirtualMachineTemplate.Nic> nics;

	@XmlElement(name = CONTEXT)
	public VirtualMachineTemplate.Context getContext() {
		return context;
	}

	public void setContext(VirtualMachineTemplate.Context context) {
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
	public VirtualMachineTemplate.Graphics getGraphics() {
		return graphics;
	}

	public void setGraphics(VirtualMachineTemplate.Graphics graphics) {
		this.graphics = graphics;
	}

	@XmlElement(name = DISK)
	public VirtualMachineTemplate.ImageDisk getImageDisk() {
		return imageDisk;
	}

	public void setImageDisk(VirtualMachineTemplate.ImageDisk imageDisk) {
		this.imageDisk = imageDisk;
	}

	@XmlElement(name = DISK)
	public VirtualMachineTemplate.VolumeDisk getVolumeDisk() {
		return volumeDisk;
	}

	public void setVolumeDisk(VirtualMachineTemplate.VolumeDisk volumeDisk) {
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
	public List<VirtualMachineTemplate.Nic> getNics() {
		return nics;
	}

	public void setNics(List<VirtualMachineTemplate.Nic> nics) {
		this.nics = nics;
	}

	@XmlRootElement(name = CONTEXT)
	public static class Context {

		private String encoding;
		private String userdata;
		private String network;
		
		@XmlElement(name = USERDATA_ENCODING)
		public String getEncoding() {
			return encoding;
		}
		
		public void setEncoding(String encoding) {
			this.encoding = encoding;
		}
		
		@XmlElement(name = USERDATA)
		public String getUserdata() {
			return userdata;
		}
		
		public void setUserdata(String userdata) {
			this.userdata = userdata;
		}
		
		@XmlElement(name = NETWORK)
		public String getNetwork() {
			return network;
		}
		
		public void setNetwork(String network) {
			this.network = network;
		}
	}
	
	@XmlRootElement(name = GRAPHICS)
	public static class Graphics {

		private String listen;
		private String type;
		
		@XmlElement(name = LISTEN)
		public String getListen() {
			return listen;
		}
		
		public void setListen(String listen) {
			this.listen = listen;
		}
		
		@XmlElement(name = TYPE)
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
	}
	
	@XmlRootElement(name = DISK)
	public static class ImageDisk {

		private String imageId;

		@XmlElement(name = IMAGE_ID)
		public String getImageId() {
			return imageId;
		}

		public void setImageId(String imageId) {
			this.imageId = imageId;
		}
	}
	
	@XmlRootElement(name = DISK)
	public static class VolumeDisk {

		private String size;
		private String type;
		
		@XmlElement(name = SIZE)
		public String getSize() {
			return size;
		}
		
		public void setSize(String size) {
			this.size = size;
		}
		
		@XmlElement(name = TYPE)
		public String getType() {
			return type;
		}
		
		public void setType(String type) {
			this.type = type;
		}
	}
	
	@XmlRootElement(name = NETWORK_INTERFACE_CONNECTED)
	public static class Nic {

		private String networkId;

		@XmlElement(name = NETWORK_ID)
		public String getNetworkId() {
			return networkId;
		}

		public void setNetworkId(String networkId) {
			this.networkId = networkId;
		}
	}
}
 
