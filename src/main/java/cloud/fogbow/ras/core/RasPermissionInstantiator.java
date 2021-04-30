package cloud.fogbow.ras.core;

import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.models.policy.Permission;
import cloud.fogbow.common.models.policy.PermissionInstantiator;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

public class RasPermissionInstantiator implements PermissionInstantiator<RasOperation> {
    private RasClassFactory classFactory;
    
    public RasPermissionInstantiator() {
        this.classFactory = new RasClassFactory();
    }
    
    @Override
    public Permission<RasOperation> getPermissionInstance(String type, String ... params) {
        return (Permission<RasOperation>) this.classFactory.createPluginInstance(type, params);
    }

    @Override
    public Permission<RasOperation> getPermissionInstance(String type, String name, Set<String> operations) {
        Permission<RasOperation> instance = (Permission<RasOperation>) this.classFactory.createPluginInstance(type);
        
        Set<Operation> rasOperations = new HashSet<Operation>();
        
        for (String operationName : operations) {
            rasOperations.add(Operation.fromString(operationName));
        }
        
        // Permission constructors require a Set as argument.
        // Since it is difficult to implement a ClassFactory able 
        // to use constructors that have interfaces in their
        // signatures, here we create Permissions using
        // the default constructor and then set the parameters.
        instance.setOperationTypes(rasOperations);
        instance.setName(name);
        return instance;
    }
}
