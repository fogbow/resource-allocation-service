package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

public class VolumeOrder extends Order {

    private int volumeSize;
    private String volumeName;

    public VolumeOrder() {
        super(UUID.randomUUID().toString());
    }

    /** Creating Order with predefined Id. */
    public VolumeOrder(String id, FederationUserToken federationUserToken, String requestingMember, String providingMember,
                       int volumeSize, String volumeName) {
        super(id, federationUserToken, requestingMember, providingMember);
        this.volumeSize = volumeSize;
        this.volumeName = volumeName;
    }

    public VolumeOrder(FederationUserToken federationUserToken, String requestingMember, String providingMember,
                       int volumeSize, String volumeName) {
        this(UUID.randomUUID().toString(), federationUserToken, requestingMember, providingMember,
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
