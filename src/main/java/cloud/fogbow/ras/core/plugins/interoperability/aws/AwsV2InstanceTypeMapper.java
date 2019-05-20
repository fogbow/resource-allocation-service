package cloud.fogbow.ras.core.plugins.interoperability.aws;

import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.ras.core.plugins.interoperability.aws.compute.v2.AwsFlavour;
import software.amazon.awssdk.services.ec2.model.InstanceType;

public class AwsV2InstanceTypeMapper {

	public static InstanceType map(int cpu, double memory) throws NoAvailableResourcesException {
		for (AwsFlavour flavour : AwsFlavour.values()) {
			if (flavour.getVCpu() <= cpu && flavour.getMemory() <= memory) {
				return InstanceType.fromValue(flavour.getName());
			}
		}
		throw new NoAvailableResourcesException("Not found compatible values of vcpu or memory.");
	}
	
}
