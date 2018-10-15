package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.publicip.v5_4;

public class CreateNicRequest {

	private NicTemplate nic;

	public NicTemplate getNic() {
		return this.nic;
	}

	public CreateNicRequest(Builder builder) {
		String nicId = builder.nicId;
		this.nic = new NicTemplate(nicId);
	}

	public static class Builder {

		private String nicId;

		public Builder nicId(String nicId) {
			this.nicId = nicId;
			return this;
		}

		public CreateNicRequest build() {
			return new CreateNicRequest(this);
		}
	}
}
