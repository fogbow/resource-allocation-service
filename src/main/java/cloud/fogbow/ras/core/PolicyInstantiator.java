package cloud.fogbow.ras.core;

import java.io.File;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.models.RolePolicy;
import cloud.fogbow.ras.core.models.policy.WrongPolicyTypeException;
import cloud.fogbow.ras.core.models.policy.XMLRolePolicy;

public class PolicyInstantiator {
    private RasClassFactory classFactory;

    public PolicyInstantiator() {
        this.classFactory = new RasClassFactory();
    }
    
    public RolePolicy getRolePolicyInstance(String policyString) throws ConfigurationErrorException, WrongPolicyTypeException {
        if (PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)) {
            String policyType = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
            return (RolePolicy) this.classFactory.createPluginInstance(policyType, policyString);            
        } else {
            return new XMLRolePolicy(policyString);
        }
    }
    
    public RolePolicy getRolePolicyInstanceFromFile(String policyFileName) throws ConfigurationErrorException, WrongPolicyTypeException {
        File policyFile = new File(policyFileName);
        
        if (PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)) {
            String policyType = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
            return (RolePolicy) this.classFactory.createPluginInstance(policyType, policyFile);            
        } else {
            return new XMLRolePolicy(policyFile);
        }
    }
}
