package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
This class deals with instances(resources) not ready in the cloud. It happen in the asynchronous operation.
 */
public class PendingInstanceManager {

    private static PendingInstanceManager pendingInstanceManager;
    private List<String> pending;

    private PendingInstanceManager() {
        this.pending = Collections.synchronizedList(new ArrayList<>());
    }

    public static PendingInstanceManager getSingleton() {
        if (pendingInstanceManager == null) {
            pendingInstanceManager = new PendingInstanceManager();
        }
        return pendingInstanceManager;
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
