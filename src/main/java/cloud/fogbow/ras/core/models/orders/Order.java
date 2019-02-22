package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.StorableBean;
import cloud.fogbow.ras.api.http.response.InstanceState;
import org.apache.log4j.Logger;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "order_table")
public abstract class Order extends StorableBean implements Serializable {
    private static final long serialVersionUID = 1L;

    protected static final String REQUESTER_COLUMN_NAME = "requester";
    protected static final String PROVIDER_COLUMN_NAME = "provider";
    protected static final String CLOUD_NAME_COLUMN_NAME = "cloud_name";
    protected static final String INSTANCE_ID_COLUMN_NAME = "instance_id";

    protected static final int FIELDS_MAX_SIZE = 255;
    protected static final int ID_FIXED_SIZE = 36; // UUID size

    @Transient
    private transient final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(Order.class);

    @Column
    @Id
    @Size(max = ID_FIXED_SIZE)
    private String id;

    @Column
    @Enumerated(EnumType.STRING)
    private OrderState orderState;

    @Column(name = REQUESTER_COLUMN_NAME)
    @Size(max = FIELDS_MAX_SIZE)
    private String requester;

    @Column(name = PROVIDER_COLUMN_NAME)
    @Size(max = FIELDS_MAX_SIZE)
    private String provider;

    @Column(name = CLOUD_NAME_COLUMN_NAME)
    @Size(max = FIELDS_MAX_SIZE)
    private String cloudName;

    @Column(name = INSTANCE_ID_COLUMN_NAME)
    @Size(max = FIELDS_MAX_SIZE)
    private String instanceId;

    @Column
    private InstanceState cachedInstanceState;

    @ElementCollection
    @MapKeyColumn
    @Column
    private Map<String, String> requirements = new HashMap<>();

    @Transient
    private FederationUser federationUser;

    @Column
    private String userId;
    @Column
    private String identityProvider;
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn
    @Column
    private Map<String, String> federationUserAttributes = new HashMap<>();

    public Order() {
    }

    public Order(String id) {
        this.id = id;
    }

    public Order(String id, String provider, String cloudName) {
        this(id);
        this.provider = provider;
        this.cloudName = cloudName;
    }

    public Order(String id, String provider, String cloudName, FederationUser federationUser, String requester) {
        this(id, provider, cloudName);
        this.requester = requester;

        try {
            setFederationUser(federationUser);
        } catch (UnexpectedException e) {
            LOGGER.fatal(e.getMessage(), e);
        }
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

    public FederationUser getFederationUser() {
        if (this.federationUser == null) {
            this.federationUser = new FederationUser(this.federationUserAttributes);
        }
        return this.federationUser;
    }

    public void setFederationUser(FederationUser federationUser) throws UnexpectedException {
        this.federationUser = federationUser;
        if (federationUser != null) {
            this.userId = federationUser.getUserId();
            this.identityProvider = federationUser.getTokenProvider();
            this.federationUserAttributes = federationUser.getAttributes();
        }
    }

    public String getRequester() {
        return this.requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    // When the provider is not set, then the request is assumed to be local, i.e. provider and request are the same.
    public String getProvider() {
        return this.provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCloudName() {
        return this.cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
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

    public Map<String, String> getRequirements() {
        return requirements;
    }

    public void setRequirements(Map<String, String> requirements) {
        this.requirements = requirements;
    }

    public boolean isProviderLocal(String localMemberId) {
        return this.provider.equals(localMemberId);
    }

    public boolean isRequesterRemote(String localMemberId) {
        return !this.requester.equals(localMemberId);
    }

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
        return "Order [id=" + this.id + ", orderState=" + this.orderState + ", federationUser="
                + this.federationUser + ", requester=" + this.requester + ", provider="
                + this.provider + ", instanceId=" + this.instanceId + "]";
    }

    public abstract ResourceType getType();

    public abstract String getSpec();

    @Override
    public abstract Logger getLogger();

    @PrePersist
    protected void checkAllColumnsSizes() {
        this.requester = treatValue(this.requester, REQUESTER_COLUMN_NAME, FIELDS_MAX_SIZE);
        this.provider = treatValue(this.requester, PROVIDER_COLUMN_NAME, FIELDS_MAX_SIZE);
        this.cloudName = treatValue(this.requester, CLOUD_NAME_COLUMN_NAME, FIELDS_MAX_SIZE);
        this.instanceId = treatValue(this.requester, INSTANCE_ID_COLUMN_NAME, FIELDS_MAX_SIZE);
    }
}
