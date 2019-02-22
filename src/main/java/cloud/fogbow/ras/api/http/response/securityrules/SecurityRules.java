package cloud.fogbow.ras.api.http.response.securityrules;

import cloud.fogbow.common.models.FederationUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SecurityRules {

    private String instanceId;
    private FederationUser federationUser;
    private List<SecurityRule> rules;

    public SecurityRules() {
        rules = new ArrayList<>();
    }

    public SecurityRules(FederationUser federationUser, List<SecurityRule> rules) {
        this.federationUser = federationUser;
        this.rules = rules;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public FederationUser getFederationUser() {
        return federationUser;
    }

    public void setFederationUser(FederationUser federationUser) {
        this.federationUser = federationUser;
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
                Objects.equals(getFederationUser(), that.getFederationUser()) &&
                Objects.equals(getRules(), that.getRules());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInstanceId(), getFederationUser(), getRules());
    }

}
