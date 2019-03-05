package cloud.fogbow.ras.api.http.response.securityrules;

import cloud.fogbow.common.models.SystemUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SecurityRules {
    private String instanceId;
    private SystemUser systemUser;
    private List<SecurityRule> rules;

    public SecurityRules() {
        rules = new ArrayList<>();
    }

    public SecurityRules(SystemUser systemUser, List<SecurityRule> rules) {
        this.systemUser = systemUser;
        this.rules = rules;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public SystemUser getSystemUser() {
        return systemUser;
    }

    public void setSystemUser(SystemUser systemUser) {
        this.systemUser = systemUser;
    }

    public List<SecurityRule> getRules() {
        return rules;
    }

    public void setRules(List<SecurityRule> rules) {
        this.rules = rules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityRules that = (SecurityRules) o;
        return Objects.equals(getInstanceId(), that.getInstanceId()) &&
                Objects.equals(getSystemUser(), that.getSystemUser()) &&
                Objects.equals(getRules(), that.getRules());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInstanceId(), getSystemUser(), getRules());
    }

}
