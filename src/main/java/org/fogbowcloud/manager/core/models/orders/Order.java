package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUser;

public abstract class Order {

    private String id;

    private OrderState orderState;

    private FederationUser federationUser;

    private String requestingMember;

    private String providingMember;

    private String instanceId;

    private InstanceState cachedInstanceState;

    public Order(String id) {
        this.id = id;
    }

    /** Creating Order with predefined Id. */
    public Order(String id, FederationUser federationUser, String requestingMember, String providingMember) {
        this(id);
        this.federationUser = federationUser;
        this.requestingMember = requestingMember;
        this.providingMember = providingMember;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public synchronized OrderState getOrderState() {
        return orderState;
    }

    public synchronized void setOrderState(OrderState state) {
        this.orderState = state;
    }

    public FederationUser getFederationUser() {
        return federationUser;
    }

    public void setFederationUser(FederationUser federationUser) {
        this.federationUser = federationUser;
    }

    public String getRequestingMember() {
        return requestingMember;
    }

    public void setRequestingMember(String requestingMember) {
        this.requestingMember = requestingMember;
    }

    public String getProvidingMember() {
        return providingMember;
    }

    public void setProvidingMember(String providingMember) {
        this.providingMember = providingMember;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public synchronized void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public InstanceState getCachedInstanceState() {
        InstanceState instanceState;
        if (this.orderState.equals(OrderState.FAILED)) {
            instanceState = InstanceState.FAILED;
        } else if (this.orderState.equals(OrderState.SPAWNING)) {
            instanceState = InstanceState.SPAWNING;
        } else {
            instanceState = getCachedInstanceState();
        }
        return instanceState;
    }

    public void setCachedInstanceState(InstanceState cachedInstanceState) {
        this.cachedInstanceState = cachedInstanceState;
    }

    public boolean isProviderLocal(String localMemberId) {
        return this.providingMember.equals(localMemberId);
    }

    public boolean isRequesterRemote(String localMemberId) {
        return !this.requestingMember.equals(localMemberId);
    }

    public abstract InstanceType getType();

    // TODO: add a comment to explain why we need to override these methods
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Order other = (Order) obj;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "Order [id=" + id + ", orderState=" + orderState + ", federationUser=" + federationUser
                + ", requestingMember=" + requestingMember + ", providingMember=" + providingMember
                + ", instanceId=" + instanceId + "]";
    }
}
