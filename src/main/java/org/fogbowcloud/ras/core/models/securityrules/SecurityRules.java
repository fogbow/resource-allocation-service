package org.fogbowcloud.ras.core.models.securityrules;

import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SecurityRules {

    private String instanceId;
    private FederationUserToken federationUserToken;
    private List<SecurityRule> rules;

    public SecurityRules() {
        rules = new ArrayList<>();
    }

    public SecurityRules(FederationUserToken federationUserToken, List<SecurityRule> rules) {
        this.federationUserToken = federationUserToken;
        this.rules = rules;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public FederationUserToken getFederationUserToken() {
        return federationUserToken;
    }

    public void setFederationUserToken(FederationUserToken federationUserToken) {
        this.federationUserToken = federationUserToken;
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
                Objects.equals(getFederationUserToken(), that.getFederationUserToken()) &&
                Objects.equals(getRules(), that.getRules());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInstanceId(), getFederationUserToken(), getRules());
    }

}
