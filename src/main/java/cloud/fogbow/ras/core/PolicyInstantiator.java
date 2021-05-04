package cloud.fogbow.ras.core;

import java.io.File;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.WrongPolicyTypeException;
import cloud.fogbow.common.models.policy.PermissionInstantiator;
import cloud.fogbow.common.models.policy.RolePolicy;
import cloud.fogbow.common.models.policy.XMLRolePolicy;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.models.RasOperation;

public class PolicyInstantiator {
    private RasClassFactory classFactory;

    public PolicyInstantiator() {
        this.classFactory = new RasClassFactory();
    }
    
    public RolePolicy<RasOperation> getRolePolicyInstance(String policyString) throws ConfigurationErrorException, WrongPolicyTypeException {
        // TODO check if the key exists
        String adminRole = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        
        String policyFileName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        String path = HomeDir.getPath();
        path += policyFileName;
        
        if (PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)) {
            String policyType = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
            return (RolePolicy<RasOperation>) this.classFactory.createPluginInstance(policyType, new PermissionInstantiator<RasOperation>(classFactory), policyString, 
                    adminRole, path);            
        } else {
            return new XMLRolePolicy<RasOperation>(new PermissionInstantiator<RasOperation>(classFactory), policyString, adminRole, path);
        }
    }
    
    public RolePolicy<RasOperation> getRolePolicyInstanceFromFile(String policyFileName) throws ConfigurationErrorException, WrongPolicyTypeException {
        File policyFile = new File(policyFileName);
        
        // TODO check if the key exists
        String adminRole = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        
        String path = HomeDir.getPath();
        path += policyFileName;
        
        if (PropertiesHolder.getInstance().getProperties().containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)) {
            String policyType = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
            return (RolePolicy<RasOperation>) this.classFactory.createPluginInstance(policyType, new PermissionInstantiator<RasOperation>(classFactory), policyFile, 
                    adminRole, path);            
        } else {
            return new XMLRolePolicy<RasOperation>(new PermissionInstantiator<RasOperation>(classFactory), policyFile, adminRole, path);
        }
    }
}
