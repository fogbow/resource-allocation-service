package cloud.fogbow.ras.core;

import java.util.Set;

import cloud.fogbow.common.models.Permission;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.permission.AllowAllExceptPermission;
import cloud.fogbow.ras.core.models.permission.AllowOnlyPermission;

public class PermissionInstantiator {
    private RasClassFactory classFactory;
    
    public PermissionInstantiator() {
        this.classFactory = new RasClassFactory();
    }
    
    public Permission<RasOperation> getPermissionInstance(String type, String ... params) {
        return (Permission<RasOperation>) this.classFactory.createPluginInstance(type, params);
    }
    
    public Permission<RasOperation> getPermissionInstance(String type, String name, Set<Operation> operations) {
        Permission<RasOperation> permission;
        
        // FIXME must change classfactory.createPluginInstance method to allow this operation
        switch (type) {
            case "cloud.fogbow.ras.core.models.permission.AllowAllExceptPermission": 
                permission = new AllowAllExceptPermission(name, operations); break;
            case "cloud.fogbow.ras.core.models.permission.AllowOnlyPermission": 
                permission = new AllowOnlyPermission(name, operations); break;
            default: permission = null;
        }
        
        return permission;
    }
}
