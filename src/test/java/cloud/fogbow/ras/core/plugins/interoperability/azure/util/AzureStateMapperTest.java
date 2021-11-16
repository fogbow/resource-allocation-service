package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import org.junit.Assert;
import org.junit.Test;

public class AzureStateMapperTest {

    // test case: When calling the map method with a compute resource type and
    // creating state, it must verify than it returns the same instance state.
    @Test
    public void testMapWithComputeResourceTypeWhenStateIsCreating() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.CREATING_STATE);

        // verify
        Assert.assertEquals(InstanceState.CREATING, instanceState);
    }

    // test case: When calling the map method with a compute resource type and
    // succeeded state, it must verify than it returns the ready instance state.
    @Test
    public void testMapWithComputeResourceTypeWhenStateIsSucceeded() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.SUCCEEDED_STATE);

        // verify
        Assert.assertEquals(InstanceState.READY, instanceState);
    }

    // test case: When calling the map method with a compute resource type and
    // failed state, it must verify than it returns the failed instance state.
    @Test
    public void testMapWithComputeResourceTypeWhenStateIsFailed() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.FAILED_STATE);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }
    
    // test case: When calling the map method with a compute resource type and
    // deallocating state, it must verify that it returns the stopping instance state.
    @Test
    public void testMapWithComputeResourceTypeWhenStateIsDeallocating() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.DEALLOCATING_STATE);

        // verify
        Assert.assertEquals(InstanceState.STOPPING, instanceState);        
    }
    
    // test case: When calling the map method with a compute resource type and
    // deallocated state, it must verify that it returns the stopped instance state.
    @Test
    public void testMapWithComputeResourceTypeWhenStateIsDeallocated() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.DEALLOCATED_STATE);

        // verify
        Assert.assertEquals(InstanceState.STOPPED, instanceState);        
    }
    
    // test case: When calling the map method with a compute resource type and
    // starting state, it must verify that it returns the resuming instance state.
    @Test
    public void testMapWithComputeResourceTypeWhenStateIsStarting() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.STARTING_STATE);

        // verify
        Assert.assertEquals(InstanceState.RESUMING, instanceState);        
    }

    // test case: When calling the map method with compute type and undefined state,
    // it must verify if It returns the instance inconsistent state.
    @Test
    public void testMapWithComputeResourceTypeWhenStateIsUndefined() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureTestUtils.UNDEFINED_STATE);

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
    }
    
    // test case: When calling the map method with a volume resource type and
    // creating state, it must verify than it returns this same instance state.
    @Test
    public void testMapWithVolumeResourceTypeWhenStateIsCreating() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.CREATING_STATE);

        // verify
        Assert.assertEquals(InstanceState.CREATING, instanceState);
    }
    
    // test case: When calling the map method with a volume resource type and
    // succeeded state, it must verify than it returns the ready instance state.
    @Test
    public void testMapWithVolumeResourceTypeWhenStateIsSucceeded() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.SUCCEEDED_STATE);

        // verify
        Assert.assertEquals(InstanceState.READY, instanceState);
    }

    // test case: When calling the map method with a volume resource type and failed
    // state, it must verify than it returns the failed instance state.
    @Test
    public void testMapWithVolumeResourceTypeWhenStateIsFailed() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.FAILED_STATE);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }
    
    // test case: When calling the map method with a volume resource type and a
    // undefined state, it must verify than it returns the inconsistent instance
    // state.
    @Test
    public void testMapWithVolumeResourceTypeWhenStateIsUndefined() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureTestUtils.UNDEFINED_STATE);

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
    }
    
    // test case: When calling the map method with an unmapped resource type, it
    // must verify than it returns the inconsistent instance no matter its state.
    @Test
    public void testMapWithUnmappedResourceType() {
        // set up
        ResourceType resourceType = ResourceType.INVALID_RESOURCE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.SUCCEEDED_STATE);

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
    }

    // test case: When calling the map method with network type and undefined state,
    // it must verify if It returns the instance inconsistent state.
    @Test
    public void testMapSuccessfullyWhenNetworkAndUndefinedState() {
        // set up
        ResourceType resourceType = ResourceType.NETWORK;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, "undefined");

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
    }

    // test case: When calling the map method with a network resource type and failed
    // state, it must verify than it returns the failed instance state.
    @Test
    public void testMapSuccessfullyWhenNetworkAndIsFailed() {
        // set up
        ResourceType resourceType = ResourceType.NETWORK;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.FAILED_STATE);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }

    // test case: When calling the map method with a network resource type and creating
    // state, it must verify than it returns the failed instance state.
    @Test
    public void testMapSuccessfullyWhenNetworkAndIsCreating() {
        // set up
        ResourceType resourceType = ResourceType.NETWORK;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.CREATING_STATE);

        // verify
        Assert.assertEquals(InstanceState.CREATING, instanceState);
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

    // test case: When calling the map method with network type and creating state,
    // it must verify if It returns the instance ready state.
    @Test
    public void testMapSuccessfullyWhenNetworkAndSucceededState() {
        // set up
        ResourceType resourceType = ResourceType.NETWORK;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.SUCCEEDED_STATE);

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
    
    // test case: When calling the map method with a public IP resource type and
    // creating state, it must verify than it returns this same instance state.
    @Test
    public void testMapWithPublicIpResourceTypeWhenStateIsCreating() {
        // set up
        ResourceType resourceType = ResourceType.PUBLIC_IP;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.CREATING_STATE);

        // verify
        Assert.assertEquals(InstanceState.CREATING, instanceState);
    }

    // test case: When calling the map method with a public IP resource type and
    // succeeded state, it must verify than it returns the ready instance state.
    @Test
    public void testMapWithPublicIpResourceTypeWhenStateIsSucceeded() {
        // set up
        ResourceType resourceType = ResourceType.PUBLIC_IP;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.SUCCEEDED_STATE);

        // verify
        Assert.assertEquals(InstanceState.READY, instanceState);
    }

    // test case: When calling the map method with a public IP resource type and failed
    // state, it must verify than it returns the failed instance state.
    @Test
    public void testMapWithPublicIpResourceTypeWhenStateIsFailed() {
        // set up
        ResourceType resourceType = ResourceType.PUBLIC_IP;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.FAILED_STATE);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }

    // test case: When calling the map method with a public IP resource type and a
    // undefined state, it must verify than it returns the inconsistent instance
    // state.
    @Test
    public void testMapWithPublicIpResourceTypeWhenStateIsUndefined() {
        // set up
        ResourceType resourceType = ResourceType.PUBLIC_IP;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureTestUtils.UNDEFINED_STATE);

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
    }

}
