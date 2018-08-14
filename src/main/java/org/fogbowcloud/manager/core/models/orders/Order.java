package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.datastore.DatabaseManager;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

public abstract class Order {

    private String id;

    private OrderState orderState;

    private FederationUserToken federationUserToken;

    private String requestingMember;

    private String providingMember;

    private String instanceId;

    private InstanceState cachedInstanceState;

    public Order(String id) {
        this.id = id;
    }

    /** Creating Order with predefined Id. */
    public Order(String id, FederationUserToken federationUserToken, String requestingMember, String providingMember) {
        this(id);
        this.federationUserToken = federationUserToken;
        this.requestingMember = requestingMember;
        this.providingMember = providingMember;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public synchronized OrderState getOrderState() {
        return this.orderState;
    }

    public synchronized void setOrderStateInRecoveryMode(OrderState state) {
        this.orderState = state;
    }

    public synchronized void setOrderStateInTestMode(OrderState state) {
        this.orderState = state;
    }

    public synchronized void setOrderState(OrderState state) throws UnexpectedException {
        this.orderState = state;
        DatabaseManager databaseManager = DatabaseManager.getInstance();
        if (state.equals(OrderState.OPEN)) {
            // Adding in stable storage newly created order
            databaseManager.add(this);
        } else {
            // Updating in stable storage already existing order
            databaseManager.update(this);
        }
    }

    public FederationUserToken getFederationUserToken() {
        return this.federationUserToken;
    }

    public void setFederationUserToken(FederationUserToken federationUserToken) {
        this.federationUserToken = federationUserToken;
    }

    public String getRequestingMember() {
        return this.requestingMember;
    }

    public void setRequestingMember(String requestingMember) {
        this.requestingMember = requestingMember;
    }

    public String getProvidingMember() {
        return this.providingMember;
    }

    public void setProvidingMember(String providingMember) {
        this.providingMember = providingMember;
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public synchronized void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public InstanceState getCachedInstanceState() {
        if (this.orderState.equals(OrderState.FAILED)) {
            // The order can go from OPEN to FAIL without ever getting an instance
            return InstanceState.FAILED;
        } else if (this.orderState.equals(OrderState.SPAWNING)) {
            // Orders of ComputerOrder type may have the instance READY, but with the order still Spawning
            // This is the case when it is checking the reachability.
            // In this case, we want o show the instance state as SPAWNING
            return InstanceState.SPAWNING;
        }

        return this.cachedInstanceState;
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

    public abstract ResourceType getType();

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Order other = (Order) obj;
        if (this.id == null) {
            if (other.getId() != null) return false;
        } else if (!this.id.equals(other.getId())) return false;
        return true;
    }

    @Override
    public String toString() {
        return "Order [id=" + this.id + ", orderState=" + this.orderState + ", federationUserToken=" + this.federationUserToken
                + ", requestingMember=" + this.requestingMember + ", providingMember=" + this.providingMember
                + ", instanceId=" + this.instanceId + "]";
    }
}
