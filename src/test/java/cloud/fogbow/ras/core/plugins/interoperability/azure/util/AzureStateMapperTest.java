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
    // undefined state, it must verify if It returns the inconsistent instance
    // state.
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
    // must verify than it returns the inconsistent instance no matter your state.
    @Test
    public void testMapWithUnmappedResourceType() {
        // set up
        ResourceType resourceType = ResourceType.GENERIC_RESOURCE;

        // exercise
        InstanceState instanceState = AzureStateMapper.map(resourceType, AzureStateMapper.SUCCEEDED_STATE);

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
    }

}
