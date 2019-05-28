package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import java.util.Map;

import cloud.fogbow.ras.core.models.HardwareRequirements;

public class AwsHardwareRequirements extends HardwareRequirements {

	public AwsHardwareRequirements(String name, String flavorId, int cpu, int memory, int disk,
			Map<String, String> requirements) {
		
		super(name, flavorId, cpu, memory, disk);
		this.setRequirements(requirements);
	}

	private Map<String, String> requirements;

	public Map<String, String> getRequirements() {
		return requirements;
	}

	public void setRequirements(Map<String, String> requirements) {
		this.requirements = requirements;
	}
}
