package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.instanceprovider.InstanceProvider;
import org.fogbowcloud.manager.core.models.token.Token;

import javax.persistence.*;

@Entity
@DiscriminatorValue(value = "compute")
public class ComputeOrder extends Order {

	@Column(name = "vCPU")
	private int vCPU;

	/** Memory attribute, must be set in MB. */
	@Column(name = "memory")
	private int memory;

	/** Disk attribute, must be set in GB. */
	@Column(name = "disk")
	private int disk;

	@Column(name = "image_name")
	private String imageName;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "user_data_id")
	private UserData userData;

	public ComputeOrder() {
	}

	/**
	 * Creating Order with predefined Id.
	 */
	public ComputeOrder(String id, Token localToken, Token federationToken, String requestingMember, String providingMember,
			int vCPU, int memory, int disk, String imageName, UserData userData) {
		super(id, localToken, federationToken, requestingMember, providingMember);
		this.vCPU = vCPU;
		this.memory = memory;
		this.disk = disk;
		this.imageName = imageName;
		this.userData = userData;
	}
	
	public ComputeOrder(Token localToken, Token federationToken, String requestingMember, String providingMember,
			int vCPU, int memory, int disk, String imageName, UserData userData) {
		super(localToken, federationToken, requestingMember, providingMember);
		this.vCPU = vCPU;
		this.memory = memory;
		this.disk = disk;
		this.imageName = imageName;
		this.userData = userData;
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

	@Override
	public OrderType getType() {
		return OrderType.COMPUTE;
	}

	/**
	 * These method handle and request an open order, for this, processOpenOrder
	 * handle the Order to be ready to change your state and request the
	 * Instance from the InstanceProvider.
	 */
	@Override
	public void processOpenOrder(InstanceProvider instanceProvider) {
		super.processOpenOrder(instanceProvider);
	}
}
