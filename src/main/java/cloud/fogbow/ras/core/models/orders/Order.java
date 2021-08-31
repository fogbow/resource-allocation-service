package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.SerializedEntityHolder;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.ResourceType;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "order_table")
public abstract class Order<T extends Order> implements Serializable {
    private static final long serialVersionUID = 1L;

    protected static final String REQUESTER_COLUMN_NAME = "requester";
    protected static final String PROVIDER_COLUMN_NAME = "provider";
    protected static final String CLOUD_NAME_COLUMN_NAME = "cloud_name";
    protected static final String INSTANCE_ID_COLUMN_NAME = "instance_id";

    public static final int FIELDS_MAX_SIZE = 255;
    public static final int ID_FIXED_SIZE = 36; // UUID size

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

    @Transient
    private Map<String, String> requirements = new HashMap<>();

    @Transient
    private SystemUser systemUser;

    @Column
    @Size(max = FIELDS_MAX_SIZE)
    private String userId;

    @Column
    @Size(max = FIELDS_MAX_SIZE)
    private String identityProviderId;

    @Column
    @Size(max = SystemUser.SERIALIZED_SYSTEM_USER_MAX_SIZE)
    private String serializedSystemUser;

    @Column
    @Enumerated(EnumType.STRING)
    protected ResourceType type;

    @Column
    @Size(max = FIELDS_MAX_SIZE)
    private String faultMessage;

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

    public Order(String id, String provider, String cloudName, SystemUser systemUser, String requester) {
        this(id, provider, cloudName);
        this.requester = requester;
        this.systemUser = systemUser;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OrderState getOrderState() {
        return this.orderState;
    }

    public void setOrderStateInTestMode(OrderState state) {
        this.orderState = state;
    }

    public void setOrderState(OrderState state) throws InternalServerErrorException {
        LOGGER.debug(String.format(Messages.Log.ORDER_S_CHANGED_STATE_TO_S, this.getId(), state));
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

    public SystemUser getSystemUser() {
        return this.systemUser;
    }

    public void setSystemUser(SystemUser systemUser) {
        this.systemUser = systemUser;
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

    public String getUserId() {
        return this.userId;
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

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public Map<String, String> getRequirements() {
        return requirements;
    }

    public void setRequirements(Map<String, String> requirements) {
        this.requirements = requirements;
    }

    private void setUserId(String userId) {
        this.userId = userId;
    }

    private void setIdentityProviderId(String identityProviderId) {
        this.identityProviderId = identityProviderId;
    }

    public String getFaultMessage() {
        return this.faultMessage;
    }

    public void setOnceFaultMessage(String faultMessage) {
        if (this.faultMessage == null) this.faultMessage = faultMessage;
    }

    private void setSerializedSystemUser(String serializedSystemUser) {
        this.serializedSystemUser = serializedSystemUser;
    }

    private String getSerializedSystemUser() {
        return this.serializedSystemUser;
    }

    // Cannot be called at @PrePersist because the transient field systemUser is set to null at this stage
    // Instead, the systemUser is explicitly serialized before being save by RecoveryService.save().
    public void serializeSystemUser() {
        SerializedEntityHolder<SystemUser> serializedSystemUserHolder = new SerializedEntityHolder<SystemUser>(this.getSystemUser());
        this.setSerializedSystemUser(GsonHolder.getInstance().toJson(serializedSystemUserHolder));
        this.setUserId(this.getSystemUser().getId());
        this.setIdentityProviderId(this.getSystemUser().getIdentityProviderId());
    }

    @PostLoad
    private void deserializeSystemUser() throws InternalServerErrorException {
        try {
            SerializedEntityHolder serializedSystemUserHolder = GsonHolder.getInstance().fromJson(this.getSerializedSystemUser(), SerializedEntityHolder.class);
            this.setSystemUser((SystemUser) serializedSystemUserHolder.getSerializedEntity());
        } catch(ClassNotFoundException exception) {
            throw new InternalServerErrorException(Messages.Exception.UNABLE_TO_DESERIALIZE_SYSTEM_USER);
        }
    }

    public boolean isProviderLocal(String localProviderId) {
        return this.provider.equals(localProviderId);
    }

    public boolean isProviderRemote(String localProviderId) {
        return !(this.provider.equals(localProviderId));
    }

    public boolean isRequesterLocal(String localProviderId) {
        return this.requester.equals(localProviderId);
    }

    public boolean isRequesterRemote(String localProviderId) {
        return !(this.requester.equals(localProviderId));
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
        return "Order [id=" + this.id + ", orderState=" + this.orderState + ", systemUser="
                + this.systemUser + ", requester=" + this.requester + ", provider="
                + this.provider + ", instanceId=" + this.instanceId + "]";
    }

    public ResourceType getType() {
        return this.type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public abstract void updateFromRemote(T remoteOrder) throws InternalServerErrorException;
}
