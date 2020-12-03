package cloud.fogbow.ras.core;

import cloud.fogbow.common.models.Permission;
import cloud.fogbow.ras.core.models.RasOperation;

public class PermissionInstantiator {
    private RasClassFactory classFactory;
    
    public PermissionInstantiator() {
        this.classFactory = new RasClassFactory();
    }
    
    public Permission<RasOperation> getPermissionInstance(String type, String ... params) {
        return (Permission<RasOperation>) this.classFactory.createPluginInstance(type, params);
    }
}
