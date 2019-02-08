package cloud.fogbow.ras.core.plugins.interoperability.openstack;

import cloud.fogbow.common.models.CloudToken;

public class OpenStackV3Token extends CloudToken {
    private String projectId;

    public OpenStackV3Token(String tokenProviderId, String userId, String tokenValue, String projectId) {
        super(tokenProviderId, userId, tokenValue);
        this.projectId = projectId;
    }

    public String getProjectId() {
        return this.projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
