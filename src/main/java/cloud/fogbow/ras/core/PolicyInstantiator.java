package cloud.fogbow.ras.core;

import java.io.File;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.RolePolicy;
import cloud.fogbow.ras.core.models.policy.WrongPolicyTypeException;
import cloud.fogbow.ras.core.models.policy.XMLRolePolicy;

public class PolicyInstantiator {
    private RasClassFactory classFactory;

    public PolicyInstantiator() {
        this.classFactory = new RasClassFactory();
    }
    
    public RolePolicy<RasOperation> getRolePolicyInstance(String policyString) throws ConfigurationErrorException, WrongPolicyTypeException {
        String adminRole = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        
        String policyFileName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        String path = HomeDir.getPath();
        path += policyFileName;
        
        if (PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)) {
            String policyType = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
            return (RolePolicy<RasOperation>) this.classFactory.createPluginInstance(policyType, policyString, adminRole, path);            
        } else {
            return new XMLRolePolicy<RasOperation>(new RasPermissionInstantiator(), policyString, adminRole, path);
        }
    }
    
    public RolePolicy<RasOperation> getRolePolicyInstanceFromFile(String policyFileName) throws ConfigurationErrorException, WrongPolicyTypeException {
        File policyFile = new File(policyFileName);
        
        String adminRole = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        
        String path = HomeDir.getPath();
        path += policyFileName;
        
        if (PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)) {
            String policyType = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
            return (RolePolicy<RasOperation>) this.classFactory.createPluginInstance(policyType, policyFile, adminRole, path);            
        } else {
            return new XMLRolePolicy<RasOperation>(new RasPermissionInstantiator(), policyFile, adminRole, path);
        }
    }
}
