package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.ArrayList;
import java.util.List;

import cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4.VirtualMachineTemplate.Disk;

/**
 * http://docs.opennebula.org/5.4/operation/references/template.html#template
 *
 * <TEMPLATE>
 *   <NAME>test_vm</NAME>
 *   <CPU>2</CPU>
 *   <MEMORY>1024</MEMORY>
 *   <DISK>
 *     <IMAGE_ID>2</IMAGE_ID>
 *   </DISK>
 *   <DISK>
 *     <IMAGE>Data</IMAGE>
 *     <IMAGE_UNAME>oneadmin</IMAGE_UNAME>
 *   </DISK>
 * </TEMPLATE>
 */
public class CreateComputeRequest {

	private VirtualMachineTemplate virtualMachine;

	public VirtualMachineTemplate getVirtualMachine() {
		return this.virtualMachine;
	}

	public CreateComputeRequest(Builder builder) {
		String cpu = builder.cpu;
		String memory = builder.memory;
		String name = builder.name;
		VirtualMachineTemplate.Context context = buildContext(builder);
		VirtualMachineTemplate.Graphics graphics = buildGraphics(builder);
		VirtualMachineTemplate.Disk disk = buildDisk(builder);
		List<VirtualMachineTemplate.Nic> nic = buildNic(builder);
		VirtualMachineTemplate.OperationalSystem os = buildOs(builder);
		
		this.virtualMachine = new VirtualMachineTemplate();
		this.virtualMachine.setName(name);
		this.virtualMachine.setContext(context);
		this.virtualMachine.setCpu(cpu);
		this.virtualMachine.setGraphics(graphics);
		this.virtualMachine.setDisk(disk);
		this.virtualMachine.setMemory(memory);
		this.virtualMachine.setNic(nic);
		this.virtualMachine.setOperationalSystem(os);
	}

	private Disk buildDisk(Builder builder) {
		VirtualMachineTemplate.Disk disk = new VirtualMachineTemplate.Disk();
		disk.setImageId(builder.imageId);
		disk.setSize(builder.diskSize);
		return disk;
	}

	private VirtualMachineTemplate.OperationalSystem buildOs(Builder builder) {
		VirtualMachineTemplate.OperationalSystem os = new VirtualMachineTemplate.OperationalSystem();
		os.setArchitecture(builder.architecture);
		return os;
	}
	
	private List<VirtualMachineTemplate.Nic> buildNic(Builder builder) {
		List<VirtualMachineTemplate.Nic> networks = new ArrayList<>();
		for (int i = 0; i < builder.networks.size(); i++) {
			VirtualMachineTemplate.Nic nic = new VirtualMachineTemplate.Nic();
			nic.setNetworkId(builder.networks.get(i));
			networks.add(nic);
		}
		return networks;
	}

	private VirtualMachineTemplate.Graphics buildGraphics(Builder builder) {
		VirtualMachineTemplate.Graphics graphics = new VirtualMachineTemplate.Graphics();
		graphics.setAddress(builder.graphicsAddress);
		graphics.setType(builder.graphicsType);
		return graphics;
	}

	private VirtualMachineTemplate.Context buildContext(Builder builder) {
		VirtualMachineTemplate.Context context = new VirtualMachineTemplate.Context();
		context.setPublicKey(builder.publicKey);
		context.setUserName(builder.userName);
		context.setStartScriptBase64(builder.startScriptBase64);
		context.setNetwork(builder.contextNetwork);
		return context;
	}

	public static class Builder {
		private String name;
		private String contextNetwork;
		private String cpu;
		private String graphicsAddress;
		private String graphicsType;
		private String imageId;
		private String diskSize;
		private String memory;
		private List<String> networks;
		private String publicKey;
		private String userName;
		private String startScriptBase64;
		private String architecture;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder contextNetwork(String contextNetwork) {
			this.contextNetwork = contextNetwork;
			return this;
		}

		public Builder cpu(String cpu) {
			this.cpu = cpu;
			return this;
		}

		public Builder graphicsAddress(String graphicsAddress) {
			this.graphicsAddress = graphicsAddress;
			return this;
		}

		public Builder graphicsType(String graphicsType) {
			this.graphicsType = graphicsType;
			return this;
		}

		public Builder imageId(String imageId) {
			this.imageId = imageId;
			return this;
		}
		
		public Builder diskSize(String diskSize) {
			this.diskSize = diskSize;
			return this;
		}
		
		public Builder memory(String memory) {
			this.memory = memory;
			return this;
		}

		public Builder networks(List<String> networks) {
			this.networks = networks;
			return this;
		}

		public Builder publicKey(String publicKey) {
			this.publicKey = publicKey;
			return this;
		}

		public Builder userName(String userName) {
			this.userName = userName;
			return this;
		}

		public Builder startScriptBase64(String startScriptBase64) {
			this.startScriptBase64 = startScriptBase64;
			return this;
		}

		public Builder architecture(String architecture) {
			this.architecture = architecture;
			return this;
		}

		public CreateComputeRequest build() {
			return new CreateComputeRequest(this);
		}
	}
}
