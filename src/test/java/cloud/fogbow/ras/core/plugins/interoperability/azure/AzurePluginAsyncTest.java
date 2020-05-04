package cloud.fogbow.ras.core.plugins.interoperability.azure;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AzurePluginAsyncTest {

    private Plugin plugin;

    @Before
    public void setUp() {
        this.plugin = new Plugin();
    }

    // test case: When calling the ** method,
    // it must verify if It **.
    @Test
    public void testFinishCreationSuccessfully() {
        // set up
        String instanceId = "instanceId";
        Runnable finishAsyncCreationCallback = this.plugin.startAsyncCreation(instanceId);

        // exercise
        boolean isCreatingAsync = this.plugin.isCreatingAsync(instanceId);

        // verify
        Assert.assertTrue(isCreatingAsync);

        // exercise
        finishAsyncCreationCallback.run();
        isCreatingAsync = this.plugin.isCreatingAsync(instanceId);

        // verify
        Assert.assertFalse(isCreatingAsync);
    }

    private class Plugin extends AzurePluginAsync {}

}
