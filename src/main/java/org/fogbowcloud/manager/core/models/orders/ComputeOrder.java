package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
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

    public ComputeOrder(){ }

    public ComputeOrder(OrderState orderState, Token localToken, Token federationToken, String requestingMember,
                        String providingMember, OrderInstance orderInstance, long fulfilledTime, int vCPU, int memory,
                        int disk, String imageName, UserData userData, String publicKey) {
        super(orderState, localToken, federationToken, requestingMember, providingMember, orderInstance, fulfilledTime);
        this.vCPU = vCPU;
        this.memory = memory;
        this.disk = disk;
        this.imageName= imageName;
        this.userData = userData;
        this.publicKey = publicKey;
    }

    public int getvCPU() {
        return vCPU;
    }

    public void setvCPU(int vCPU) {
        this.vCPU = vCPU;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public int getDisk() {
        return disk;
    }

    public void setDisk(int disk) {
        this.disk = disk;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public UserData getUserData() {
        return userData;
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public OrderType getType() {
        return OrderType.COMPUTE;
    }

    @Override
    public void handleOpenOrder() {
        // TODO
    }
}
