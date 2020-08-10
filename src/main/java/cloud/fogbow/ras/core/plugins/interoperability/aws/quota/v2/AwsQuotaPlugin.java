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

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.util.BinaryUnit;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;
import com.google.common.annotations.VisibleForTesting;
import org.apache.log4j.Logger;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

/**
 * This class calculates the quota of available instances according to the
 * disparity between the amount of resource for each instance type listed in the
 * flavors.csv file, and the amount of resource for each instance used by the
 * user.
 */
public class AwsQuotaPlugin implements QuotaPlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsQuotaPlugin.class);

    @VisibleForTesting
    static final String COMMENTED_LINE_PREFIX = "#";
    @VisibleForTesting
    static final String CSV_COLUMN_SEPARATOR = ",";

    @VisibleForTesting
    static final int INSTANCE_TYPE_COLUMN = 0;
    @VisibleForTesting
    static final int VCPU_COLUMN = 1;
    @VisibleForTesting
    static final int MEMORY_COLUMN = 2;
    @VisibleForTesting
    static final int LIMITS_COLUMN = 11;

    public static int maximumStorage;
    public static int maximumNetworks;
    public static int maximumPublicIpAddresses;

    private Map<String, ComputeAllocation> totalComputeAllocationMap;
    private Map<String, ComputeAllocation> computeAllocationMap;
    private String flavorsFilePath;
    private String region;

    public AwsQuotaPlugin(String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        maximumStorage = Integer.parseInt(properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_STORAGE_QUOTA_KEY));
        maximumNetworks = Integer.parseInt(properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_VPC_QUOTA_KEY));
        maximumPublicIpAddresses = Integer.parseInt(properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_ELASTIC_IP_ADDRESSES_QUOTA_KEY));
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
        this.flavorsFilePath = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_FLAVORS_TYPES_FILE_PATH_KEY);
        this.totalComputeAllocationMap = new HashMap<>();
        this.computeAllocationMap = new HashMap<>();
    }

    @Override
    public ResourceQuota getUserQuota(AwsV2User cloudUser) throws FogbowException {
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);

        loadAvailableAllocations();
        loadInstancesAllocated(client);

        ResourceAllocation totalQuota = calculateTotalQuota();
        ResourceAllocation usedQuota = calculateUsedQuota(client);
        return new ResourceQuota(totalQuota, usedQuota);
    }

    @VisibleForTesting
    ResourceAllocation calculateUsedQuota(Ec2Client client) throws FogbowException {
        ComputeAllocation computeAllocation = this.calculateComputeUsedQuota();
        int storage = this.calculateUsedStorage(client);
        int elasticIps = this.calculateUsedElasticIp(client);
        int networks = this.calculateUsedNetworks(client);
        int volumes = this.calculateUsedVolumes(client);

        ResourceAllocation allocation = ResourceAllocation.builder()
                .ram(computeAllocation.getRam())
                .vCPU(computeAllocation.getvCPU())
                .instances(computeAllocation.getInstances())
                .storage(storage)
                .volumes(volumes)
                .networks(networks)
                .publicIps(elasticIps)
                .build();

        return allocation;
    }

    @VisibleForTesting
    int calculateUsedVolumes(Ec2Client client) {
        List<Volume> volumes = client.describeVolumes().volumes();
        return volumes.size();
    }

    @VisibleForTesting
    int calculateUsedNetworks(Ec2Client client) {
        List<Vpc> vpcs = client.describeVpcs().vpcs();
        return vpcs.size();
    }

    @VisibleForTesting
    int calculateUsedElasticIp(Ec2Client client) throws FogbowException {
        DescribeAddressesRequest request = DescribeAddressesRequest.builder().build();
        DescribeAddressesResponse response = AwsV2CloudUtil.doDescribeAddressesRequests(request, client);
        return response.addresses().size();
    }

    @VisibleForTesting
    int calculateUsedStorage(Ec2Client client) throws FogbowException {
        DescribeVolumesRequest request = DescribeVolumesRequest.builder().build();
        DescribeVolumesResponse response = AwsV2CloudUtil.doDescribeVolumesRequest(request, client);
        return getAllVolumesSize(response.volumes());
    }

    @VisibleForTesting
    ResourceAllocation calculateTotalQuota() {
        ComputeAllocation computeAllocation = this.calculateComputeTotalQuota();

        ResourceAllocation allocation = ResourceAllocation.builder()
                .ram(computeAllocation.getRam())
                .vCPU(computeAllocation.getvCPU())
                .instances(computeAllocation.getInstances())
                .storage(maximumStorage)
                .volumes(FogbowConstants.UNLIMITED_RESOURCE)
                .networks(maximumNetworks)
                .publicIps(maximumPublicIpAddresses)
                .build();

        return allocation;
    }

    @VisibleForTesting
    ComputeAllocation calculateComputeUsedQuota() {
        int usedInstances = 0;
        int usedRam = 0;
        int usedVCPU = 0;

        for (Entry<String, ComputeAllocation> instanceAllocated : getComputeAllocationMap().entrySet()) {
            usedInstances += instanceAllocated.getValue().getInstances();
            usedVCPU += instanceAllocated.getValue().getvCPU();
            usedRam += instanceAllocated.getValue().getRam();
        }
        return new ComputeAllocation(usedInstances, usedVCPU, usedRam);
    }

    @VisibleForTesting
    ComputeAllocation calculateComputeTotalQuota() {
        int totalInstances = 0;
        int totalRam = 0;
        int totalVCPU = 0;

        for (Entry<String, ComputeAllocation> availableAllocation : getTotalComputeAllocationMap().entrySet()) {
            totalInstances += availableAllocation.getValue().getInstances();
            totalVCPU += availableAllocation.getValue().getvCPU();
            totalRam += availableAllocation.getValue().getRam();
        }
        return new ComputeAllocation(totalInstances, totalVCPU, totalRam);
    }

    @VisibleForTesting
    void loadInstancesAllocated(Ec2Client client) throws FogbowException {
        List<Instance> instances = getInstanceReservations(client);
        ComputeAllocation allocation;
        if (!instances.isEmpty()) {
            for (Instance instance : instances) {
                String instanceType = instance.instanceTypeAsString();
                allocation = buildAllocatedInstance(instance);
                this.computeAllocationMap.put(instanceType, allocation);
            }
        }
    }

    @VisibleForTesting
    ComputeAllocation buildAllocatedInstance(Instance instance) {
        String instanceType = instance.instanceTypeAsString();
        ComputeAllocation totalAllocation = getTotalComputeAllocationMap().get(instanceType);
        ComputeAllocation allocatedInstance = getComputeAllocationMap().get(instanceType);
        int instances = allocatedInstance != null ? allocatedInstance.getInstances() + 1 : 1;
        int vCPU = totalAllocation.getvCPU() * instances;
        int ram = totalAllocation.getRam() * instances;
        return new ComputeAllocation(instances, vCPU, ram);
    }

    @VisibleForTesting
    int getAllVolumesSize(List<Volume> volumes) {
        int size = 0;
        for (Volume volume : volumes) {
            size += volume.size();
        }
        return size;
    }

    @VisibleForTesting
    List<Instance> getInstanceReservations(Ec2Client client) throws FogbowException {
        DescribeInstancesResponse response = AwsV2CloudUtil.doDescribeInstances(client);
        List<Instance> instances = new ArrayList<>();
        for (Reservation reservation : response.reservations()) {
            instances.addAll(reservation.instances());
        }
        return instances;
    }

    @VisibleForTesting
    void loadAvailableAllocations() throws FogbowException {
        List<String> lines = loadLinesFromFlavorFile();
        String[] requirements;
        String instanceType;
        ComputeAllocation allocation;
        for (String line : lines) {
            if (!line.startsWith(COMMENTED_LINE_PREFIX)) {
                requirements = line.split(CSV_COLUMN_SEPARATOR);
                instanceType = requirements[INSTANCE_TYPE_COLUMN];
                allocation = buildAvailableInstance(requirements);
                this.totalComputeAllocationMap.put(instanceType, allocation);
            }
        }
    }

    @VisibleForTesting
    ComputeAllocation buildAvailableInstance(String[] requirements) {
        int instances = Integer.parseInt(requirements[LIMITS_COLUMN]);
        int vCPU = Integer.parseInt(requirements[VCPU_COLUMN]);
        Double memoryInGB = Double.parseDouble(requirements[MEMORY_COLUMN]);
        double memoryInMB = BinaryUnit.gigabytes(memoryInGB).asMegabytes();
        int ram = (int) Math.ceil(memoryInMB);
        return new ComputeAllocation(instances, vCPU, ram);
    }

    @VisibleForTesting
    List<String> loadLinesFromFlavorFile() throws FogbowException {
        String flavorsPath = this.getFlavorsFilePath();
        Path path = Paths.get(flavorsPath);
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new ConfigurationErrorException(e.getMessage());
        }
    }

    @VisibleForTesting
    Map<String, ComputeAllocation> getTotalComputeAllocationMap() {
        return totalComputeAllocationMap;
    }

    @VisibleForTesting
    Map<String, ComputeAllocation> getComputeAllocationMap() {
        return computeAllocationMap;
    }

    @VisibleForTesting
    String getFlavorsFilePath() {
        return flavorsFilePath;
    }
}