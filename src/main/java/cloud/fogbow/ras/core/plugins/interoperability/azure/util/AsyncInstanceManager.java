package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
This class manager resources(instances) not ready in the cloud. It happen in the asynchronous operation.
 */
public class AsyncInstanceManager {

    private static AsyncInstanceManager asyncInstanceManager;
    private List<String> pending;

    private AsyncInstanceManager() {
        this.pending = Collections.synchronizedList(new ArrayList<>());
    }

    public static AsyncInstanceManager getInstance() {
        if (asyncInstanceManager == null) {
            asyncInstanceManager = new AsyncInstanceManager();
        }
        return asyncInstanceManager;
    }

    /*
    It must add the instanceId in pending list when the resource is not ready in the cloud.
     */
    public void defineAsPending(String instanceId) {
        this.pending.add(instanceId);
    }

    /*
    It must remove the instanceId in pending list when the resource is ready in the cloud.
    */
    public void defineAsReady(String instanceId) {
        this.pending.remove(instanceId);
    }

    /*
    It must check if the instance exists pending in the pending list.
    */
    public boolean isPending(String instanceId) {
        return this.pending.contains(instanceId);
    }

}
