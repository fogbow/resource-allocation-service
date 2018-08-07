package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUserAttributes;

public class VolumeOrder extends Order {

    private int volumeSize;
    private String volumeName;

    public VolumeOrder() {
        super(UUID.randomUUID().toString());
    }

    /** Creating Order with predefined Id. */
    public VolumeOrder(String id, FederationUserAttributes federationUserAttributes, String requestingMember, String providingMember,
                       int volumeSize, String volumeName) {
        super(id, federationUserAttributes, requestingMember, providingMember);
        this.volumeSize = volumeSize;
        this.volumeName = volumeName;
    }

    public VolumeOrder(FederationUserAttributes federationUserAttributes, String requestingMember, String providingMember,
                       int volumeSize, String volumeName) {
        this(UUID.randomUUID().toString(), federationUserAttributes, requestingMember, providingMember,
                volumeSize, volumeName);
    }

    public int getVolumeSize() {
        return volumeSize;
    }

    public String getVolumeName() {
        return volumeName;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.VOLUME;
    }
}
