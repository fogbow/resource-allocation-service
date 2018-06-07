package org.fogbowcloud.manager.core.models.quotas.allocation;

public class ComputeAllocation extends Allocation {

	private int vCPU;
	private int ram;
	private int disk;
	
	public ComputeAllocation(int vCPU, int ram, int disk) {
		this.vCPU = vCPU;
		this.ram = ram;
		this.disk = disk;
	}
	
	public int getvCPU() {
		return this.vCPU;
	}

	public int getRam() {
		return this.ram;
	}

	public int getDisk() {
		return this.disk;
	}

	public int getInstances() {
		return this.disk;
	}	
	
}
