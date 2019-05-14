package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.securityrules.SecurityRule;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;

import java.util.List;
import java.util.Properties;

public class AwsV2SecurityRulePlugin implements SecurityRulePlugin<AwsV2User> {

    private String region;
    private Properties properties;

    public AwsV2SecurityRulePlugin(String confFilePath) {
        this.properties = PropertiesUtil.readProperties(confFilePath);
        this.region = this.properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
    }

    public String requestSecurityRule(SecurityRule securityRule, Order majorOrder, AwsV2User cloudUser) throws FogbowException {
        return null;
    }

    public List<SecurityRule> getSecurityRules(Order majorOrder, AwsV2User cloudUser) throws FogbowException {
        return null;
    }

    public void deleteSecurityRule(String securityRuleId, AwsV2User cloudUser) throws FogbowException {

    }
}
