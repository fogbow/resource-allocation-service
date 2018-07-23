package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.junit.Assert;
import org.junit.Test;

public class OpenStackStateMapperTest {

    @Test
    public void testComputeActiveStatusToReady() {
        // set up
        InstanceType instanceType = InstanceType.COMPUTE;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.ACTIVE_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.READY);
    }

    @Test
    public void testComputeBuildStatusToSpawning() {
        // set up
        InstanceType instanceType = InstanceType.COMPUTE;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.BUILD_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.SPAWNING);
    }

    @Test
    public void testComputeErrorStatusToFailed() {
        // set up
        InstanceType instanceType = InstanceType.COMPUTE;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.ERROR_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.FAILED);
    }

    @Test
    public void testComputeInvalidStatusToInconsistent() {
        // set up
        InstanceType instanceType = InstanceType.COMPUTE;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, "invalid");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "INVALID");

        // verify
        Assert.assertEquals(instanceState, InstanceState.INCONSISTENT);
        Assert.assertEquals(instanceState2, InstanceState.INCONSISTENT);
    }

    @Test
    public void testNetworkBuildStatusToInactive() {
        // set up
        InstanceType instanceType = InstanceType.NETWORK;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.BUILD_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.INACTIVE);
    }

    @Test
    public void testNetworkDownStatusToInactive() {
        // set up
        InstanceType instanceType = InstanceType.NETWORK;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.DOWN_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.INACTIVE);
    }

    @Test
    public void testNetworkActiveStatusToReady() {
        // set up
        InstanceType instanceType = InstanceType.NETWORK;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.ACTIVE_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.READY);
    }

    @Test
    public void testNetworkErrorStatusToFailed() {
        // set up
        InstanceType instanceType = InstanceType.NETWORK;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.ERROR_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.FAILED);
    }

    @Test
    public void testNetworkInvalidStatusToFailed() {
        // set up
        InstanceType instanceType = InstanceType.NETWORK;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, "invalid");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "INVALID");

        // verify
        Assert.assertEquals(instanceState, InstanceState.INCONSISTENT);
        Assert.assertEquals(instanceState2, InstanceState.INCONSISTENT);
    }

    @Test
    public void testVolumeCreatingStatusToCreating() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.CREATING_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.CREATING);
    }

    @Test
    public void testVolumeAvailableStatusToReady() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.AVAILABLE_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.READY);
    }

    @Test
    public void testVolumeDetachingStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.DETACHING_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeMaintenceStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.MAINTENANCE_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeDeletingStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.DELETING_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeAwaitingTransferStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.AWAITING_TRANSFER_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeBackingUpStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.BACKING_UP_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeRestoringBackupStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.RESTORING_BACKUP_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeDownloadingStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.DOWNLOADING_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeUploadingStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.UPLOADING_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeRetypingStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.RETYPING_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeExtendingStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.EXTENDING_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeAttachingStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.ATTACHING_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.ATTACHING);
    }

    @Test
    public void testVolumeInUseStatusToUnavailable() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.IN_USE_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.IN_USE);
    }

    @Test
    public void testVolumeErrorBackingUpStatusToFailed() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.ERROR_BACKING_UP_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.FAILED);
    }

    @Test
    public void testVolumeErrorDeletingStatusToFailed() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.ERROR_DELETING_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.FAILED);
    }

    @Test
    public void testVolumeErrorExtendingStatusToFailed() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.ERROR_EXTENDING_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.FAILED);
    }

    @Test
    public void testVolumeErrorRestoringStatusToFailed() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.ERROR_RESTORING_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.FAILED);
    }

    @Test
    public void testVolumeErrorStatusToFailed() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, OpenStackStateMapper.ERROR_STATUS);

        // verify
        Assert.assertEquals(instanceState, InstanceState.FAILED);
    }

    @Test
    public void testVolumeInvalidStatusToInconsistent() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, "invalid");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "INVALID");

        // verify
        Assert.assertEquals(instanceState, InstanceState.INCONSISTENT);
        Assert.assertEquals(instanceState2, InstanceState.INCONSISTENT);
    }

    @Test
    public void testVolumeInvalidStatusToFailed() {
        // set up
        InstanceType instanceType = InstanceType.VOLUME;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, "invalid");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "invalid");

        // verify
        Assert.assertEquals(instanceState, InstanceState.INCONSISTENT);
        Assert.assertEquals(instanceState2, InstanceState.INCONSISTENT);
    }

    @Test
    public void testAttachmentAnyStatusToReady() {
        // set up
        InstanceType instanceType = InstanceType.ATTACHMENT;

        // exercise
        InstanceState instanceState = OpenStackStateMapper.map(instanceType, "any_state");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "ANY_STATE");

        // verify
        Assert.assertEquals(instanceState, InstanceState.READY);
        Assert.assertEquals(instanceState2, InstanceState.READY);
    }
}
