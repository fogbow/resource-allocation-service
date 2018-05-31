package org.fogbowcloud.manager.core.models.quotas;

public class ComputeQuota {

	private ComputeQuotaInfo totalQuota;
	private ComputeQuotaInfo usedQuota;
	
	public ComputeQuota(ComputeQuotaInfo totalQuota, ComputeQuotaInfo usedQuota) {
		this.totalQuota = totalQuota;
		this.usedQuota = usedQuota;
	}
	
	public ComputeQuotaInfo getTotalQuota() {
		return this.totalQuota;
	}
	
	public ComputeQuotaInfo getUsedQuota() {
		return this.usedQuota;
	}
	
	public void setTotalQuota(ComputeQuotaInfo totalQuota) {
		this.totalQuota = totalQuota;
	}
	
	public void setUsedQuota(ComputeQuotaInfo usedQuota) {
		this.usedQuota = usedQuota;
	}
	
	public ComputeQuotaInfo getAvailableQuota() {
		int availableVCpu = this.totalQuota.getvCPU() - this.usedQuota.getvCPU();
		int availableRam = this.totalQuota.getRam() - this.usedQuota.getRam();
		int availableInstance = this.totalQuota.getInstances() - this.usedQuota.getInstances();
		return new ComputeQuotaInfo(availableVCpu, availableRam, availableInstance);
	}
	
}
