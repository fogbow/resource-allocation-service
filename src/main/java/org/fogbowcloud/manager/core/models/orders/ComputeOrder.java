package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.models.instances.InstanceType;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.token.FederationUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
            String publicKey, 
            List<String> networksId) {
        super(id, federationUser, requestingMember, providingMember);
        this.vCPU = vCPU;
        this.memory = memory;
        this.disk = disk;
        this.imageId = imageId;
        this.userData = userData;
        this.publicKey = publicKey;
        this.networksId = networksId;
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
            String publicKey,
            List<String> networksId) {
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
                publicKey,
                networksId);
    }

    public static ComputeOrder from(ComputeOrder baseOrder) {
        return new ComputeOrder(baseOrder.getId(), baseOrder.getFederationUser(), baseOrder.getRequestingMember(),
                baseOrder.getProvidingMember(), baseOrder.getvCPU(), baseOrder.getMemory(), baseOrder.getDisk(),
                baseOrder.getImageId(), baseOrder.getUserData(), baseOrder.getPublicKey(), baseOrder.getNetworksId());
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
        if (networksId == null) {
            return Collections.unmodifiableList(new ArrayList<>());
        }
        return Collections.unmodifiableList(networksId);
    }

    public void setNetworksId(List<String> networksId) {
        this.networksId = networksId;
    }

}
