package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.models.token.Token;

import java.util.UUID;

public class ComputeOrder extends Order {

	private int vCPU;

	/** Memory attribute, must be set in MB. */
	private int memory;

	/** Disk attribute, must be set in GB. */
	private int disk;

	private String imageName;

	private UserData userData;

	/**
	 * Creating Order with predefined Id.
	 */
	public ComputeOrder(String id, Token localToken, Token federationToken, String requestingMember,
			String providingMember, int vCPU, int memory, int disk, String imageName, UserData userData) {
		super(id, localToken, federationToken, requestingMember, providingMember);
		this.vCPU = vCPU;
		this.memory = memory;
		this.disk = disk;
		this.imageName = imageName;
		this.userData = userData;
	}

	public ComputeOrder(Token localToken, Token federationToken, String requestingMember, String providingMember,
			int vCPU, int memory, int disk, String imageName, UserData userData) {
		this(UUID.randomUUID().toString(), localToken, federationToken, requestingMember, providingMember, vCPU, memory,
				disk, imageName, userData);
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

	/**
	 * These method handle an open order, for this, handleOpenOrder handle the
	 * Order to be ready to change your state from OPEN to SPAWNING.
	 */
	@Override
	public synchronized void handleOpenOrder() {
		// TODO:
	}
}
