package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "public_ip_order_table")
public class PublicIpOrder extends Order {

    @Column
    private String computeOrderId;

    public PublicIpOrder() {
        this(UUID.randomUUID().toString());
    }

    public PublicIpOrder(String id) {
        super(id);
    }

    public PublicIpOrder(String providingMember, String computeOrderId) {
        this(null, null, providingMember, computeOrderId);
    }

    public PublicIpOrder(FederationUserToken federationUserToken, String requestingMember,
                         String providingMember, String computeOrderId) {
        super(UUID.randomUUID().toString(), providingMember, federationUserToken, requestingMember);
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
