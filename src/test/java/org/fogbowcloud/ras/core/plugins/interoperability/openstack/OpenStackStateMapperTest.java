package org.fogbowcloud.ras.core.plugins.interoperability.openstack;

import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.junit.Assert;
import org.junit.Test;

public class OpenStackStateMapperTest {

    @Test
    public void testComputeActiveStatusToReady() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.ACTIVE_STATUS);

        // verify
        Assert.assertEquals(InstanceState.READY, instanceState);
    }

    @Test
    public void testComputeBuildStatusToSpawning() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.BUILD_STATUS);

        // verify
        Assert.assertEquals(InstanceState.SPAWNING, instanceState);
    }

    @Test
    public void testComputeErrorStatusToFailed() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.ERROR_STATUS);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }

    @Test
    public void testComputeInvalidStatusToInconsistent() {
        // set up
        ResourceType resourceType = ResourceType.COMPUTE;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, "invalid");
        InstanceState instanceState2 = OpenStackStateMapper.map(resourceType, "INVALID");

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState2);
    }

    @Test
    public void testNetworkBuildStatusToInactive() {
        // set up
        ResourceType resourceType = ResourceType.NETWORK;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.BUILD_STATUS);

        // verify
        Assert.assertEquals(InstanceState.INACTIVE, instanceState);
    }

    @Test
    public void testNetworkDownStatusToInactive() {
        // set up
        ResourceType resourceType = ResourceType.NETWORK;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.DOWN_STATUS);

        // verify
        Assert.assertEquals(InstanceState.INACTIVE, instanceState);
    }

    @Test
    public void testNetworkActiveStatusToReady() {
        // set up
        ResourceType resourceType = ResourceType.NETWORK;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.ACTIVE_STATUS);

        // verify
        Assert.assertEquals(InstanceState.READY, instanceState);
    }

    @Test
    public void testNetworkErrorStatusToFailed() {
        // set up
        ResourceType resourceType = ResourceType.NETWORK;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.ERROR_STATUS);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }

    @Test
    public void testNetworkInvalidStatusToFailed() {
        // set up
        ResourceType resourceType = ResourceType.NETWORK;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, "invalid");
        InstanceState instanceState2 = OpenStackStateMapper.map(resourceType, "INVALID");

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState2);
    }

    @Test
    public void testVolumeCreatingStatusToCreating() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.CREATING_STATUS);

        // verify
        Assert.assertEquals(InstanceState.CREATING, instanceState);
    }

    @Test
    public void testVolumeAvailableStatusToReady() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.AVAILABLE_STATUS);

        // verify
        Assert.assertEquals(InstanceState.READY, instanceState);
    }

    @Test
    public void testVolumeDetachingStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.DETACHING_STATUS);

        // verify
        Assert.assertEquals(InstanceState.UNAVAILABLE, instanceState);
    }

    @Test
    public void testVolumeMaintenceStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.MAINTENANCE_STATUS);

        // verify
        Assert.assertEquals(InstanceState.UNAVAILABLE, instanceState);
    }

    @Test
    public void testVolumeDeletingStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.DELETING_STATUS);

        // verify
        Assert.assertEquals(InstanceState.UNAVAILABLE, instanceState);
    }

    @Test
    public void testVolumeAwaitingTransferStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.AWAITING_TRANSFER_STATUS);

        // verify
        Assert.assertEquals(InstanceState.UNAVAILABLE, instanceState);
    }

    @Test
    public void testVolumeBackingUpStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.BACKING_UP_STATUS);

        // verify
        Assert.assertEquals(InstanceState.UNAVAILABLE, instanceState);
    }

    @Test
    public void testVolumeRestoringBackupStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.RESTORING_BACKUP_STATUS);

        // verify
        Assert.assertEquals(InstanceState.UNAVAILABLE, instanceState);
    }

    @Test
    public void testVolumeDownloadingStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.DOWNLOADING_STATUS);

        // verify
        Assert.assertEquals(InstanceState.UNAVAILABLE, instanceState);
    }

    @Test
    public void testVolumeUploadingStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.UPLOADING_STATUS);

        // verify
        Assert.assertEquals(InstanceState.UNAVAILABLE, instanceState);
    }

    @Test
    public void testVolumeRetypingStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.RETYPING_STATUS);

        // verify
        Assert.assertEquals(InstanceState.UNAVAILABLE, instanceState);
    }

    @Test
    public void testVolumeExtendingStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.EXTENDING_STATUS);

        // verify
        Assert.assertEquals(InstanceState.UNAVAILABLE, instanceState);
    }

    @Test
    public void testVolumeAttachingStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.ATTACHING_STATUS);

        // verify
        Assert.assertEquals(InstanceState.ATTACHING, instanceState);
    }

    @Test
    public void testVolumeInUseStatusToUnavailable() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.IN_USE_STATUS);

        // verify
        Assert.assertEquals(InstanceState.IN_USE, instanceState);
    }

    @Test
    public void testVolumeErrorBackingUpStatusToFailed() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.ERROR_BACKING_UP_STATUS);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }

    @Test
    public void testVolumeErrorDeletingStatusToFailed() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.ERROR_DELETING_STATUS);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }

    @Test
    public void testVolumeErrorExtendingStatusToFailed() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.ERROR_EXTENDING_STATUS);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }

    @Test
    public void testVolumeErrorRestoringStatusToFailed() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.ERROR_RESTORING_STATUS);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }

    @Test
    public void testVolumeErrorStatusToFailed() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, OpenStackStateMapper.ERROR_STATUS);

        // verify
        Assert.assertEquals(InstanceState.FAILED, instanceState);
    }

    @Test
    public void testVolumeInvalidStatusToInconsistent() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, "invalid");
        InstanceState instanceState2 = OpenStackStateMapper.map(resourceType, "INVALID");

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState2);
    }

    @Test
    public void testVolumeInvalidStatusToFailed() {
        // set up
        ResourceType resourceType = ResourceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, "invalid");
        InstanceState instanceState2 = OpenStackStateMapper.map(resourceType, "invalid");

        // verify
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState);
        Assert.assertEquals(InstanceState.INCONSISTENT, instanceState2);
    }

    @Test
    public void testAttachmentAnyStatusToReady() {
        // set up
        ResourceType resourceType = ResourceType.ATTACHMENT;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(resourceType, "any_state");
        InstanceState instanceState2 = OpenStackStateMapper.map(resourceType, "ANY_STATE");

        // verify
        Assert.assertEquals(InstanceState.READY, instanceState);
        Assert.assertEquals(InstanceState.READY, instanceState2);
    }
}
