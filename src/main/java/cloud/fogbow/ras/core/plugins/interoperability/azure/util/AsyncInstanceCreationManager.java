package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import java.util.*;

/*
This class helps to cope Plugin that it has asynchronous instances creation.

Problem: It might generate resource trash in the cloud due to the fact that
If the RAS shutdown this class will lost its data in memory.

TODO(chico) - Create the issue in the GitHub
Note: This context helps to fix this issue:

 */
public class AsyncInstanceCreationManager {

    // It stores list of "Creating" instances related to each plugin.
    private final static Map<String, List<String>> creating = new HashMap<>();
    private final String pluginNameKey;

    public AsyncInstanceCreationManager(Class plugin) {
        this.pluginNameKey = plugin.getSimpleName();
        this.creating.put(this.pluginNameKey, Collections.synchronizedList(new ArrayList<>()));
    }

    /*
    It must be used soon before the plugin makes asynchronous creation operation in the cloud.

    @return It is a callback that it must be used when the cloud finish asynchronous creation operation in the cloud;
    Suggestion: Use "finishCreationCallback" as attribute name.
       */
    public Runnable startCreation(String instanceId) {
        defineAsCreating(instanceId);
        return () -> defineAsCreated(instanceId);
    }

    /*
    It must check if the instance is still creating by the asynchronous creation operation in the cloud.
    */
    public boolean isCreating(String instanceId) {
        return this.creating.get(this.pluginNameKey).contains(instanceId);
    }

    /*
    It must add the instanceId in "Creating" list when the resource is not created in the cloud.
     */
    private void defineAsCreating(String instanceId) {
        this.creating.get(this.pluginNameKey).add(instanceId);
    }

    /*
    It must remove the instanceId in "Creating" list when the resource is created in the cloud.
    */
    private void defineAsCreated(String instanceId) {
        this.creating.get(this.pluginNameKey).remove(instanceId);
    }

}
