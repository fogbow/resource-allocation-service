package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.network.v5_4;

public class CreateNetworkRequest {

	private VirtualNetworkTemplate virtualNetwork;
	
	public VirtualNetworkTemplate getVirtualNetwork() {
		return this.virtualNetwork;
	}
	
	public CreateNetworkRequest(Builder builder) {
		String name = builder.name;
		String description = builder.description;
		String type = builder.type;
		String bridge = builder.bridge;
		String address = builder.address;
		String gateway = builder.gateway;
		VirtualNetworkAddressRange addressRange = buildAddressRange(builder);
		
		this.virtualNetwork = new VirtualNetworkTemplate(
				name, 
				description, 
				type, 
				bridge, 
				address, 
				gateway, 
				addressRange);
	}
	
	private VirtualNetworkAddressRange buildAddressRange(Builder builder) {
		VirtualNetworkAddressRange addressRange = new VirtualNetworkAddressRange();
		addressRange.setType(builder.rangeType);
		addressRange.setIpAddress(builder.rangeIp);
		addressRange.setRangeSize(builder.rangeSize);
		return addressRange;
	}
	
	public static class Builder {
		private String name;
		private String description;
		private String type;
		private String bridge;
		private String address;
		private String gateway;
		private String rangeType;
		private String rangeIp;
		private String rangeSize;
		
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		public Builder description(String description) {
			this.description = description;
			return this;
		}
		
		public Builder type(String type) {
			this.type = type;
			return this;
		}
		
		public Builder bridge(String bridge) {
			this.bridge = bridge;
			return this;
		}
		
		public Builder address(String address) {
			this.address = address;
			return this;
		}
		
		public Builder gateway(String gateway) {
			this.gateway = gateway;
			return this;
		}
		
		public Builder rangeType(String rangeType) {
			this.rangeType = rangeType;
			return this;
		}
		
		public Builder rangeIp(String rangeIp) {
			this.rangeIp = rangeIp;
			return this;
		}
		
		public Builder rangeSize(String rangeSize) {
			this.rangeSize = rangeSize;
			return this;
		}
		
		public CreateNetworkRequest build() {
			return new CreateNetworkRequest(this);
		}
	}
}
