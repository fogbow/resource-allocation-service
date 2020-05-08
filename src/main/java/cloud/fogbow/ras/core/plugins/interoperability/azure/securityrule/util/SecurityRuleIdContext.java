package cloud.fogbow.ras.core.plugins.interoperability.azure.securityrule.util;

import org.apache.commons.lang.StringUtils;

public class SecurityRuleIdContext {

    private static final int NETWORK_SECURITY_GROUP_NAME_POSITION = 0;
    private static final int SECURITY_RULE_NAME_POSITION = 1;
    private static final int INSTANCE_ID_ARRAY_SIZE = 2;
    private static final String SEPARATOR = "_";

    private String networkSecurityGroupName;
    private String securityRuleName;

    public SecurityRuleIdContext(String securityRuleId) {
        String[] securityRuleInstanceIdChucks = securityRuleId.split(SEPARATOR);
        this.networkSecurityGroupName = securityRuleInstanceIdChucks[NETWORK_SECURITY_GROUP_NAME_POSITION];
        this.securityRuleName = securityRuleInstanceIdChucks[SECURITY_RULE_NAME_POSITION];
    }

    public static String buildInstanceId(String networkSecurityGroupName, String securityRuleName) {
        String[] instanceIdArray = new String[INSTANCE_ID_ARRAY_SIZE];
        instanceIdArray[NETWORK_SECURITY_GROUP_NAME_POSITION] = networkSecurityGroupName;
        instanceIdArray[SECURITY_RULE_NAME_POSITION] = securityRuleName;
        return StringUtils.join(instanceIdArray, SEPARATOR);
    }

    public String getNetworkSecurityGroupName() {
        return networkSecurityGroupName;
    }

    public String getSecurityRuleName() {
        return securityRuleName;
    }

}
