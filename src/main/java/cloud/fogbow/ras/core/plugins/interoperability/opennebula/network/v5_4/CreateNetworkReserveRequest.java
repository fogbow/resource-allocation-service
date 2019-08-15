package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

public class CreateNetworkReserveRequest {

	private VirtualNetworkReserveTemplate virtualNetworkReserved;
	
	public VirtualNetworkReserveTemplate getVirtualNetworkReserved() {
		return virtualNetworkReserved;
	}
	
	public CreateNetworkReserveRequest(Builder builder) {
		String name = builder.name;
		String ip = builder.ip;
		String addressRangeId = builder.addressRangeId;
		int size = builder.size;
		
		this.virtualNetworkReserved = new VirtualNetworkReserveTemplate();
		this.virtualNetworkReserved.setName(name);
		this.virtualNetworkReserved.setIp(ip);
		this.virtualNetworkReserved.setAddressRangeId(addressRangeId);
		this.virtualNetworkReserved.setSize(size);
	}

	public static class Builder {
		private String name;
		private String ip;
		private String addressRangeId;
		private int size;
		
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder ip(String ip) {
			this.ip = ip;
			return this;
		}

		public Builder addressRangeId(String addressRangeId) {
			this.addressRangeId = addressRangeId;
			return this;
		}

		public Builder size(int size) {
			this.size = size;
			return this;
		}
		
		public CreateNetworkReserveRequest build() {
			return new CreateNetworkReserveRequest(this);
		}
	}
	
}
