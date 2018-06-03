package org.fogbowcloud.manager.core.models.quotas.allocation;

public class ComputeAllocation extends Allocation {

	private int vCPU;
	private int ram;
	private int instances;
	
	public ComputeAllocation(int vCPU, int ram, int instances) {
		this.vCPU = vCPU;
		this.ram = ram;
		this.instances = instances;
	}
	
	public int getvCPU() {
		return vCPU;
	}

	public int getRam() {
		return ram;
	}

	public int getInstances() {
		return instances;
	}	
	
}
