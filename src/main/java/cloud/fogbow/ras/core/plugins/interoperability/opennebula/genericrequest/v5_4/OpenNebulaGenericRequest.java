package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;

import java.util.Map;

public class OpenNebulaGenericRequest implements GenericRequest {
    private String oneResource;
    private String method;
    private String resourceId;
    private Map<String, String> params;

    public OpenNebulaGenericRequest(String oneResource, String method, String resourceId, Map<String, String> params) {
        this.oneResource = oneResource;
        this.method = method;
        this.resourceId = resourceId;
        this.params = params;
    }

    public String getOneResource() {
        return oneResource;
    }

    public void setOneResource(String oneResource) {
        this.oneResource = oneResource;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }
}
