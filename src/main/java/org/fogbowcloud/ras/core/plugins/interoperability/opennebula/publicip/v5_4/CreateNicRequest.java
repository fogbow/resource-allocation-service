package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

public class CreateNicRequest {

	private NicTemplate nic;

	public NicTemplate getNic() {
		return this.nic;
	}

	public CreateNicRequest(Builder builder) {
		String nicId = builder.nicId;
		String sgId = builder.sgId;
		this.nic = new NicTemplate(nicId, sgId);
	}

	public static class Builder {

		private String nicId;
		private String sgId;

		public Builder nicId(String nicId) {
			this.nicId = nicId;
			return this;
		}
		
		public Builder sgId(String sgId) {
			this.sgId = sgId;
			return this;
		}

		public CreateNicRequest build() {
			return new CreateNicRequest(this);
		}
	}
}
