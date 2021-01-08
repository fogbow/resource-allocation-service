package cloud.fogbow.ras.core;

import java.util.Set;

import cloud.fogbow.common.models.Permission;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

public class PermissionInstantiator {
    private RasClassFactory classFactory;
    
    public PermissionInstantiator() {
        this.classFactory = new RasClassFactory();
    }
    
    public Permission<RasOperation> getPermissionInstance(String type, String ... params) {
        return (Permission<RasOperation>) this.classFactory.createPluginInstance(type, params);
    }
    
    public Permission<RasOperation> getPermissionInstance(String type, String name, Set<Operation> operations) {
        Permission<RasOperation> instance = (Permission<RasOperation>) this.classFactory.createPluginInstance(type);
        
        // Permission constructors require a Set as argument.
        // Since it is difficult to implement a ClassFactory able 
        // to use constructors that have interfaces in their
        // signatures, here we create Permissions using
        // the default constructor and then set the parameters.
        instance.setOperationTypes((Set) operations);
        instance.setName(name);
        return instance;
    }
}
