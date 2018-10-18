package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

public class CreateAddressRangeRequest {

	private AddressRange addressRange;
	
	public AddressRange getAddressRange() {
		return this.addressRange;
	}
	
	public CreateAddressRangeRequest(Builder builder) {
		String type = builder.type;
		String ip = builder.ip;
		String size = builder.size;
		this.addressRange = new AddressRange(type, ip, size);
	}
	
	public static class Builder {

		private String type;
		private String ip;
		private String size;
		
		public Builder type(String type) {
			this.type = type;
			return this;
		}
		
		public Builder ip(String ip) {
			this.ip = ip;
			return this;
		}
		
		public Builder size(String size) {
			this.size = size;
			return this;
		}
		
		public CreateAddressRangeRequest build() {
			return new CreateAddressRangeRequest(this);
		}
	}
}
