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
        AsyncInstanceCreationManager.Callbacks finishCreationAsyncInstanceCreationCallbacks =
                this.asyncInstanceCreationManagerPlugin.startCreation(instanceId);

        // verify
        Assert.assertEquals(AsyncInstanceCreationManager.Status.CREATING,
                this.asyncInstanceCreationManagerPlugin.getStatus(instanceId));

        // exercise
        finishCreationAsyncInstanceCreationCallbacks.runOnComplete();

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
        AsyncInstanceCreationManager.Callbacks finishCreationAsyncInstanceCreationCallbacks =
                this.asyncInstanceCreationManagerPlugin.startCreation(instanceId);

        // verify
        AsyncInstanceCreationManager.Status status = this.asyncInstanceCreationManagerPlugin.getStatus(instanceId);
        Assert.assertEquals(AsyncInstanceCreationManager.Status.CREATING, status);

        // exercise
        finishCreationAsyncInstanceCreationCallbacks.runOnError();
        finishCreationAsyncInstanceCreationCallbacks.runOnComplete();

        // verify
        status = this.asyncInstanceCreationManagerPlugin.getStatus(instanceId);
        Assert.assertEquals(AsyncInstanceCreationManager.Status.FAILED, status);
    }

    // test case: When calling the endCreation method,
    // it must verify if the Status is null.
    @Test
    public void testEndCreationSuccessfully() {
        // set up
        String instanceId = "instanceId";

        // exercise
        this.asyncInstanceCreationManagerPlugin.endCreation(instanceId);
        this.asyncInstanceCreationManagerPlugin.startCreation(instanceId);

        // verify
        AsyncInstanceCreationManager.Status status = this.asyncInstanceCreationManagerPlugin.getStatus(instanceId);
        Assert.assertEquals(AsyncInstanceCreationManager.Status.CREATING, status);

        // exercise
        this.asyncInstanceCreationManagerPlugin.endCreation(instanceId);

        // verify
        status = this.asyncInstanceCreationManagerPlugin.getStatus(instanceId);
        Assert.assertNull(status);
    }

}
