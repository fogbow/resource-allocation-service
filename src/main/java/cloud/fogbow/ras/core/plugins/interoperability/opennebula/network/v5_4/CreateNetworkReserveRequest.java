package cloud.fogbow.ras.core.plugins.interoperability.opennebula.network.v5_4;

public class CreateNetworkReserveRequest {

	private VirtualNetworkReserveTemplate virtualNetworkReserved;
	
	public VirtualNetworkReserveTemplate getVirtualNetworkReserved() {
		return virtualNetworkReserved;
	}
	
	public CreateNetworkReserveRequest(Builder builder) {
		String name = builder.name;
		int size = builder.size;
		
		this.virtualNetworkReserved = new VirtualNetworkReserveTemplate();
		this.virtualNetworkReserved.setName(name);
		this.virtualNetworkReserved.setSize(size);
	}

	public static class Builder {
		private String name;
		private int size;
		
		public Builder name(String name) {
			this.name = name;
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
