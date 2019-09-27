package cloud.fogbow.ras.core.plugins.interoperability.aws.quota.v2;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Volume;

@PrepareForTest({ AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class })
public class AwsV2ComputeQuotaPluginTest extends BaseUnitTests {

    private static final String CLOUD_NAME = "amazon";
    private static final String FAKE_VOLUME_ID = "fake-volume-id";
    private static final String FLAVOR_LINE_FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s";

    private static final int ONE_VALUE = 1;

    private AwsV2ComputeQuotaPlugin plugin;
    private Ec2Client client;

    @Before
    public void setUp() throws FogbowException {
        this.testUtils.mockReadOrdersFromDataBase();
        String awsConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.plugin = Mockito.spy(new AwsV2ComputeQuotaPlugin(awsConfFilePath));
        this.client = this.testUtils.getAwsMockedClient();
    }

    // test case: When calling the getUserQuota method, it must verify
    // that is call was successful and the expected results returned.
    @Test
    public void testGetUserQuota() throws FogbowException {
        // set up
        AwsV2User cloudUser = Mockito.mock(AwsV2User.class);

        Mockito.doNothing().when(this.plugin).loadAvailableAllocations();
        Mockito.doNothing().when(this.plugin).loadInstancesAllocated(Mockito.eq(this.client));

        ComputeAllocation totalQuota = createTotalComputeAllocation();
        Mockito.doReturn(totalQuota).when(this.plugin).calculateTotalQuota();

        ComputeAllocation usedQuota = createComputeAllocation();
        Mockito.doReturn(usedQuota).when(this.plugin).calculateUsedQuota();

        int instanceExpected = totalQuota.getInstances() - usedQuota.getInstances();
        int cpuExpected = totalQuota.getvCPU() - usedQuota.getvCPU();
        int ramExpected = totalQuota.getRam() - usedQuota.getRam();
        int diskExpected = totalQuota.getDisk() - usedQuota.getDisk();

        // exercise
        ComputeQuota quota = this.plugin.getUserQuota(cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).loadAvailableAllocations();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).loadInstancesAllocated(Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).calculateTotalQuota();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).calculateUsedQuota();

        Assert.assertEquals(instanceExpected, quota.getAvailableQuota().getInstances());
        Assert.assertEquals(cpuExpected, quota.getAvailableQuota().getvCPU());
        Assert.assertEquals(ramExpected, quota.getAvailableQuota().getRam());
        Assert.assertEquals(diskExpected, quota.getAvailableQuota().getDisk());
    }

    // test case: When calling the calculateUsedQuota method, it must verify that
    // returned the expected values.
    @Test
    public void testCalculateUsedQuota() {
        // set up
        Map<String, ComputeAllocation> instancesAllocated = createInstanceAllocatedMap();
        Mockito.doReturn(instancesAllocated).when(this.plugin).getInstancesAllocatedMap();

        int instanceExpected = ONE_VALUE;
        int cpuExpected = TestUtils.CPU_VALUE;
        int ramExpected = TestUtils.MEMORY_VALUE;
        int diskExpected = TestUtils.DISK_VALUE;

        // exercise
        ComputeAllocation usedQuota = this.plugin.calculateUsedQuota();

        // verify
        Assert.assertEquals(instanceExpected, usedQuota.getInstances());
        Assert.assertEquals(cpuExpected, usedQuota.getvCPU());
        Assert.assertEquals(ramExpected, usedQuota.getRam());
        Assert.assertEquals(diskExpected, usedQuota.getDisk());
    }

    // test case: When calling the calculateTotalQuota method, it must verify that
    // returned the expected values.
    @Test
    public void testCalculateTotalQuota() {
        // set up
        Map<String, ComputeAllocation> totalAllocations = createTotalAllocationMap();
        Mockito.doReturn(totalAllocations).when(this.plugin).getTotalAllocationsMap();

        int instanceExpected = ONE_VALUE * 2;
        int cpuExpected = TestUtils.CPU_VALUE * 2;
        int ramExpected = TestUtils.MEMORY_VALUE * 2;
        int diskExpected = AwsV2ComputeQuotaPlugin.MAXIMUM_STORAGE_VALUE * AwsV2ComputeQuotaPlugin.ONE_TERABYTE;

        // exercise
        ComputeAllocation usedQuota = this.plugin.calculateTotalQuota();

        // verify
        Assert.assertEquals(instanceExpected, usedQuota.getInstances());
        Assert.assertEquals(cpuExpected, usedQuota.getvCPU());
        Assert.assertEquals(ramExpected, usedQuota.getRam());
        Assert.assertEquals(diskExpected, usedQuota.getDisk());
    }

    // test case: When calling the loadInstancesAllocated method, it must verify
    // that is call was successful.
    @Test
    public void testLoadInstancesAllocated() throws FogbowException {
        // set up
        List<Instance> instances = buildInstancesCollections();
        Mockito.doReturn(instances).when(this.plugin).getInstanceReservations(Mockito.eq(this.client));

        Instance instance = instances.listIterator().next();
        ComputeAllocation allocation = createComputeAllocation();
        Mockito.doReturn(allocation).when(this.plugin).buildAllocatedInstance(Mockito.eq(instance),
                Mockito.eq(this.client));

        String expectedMapKey = InstanceType.T1_MICRO.toString();
        
        // exercise
        this.plugin.loadInstancesAllocated(this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getInstanceReservations(Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildAllocatedInstance(Mockito.eq(instance),
                Mockito.eq(this.client));
        
        Assert.assertTrue(this.plugin.getInstancesAllocatedMap().containsKey(expectedMapKey));
        Assert.assertEquals(allocation, this.plugin.getInstancesAllocatedMap().get(expectedMapKey));
    }

    // test case: When calling the buildAllocatedInstance method with an instance
    // type by the first time, it must verify that is call was successful and return
    // the expected result.
    @Test
    public void testBuildAllocatedInstanceByFirstTime() throws FogbowException {
        // set up
        Instance instance = buildInstance(InstanceType.T2_MICRO);

        Map<String, ComputeAllocation> totalAllocations = createTotalAllocationMap();
        Mockito.doReturn(totalAllocations).when(this.plugin).getTotalAllocationsMap();

        List<Volume> volumes = buildVolumesCollection();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        Mockito.when(AwsV2CloudUtil.getInstanceVolumes(Mockito.eq(instance), Mockito.eq(this.client)))
                .thenReturn(volumes);

        int instanceExpected = ONE_VALUE;
        int cpuExpected = TestUtils.CPU_VALUE;
        int ramExpected = TestUtils.MEMORY_VALUE;
        int diskExpected = TestUtils.DISK_VALUE;

        // exercise
        ComputeAllocation allocation = this.plugin.buildAllocatedInstance(instance, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getInstanceVolumes(Mockito.eq(instance), Mockito.eq(this.client));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalAllocationsMap();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getInstancesAllocatedMap();

        Assert.assertEquals(instanceExpected, allocation.getInstances());
        Assert.assertEquals(cpuExpected, allocation.getvCPU());
        Assert.assertEquals(ramExpected, allocation.getRam());
        Assert.assertEquals(diskExpected, allocation.getDisk());
    }

    // test case: When calling the buildAllocatedInstance method with an instance
    // type already allocated, it must verify that is call was successful with the
    // duplication of the expected result.
    @Test
    public void testBuildAllocatedInstanceFromOneExisting() throws FogbowException {
        // set up
        InstanceType existingInstanceType = InstanceType.T1_MICRO;
        Instance instance = buildInstance(existingInstanceType);

        Map<String, ComputeAllocation> totalAllocations = createTotalAllocationMap();
        Mockito.doReturn(totalAllocations).when(this.plugin).getTotalAllocationsMap();

        Map<String, ComputeAllocation> instancesAllocated = createInstanceAllocatedMap();
        Mockito.doReturn(instancesAllocated).when(this.plugin).getInstancesAllocatedMap();

        List<Volume> volumes = buildVolumesCollection();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        Mockito.when(AwsV2CloudUtil.getInstanceVolumes(Mockito.eq(instance), Mockito.eq(this.client)))
                .thenReturn(volumes);

        int instanceExpected = ONE_VALUE * 2;
        int cpuExpected = TestUtils.CPU_VALUE * 2;
        int ramExpected = TestUtils.MEMORY_VALUE * 2;
        int diskExpected = TestUtils.DISK_VALUE * 2;

        // exercise
        ComputeAllocation allocation = this.plugin.buildAllocatedInstance(instance, this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.getInstanceVolumes(Mockito.eq(instance), Mockito.eq(this.client));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalAllocationsMap();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getInstancesAllocatedMap();

        Assert.assertEquals(instanceExpected, allocation.getInstances());
        Assert.assertEquals(cpuExpected, allocation.getvCPU());
        Assert.assertEquals(ramExpected, allocation.getRam());
        Assert.assertEquals(diskExpected, allocation.getDisk());
    }
    
    // test case: When calling the getInstanceReservations method, it must verify
    // that is call was successful.
    @Test
    public void testGetInstanceReservations() throws FogbowException {
        // set up
        DescribeInstancesResponse response = buildDescribeInstance();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        Mockito.when(AwsV2CloudUtil.describeInstances(Mockito.eq(this.client))).thenReturn(response);

        List<Instance> expected = buildInstancesCollections();
        // exercise
        List<Instance> instances = this.plugin.getInstanceReservations(this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.describeInstances(Mockito.eq(this.client));
        
        Assert.assertEquals(expected, instances);
    }
    
    // test case: When calling the loadAvailableAllocations method, it must verify
    // that is call was successful.
    @Test
    public void testLoadAvailableAllocations() throws FogbowException {
        // set up
        String[] requirements = generateRequirements();
        String line = String.format(FLAVOR_LINE_FORMAT, requirements);
        Mockito.doReturn(Arrays.asList(line)).when(this.plugin).loadLinesFromFlavorFile();

        ComputeAllocation allocation = createComputeAllocation();
        Mockito.doReturn(allocation).when(this.plugin).buildAvailableInstance(Mockito.eq(requirements));

        String expectedMapKey = InstanceType.T1_MICRO.toString();

        // exercise
        this.plugin.loadAvailableAllocations();

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).loadLinesFromFlavorFile();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildAvailableInstance(Mockito.eq(requirements));

        Assert.assertTrue(this.plugin.getTotalAllocationsMap().containsKey(expectedMapKey));
        Assert.assertEquals(allocation, this.plugin.getTotalAllocationsMap().get(expectedMapKey));
    }
    
    // test case: When calling the buildAvailableInstance method, it must verify
    // that is call was successful and return the expected result.
    @Test
    public void testBuildAvailableInstance() {
        // set up
        String[] requirements = generateRequirements();

        int instanceExpected = ONE_VALUE;
        int cpuExpected = TestUtils.CPU_VALUE;
        int ramExpected = TestUtils.MEMORY_VALUE;
        
        // exercise
        ComputeAllocation allocation = this.plugin.buildAvailableInstance(requirements);

        // verify
        Assert.assertEquals(instanceExpected, allocation.getInstances());
        Assert.assertEquals(cpuExpected, allocation.getvCPU());
        Assert.assertEquals(ramExpected, allocation.getRam());
    }

    // test case: When calling the loadLinesFromFlavorFile method, without a valid
    // file path, the ConfigurationErrorException will be thrown.
    @Test
    public void testLoadLinesFromFlavorFile() throws FogbowException {
        // set up
        Mockito.doReturn(TestUtils.ANY_VALUE).when(this.plugin).getFlavorsFilePath();

        NoSuchFileException exception = new NoSuchFileException(TestUtils.ANY_VALUE);
        String expected = String.format(Messages.Error.ERROR_MESSAGE, exception);

        try {
            // exercise
            this.plugin.loadLinesFromFlavorFile();
            Assert.fail();
        } catch (ConfigurationErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    private String[] generateRequirements() {
        String[] requirements = { 
                InstanceType.T1_MICRO.toString(), 
                String.valueOf(TestUtils.CPU_VALUE), 
                String.valueOf(ONE_VALUE), 
                TestUtils.ANY_VALUE, 
                TestUtils.ANY_VALUE,
                TestUtils.ANY_VALUE,
                TestUtils.ANY_VALUE,
                TestUtils.ANY_VALUE,
                TestUtils.ANY_VALUE,
                TestUtils.ANY_VALUE,
                TestUtils.ANY_VALUE,
                String.valueOf(ONE_VALUE)
        };
        return requirements;
    }
    
    private DescribeInstancesResponse buildDescribeInstance() {
        DescribeInstancesResponse response = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(buildInstance(InstanceType.T1_MICRO))
                        .build())
                .build();
        
        return response;
    }

    private List<Volume> buildVolumesCollection() {
        Volume[] volumes = { Volume.builder()
                .volumeId(FAKE_VOLUME_ID)
                .size(TestUtils.DISK_VALUE)
                .build() 
        };
        return Arrays.asList(volumes);
    }

    private List<Instance> buildInstancesCollections() {
        Instance[] instances = { buildInstance(InstanceType.T1_MICRO) };
        return Arrays.asList(instances);
    }

    private Instance buildInstance(InstanceType instanceType) {
        Instance instance = Instance.builder()
                .instanceType(instanceType)
                .blockDeviceMappings(InstanceBlockDeviceMapping.builder()
                        .ebs(EbsInstanceBlockDevice.builder()
                                .volumeId(FAKE_VOLUME_ID)
                                .build())
                        .build())
                .build();

        return instance;
    }

    private Map<String, ComputeAllocation> createTotalAllocationMap() {
        Map<String, ComputeAllocation> totalAllocations = new HashMap();
        totalAllocations.put(InstanceType.T1_MICRO.toString(), createComputeAllocation());
        totalAllocations.put(InstanceType.T2_MICRO.toString(), createComputeAllocation());
        return totalAllocations;
    }

    private Map<String, ComputeAllocation> createInstanceAllocatedMap() {
        Map<String, ComputeAllocation> instancesAllocated = new HashMap();
        instancesAllocated.put(InstanceType.T1_MICRO.toString(), createComputeAllocation());
        return instancesAllocated;
    }

    private ComputeAllocation createTotalComputeAllocation() {
        int vCPU = TestUtils.CPU_VALUE * 2;
        int ram = TestUtils.MEMORY_VALUE * 2;
        int instances = ONE_VALUE * 2;
        int disk = AwsV2ComputeQuotaPlugin.MAXIMUM_STORAGE_VALUE * AwsV2ComputeQuotaPlugin.ONE_TERABYTE;
        return new ComputeAllocation(vCPU, ram, instances, disk);
    }

    private ComputeAllocation createComputeAllocation() {
        int vCPU = TestUtils.CPU_VALUE;
        int ram = TestUtils.MEMORY_VALUE;
        int instances = ONE_VALUE;
        int disk = TestUtils.DISK_VALUE;
        return new ComputeAllocation(vCPU, ram, instances, disk);
    }

}
