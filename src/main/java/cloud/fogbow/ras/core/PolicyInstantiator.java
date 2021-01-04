package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.models.RolePolicy;
import cloud.fogbow.ras.core.models.policy.SeparatorRolePolicy.WrongPolicyType;
import cloud.fogbow.ras.core.models.policy.XMLRolePolicy;

public class PolicyInstantiator {
    private RasClassFactory classFactory;

    public RolePolicy getRolePolicyInstance(String policyString) throws ConfigurationErrorException, WrongPolicyType {
        if (PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)) {
            String policyType = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
            return (RolePolicy) this.classFactory.createPluginInstance(policyType, policyString);            
        } else {
            return new XMLRolePolicy(policyString);
        }
    }
}
