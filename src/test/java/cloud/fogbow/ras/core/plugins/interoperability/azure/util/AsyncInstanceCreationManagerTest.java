package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AsyncInstanceCreationManagerTest {

    private AsyncInstanceCreationManager asyncInstanceCreationManagerPlugin;

    @Before
    public void setUp() {
        this.asyncInstanceCreationManagerPlugin = new AsyncInstanceCreationManager();
    }

    // test case: When calling the startCreation method,
    // it must verify if it has a Creating status;
    // Afterwards, when calling the callback and the method runOnComplete,
    // it must verify if the status is null because it was finished.
    @Test
    public void testCreationContextSuccessfully() {
        // set up
        String instanceId = "instanceId";

        // exercise
        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks =
                this.asyncInstanceCreationManagerPlugin.startCreation(instanceId);

        // verify
        Assert.assertEquals(AsyncInstanceCreationManager.Status.CREATING, this.asyncInstanceCreationManagerPlugin.getStatus(instanceId));

        // exercise
        finishCreationCallbacks.runOnComplete();

        // verify
        Assert.assertNull(this.asyncInstanceCreationManagerPlugin.getStatus(instanceId));
    }

    // test case: When calling the startCreation method,
    // it must verify if it has a Creating status;
    // Afterwards, when calling the callback and the method runOnError,
    // it must verify if the status is FAILED;
    @Test
    public void testCreationContextSuccessfullyWhenInstanceCreationFails() {
        // set up
        String instanceId = "instanceId";

        // exercise
        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks =
                this.asyncInstanceCreationManagerPlugin.startCreation(instanceId);

        // verify
        AsyncInstanceCreationManager.Status status = this.asyncInstanceCreationManagerPlugin.getStatus(instanceId);
        Assert.assertEquals(AsyncInstanceCreationManager.Status.CREATING, status);

        // exercise
        finishCreationCallbacks.runOnError();

        // verify
        status = this.asyncInstanceCreationManagerPlugin.getStatus(instanceId);
        Assert.assertEquals(AsyncInstanceCreationManager.Status.FAILED, status);
    }

}
