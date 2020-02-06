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
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ConfigurationPropertyKeys;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import javax.validation.constraints.NotNull;

/**
 * This class calculates the quota of available instances according to the
 * disparity between the amount of resource for each instance type listed in the
 * flavors.csv file, and the amount of resource for each instance used by the
 * user.
 */
public class AwsV2QuotaPlugin implements QuotaPlugin<AwsV2User> {

    private static final Logger LOGGER = Logger.getLogger(AwsV2QuotaPlugin.class);

    protected static final String COMMENTED_LINE_PREFIX = "#";
    protected static final String CSV_COLUMN_SEPARATOR = ",";

    protected static final int INSTANCE_TYPE_COLUMN = 0;
    protected static final int VCPU_COLUMN = 1;
    protected static final int MEMORY_COLUMN = 2;
    protected static final int LIMITS_COLUMN = 11;
    protected static final int ONE_GIGABYTE = 1024;
    protected static final int ONE_TERABYTE = 1000;

    public static int maximumStorage;
    public static int maximumSubnets;
    public static int maximumPublicIpAddresses;

    private Map<String, ComputeAllocation> totalComputeAllocationMap;
    private Map<String, ComputeAllocation> computeAllocationMap;
    private String flavorsFilePath;
    private String region;

    public AwsV2QuotaPlugin(@NotNull String confFilePath) {
        Properties properties = PropertiesUtil.readProperties(confFilePath);
        this.region = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_REGION_SELECTION_KEY);
        this.flavorsFilePath = properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_FLAVORS_TYPES_FILE_PATH_KEY);
        this.maximumStorage = Integer.parseInt(properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_STORAGE_QUOTA_KEY));
        this.maximumSubnets = Integer.parseInt(properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_SUBNETS_QUOTA_KEY));
        this.maximumPublicIpAddresses = Integer.parseInt(properties.getProperty(AwsV2ConfigurationPropertyKeys.AWS_ELASTIC_IP_ADDRESSES_QUOTA_KEY));
        this.totalComputeAllocationMap = new HashMap<String, ComputeAllocation>();
        this.computeAllocationMap = new HashMap<String, ComputeAllocation>();
    }

    @Override
    public ResourceQuota getUserQuota(@NotNull AwsV2User cloudUser) throws FogbowException {
        Ec2Client client = AwsV2ClientUtil.createEc2Client(cloudUser.getToken(), this.region);

        loadAvailableAllocations();
        loadInstancesAllocated(client);

        ResourceAllocation totalQuota = calculateTotalQuota();
        ResourceAllocation usedQuota = calculateUsedQuota(client);
        return new ResourceQuota(totalQuota, usedQuota);
    }

    @VisibleForTesting
    ResourceAllocation calculateUsedQuota(@NotNull Ec2Client client) throws FogbowException {
        ComputeAllocation computeAllocation = this.calculateComputeUsedQuota();
        int volumesDiskUsage = this.calculateVolumesUsage(client);
        int elasticIps = this.calculateUsedElasticIp(client);
        int subnets = this.calculateUsedSubnets(client);

        ResourceAllocation allocation = ResourceAllocation.builder()
                .ram(computeAllocation.getRam())
                .vCPU(computeAllocation.getvCPU())
                .instances(computeAllocation.getInstances())
                .storage(volumesDiskUsage)
                .networks(subnets)
                .publicIps(elasticIps)
                .build();

        return allocation;
    }

    @VisibleForTesting
    int calculateUsedSubnets(@NotNull Ec2Client client) throws FogbowException {
        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder().build();
        DescribeSubnetsResponse response = AwsV2CloudUtil.doDescribeSubnetsRequest(request, client);
        return response.subnets().size();
    }

    @VisibleForTesting
    int calculateUsedElasticIp(@NotNull Ec2Client client) throws FogbowException {
        DescribeAddressesRequest request = DescribeAddressesRequest.builder().build();
        DescribeAddressesResponse response = AwsV2CloudUtil.doDescribeAddressesRequests(request, client);
        return response.addresses().size();
    }

    @VisibleForTesting
    int calculateVolumesUsage(@NotNull Ec2Client client) throws FogbowException {
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
                .networks(maximumSubnets)
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
        return new ComputeAllocation(usedVCPU, usedRam, usedInstances);
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
        return new ComputeAllocation(totalVCPU, totalRam, totalInstances);
    }

    @VisibleForTesting
    void loadInstancesAllocated(@NotNull Ec2Client client) throws FogbowException {
        List<Instance> instances = getInstanceReservations(client);
        ComputeAllocation allocation;
        if (!instances.isEmpty()) {
            for (Instance instance : instances) {
                String instanceType = instance.instanceTypeAsString();
                allocation = buildAllocatedInstance(instance, client);
                this.computeAllocationMap.put(instanceType, allocation);
            }
        }
    }

    @VisibleForTesting
    ComputeAllocation buildAllocatedInstance(@NotNull Instance instance, @NotNull Ec2Client client) throws FogbowException {
        String instanceType = instance.instanceTypeAsString();
        ComputeAllocation totalAllocation = getTotalComputeAllocationMap().get(instanceType);
        ComputeAllocation allocatedInstance = getComputeAllocationMap().get(instanceType);
        int instances = allocatedInstance != null ? allocatedInstance.getInstances() + 1 : 1;
        int vCPU = totalAllocation.getvCPU() * instances;
        int ram = totalAllocation.getRam() * instances;
        return new ComputeAllocation(vCPU, ram, instances);
    }

    @VisibleForTesting
    int getAllVolumesSize(@NotNull List<Volume> volumes) {
        int size = 0;
        for (Volume volume : volumes) {
            size += volume.size();
        }
        return size;
    }

    @VisibleForTesting
    List<Instance> getInstanceReservations(@NotNull Ec2Client client) throws FogbowException {
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
    ComputeAllocation buildAvailableInstance(@NotNull String[] requirements) {
        int instances = Integer.parseInt(requirements[LIMITS_COLUMN]);
        int vCPU = Integer.parseInt(requirements[VCPU_COLUMN]);
        Double memory = Double.parseDouble(requirements[MEMORY_COLUMN]) * ONE_GIGABYTE;
        int ram = memory.intValue();
        return new ComputeAllocation(vCPU, ram, instances);
    }

    @VisibleForTesting
    List<String> loadLinesFromFlavorFile() throws FogbowException {
        String flavorsPath = this.getFlavorsFilePath();
        Path path = Paths.get(flavorsPath);
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            String message = String.format(Messages.Error.ERROR_MESSAGE, e);
            LOGGER.error(message, e);
            throw new ConfigurationErrorException(message);
        }
    }

    // These methods are used to assist in testing.

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