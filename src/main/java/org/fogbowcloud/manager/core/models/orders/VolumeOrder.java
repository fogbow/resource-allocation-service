package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

@Entity
@Table(name = "volume_order_table")
public class VolumeOrder extends Order {
	
	private static final long serialVersionUID = 1L;
	
	@Column
    private int volumeSize;
	
	@Column
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
