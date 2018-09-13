package org.fogbowcloud.ras.core.models.orders;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

@Entity
@Table(name = "public_ip_order_table")
public class PublicIpOrder extends Order {

	@Column
    private String computeOrderId;

    public PublicIpOrder() {
        super(UUID.randomUUID().toString());
    }

    public PublicIpOrder(FederationUserToken federationUserToken, String requestingMember,
        String providingMember, String computeOrderId) {
        this(UUID.randomUUID().toString(), federationUserToken, requestingMember, providingMember, computeOrderId);
    }

    public PublicIpOrder(String id, FederationUserToken federationUserToken, String requestingMember,
        String providingMember, String computeOrderId) {
        super(id, federationUserToken, requestingMember, providingMember);
        this.computeOrderId = computeOrderId;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.PUBLIC_IP;
    }

    public String getComputeOrderId() {
		return computeOrderId;
	}
    
    @Override
    public String getSpec() {
        return "";
    }
}
