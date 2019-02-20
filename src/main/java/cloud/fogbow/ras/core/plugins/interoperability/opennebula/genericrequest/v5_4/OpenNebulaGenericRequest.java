package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;

import java.util.Map;

public class OpenNebulaGenericRequest implements GenericRequest {
    private String url;
	private String oneResource;
    private String oneMethod;
    private String resourceId;
    private Map<String, String> parameters;

    public OpenNebulaGenericRequest(String url, String oneResource, String oneMethod, String resourceId, Map<String, String> parameters) {
        this.url = url;
    	this.oneResource = oneResource;
        this.oneMethod = oneMethod;
        this.resourceId = resourceId;
        this.parameters = parameters;
    }

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getOneResource() {
		return oneResource;
	}

	public void setOneResource(String oneResource) {
		this.oneResource = oneResource;
	}

	public String getOneMethod() {
		return oneMethod;
	}

	public void setOneMethod(String oneMethod) {
		this.oneMethod = oneMethod;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

}
