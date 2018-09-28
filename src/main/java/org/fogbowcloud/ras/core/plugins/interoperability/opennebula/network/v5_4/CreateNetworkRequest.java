package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.network.v5_4;

public class CreateNetworkRequest {

	private VirtualNetworkTemplate virtualNetwork;
	
	public VirtualNetworkTemplate getVirtualNetwork() {
		return this.virtualNetwork;
	}
	
	public CreateNetworkRequest(Builder builder) {
		String name = builder.name;
		String description = builder.description;
		String type = builder.networkType;
		String bridge = builder.bridge;
		String networkAddress = builder.networkAddress;
		String networkGateway = builder.networkGateway;
		VirtualNetworkAddressRange addressRange = buildAddressRange(builder);
		
		this.virtualNetwork = new VirtualNetworkTemplate(
				name, 
				description, 
				type, 
				bridge, 
				networkAddress, 
				networkGateway, 
				addressRange);
	}
	
	private VirtualNetworkAddressRange buildAddressRange(Builder builder) {
		VirtualNetworkAddressRange addressRange = new VirtualNetworkAddressRange();
		addressRange.setType(builder.addressRangeType);
		addressRange.setIpAddress(builder.addressRangeIp);
		addressRange.setRangeSize(builder.addressRangeSize);
		return addressRange;
	}
	
	public static class Builder {
		private String name;
		private String description;
		private String networkType;
		private String bridge;
		private String networkAddress;
		private String networkGateway;
		private String addressRangeType;
		private String addressRangeIp;
		private String addressRangeSize;
		
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		public Builder setDescription(String description) {
			this.description = description;
			return this;
		}
		
		public Builder setNetworkType(String networkType) {
			this.networkType = networkType;
			return this;
		}
		
		public Builder setBridge(String bridge) {
			this.bridge = bridge;
			return this;
		}
		
		public Builder setNetworkAddress(String networkAddress) {
			this.networkAddress = networkAddress;
			return this;
		}
		
		public Builder setNetworkGateway(String networkGateway) {
			this.networkGateway = networkGateway;
			return this;
		}
		
		public Builder setAddressRangeType(String addressRangeType) {
			this.addressRangeType = addressRangeType;
			return this;
		}
		
		public Builder setAddressRangeIp(String addressRangeIp) {
			this.addressRangeIp = addressRangeIp;
			return this;
		}
		
		public Builder setAddressRangeSize(String addressRangeSize) {
			this.addressRangeSize = addressRangeSize;
			return this;
		}
		
		public CreateNetworkRequest build() {
			return new CreateNetworkRequest(this);
		}
	}
}
