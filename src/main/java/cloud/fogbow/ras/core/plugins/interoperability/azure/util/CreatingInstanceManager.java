package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
This class deals with instances(resources) not ready in the cloud. It happen in the asynchronous operation.
 */
public class CreatingInstanceManager {

    private static CreatingInstanceManager creatingInstanceManager;
    private List<String> creating;

    private CreatingInstanceManager() {
        this.creating = Collections.synchronizedList(new ArrayList<>());
    }

    public static CreatingInstanceManager getSingleton() {
        if (creatingInstanceManager == null) {
            creatingInstanceManager = new CreatingInstanceManager();
        }
        return creatingInstanceManager;
    }

    /*
    It must add the instanceId in creating list when the resource is not created in the cloud.
     */
    public void defineAsCreating(String instanceId) {
        this.creating.add(instanceId);
    }

    /*
    It must remove the instanceId in creating list when the resource is created in the cloud.
    */
    public void defineAsCreated(String instanceId) {
        this.creating.remove(instanceId);
    }

    /*
    It must check if the instance exists in the creating list.
    */
    public boolean isCreating(String instanceId) {
        return this.creating.contains(instanceId);
    }

}
