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
        super(UUID.randomUUID().toString());
    }

    /**
     * Creating Order with predefined Id.
     */
    public VolumeOrder(String id, FederationUserToken federationUserToken, String requestingMember,
                       String providingMember, int volumeSize, String name) {
        super(id, federationUserToken, requestingMember, providingMember);
        this.volumeSize = volumeSize;
        this.name = name;
    }

    public VolumeOrder(FederationUserToken federationUserToken, String requestingMember, String providingMember,
                       int volumeSize, String name) {
        this(UUID.randomUUID().toString(), federationUserToken, requestingMember, providingMember,
                volumeSize, name);
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
