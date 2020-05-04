package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import java.util.*;

/*
This class deals with instances(resources) not ready in the cloud. It happen in the asynchronous operation.
It define as "Creating" status until the instance(resource) is not ready in the cloud.
 */
public class CreatingInstanceManager {

    /*
    It stores list of "Creating" instances related to each key. Unique instance.
     */
    private static Map<String, List<String>> creating = new HashMap<>();

    /*
    The "key" represent each list of "Creating" instance key.
     */
    private String key;

    public CreatingInstanceManager(String className) {
        this.key = className;
        this.creating.put(this.key, Collections.synchronizedList(new ArrayList<>()));
    }

    /*
    It must add the instanceId in "Creating" list when the resource is not created in the cloud.
     */
    public void defineAsCreating(String instanceId) {
        this.creating.get(this.key).add(instanceId);
    }

    /*
    It must remove the instanceId in "Creating" list when the resource is created in the cloud.
    */
    public void defineAsCreated(String instanceId) {
        this.creating.get(this.key).remove(instanceId);
    }

    /*
    It must check if the instance exists in the creating list.
    It means that the instance is still not ready in the cloud.
    */
    public boolean isCreating(String instanceId) {
        return this.creating.get(this.key).contains(instanceId);
    }

}
