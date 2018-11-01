package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "compute_order_table")
public class ComputeOrder extends Order {
    private static final long serialVersionUID = 1L;
    public static final int MAX_PUBLIC_KEY_SIZE = 1024;
    @Column
    private String name;
    @Column
    private int vCPU;
    /**
     * Memory attribute, must be set in MB.
     */
    @Column
    private int memory;
    /**
     * Disk attribute, must be set in GB.
     */
    @Column
    private int disk;
    @Column
    private String imageId;
    @Embedded
    private ArrayList<UserData> userDataScripts;
    @Column(length = MAX_PUBLIC_KEY_SIZE)
    private String publicKey;
    @Embedded
    private ComputeAllocation actualAllocation;
    @Column
    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> networkIds;

    public ComputeOrder() {
        this(UUID.randomUUID().toString());
    }

    public ComputeOrder(String id) {
        super(id);
    }

    public ComputeOrder(String id, FederationUserToken federationUserToken, String requestingMember, String providingMember,
                        String name, int vCPU, int memory, int disk, String imageId, ArrayList<UserData> userDataScripts, String publicKey,
                        List<String> networkIds) {
        super(id, providingMember, federationUserToken, requestingMember);
        this.name = name;
        this.vCPU = vCPU;
        this.memory = memory;
        this.disk = disk;
        this.imageId = imageId;
        this.userDataScripts = userDataScripts;
        this.publicKey = publicKey;
        this.networkIds = networkIds;
        this.actualAllocation = new ComputeAllocation();
    }

    public ComputeOrder(String providingMember, String name, int vCPU, int memory, int disk, String imageId,
                        ArrayList<UserData> userDataScripts, String publicKey, List<String> networkIds) {
        this(null, null, providingMember, name, vCPU, memory, disk, imageId,
                userDataScripts, publicKey, networkIds);
    }

    public ComputeOrder(FederationUserToken federationUserToken, String requestingMember, String providingMember,
                        String name, int vCPU, int memory, int disk, String imageId, ArrayList<UserData> userDataScripts, String publicKey,
                        List<String> networkIds) {
        this(UUID.randomUUID().toString(), federationUserToken, requestingMember, providingMember, name, vCPU, memory,
                disk, imageId, userDataScripts, publicKey, networkIds);
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

    public List<UserData> getUserDataScripts() {
        return userDataScripts;
    }

    public void setUserDataScripts(ArrayList<UserData> userDataScripts) {
        this.userDataScripts = userDataScripts;
    }

    @Override
    public ResourceType getType() {
        return ResourceType.COMPUTE;
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

    @Override
    public String getSpec() {
        if (this.actualAllocation == null) {
            return "";
        }
        return this.actualAllocation.getvCPU() + "/" + this.actualAllocation.getRam();
    }
}
