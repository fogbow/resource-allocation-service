package cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import cloud.fogbow.ras.core.models.HardwareRequirements;

public class AwsHardwareRequirements extends HardwareRequirements {

	private static final String[] COST_LEVEL = { "a1", "t2", "t1", "t3", "t3a", "m1", "m3", "m4", "m5", "m5d", "m5a",
			"m5ad", "c1", "c3", "c4", "c5", "c5d", "c5n", "i3", "i3en", "d2", "h1", "r4", "r5", "r5d", "r5a", "r5ad",
			"r3", "m2", "x1e", "x1", "u-6tb1", "u-9tb1", "u-12tb1", "z1d", "p3", "p3dn", "p2", "g3s", "g3", "f1" };
	
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
			comparisonValue = costX.compareTo(costY);
			if (comparisonValue == ZERO_VALUE) {
				return this.getFlavorId().compareTo(hardwareRequirements.getFlavorId());
			}
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
