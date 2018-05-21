package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;
import org.fogbowcloud.manager.core.models.token.Token;

public class ComputeOrder extends Order {

    private int vCPU;

    /** Memory attribute, must be set in MB. */
    private int memory;

    /** Disk attribute, must be set in GB. */
    private int disk;

    private String imageName;

    private UserData userData;

    private String publicKey;

    public ComputeOrder() {
        super(UUID.randomUUID().toString());
    }

    /** Creating Order with predefined Id. */
    public ComputeOrder(
            String id,
            Token federationToken,
            String requestingMember,
            String providingMember,
            int vCPU,
            int memory,
            int disk,
            String imageName,
            UserData userData,
            String publicKey) {
        super(id, federationToken, requestingMember, providingMember);
        this.vCPU = vCPU;
        this.memory = memory;
        this.disk = disk;
        this.imageName = imageName;
        this.userData = userData;
        this.publicKey = publicKey;
    }

    public ComputeOrder(
            Token federationToken,
            String requestingMember,
            String providingMember,
            int vCPU,
            int memory,
            int disk,
            String imageName,
            UserData userData,
            String publicKey) {
        this(
                UUID.randomUUID().toString(),
                federationToken,
                requestingMember,
                providingMember,
                vCPU,
                memory,
                disk,
                imageName,
                userData,
                publicKey);
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

    public String getImageName() {
        return imageName;
    }

    public UserData getUserData() {
        return userData;
    }

    @Override
    public OrderType getType() {
        return OrderType.COMPUTE;
    }

    public String getPublicKey() {
        return publicKey;
    }
}
