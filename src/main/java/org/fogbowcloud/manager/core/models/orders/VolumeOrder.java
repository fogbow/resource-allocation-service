package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;

public class VolumeOrder extends Order {

    private int volumeSize;
    private String volumeName;

    public VolumeOrder() {
        super(UUID.randomUUID().toString());
    }

    /** Creating Order with predefined Id. */
    public VolumeOrder(String id, FederationUser federationUser, String requestingMember, String providingMember,
            int volumeSize, String volumeName) {
        super(id, federationUser, requestingMember, providingMember);
        this.volumeSize = volumeSize;
        this.volumeName = volumeName;
    }

    public VolumeOrder(FederationUser federationUser, String requestingMember, String providingMember,
            int volumeSize, String volumeName) {
        this(UUID.randomUUID().toString(), federationUser, requestingMember, providingMember,
                volumeSize, volumeName);
    }

    public int getVolumeSize() {
        return volumeSize;
    }

    public String getVolumeName() {
        return volumeName;
    }

    @Override
    public InstanceType getType() {
        return InstanceType.VOLUME;
    }
}
