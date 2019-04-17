package cloud.fogbow.ras.core.models.orders;

import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import org.apache.log4j.Logger;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.*;

@Entity
@Table(name = "compute_order_table")
public class ComputeOrder extends Order {
    private static final long serialVersionUID = 1L;

    private static final String NAME_COLUMN_NAME = "name";
    private static final String IMAGE_ID_COLUMN_NAME = "image_id";
    private static final String PUBLIC_KEY_COLUMN_NAME = "public_key";

    public static final int PUBLIC_KEY_MAX_SIZE = 1024;

    @Transient
    private transient final Logger LOGGER = Logger.getLogger(ComputeOrder.class);

    @Column
    private int vCPU;

    // Memory attribute, must be set in MB.
    @Column
    private int memory;

    // Disk attribute, must be set in GB.
    @Column
    private int disk;

    @Embedded
    private ArrayList<UserData> userData;

    @Size(max = Order.FIELDS_MAX_SIZE)
    @Column(name = NAME_COLUMN_NAME)
    private String name;

    @Size(max = Order.FIELDS_MAX_SIZE)
    @Column(name = IMAGE_ID_COLUMN_NAME)
    private String imageId;

    @Size(max = PUBLIC_KEY_MAX_SIZE)
    @Column(name = PUBLIC_KEY_COLUMN_NAME)
    private String publicKey;

    @Embedded
    private ComputeAllocation actualAllocation;

    // this attribute refers to the list of networkIds to be attached to the compute
    @Column
    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<String> networkIds;

    /**
     * Using FetchType.EAGER in both networkIds and networkOrderIds didn't work;
     * @LazyCollection(LazyCollectionOption.FALSE) is a workaround.
     */
    @Column
    @ElementCollection
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<String> networkOrderIds;


    public ComputeOrder() {
        this(UUID.randomUUID().toString());
        this.type = ResourceType.COMPUTE;
    }

    public ComputeOrder(String id) {
        super(id);
        this.type = ResourceType.COMPUTE;
    }

    public ComputeOrder(String id, SystemUser systemUser, String requestingMember, String providingMember,
                        String cloudName, String name, int vCPU, int memory, int disk, String imageId,
                        ArrayList<UserData> userData, String publicKey, List<String> networkOrderIds) {
        super(id, providingMember, cloudName, systemUser, requestingMember);
        this.name = name;
        this.vCPU = vCPU;
        this.memory = memory;
        this.disk = disk;
        this.imageId = imageId;
        this.userData = userData;
        this.publicKey = publicKey;
        this.networkOrderIds = networkOrderIds;
        this.actualAllocation = new ComputeAllocation();
        this.type = ResourceType.COMPUTE;
    }

    public ComputeOrder(String providingMember, String cloudName, String name, int vCPU, int memory, int disk,
                        String imageId, ArrayList<UserData> userData, String publicKey, List<String> networkOrderIds) {
        this(null, null, providingMember, cloudName, name, vCPU, memory, disk, imageId,
                userData, publicKey, networkOrderIds);
        this.type = ResourceType.COMPUTE;
    }

    public ComputeOrder(SystemUser systemUser, String requestingMember, String providingMember, String cloudName,
                        String name, int vCPU, int memory, int disk, String imageId, ArrayList<UserData> userData,
                        String publicKey, List<String> networkOrderIds) {
        this(UUID.randomUUID().toString(), systemUser, requestingMember, providingMember, cloudName, name, vCPU, memory,
                disk, imageId, userData, publicKey, networkOrderIds);
        this.type = ResourceType.COMPUTE;
    }

    public ComputeAllocation getActualAllocation() {
        return actualAllocation;
    }

    public void setActualAllocation(ComputeAllocation actualAllocation) {
        this.actualAllocation = actualAllocation;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getvCPU() {
        return vCPU;
    }

    public int getMemory() {
        return memory;
    }

    public int getDisk() {
        return disk;
    }

    public String getImageId() {
        return imageId;
    }

    public ArrayList<UserData> getUserData() {
        return userData;
    }

    public void setUserData(ArrayList<UserData> userData) {
        this.userData = userData;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public List<String> getNetworkIds() {
        if (networkIds == null) {
            return Collections.unmodifiableList(new ArrayList<>());
        }
        return Collections.unmodifiableList(this.networkIds);
    }

    public void setNetworkIds(List<String> networkIds) {
        this.networkIds = networkIds;
    }

    public List<String> getNetworkOrderIds() {
        if (networkOrderIds == null) {
            return Collections.unmodifiableList(new ArrayList<>());
        }
        return Collections.unmodifiableList(this.networkOrderIds);
    }

    @Override
    public String getSpec() {
        if (this.actualAllocation == null) {
            return "";
        }
        return this.actualAllocation.getvCPU() + "/" + this.actualAllocation.getRam();
    }
}
