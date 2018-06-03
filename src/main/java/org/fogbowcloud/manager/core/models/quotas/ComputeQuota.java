package org.fogbowcloud.manager.core.models.quotas;

import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;

public class ComputeQuota implements Quota {

	private ComputeAllocation totalQuota;
	private ComputeAllocation usedQuota;
	
	public ComputeQuota(ComputeAllocation totalQuota, ComputeAllocation usedQuota) {
		this.totalQuota = totalQuota;
		this.usedQuota = usedQuota;
	}
	
	public ComputeAllocation getTotalQuota() {
		return this.totalQuota;
	}
	
	public ComputeAllocation getUsedQuota() {
		return this.usedQuota;
	}
	
	public void setTotalQuota(ComputeAllocation totalQuota) {
		this.totalQuota = totalQuota;
	}
	
	public void setUsedQuota(ComputeAllocation usedQuota) {
		this.usedQuota = usedQuota;
	}
	
	public ComputeAllocation getAvailableQuota() {
		int availableVCpu = this.totalQuota.getvCPU() - this.usedQuota.getvCPU();
		int availableRam = this.totalQuota.getRam() - this.usedQuota.getRam();
		int availableInstance = this.totalQuota.getInstances() - this.usedQuota.getInstances();
		return new ComputeAllocation(availableVCpu, availableRam, availableInstance);
	}
	
}
