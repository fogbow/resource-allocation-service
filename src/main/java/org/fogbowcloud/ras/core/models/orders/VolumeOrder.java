package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "volume_order_table")
public class VolumeOrder extends Order {
    private static final long serialVersionUID = 1L;
    @Column
    private int volumeSize;
    @Column
    private String name;

    public VolumeOrder() {
        this(UUID.randomUUID().toString());
    }

    public VolumeOrder(String id) {
        super(id);
    }

    public VolumeOrder(String providingMember, String cloudName, String name, int volumeSize) {
        this(null, null, providingMember, cloudName, name, volumeSize);
    }

    public VolumeOrder(FederationUserToken federationUserToken, String requestingMember, String providingMember,
                       String cloudName, String name, int volumeSize) {
        super(UUID.randomUUID().toString(), providingMember, cloudName, federationUserToken, requestingMember);
        this.name = name;
        this.volumeSize = volumeSize;
    }

    public int getVolumeSize() {
        return volumeSize;
    }

    public String getName() {
        return name;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.VOLUME;
    }

    @Override
    public String getSpec() {
        return String.valueOf(this.volumeSize);
    }
}
