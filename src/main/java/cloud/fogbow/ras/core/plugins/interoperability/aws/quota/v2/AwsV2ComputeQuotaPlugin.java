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

	private static final String DEFAULT_INSTANCE_TYPE = "t1.micro";
	private static final String COMMENTED_LINE_PREFIX = "#";
	private static final String CSV_COLUMN_SEPARATOR = ",";

	private static final int INSTANCE_TYPE_COLUMN = 0;
	private static final int LIMITS_COLUMN = 11;
	private static final int MAXIMUM_STORAGE_VALUE = 300;
	private static final int MEMORY_COLUMN = 2;
	private static final int ONE_GIGABYTE = 1024;
	private static final int ONE_TERABYTE = 1000;
	private static final int VCPU_COLUMN = 1;

	private Map<String, ComputeAllocation> allocatedInstancesMap;
	private Map<String, ComputeAllocation> availableInstanceMap;
	private String flavorsFilePath;
	private String region;

	public AwsV2ComputeQuotaPlugin(String confFilePath) {
		Properties properties = PropertiesUtil.readProperties(confFilePath);
		this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
		this.flavorsFilePath = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_FLAVORS_TYPES_FILE_PATH_KEY);
		this.allocatedInstancesMap = new HashMap<String, ComputeAllocation>();
		this.availableInstanceMap = new HashMap<String, ComputeAllocation>();
	}

	@Override
	public ComputeQuota getUserQuota(AwsV2User cloudUser) throws FogbowException {
		updateAllocationsMap();
		Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);
		Map<String, ComputeQuota> quotas = loadQuotasByInstances(client);
		return calculateQuotas(quotas);
	}

	private ComputeQuota calculateQuotas(Map<String, ComputeQuota> quotas) {
		ComputeAllocation totalQuota;
		ComputeAllocation usedQuota;
		ComputeQuota result = null;
		int totalDisk = MAXIMUM_STORAGE_VALUE * ONE_TERABYTE;
		int totalInstances = 0;
		int totalRam = 0;
		int totalVCPU = 0;
		int usedDisk = 0;
		int usedInstances = 0;
		int usedRam = 0;
		int usedVCPU = 0;

		for (Entry<String, ComputeQuota> quota : quotas.entrySet()) {
			totalInstances += quota.getValue().getTotalQuota().getInstances();
			totalVCPU += quota.getValue().getTotalQuota().getvCPU();
			totalRam += quota.getValue().getTotalQuota().getRam();
			totalQuota = new ComputeAllocation(totalVCPU, totalRam, totalInstances, totalDisk);

			usedInstances += quota.getValue().getUsedQuota().getInstances();
			usedVCPU += quota.getValue().getUsedQuota().getvCPU();
			usedRam += quota.getValue().getUsedQuota().getRam();
			usedDisk += quota.getValue().getUsedQuota().getDisk();
			usedQuota = new ComputeAllocation(usedVCPU, usedRam, usedInstances, usedDisk);

			result = new ComputeQuota(totalQuota, usedQuota);
		}
		return result;
	}

	private Map<String, ComputeQuota> loadQuotasByInstances(Ec2Client client) throws FogbowException {
		Map<String, ComputeQuota> instanceQuotas = new HashMap<String, ComputeQuota>();
		ComputeAllocation totalAllocation;
		ComputeAllocation usedAllocation;
		ComputeQuota quota;

		List<Instance> instanceList = getInstanceReservation(client);
		if (!instanceList.isEmpty()) {
			for (Instance instance : instanceList) {
				String instanceType = instance.instanceTypeAsString();
				totalAllocation = calculateTotalAllocation(instanceType);
				usedAllocation = calculateUsedAllocation(instance, client);
				quota = new ComputeQuota(totalAllocation, usedAllocation);
				instanceQuotas.put(instanceType, quota);
			}
		} else {
			totalAllocation = calculateTotalAllocation(DEFAULT_INSTANCE_TYPE);
			usedAllocation = new ComputeAllocation();
			quota = new ComputeQuota(totalAllocation, usedAllocation);
			instanceQuotas.put(DEFAULT_INSTANCE_TYPE, quota);
		}
		return instanceQuotas;
	}

	private ComputeAllocation calculateUsedAllocation(Instance instance, Ec2Client client) throws FogbowException {
		String instanceType = instance.instanceTypeAsString();
		ComputeAllocation allocationAvailable = this.availableInstanceMap.get(instanceType);
		ComputeAllocation allocatedInstance = this.allocatedInstancesMap.get(instanceType);
		int instances = allocatedInstance != null ? allocatedInstance.getInstances() + 1 : 1;
		int vCPU = allocationAvailable.getvCPU() * instances;
		int ram = allocationAvailable.getRam() * instances;
		int disk = allocatedInstance != null ? allocatedInstance.getDisk() : 0;
		List<Volume> volumes = getInstanceVolumes(instance, client);
		disk += getAllDisksSize(volumes);
		allocatedInstance = new ComputeAllocation(vCPU, ram, instances, disk);
		this.allocatedInstancesMap.put(instanceType, allocatedInstance);
		return allocatedInstance;
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

	private ComputeAllocation calculateTotalAllocation(String instanceType) {
		ComputeAllocation allocationAvailable = this.availableInstanceMap.get(instanceType);
		int instances = allocationAvailable.getInstances();
		int vCPU = allocationAvailable.getvCPU() * instances;
		int ram = allocationAvailable.getRam() * instances;
		int disk = MAXIMUM_STORAGE_VALUE * ONE_TERABYTE;
		return new ComputeAllocation(vCPU, ram, instances, disk);
	}

	private List<Instance> getInstanceReservation(Ec2Client client) throws FogbowException {
		DescribeInstancesResponse response = doDescribeInstances(client);
		Reservation reservation = response.reservations().listIterator().next();
		return reservation.instances();
	}

	protected DescribeInstancesResponse doDescribeInstances(Ec2Client client) throws FogbowException {
		try {
			return client.describeInstances();
		} catch (SdkException e) {
			throw new UnexpectedException(String.format(Messages.Exception.GENERIC_EXCEPTION, e), e);
		}
	}

	private void updateAllocationsMap() throws FogbowException {
		List<String> lines = loadLinesFromFlavorFile();
		String[] requirements;
		String instanceType;
		ComputeAllocation allocation;
		for (String line : lines) {
			if (!line.startsWith(COMMENTED_LINE_PREFIX)) {
				requirements = line.split(CSV_COLUMN_SEPARATOR);
				instanceType = requirements[INSTANCE_TYPE_COLUMN];
				allocation = mountInstanceAllocation(requirements);
				this.availableInstanceMap.put(instanceType, allocation);
			}
		}
	}

	private ComputeAllocation mountInstanceAllocation(String[] requirements) {
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