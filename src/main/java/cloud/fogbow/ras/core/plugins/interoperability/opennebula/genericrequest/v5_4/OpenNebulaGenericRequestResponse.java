package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import cloud.fogbow.common.util.connectivity.GenericRequestResponse;

public class OpenNebulaGenericRequestResponse extends GenericRequestResponse {
	private String message;
	private boolean isError;

	public OpenNebulaGenericRequestResponse(String message) {
		// NOTE(pauloewerton): either 'message' or 'errorMessage' should be passed as
		// the response content.
		super(message);
	}

	public OpenNebulaGenericRequestResponse(String message, boolean isError) {
		super(message);
		this.message = message;
		this.isError = isError;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public boolean isError() {
		return isError;
	}

	public void setError(boolean isError) {
		this.isError = isError;
	}

}
