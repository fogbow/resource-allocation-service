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
    // it must verify if the instanceId is creating;
    // Afterwards, when calling the runnable(finishCreationCallback),
    // it must verify if the instanceId is not creating.
    @Test
    public void testCreationContextSuccessfully() {
        // set up
        String instanceId = "instanceId";

        // exercise
        Runnable finishCreationCallback = this.asyncInstanceCreationManagerPlugin.startCreation(instanceId);

        // verify
        Assert.assertTrue(this.asyncInstanceCreationManagerPlugin.isCreating(instanceId));

        // exercise
        finishCreationCallback.run();

        // verify
        Assert.assertFalse(this.asyncInstanceCreationManagerPlugin.isCreating(instanceId));
    }

}
