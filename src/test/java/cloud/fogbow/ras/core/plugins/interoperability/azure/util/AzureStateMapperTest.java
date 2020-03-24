package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;

import org.junit.Assert;
import org.junit.Test;

public class AzureStateMapperTest {

    // test case: When calling the map method with compute type and creating state,
    // it must verify if It returns the instance creating state.
    @Test
    public void testMapSuccessfullyWhenCreatingState() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.CREATING_STATE);

        // verify
        Assert.assertEquals(InstanceState.CREATING, instanceState);
    }

    // test case: When calling the map method with compute type and creating state,
    // it must verify if It returns the instance ready state.
    @Test
    public void testMapSuccessfullyWhenSucceededState() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.SUCCEEDED_STATE);

        // verify
        Assert.assertEquals(InstanceState.READY, instanceState);
    }

    // test case: When calling the map method with compute type and failed state,
    // it must verify if It returns the instance failed state.
    @Test
    public void testMapSuccessfullyWhenFailedState() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.FAILED_STATE);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }

    // test case: When calling the map method with compute type and creating state,
    // it must verify if It returns the instance inconsistent state.
    @Test
    public void testMapSuccessfullyWhenUndefinedState() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, "undefined");

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
    }

    // test case: When calling the map method with a attachment resource type and
    // attached state, it must verify than it returns the ready instance state.
    @Test
    public void testMapWithAttachmentResourceTypeWhenStateIsAttached() {
        // set up
        ResourceType resourceType = ResourceType.ATTACHMENT;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.ATTACHED_STATE);

        // verify
        Assert.assertEquals(InstanceState.READY, instanceState);
    }
    
    // test case: When calling the map method with a attachment resource type and
    // unattached state, it must verify than it returns the failed instance state.
    @Test
    public void testMapWithAttachmentResourceTypeWhenStateIsUnattached() {
        // set up
        ResourceType resourceType = ResourceType.ATTACHMENT;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.UNATTACHED_STATE);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }
    
    // test case: When calling the map method with a attachment resource type and
    // failed state, it must verify than it returns this same instance state.
    @Test
    public void testMapWithAttachmentResourceTypeWhenStateIsFailed() {
        // set up
        ResourceType resourceType = ResourceType.ATTACHMENT;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.FAILED_STATE);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }
    
    // test case: When calling the map method with a attachment resource type and a
    // undefined state, it must verify than it returns the inconsistent instance
    // state.
    @Test
    public void testMapWithAttachmentResourceTypeWhenStateIsUndefined() {
        // set up
        ResourceType resourceType = ResourceType.ATTACHMENT;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureTestUtils.UNDEFINED_STATE);

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
    }
    
}
