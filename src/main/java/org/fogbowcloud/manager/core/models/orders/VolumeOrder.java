package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;
import org.fogbowcloud.manager.core.models.token.Token;

public class VolumeOrder extends Order {

    private int volumeSize;

    public VolumeOrder() {
        super(UUID.randomUUID().toString());
    }
    
    /** Creating Order with predefined Id. */
    public VolumeOrder(
            String id,
            Token federationToken,
            String requestingMember,
            String providingMember,
            int volumeSize) {
        super(id, federationToken, requestingMember, providingMember);
        this.volumeSize = volumeSize;
    }

    public VolumeOrder(
            Token federationToken,
            String requestingMember,
            String providingMember,
            int volumeSize) {
        this(
                UUID.randomUUID().toString(),
                federationToken,
                requestingMember,
                providingMember,
                volumeSize);
    }

    public int getVolumeSize() {
        return volumeSize;
    }

    @Override
    public OrderType getType() {
        return OrderType.VOLUME;
    }
}
