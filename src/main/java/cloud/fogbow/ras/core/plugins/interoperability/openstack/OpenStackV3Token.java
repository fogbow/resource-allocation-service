package cloud.fogbow.ras.core.plugins.interoperability.openstack;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;

public class OpenStackV3Token extends CloudToken {
    private String projectId;

    public OpenStackV3Token(FederationUser federationUser) throws UnexpectedException {
        super(federationUser);
        this.projectId = federationUser.getExtraAttribute(OpenStackConstants.Identity.PROJECT_KEY_JSON);
    }

    public String getProjectId() {
        return this.projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
}
