package cloud.fogbow.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.ArrayList;
import java.util.List;

public class CreateComputeRequest {

	private VirtualMachineTemplate virtualMachine;

	public VirtualMachineTemplate getVirtualMachine() {
		return this.virtualMachine;
	}

	public CreateComputeRequest(Builder builder) {
		String cpu = builder.cpu;
		String memory = builder.memory;
		VirtualMachineTemplate.Context context = buildContext(builder);
		VirtualMachineTemplate.Graphics graphics = buildGraphics(builder);
		VirtualMachineTemplate.ImageDisk imageDisk = buildImage(builder);
		List<VirtualMachineTemplate.Nic> nicList = buildNics(builder);
		VirtualMachineTemplate.OperationalSystem os = buildOs(builder);
		
		this.virtualMachine = new VirtualMachineTemplate();
		this.virtualMachine.setContext(context);
		this.virtualMachine.setCpu(cpu);
		this.virtualMachine.setGraphics(graphics);
		this.virtualMachine.setImageDisk(imageDisk);
		this.virtualMachine.setMemory(memory);
		this.virtualMachine.setNicList(nicList);
		this.virtualMachine.setOperationalSystem(os);
	}

	private VirtualMachineTemplate.OperationalSystem buildOs(Builder builder) {
		VirtualMachineTemplate.OperationalSystem os = new VirtualMachineTemplate.OperationalSystem();
		os.setArchitecture(builder.architecture);
		return os;
	}
	
	private List<VirtualMachineTemplate.Nic> buildNics(Builder builder) {
		List<VirtualMachineTemplate.Nic> networks = new ArrayList<>();
		for (int i = 0; i < builder.networks.size(); i++) {
			VirtualMachineTemplate.Nic nic = new VirtualMachineTemplate.Nic();
			nic.setNetworkId(builder.networks.get(i));
			networks.add(nic);
		}
		return networks;
	}

	private VirtualMachineTemplate.ImageDisk buildImage(Builder builder) {
		VirtualMachineTemplate.ImageDisk imageDisk = new VirtualMachineTemplate.ImageDisk();
		imageDisk.setImageId(builder.imageId);
		return imageDisk;
	}

	private VirtualMachineTemplate.Graphics buildGraphics(Builder builder) {
		VirtualMachineTemplate.Graphics graphics = new VirtualMachineTemplate.Graphics();
		graphics.setListen(builder.graphicsListen);
		graphics.setType(builder.graphicsType);
		return graphics;
	}

	private VirtualMachineTemplate.Context buildContext(Builder builder) {
		VirtualMachineTemplate.Context context = new VirtualMachineTemplate.Context();
		context.setEncoding(builder.contextEncoding);
		context.setUserdata(builder.contextUserdata);
		context.setNetwork(builder.contextNetwork);
		return context;
	}

	public static class Builder {
		private String contextEncoding;
		private String contextUserdata;
		private String contextNetwork;
		private String cpu;
		private String graphicsListen;
		private String graphicsType;
		private String imageId;
		private String memory;
		private List<String> networks;
		private String architecture;
		
		public Builder contextEncoding(String contextEncoding) {
			this.contextEncoding = contextEncoding;
			return this;
		}

		public Builder contextUserdata(String contextUserdata) {
			this.contextUserdata = contextUserdata;
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

		public Builder graphicsListen(String graphicsListen) {
			this.graphicsListen = graphicsListen;
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

		public Builder memory(String memory) {
			this.memory = memory;
			return this;
		}

		public Builder networks(List<String> networks) {
			this.networks = networks;
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
