package cloud.fogbow.ras.core.plugins.interoperability.aws.quota.v2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.ComputeQuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Volume;

public class AwsV2ComputeQuotaPlugin implements ComputeQuotaPlugin<AwsV2User> {

	private static final Logger LOGGER = Logger.getLogger(AwsV2ComputeQuotaPlugin.class);

	protected static final String COMMENTED_LINE_PREFIX = "#";
	protected static final String CSV_COLUMN_SEPARATOR = ",";
	
	protected static final int INSTANCE_TYPE_COLUMN = 0;
	protected static final int LIMITS_COLUMN = 11;
	protected static final int MAXIMUM_STORAGE_VALUE = 300;
	protected static final int MEMORY_COLUMN = 2;
	protected static final int ONE_GIGABYTE = 1024;
	protected static final int ONE_TERABYTE = 1000;
	protected static final int VCPU_COLUMN = 1;

	private Map<String, ComputeAllocation> availableAllocationsMap;
	private Map<String, ComputeAllocation> instancesAllocatedMap;
	private String flavorsFilePath;
	private String region;

	public AwsV2ComputeQuotaPlugin(String confFilePath) {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
		this.flavorsFilePath = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_FLAVORS_TYPES_FILE_PATH_KEY);
		this.availableAllocationsMap = new HashMap<String, ComputeAllocation>();
		this.instancesAllocatedMap = new HashMap<String, ComputeAllocation>();
	}

	@Override
	public ComputeQuota getUserQuota(AwsV2User cloudUser) throws FogbowException {
		loadAvailableAllocations();

		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		loadInstancesAllocated(client);

		ComputeAllocation totalQuota = calculateTotalQuota();
		ComputeAllocation usedQuota = calculateUsedQuota();
		return new ComputeQuota(totalQuota, usedQuota);
	}

	private ComputeAllocation calculateUsedQuota() {
		int usedDisk = 0;
		int usedInstances = 0;
		int usedRam = 0;
		int usedVCPU = 0;

		for (Entry<String, ComputeAllocation> instanceAllocated : this.instancesAllocatedMap.entrySet()) {
			usedInstances += instanceAllocated.getValue().getInstances();
			usedVCPU += instanceAllocated.getValue().getvCPU();
			usedRam += instanceAllocated.getValue().getRam();
			usedDisk += instanceAllocated.getValue().getDisk();
		}
		return new ComputeAllocation(usedVCPU, usedRam, usedInstances, usedDisk);
	}

	private ComputeAllocation calculateTotalQuota() {
		int totalDisk = MAXIMUM_STORAGE_VALUE * ONE_TERABYTE;
		int totalInstances = 0;
		int totalRam = 0;
		int totalVCPU = 0;

		for (Entry<String, ComputeAllocation> availableAllocation : this.availableAllocationsMap.entrySet()) {
			totalInstances += availableAllocation.getValue().getInstances();
			totalVCPU += availableAllocation.getValue().getvCPU();
			totalRam += availableAllocation.getValue().getRam();
		}
		return new ComputeAllocation(totalVCPU, totalRam, totalInstances, totalDisk);
	}

	private void loadInstancesAllocated(Ec2Client client) throws FogbowException {
		List<Instance> instances = getInstanceReservation(client);
		ComputeAllocation allocation;
		if (!instances.isEmpty()) {
			for (Instance instance : instances) {
				String instanceType = instance.instanceTypeAsString();
				allocation = mountAllocatedInstance(instance, client);
				this.instancesAllocatedMap.put(instanceType, allocation);
			}
		}
	}

	private ComputeAllocation mountAllocatedInstance(Instance instance, Ec2Client client) throws FogbowException {
		String instanceType = instance.instanceTypeAsString();
		ComputeAllocation allocationAvailable = this.availableAllocationsMap.get(instanceType);
		ComputeAllocation allocatedInstance = this.instancesAllocatedMap.get(instanceType);
		int instances = allocatedInstance != null ? allocatedInstance.getInstances() + 1 : 1;
		int vCPU = allocationAvailable.getvCPU() * instances;
		int ram = allocationAvailable.getRam() * instances;
		int disk = allocatedInstance != null ? allocatedInstance.getDisk() : 0;
		List<Volume> volumes = getInstanceVolumes(instance, client);
		disk += getAllDisksSize(volumes);
		return new ComputeAllocation(vCPU, ram, instances, disk);
	}

	private int getAllDisksSize(List<Volume> volumes) {
		int size = 0;
		for (Volume volume : volumes) {
			size += volume.size();
		}
		return size;
	}

	private List<Volume> getInstanceVolumes(Instance instance, Ec2Client client) throws FogbowException {
		List<Volume> volumes = new ArrayList<>();
		DescribeVolumesResponse response;
		List<String> volumeIds = getVolumeIds(instance);
		for (String volumeId : volumeIds) {
			response = doDescribeVolumes(volumeId, client);
			volumes.addAll(response.volumes());
		}
		return volumes;
	}

	protected DescribeVolumesResponse doDescribeVolumes(String volumeId, Ec2Client client) throws FogbowException {
		DescribeVolumesRequest request = DescribeVolumesRequest.builder()
				.volumeIds(volumeId)
				.build();
		try {
			return client.describeVolumes(request);
		} catch (SdkException e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private List<String> getVolumeIds(Instance instance) {
		List<String> volumeIds = new ArrayList<String>();
		String volumeId;
		for (int i = 0; i < instance.blockDeviceMappings().size(); i++) {
			volumeId = instance.blockDeviceMappings().get(i).ebs().volumeId();
			volumeIds.add(volumeId);
		}
		return volumeIds;
	}

	private List<Instance> getInstanceReservation(Ec2Client client) throws FogbowException {
		DescribeInstancesResponse response = doDescribeInstances(client);
		List<Instance> instances = new ArrayList<>();
		for (Reservation reservation : response.reservations()) {
			instances.addAll(reservation.instances());
		}
		return instances;
	}

	protected DescribeInstancesResponse doDescribeInstances(Ec2Client client) throws FogbowException {
		try {
			return client.describeInstances();
		} catch (SdkException e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private void loadAvailableAllocations() throws FogbowException {
		List<String> lines = loadLinesFromFlavorFile();
		String[] requirements;
		String instanceType;
		ComputeAllocation allocation;
		for (String line : lines) {
			if (!line.startsWith(COMMENTED_LINE_PREFIX)) {
				requirements = line.split(CSV_COLUMN_SEPARATOR);
				instanceType = requirements[INSTANCE_TYPE_COLUMN];
				allocation = mountAvailableInstance(requirements);
				this.availableAllocationsMap.put(instanceType, allocation);
			}
		}
	}

	private ComputeAllocation mountAvailableInstance(String[] requirements) {
		int instances = Integer.parseInt(requirements[LIMITS_COLUMN]);
		int vCPU = Integer.parseInt(requirements[VCPU_COLUMN]);
		Double memory = Double.parseDouble(requirements[MEMORY_COLUMN]) * ONE_GIGABYTE;
		int ram = memory.intValue();
		return new ComputeAllocation(vCPU, ram, instances);
	}

	protected List<String> loadLinesFromFlavorFile() throws FogbowException {
		String file = getFlavorsFilePath();
		Path path = Paths.get(file);
		try {
			return Files.readAllLines(path);
		} catch (IOException e) {
			LOGGER.error(String.format(Messages.Error.ERROR_MESSAGE, e), e);
			throw new ConfigurationErrorException();
		}
	}

	// This method is used to aid in the tests
	protected String getFlavorsFilePath() {
		return flavorsFilePath;
	}

}