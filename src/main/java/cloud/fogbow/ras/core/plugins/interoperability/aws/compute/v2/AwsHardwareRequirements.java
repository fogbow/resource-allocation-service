package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cloud.fogbow.ras.core.models.HardwareRequirements;

public class AwsHardwareRequirements extends HardwareRequirements {

	private static final String[] COST_LEVEL = {"t1", "a1", "t3", "t3a", "t2", "m5", "m5d", "m5a", "m5ad", "m4"};
	private static final String TOKEN_SEPARATOR = "[.]";
	private static final int ZERO_VALUE = 0;

	public AwsHardwareRequirements(String name, String flavorId, int cpu, int memory, int disk,
			String imageId, Map<String, String> requirements) {
		
		super(name, flavorId, cpu, memory, disk);
		this.setImageId(imageId);
		this.setRequirements(requirements);
	}
	
	private String imageId;
	private Map<String, String> requirements;
	
	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public Map<String, String> getRequirements() {
		return requirements;
	}

	public void setRequirements(Map<String, String> requirements) {
		this.requirements = requirements;
	}
	
	@Override
	public int compareTo(HardwareRequirements hardwareRequirements) {
		int comparisonValue = super.compareTo(hardwareRequirements);
		if (comparisonValue == ZERO_VALUE) {
			List<String> costLevels = Arrays.asList(COST_LEVEL);
			Integer costX = getCostLevel(costLevels, this.getName());
			Integer costY = getCostLevel(costLevels, hardwareRequirements.getName());
			return costX.compareTo(costY);
		}
		return comparisonValue;
	}

	private int getCostLevel(List<String> list, String arg) {
		String prefix = getPrefix(arg);
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).equals(prefix)) {
				return i+1;
			}
		}
		return 0;
	}
	
	private String getPrefix(String arg) {
		String[] slice = arg.split(TOKEN_SEPARATOR);
		return slice[ZERO_VALUE];
	}
}
