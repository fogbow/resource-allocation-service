package org.fogbowcloud.manager.core.models.orders;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.fogbowcloud.manager.core.models.token.Token;

@Entity
@DiscriminatorValue(value = "compute")
public class ComputeOrder extends Order {

	@Column(name = "vCPU")
	private Integer vCPU;

	/** Memory attribute, must be set in MB. */
	@Column(name = "memory")
	private Integer memory;

	/** Disk attribute, must be set in GB. */
	@Column(name = "disk")
	private Integer disk;

//	@OneToOne(cascade = CascadeType.ALL)
//	@JoinColumn(name = "user_data_id")
	@Transient
	private UserData userData;

	@Column(name = "image_name")
    private String imageName;

	@Column(name = "public_key")
    private String publicKey;
	
	public ComputeOrder() {}

    /**
     * Creating Order with predefined Id.
     */
    public ComputeOrder(String id, Token localToken, Token federationToken, String requestingMember,
                        String providingMember, int vCPU, int memory, int disk, String imageName, UserData userData, String publicKey) {
        super(id, localToken, federationToken, requestingMember, providingMember);
        this.vCPU = vCPU;
        this.memory = memory;
        this.disk = disk;
        this.imageName = imageName;
        this.userData = userData;
        this.publicKey = publicKey;
    }

    public ComputeOrder(Token localToken, Token federationToken, String requestingMember, String providingMember,
                        int vCPU, int memory, int disk, String imageName, UserData userData, String publicKey) {
        this(UUID.randomUUID().toString(), localToken, federationToken, requestingMember, providingMember, vCPU, memory,
                disk, imageName, userData, publicKey);
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

	public void setvCPU(Integer vCPU) {
		this.vCPU = vCPU;
	}

	public void setMemory(Integer memory) {
		this.memory = memory;
	}

	public void setDisk(Integer disk) {
		this.disk = disk;
	}

	public void setUserData(UserData userData) {
		this.userData = userData;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
}
