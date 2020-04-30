package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import java.util.*;

/*
This class deals with instances(resources) not ready in the cloud. It happen in the asynchronous operation.
 */
public class CreatingInstanceManager {

    private static Map<String, List<String>> creating;
    static {
        creating = new HashMap<>();
    }

    private String key;

    public CreatingInstanceManager(String className) {
        this.key = className;
        this.creating.put(this.key, Collections.synchronizedList(new ArrayList<>()));
    }

    /*
    It must add the instanceId in creating list when the resource is not created in the cloud.
     */
    public void defineAsCreating(String instanceId) {
        this.creating.get(this.key).add(instanceId);
    }

    /*
    It must remove the instanceId in creating list when the resource is created in the cloud.
    */
    public void defineAsCreated(String instanceId) {
        this.creating.get(this.key).remove(instanceId);
    }

    /*
    It must check if the instance exists in the creating list.
    */
    public boolean isCreating(String instanceId) {
        return this.creating.get(this.key).contains(instanceId);
    }

}
