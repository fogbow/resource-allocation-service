package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.models.NetworkLink;
import org.fogbowcloud.manager.core.models.StorageLink;
import org.fogbowcloud.manager.core.models.orders.instances.OrderInstance;
import org.fogbowcloud.manager.core.models.token.Token;

public class ComputeOrder extends Order {

	private int vCPU;
	/** Memory attribute, must be set in MB. */
	private int memory;
	/** Disk attribute, must be set in GB. */
	private int disk;
	private UserData userData;
	private NetworkLink networkLink;
	private StorageLink storageLink;
	private String publicKey;

	public ComputeOrder(String id, OrderState orderState, Token localToken, Token federationToken,
						String requestingMember, String providingMember, OrderInstance orderInstance,
						long fulfilledTime, int vCPU, int memory, int disk, UserData userData,
						NetworkLink networkLink, StorageLink storageLink, String publicKey) {

		super(id, orderState, localToken, federationToken, requestingMember, providingMember, orderInstance, fulfilledTime);
		this.vCPU = vCPU;
		this.memory = memory;
		this.disk = disk;
		this.userData = userData;
		this.networkLink = networkLink;
		this.storageLink = storageLink;
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

	public UserData getUserData() {
		return userData;
	}

	public void setUserData(UserData userData) {
		this.userData = userData;
	}

	public NetworkLink getNetworkLink() {
		return networkLink;
	}

	public void setNetworkLink(NetworkLink networkLink) {
		this.networkLink = networkLink;
	}

	public StorageLink getStorageLink() {
		return storageLink;
	}

	public void setStorageLink(StorageLink storageLink) {
		this.storageLink = storageLink;
	}

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ComputeOrder that = (ComputeOrder) o;

        if (vCPU != that.vCPU) return false;
        if (memory != that.memory) return false;
        if (disk != that.disk) return false;
        if (userData != null ? !userData.equals(that.userData) : that.userData != null) return false;
        if (networkLink != null ? !networkLink.equals(that.networkLink) : that.networkLink != null) return false;
        if (storageLink != null ? !storageLink.equals(that.storageLink) : that.storageLink != null) return false;
        if (publicKey != null ? !publicKey.equals(that.publicKey) : that.publicKey != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + vCPU;
        result = 31 * result + memory;
        result = 31 * result + disk;
        result = 31 * result + (userData != null ? userData.hashCode() : 0);
        result = 31 * result + (networkLink != null ? networkLink.hashCode() : 0);
        result = 31 * result + (storageLink != null ? storageLink.hashCode() : 0);
        result = 31 * result + (publicKey != null ? publicKey.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ComputeOrder{" +
                "vCPU=" + vCPU +
                ", memory=" + memory +
                ", disk=" + disk +
                ", userData=" + userData +
                ", networkLink=" + networkLink +
                ", storageLink=" + storageLink +
                ", publicKey='" + publicKey + '\'' +
                '}';
    }
}
