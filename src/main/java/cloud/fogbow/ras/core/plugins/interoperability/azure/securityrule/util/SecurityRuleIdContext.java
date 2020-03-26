package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util;

public class SecurityRuleIdContext {

    // TODO (chico) - Use the Fogbow pattern.
    private static final String SEPARATOR = "@-@";

    private String networkSecurityGroupName;
    private String securityRuleName;

    public SecurityRuleIdContext(String networkSecurityGroupName, String securityRuleName) {
        this.networkSecurityGroupName = networkSecurityGroupName;
        this.securityRuleName = securityRuleName;
    }

    public SecurityRuleIdContext(String securityRuleInstanceId) {
        String[] securityRuleInstanceIdChucks = securityRuleInstanceId.split(SEPARATOR);
        this.networkSecurityGroupName = securityRuleInstanceIdChucks[0];
        this.securityRuleName = securityRuleInstanceIdChucks[1];
    }

    public String buildInstanceId() {
        return this.networkSecurityGroupName + SEPARATOR + this.securityRuleName;
    }

    public String getNetworkSecurityGroupName() {
        return networkSecurityGroupName;
    }

    public String getSecurityRuleName() {
        return securityRuleName;
    }

}
