package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.junit.Assert;
import org.junit.Test;

public class OpenStackStateMapperTest {

    @Test
    public void testComputeActiveStatusToReady() {
        InstanceType instanceType = InstanceType.COMPUTE;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "active");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "ACTIVE");

        Assert.assertEquals(instanceState1, InstanceState.READY);
        Assert.assertEquals(instanceState2, InstanceState.READY);
    }

    @Test
    public void testComputeBuildStatusToSpawning() {
        InstanceType instanceType = InstanceType.COMPUTE;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "build");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "BUILD");

        Assert.assertEquals(instanceState1, InstanceState.SPAWNING);
        Assert.assertEquals(instanceState2, InstanceState.SPAWNING);
    }

    @Test
    public void testComputeInvalidStatusToFailed() {
        InstanceType instanceType = InstanceType.COMPUTE;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "invalid");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "INVALID");

        Assert.assertEquals(instanceState1, InstanceState.FAILED);
        Assert.assertEquals(instanceState2, InstanceState.FAILED);
    }

    @Test
    public void testNetworkBuildStatusToInactive() {
        InstanceType instanceType = InstanceType.NETWORK;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "build");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "BUILD");

        Assert.assertEquals(instanceState1, InstanceState.INACTIVE);
        Assert.assertEquals(instanceState2, InstanceState.INACTIVE);
    }

    @Test
    public void testNetworkDownStatusToInactive() {
        InstanceType instanceType = InstanceType.NETWORK;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "down");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "DOWN");

        Assert.assertEquals(instanceState1, InstanceState.INACTIVE);
        Assert.assertEquals(instanceState2, InstanceState.INACTIVE);
    }

    @Test
    public void testNetworkActiveStatusToReady() {
        InstanceType instanceType = InstanceType.NETWORK;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "active");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "ACTIVE");

        Assert.assertEquals(instanceState1, InstanceState.READY);
        Assert.assertEquals(instanceState2, InstanceState.READY);
    }

    @Test
    public void testNetworkErrorStatusToFailed() {
        InstanceType instanceType = InstanceType.NETWORK;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "error");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "ERROR");

        Assert.assertEquals(instanceState1, InstanceState.FAILED);
        Assert.assertEquals(instanceState2, InstanceState.FAILED);
    }

    @Test
    public void testNetworkInvalidStatusToFailed() {
        InstanceType instanceType = InstanceType.NETWORK;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "invalid");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "INVALID");

        Assert.assertEquals(instanceState1, InstanceState.INCONSISTENT);
        Assert.assertEquals(instanceState2, InstanceState.INCONSISTENT);
    }

    @Test
    public void testVolumeCreatingStatusToCreating() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "creating");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "CREATING");

        Assert.assertEquals(instanceState1, InstanceState.CREATING);
        Assert.assertEquals(instanceState2, InstanceState.CREATING);
    }

    @Test
    public void testVolumeAvailableStatusToReady() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "available");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "AVAILABLE");

        Assert.assertEquals(instanceState1, InstanceState.READY);
        Assert.assertEquals(instanceState2, InstanceState.READY);
    }

    @Test
    public void testVolumeDetachingStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "detaching");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "DETACHING");

        Assert.assertEquals(instanceState1, InstanceState.UNAVAILABLE);
        Assert.assertEquals(instanceState2, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeMaintenceStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "maintenance");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "MAINTENANCE");

        Assert.assertEquals(instanceState1, InstanceState.UNAVAILABLE);
        Assert.assertEquals(instanceState2, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeDeletingStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "deleting");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "DELETING");

        Assert.assertEquals(instanceState1, InstanceState.UNAVAILABLE);
        Assert.assertEquals(instanceState2, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeAwaitingTransferStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "awaiting-transfer");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "AWAITING-TRANSFER");

        Assert.assertEquals(instanceState1, InstanceState.UNAVAILABLE);
        Assert.assertEquals(instanceState2, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeBackingUpStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "backing-up");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "BACKING-UP");

        Assert.assertEquals(instanceState1, InstanceState.UNAVAILABLE);
        Assert.assertEquals(instanceState2, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeRestoringBackupStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "restoring-backup");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "RESTORING-BACKUP");

        Assert.assertEquals(instanceState1, InstanceState.UNAVAILABLE);
        Assert.assertEquals(instanceState2, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeDownloadingStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "downloading");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "DOWNLOADING");

        Assert.assertEquals(instanceState1, InstanceState.UNAVAILABLE);
        Assert.assertEquals(instanceState2, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeUploadingStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "uploading");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "UPLOADING");

        Assert.assertEquals(instanceState1, InstanceState.UNAVAILABLE);
        Assert.assertEquals(instanceState2, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeRetypingStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "retyping");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "RETYPING");

        Assert.assertEquals(instanceState1, InstanceState.UNAVAILABLE);
        Assert.assertEquals(instanceState2, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeExtendingStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "extending");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "EXTENDING");

        Assert.assertEquals(instanceState1, InstanceState.UNAVAILABLE);
        Assert.assertEquals(instanceState2, InstanceState.UNAVAILABLE);
    }

    @Test
    public void testVolumeAttachingStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "attaching");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "ATTACHING");

        Assert.assertEquals(instanceState1, InstanceState.ATTACHING);
        Assert.assertEquals(instanceState2, InstanceState.ATTACHING);
    }

    @Test
    public void testVolumeInUseStatusToUnavailable() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "in-use");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "IN-USE");

        Assert.assertEquals(instanceState1, InstanceState.IN_USE);
        Assert.assertEquals(instanceState2, InstanceState.IN_USE);
    }

    @Test
    public void testVolumeErrorBackingUpStatusToFailed() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "error_backing-up");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "ERROR_BACKING-UP");

        Assert.assertEquals(instanceState1, InstanceState.FAILED);
        Assert.assertEquals(instanceState2, InstanceState.FAILED);
    }

    @Test
    public void testVolumeErrorDeletingStatusToFailed() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "error_deleting");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "ERROR_DELETING");

        Assert.assertEquals(instanceState1, InstanceState.FAILED);
        Assert.assertEquals(instanceState2, InstanceState.FAILED);
    }

    @Test
    public void testVolumeErrorExtendingStatusToFailed() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "error_extending");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "ERROR_EXTENDING");

        Assert.assertEquals(instanceState1, InstanceState.FAILED);
        Assert.assertEquals(instanceState2, InstanceState.FAILED);
    }

    @Test
    public void testVolumeErrorRestoringStatusToFailed() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "error_restoring");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "ERROR_RESTORING");

        Assert.assertEquals(instanceState1, InstanceState.FAILED);
        Assert.assertEquals(instanceState2, InstanceState.FAILED);
    }

    @Test
    public void testVolumeErrorStatusToFailed() {
        InstanceType instanceType = InstanceType.VOLUME;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "error");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "ERROR");

        Assert.assertEquals(instanceState1, InstanceState.FAILED);
        Assert.assertEquals(instanceState2, InstanceState.FAILED);
    }

    @Test
    public void testAttachmentAnyStatusToReady() {
        InstanceType instanceType = InstanceType.ATTACHMENT;

        InstanceState instanceState1 = OpenStackStateMapper.map(instanceType, "any_state");
        InstanceState instanceState2 = OpenStackStateMapper.map(instanceType, "ANY_STATE");

        Assert.assertEquals(instanceState1, InstanceState.READY);
        Assert.assertEquals(instanceState2, InstanceState.READY);
    }
}
