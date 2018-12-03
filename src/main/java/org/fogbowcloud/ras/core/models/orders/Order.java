package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.datastore.DatabaseManager;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
//import org.hibernate.annotations.CollectionOfElements;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "order_table")
public abstract class Order implements Serializable {
    private static final long serialVersionUID = 1L;
    @Column
    @Id
    private String id;
    @Column
    @Enumerated(EnumType.STRING)
    private OrderState orderState;
    @JoinColumn
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private FederationUserToken federationUserToken;
    @Column
    private String requester;
    @Column
    private String provider;
    @Column
    private String instanceId;
    @Column
    private InstanceState cachedInstanceState;

    public Map<String, String> getRequirements() {
        return requirements;
    }

    public void setRequirements(Map<String, String> requirements) {
        this.requirements = requirements;
    }

    @ElementCollection
    @MapKeyColumn
    @Column
    private Map<String, String> requirements = new HashMap<>();

    public Order() {
    }

    public Order(String id) {
        this.id = id;
    }

    public Order(String id, String provider) {
        this(id);
        this.provider = provider;
    }

    public Order(String id, String provider, FederationUserToken federationUserToken, String requester) {
        this(id, provider);
        this.federationUserToken = federationUserToken;
        this.requester = requester;
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

    public String getRequester() {
        return this.requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getInstanceId() {
        return this.instanceId;
    }

    public synchronized void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public InstanceState getCachedInstanceState() {
        return this.cachedInstanceState;
    }

    public void setCachedInstanceState(InstanceState cachedInstanceState) {
        this.cachedInstanceState = cachedInstanceState;
    }

    public boolean isProviderLocal(String localMemberId) {
        return this.provider.equals(localMemberId);
    }

    public boolean isRequesterRemote(String localMemberId) {
        return !this.requester.equals(localMemberId);
    }

    public abstract ResourceType getType();

    public abstract String getSpec();

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
        return "Order [id=" + this.id + ", orderState=" + this.orderState + ", federationUserToken="
                + this.federationUserToken + ", requester=" + this.requester + ", provider="
                + this.provider + ", instanceId=" + this.instanceId + "]";
    }
}
