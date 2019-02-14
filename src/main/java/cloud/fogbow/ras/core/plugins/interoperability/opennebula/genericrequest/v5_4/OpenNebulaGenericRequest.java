package cloud.fogbow.ras.core.plugins.interoperability.opennebula.genericrequest.v5_4;

import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;

import java.util.Map;

public class OpenNebulaGenericRequest extends GenericRequest {
    private String poolElement;
    private String method;
    private String resourceId;
    private Map<String, String> params;

    public OpenNebulaGenericRequest() {
    }

    public OpenNebulaGenericRequest(String poolElement, String method, String resourceId, Map<String, String> params) {
        this.poolElement = poolElement;
        this.method = method;
        this.resourceId = resourceId;
        this.params = params;
    }

    public String getPoolElement() {
        return poolElement;
    }

    public void setPoolElement(String poolElement) {
        this.poolElement = poolElement;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
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
