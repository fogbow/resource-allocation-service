package org.fogbowcloud.manager.core.models.orders;

import java.util.List;
import java.util.UUID;

import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.token.FederationUser;

public class ComputeOrder extends Order {

    private int vCPU;

    /** Memory attribute, must be set in MB. */
    private int memory;

    /** Disk attribute, must be set in GB. */
    private int disk;

    private String imageId;

    private UserData userData;

    private String publicKey;

    private ComputeAllocation actualAllocation;
    
    private List<String> networksId;

    public ComputeOrder() {
        super(UUID.randomUUID().toString());
    }

    /** Creating Order with predefined Id. */
    public ComputeOrder(
            String id,
            FederationUser federationUser,
            String requestingMember,
            String providingMember,
            int vCPU,
            int memory,
            int disk,
            String imageId,
            UserData userData,
            String publicKey) {
        super(id, federationUser, requestingMember, providingMember);
        this.vCPU = vCPU;
        this.memory = memory;
        this.disk = disk;
        this.imageId = imageId;
        this.userData = userData;
        this.publicKey = publicKey;
    }

    public ComputeOrder(
            FederationUser federationUser,
            String requestingMember,
            String providingMember,
            int vCPU,
            int memory,
            int disk,
            String imageId,
            UserData userData,
            String publicKey) {
        this(
                UUID.randomUUID().toString(),
                federationUser,
                requestingMember,
                providingMember,
                vCPU,
                memory,
                disk,
                imageId,
                userData,
                publicKey);
    }

    public static ComputeOrder from(ComputeOrder baseOrder) {
        return new ComputeOrder(baseOrder.getId(), baseOrder.getFederationUser(), baseOrder.getRequestingMember(),
                baseOrder.getProvidingMember(), baseOrder.getvCPU(), baseOrder.getMemory(), baseOrder.getDisk(),
                baseOrder.getImageId(), baseOrder.getUserData(), baseOrder.getPublicKey());
    }

    public ComputeAllocation getActualAllocation() {
        return actualAllocation;
    }

    public void setActualAllocation(ComputeAllocation actualAllocation) {
        this.actualAllocation = actualAllocation;
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

    public UserData getUserData() {
        return userData;
    }

    @Override
    public InstanceType getType() {
        return InstanceType.COMPUTE;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public List<String> getNetworksId() {
        return networksId;
    }

}
