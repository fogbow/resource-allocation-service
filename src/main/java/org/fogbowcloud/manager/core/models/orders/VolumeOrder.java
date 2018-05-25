package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.models.token.Token;

public class VolumeOrder extends Order {

    private int volumeSize;

    public VolumeOrder() {
        super(UUID.randomUUID().toString());
    }

    /** Creating Order with predefined Id. */
    public VolumeOrder(
            String id,
            FederationUser federationUser,
            String requestingMember,
            String providingMember,
            int volumeSize) {
        super(id, federationUser, requestingMember, providingMember);
        this.volumeSize = volumeSize;
    }

    public VolumeOrder(
            FederationUser federationUser,
            String requestingMember,
            String providingMember,
            int volumeSize) {
        this(
                UUID.randomUUID().toString(),
                federationUser,
                requestingMember,
                providingMember,
                volumeSize);
    }

    public int getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(int volumeSize) {
        this.volumeSize = volumeSize;
    }

    @Override
    public OrderType getType() {
        return OrderType.VOLUME;
    }
}
