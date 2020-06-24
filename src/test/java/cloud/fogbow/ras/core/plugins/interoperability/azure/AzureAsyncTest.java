package cloud.fogbow.ras.core.plugins.interoperability.azure;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.api.http.response.OrderInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AsyncInstanceCreationManager;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureStateMapper;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Thread.class, AzureAsync.class})
public class AzureAsyncTest {

    private AzureWrapper azureWrapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private LoggerAssert loggerTestChecking = new LoggerAssert(AzureAsync.class);

    @Before
    public void setUp() {
        this.azureWrapper = Mockito.spy(new AzureWrapper());
    }

    // test case: When calling the startInstanceCreation method,
    // it must verify if It returns callbacks.
    @Test
    public void testStartInstanceCreationSuccessfully() {
        // set up
        String instanceId = TestUtils.ANY_VALUE;

        // exercise
        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = this.azureWrapper.startInstanceCreation(instanceId);

        // verify
        Assert.assertNotNull(finishCreationCallbacks);
    }

    // test case: When calling the endInstanceCreation method,
    // it must verify if It returns null to the CreatingInstance.
    @Test
    public void testEndInstanceCreationSuccessfully() throws InternalServerErrorException {
        // set up
        String instanceId = TestUtils.ANY_VALUE;

        // exercise before
        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = this.azureWrapper.startInstanceCreation(instanceId);

        // verify before
        OrderInstance creatingInstance = this.azureWrapper.getCreatingInstance(instanceId);
        Assert.assertEquals(AzureStateMapper.CREATING_STATE, creatingInstance.getCloudState());
        Assert.assertNotNull(finishCreationCallbacks);

        // exercise
        this.azureWrapper.endInstanceCreation(instanceId);

        // verify
        creatingInstance = this.azureWrapper.getCreatingInstance(instanceId);
        Assert.assertNull(creatingInstance);
    }

    // test case: When calling the getCreatingInstance method when it starts instance creation,
    // it must verify if It return instanceState Creating.
    @Test
    public void testGetCreatingInstanceSuccessfullyWhenIsCreating() throws InternalServerErrorException {
        // set up
        String instanceId = TestUtils.ANY_VALUE;

        this.azureWrapper.startInstanceCreation(instanceId);

        // exercise
        OrderInstance creatingInstance = this.azureWrapper.getCreatingInstance(instanceId);

        // verify
        Assert.assertEquals(AzureStateMapper.CREATING_STATE, creatingInstance.getCloudState());
    }

    // test case: When calling the getCreatingInstance method when it ends instance creation,
    // it must verify if It return instance null.
    @Test
    public void testGetCreatingInstanceSuccessfullyWhenIsNull() throws InternalServerErrorException {
        // set up
        String instanceId = TestUtils.ANY_VALUE;

        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = this.azureWrapper.startInstanceCreation(instanceId);
        finishCreationCallbacks.runOnComplete();

        // exercise
        OrderInstance creatingInstance = this.azureWrapper.getCreatingInstance(instanceId);

        // verify
        Assert.assertNull(creatingInstance);
    }

    // test case: When calling the getCreatingInstance method when it occurs a fail at instance creation,
    // it must verify if It return instanceState Failed.
    @Test
    public void testGetCreatingInstanceSuccessfullyWhenIsFailed() throws InternalServerErrorException {
        // set up
        String instanceId = TestUtils.ANY_VALUE;
        String faultMessageExpected = TestUtils.ANY_VALUE;

        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = this.azureWrapper.startInstanceCreation(instanceId);
        finishCreationCallbacks.runOnError(faultMessageExpected);
        finishCreationCallbacks.runOnComplete();

        // exercise
        OrderInstance creatingInstance = this.azureWrapper.getCreatingInstance(instanceId);

        // verify
        Assert.assertEquals(AzureStateMapper.FAILED_STATE, creatingInstance.getCloudState());
        Assert.assertEquals(faultMessageExpected, creatingInstance.getFaultMessage());
    }

    // test case: When calling the waitAndCheckForInstanceCreationFailed method and the status is null,
    // it must verify if It throw a InternalServerErrorException.
    @Test
    public void testWaitAndCheckForInstanceCreationFailedSuccessfullyWhenIsNull()
            throws InterruptedException, FogbowException {
        // set up
        String instanceId = TestUtils.EMPTY_STRING;

        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(Mockito.anyLong());

        // verify
        this.expectedException.expect(InternalServerErrorException.class);
        this.expectedException.expectMessage(Messages.Exception.UNEXPECTED_ERROR);

        // exercise
        this.azureWrapper.waitAndCheckForInstanceCreationFailed(instanceId);
    }

    // test case: When calling the waitAndCheckForInstanceCreationFailed method and there is a failed,
    // it must verify if It throw a FogbowException.
    @Test
    public void testWaitAndCheckForInstanceCreationFailedSuccessfullyWhenIsFailed()
            throws InterruptedException, FogbowException {
        // set up
        String instanceId = TestUtils.ANY_VALUE;

        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(Mockito.anyLong());

        AsyncInstanceCreationManager.Callbacks finishCreationCallbacks = this.azureWrapper.startInstanceCreation(instanceId);
        finishCreationCallbacks.runOnError(TestUtils.ANY_VALUE);

        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(Messages.Log.ERROR_ON_REQUEST_ASYNC_PLUGIN);

        // exercise
        this.azureWrapper.waitAndCheckForInstanceCreationFailed(instanceId);
    }

    // test case: When calling the waitAndCheckForInstanceCreationFailed method and there is not a failed,
    // it must verify if It does not throw a FogbowException.
    @Test
    public void testWaitAndCheckForInstanceCreationFailedSuccessfullyWhenIsNotFailed()
            throws InterruptedException {
        // set up
        String instanceId = TestUtils.ANY_VALUE;

        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(Mockito.anyLong());

        this.azureWrapper.startInstanceCreation(instanceId);

        try {
            // exercise
            this.azureWrapper.waitAndCheckForInstanceCreationFailed(instanceId);
        } catch (Throwable e) {
            Assert.fail();
        }

        // verify
        this.loggerTestChecking.verifyIfEmpty();
        PowerMockito.verifyStatic(Thread.class, VerificationModeFactory.times(TestUtils.RUN_ONCE));
        Thread.sleep(Mockito.anyLong());
    }

    // test case: When calling the waitAndCheckForInstanceCreationFailed method and there is not a failed,
    // it must verify if It does not throw a FogbowException.
    @Test
    public void testWaitAndCheckForInstanceCreationFailedFail()
            throws InterruptedException {
        // set up
        String instanceId = TestUtils.ANY_VALUE;

        PowerMockito.spy(Thread.class);
        PowerMockito.doThrow(new InterruptedException()).when(Thread.class);
        Thread.sleep(Mockito.anyLong());

        this.azureWrapper.startInstanceCreation(instanceId);

        try {
            // exercise
            this.azureWrapper.waitAndCheckForInstanceCreationFailed(instanceId);
        } catch (Throwable e) {
            // verify
            Assert.fail();
        }

        // verify
        this.loggerTestChecking.assertEqualsInOrder(Level.WARN, Messages.Log.SLEEP_THREAD_INTERRUPTED);
    }

    private class AzureWrapper implements AzureAsync<OrderInstance> {

        @Override
        public OrderInstance buildCreatingInstance(String instanceId) {
            return new OrderInstance(instanceId);
        }

    }

}
