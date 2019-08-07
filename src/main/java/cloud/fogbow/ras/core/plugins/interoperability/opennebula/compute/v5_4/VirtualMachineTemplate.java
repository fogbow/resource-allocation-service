package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;


import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaMarshallerTemplate;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

import static cloud.fogbow.common.constants.OpenNebulaConstants.*;

@XmlRootElement(name = TEMPLATE)
public class VirtualMachineTemplate extends OpenNebulaMarshallerTemplate {

	private VirtualMachineTemplate.Context context;
	private String name;
	private String cpu;
	private VirtualMachineTemplate.Graphics graphics;
	private VirtualMachineTemplate.Disk disk;
	private String memory;
	private List<VirtualMachineTemplate.Nic> nic;
	private VirtualMachineTemplate.OperationalSystem os;

	@XmlElement(name = NAME)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

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
	public VirtualMachineTemplate.Disk getDisk() {
		return disk;
	}

	public void setDisk(VirtualMachineTemplate.Disk disk) {
		this.disk = disk;
	}

	@XmlElement(name = MEMORY)
	public String getMemory() {
		return memory;
	}

	public void setMemory(String memory) {
		this.memory = memory;
	}

	@XmlElement(name = NETWORK_INTERFACE_CONNECTED)
	public List<VirtualMachineTemplate.Nic> getNic() {
		return nic;
	}

	public void setNic(List<VirtualMachineTemplate.Nic> nic) {
		this.nic = nic;
	}
	
	@XmlElement(name = OPERATIONAL_SYSTEM)
	public VirtualMachineTemplate.OperationalSystem getOperationalSystem() {
		return os;
	}

	public void setOperationalSystem(VirtualMachineTemplate.OperationalSystem os) {
		this.os = os;
	}

	@XmlRootElement(name = CONTEXT)
	public static class Context {
		private String network;
		private String publicKey;
		private String userName;
		private String startScriptBase64;

		// TODO(pauloewerton): move constants below to common
		private static final String SSH_PUBLIC_KEY = "SSH_PUBLIC_KEY";
		private static final String USERNAME = "USERNAME";
		private static final String START_SCRIPT_BASE64 = "START_SCRIPT_BASE64";

		@XmlElement(name = SSH_PUBLIC_KEY)
		public String getPublicKey() {
			return publicKey;
		}

		public void setPublicKey(String publicKey) {
			this.publicKey = publicKey;
		}

		@XmlElement(name = USERNAME)
		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		@XmlElement(name = START_SCRIPT_BASE64)
		public String getStartScriptBase64() {
			return startScriptBase64;
		}

		public void setStartScriptBase64(String startScriptBase64) {
			this.startScriptBase64 = startScriptBase64;
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

		private String address;
		private String type;
		
		@XmlElement(name = LISTEN)
		public String getAddress() {
			return address;
		}
		
		public void setAddress(String address) {
			this.address = address;
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
	public static class Disk {
		private String imageId;
		private String size;

		@XmlElement(name = IMAGE_ID)
		public String getImageId() {
			return imageId;
		}

		public void setImageId(String imageId) {
			this.imageId = imageId;
		}

		@XmlElement(name = SIZE)
		public String getSize() {
			return size;
		}

		public void setSize(String size) {
			this.size = size;
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
	
	@XmlRootElement(name = OPERATIONAL_SYSTEM)
	public static class OperationalSystem {
		
		private String architecture;

		@XmlElement(name = ARCHITECTURE)
		public String getArchitecture() {
			return architecture;
		}

		public void setArchitecture(String architecture) {
			this.architecture = architecture;
		}
	}
}
 
