package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

public class CreateComputeRequest {

	private VirtualMachineTemplate virtualMachine;

	public VirtualMachineTemplate getVirtualMachine() {
		return this.virtualMachine;
	}

	public CreateComputeRequest(Builder builder) {
		String cpu = builder.cpu;
		String memory = builder.memory;
		VirtualMachineContext context = buildContext(builder);
		VirtualMachineGraphics graphics = buildGraphics(builder);
		VirtualMachineImageDisk imageDisk = buildImage(builder);
		VirtualMachineVolumeDisk volumeDisk = buildVolume(builder);
		VirtualMachineNic nic = buildNetworkInterfaceConnected(builder);
		
		this.virtualMachine = new VirtualMachineTemplate();
		this.virtualMachine.setContext(context);
		this.virtualMachine.setCpu(cpu);
		this.virtualMachine.setGraphics(graphics);
		this.virtualMachine.setImageDisk(imageDisk);
		this.virtualMachine.setVolumeDisk(volumeDisk);
		this.virtualMachine.setMemory(memory);
		this.virtualMachine.setNetworkInterfaceConnected(nic);
	}

	private VirtualMachineNic buildNetworkInterfaceConnected(Builder builder) {
		VirtualMachineNic nic = new VirtualMachineNic();
		nic.setNetworkId(builder.networkId);
		return nic;
	}

	private VirtualMachineVolumeDisk buildVolume(Builder builder) {
		VirtualMachineVolumeDisk volumeDisk = new VirtualMachineVolumeDisk();
		volumeDisk.setSize(builder.volumeSize);
		volumeDisk.setType(builder.volumeType);
		return volumeDisk;
	}

	private VirtualMachineImageDisk buildImage(Builder builder) {
		VirtualMachineImageDisk imageDisk = new VirtualMachineImageDisk();
		imageDisk.setImageId(builder.imageId);
		return imageDisk;
	}

	private VirtualMachineGraphics buildGraphics(Builder builder) {
		VirtualMachineGraphics graphics = new VirtualMachineGraphics();
		graphics.setListen(builder.graphicsListen);
		graphics.setType(builder.graphicsType);
		return graphics;
	}

	private VirtualMachineContext buildContext(Builder builder) {
		VirtualMachineContext context = new VirtualMachineContext();
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
		private String volumeSize;
		private String volumeType;
		private String memory;
		private String networkId;

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

		public Builder volumeSize(String volumeSize) {
			this.volumeSize = volumeSize;
			return this;
		}

		public Builder volumeType(String volumeType) {
			this.volumeType = volumeType;
			return this;
		}

		public Builder memory(String memory) {
			this.memory = memory;
			return this;
		}

		public Builder networkId(String networkId) {
			this.networkId = networkId;
			return this;
		}

		public CreateComputeRequest build() {
			return new CreateComputeRequest(this);
		}
	}
}
