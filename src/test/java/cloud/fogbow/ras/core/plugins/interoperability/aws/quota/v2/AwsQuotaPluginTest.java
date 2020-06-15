package cloud.fogbow.ras.core.plugins.interoperability.aws.quota.v2;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2CloudUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import javax.ws.rs.InternalServerErrorException;
import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PrepareForTest({AwsV2ClientUtil.class, AwsV2CloudUtil.class, DatabaseManager.class })
public class AwsQuotaPluginTest extends BaseUnitTests {
    private static final String CLOUD_NAME = "amazon";
    private static final String FAKE_VOLUME_ID = "fake-volume-id";
    private static final String FAKE_VPC_ID = "fake-vpc-id";
    private static final String FLAVOR_LINE_FORMAT = "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s";
    private static final int ONE_VALUE = 1;
    private static final String FAKE_PUBLIC_ID = "173.4.1.2";

    private AwsQuotaPlugin plugin;
    private Ec2Client client;

    @Before
    public void setUp() throws FogbowException {
        this.testUtils.mockReadOrdersFromDataBase();
        String awsConfFilePath = HomeDir.getPath()
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator
                + CLOUD_NAME
                + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.plugin = Mockito.spy(new AwsQuotaPlugin(awsConfFilePath));
        this.client = this.testUtils.getAwsMockedClient();
    }

    // test case: When calling the getUserQuota method, it must verify
    // that is call was successful and the expected results returned.
    @Test
    public void testGetUserQuota() throws FogbowException {
        // set up
        AwsV2User user = Mockito.mock(AwsV2User.class);
        Mockito.doNothing().when(this.plugin).loadAvailableAllocations();
        Mockito.doNothing().when(this.plugin).loadInstancesAllocated(Mockito.eq(this.client));

        ResourceAllocation totalQuota = this.testUtils.createTotalQuota();
        ResourceAllocation usedQuota = this.testUtils.createUsedQuota();
        ResourceQuota expectedQuota = new ResourceQuota(totalQuota, usedQuota);

        Mockito.doReturn(totalQuota).when(this.plugin).calculateTotalQuota();
        Mockito.doReturn(usedQuota).when(this.plugin).calculateUsedQuota(Mockito.eq(this.client));

        // exercise
        ResourceQuota actualQuota = this.plugin.getUserQuota(user);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).loadAvailableAllocations();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).loadInstancesAllocated(Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).calculateTotalQuota();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).calculateUsedQuota(Mockito.eq(this.client));

        Assert.assertEquals(expectedQuota, actualQuota);
    }

    // test case: When calling the calculateUsedQuota method, it must verify that
    // returned the expected values.
    @Test
    public void testCalculateUsedQuota() throws FogbowException {
        // set up
        ResourceAllocation expectedQuota = this.testUtils.createUsedQuota();

        int instances = expectedQuota.getInstances();
        int ram = expectedQuota.getRam();
        int vCPUs = expectedQuota.getvCPU();
        ComputeAllocation computeAllocation = new ComputeAllocation(instances, vCPUs, ram);

        Mockito.doReturn(expectedQuota.getPublicIps()).when(this.plugin).calculateUsedElasticIp(Mockito.eq(this.client));
        Mockito.doReturn(expectedQuota.getNetworks()).when(this.plugin).calculateUsedNetworks(Mockito.eq(this.client));
        Mockito.doReturn(expectedQuota.getStorage()).when(this.plugin).calculateUsedStorage(Mockito.eq(this.client));
        Mockito.doReturn(expectedQuota.getVolumes()).when(this.plugin).calculateUsedVolumes(Mockito.eq(this.client));
        Mockito.doReturn(computeAllocation).when(this.plugin).calculateComputeUsedQuota();

        // exercise
        ResourceAllocation usedQuota = this.plugin.calculateUsedQuota(this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).calculateComputeUsedQuota();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).calculateUsedStorage(Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).calculateUsedVolumes(Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).calculateUsedElasticIp(Mockito.eq(this.client));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).calculateUsedNetworks(Mockito.eq(this.client));

        Assert.assertEquals(expectedQuota, usedQuota);
    }

    // test case: When calling the calculateComputeUsedQuota method, it must verify that
    // returned the expected values.
    @Test
    public void testCalculateComputeUsedQuota() {
        // set up
        Map<String, ComputeAllocation> instancesAllocated = createComputeAllocationMap();
        Mockito.doReturn(instancesAllocated).when(this.plugin).getComputeAllocationMap();
        ComputeAllocation expectedAllocation = this.createComputeAllocation();

        // exercise
        ComputeAllocation computeAllocation = this.plugin.calculateComputeUsedQuota();

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getComputeAllocationMap();
        Assert.assertEquals(expectedAllocation.getInstances(), computeAllocation.getInstances());
        Assert.assertEquals(expectedAllocation.getvCPU(), computeAllocation.getvCPU());
        Assert.assertEquals(expectedAllocation.getRam(), computeAllocation.getRam());
    }

    // test case: When calling the calculateVolumesUsage method, it must verify that
    // returned the expected values.
    @Test
    public void testCalculateVolumesUsage() throws Exception {
        // set up
        List<Volume> volumes = this.buildVolumesCollection();
        Mockito.doReturn(TestUtils.DISK_VALUE).when(this.plugin).getAllVolumesSize(Mockito.eq(volumes));

        DescribeVolumesRequest request = DescribeVolumesRequest.builder().build();
        DescribeVolumesResponse response = DescribeVolumesResponse.builder().volumes(volumes).build();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(response).when(AwsV2CloudUtil.class, "doDescribeVolumesRequest",
                Mockito.eq(request), Mockito.eq(this.client));

        // exercise
        int volumesUsage = this.plugin.calculateUsedStorage(this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getAllVolumesSize(Mockito.eq(volumes));

        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDescribeVolumesRequest(Mockito.eq(request), Mockito.eq(this.client));

        Assert.assertEquals(TestUtils.DISK_VALUE, volumesUsage);
    }

    // test case: When calling the calculateUsedVolumes method, it must verify
    // that returned the expected values.
    @Test
    public void testCalculateUsedVolumes() throws Exception {
        // set up
        List<Volume> volumes = this.buildVolumesCollection();

        DescribeVolumesResponse response = DescribeVolumesResponse.builder()
                .volumes(volumes)
                .build();

        Mockito.when(this.client.describeVolumes()).thenReturn(response);

        // exercise
        int usedVolumes = this.plugin.calculateUsedVolumes(this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).describeVolumes();
        Assert.assertEquals(volumes.size(), usedVolumes);
    }

    // test case: When calling the calculateUsedNetworks method, it must verify that
    // returned the expected values.
    @Test
    public void testCalculateUsedNetworks() throws Exception {
        // set up
        List<Vpc> vpcs = this.buildVpcCollection();

        DescribeVpcsResponse response = DescribeVpcsResponse.builder()
                .vpcs(vpcs)
                .build();

        Mockito.when(this.client.describeVpcs()).thenReturn(response);

        // exercise
        int usedVpcs = this.plugin.calculateUsedNetworks(this.client);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).describeVpcs();
        Assert.assertEquals(vpcs.size(), usedVpcs);
    }

    // test case: When calling the calculateUsedElasticIps method, it must verify that
    // returned the expected values.
    @Test
    public void testCalculateUsedElasticIps() throws Exception {
        // set up
        List<Address> addresses = this.buildAddressesCollection();

        DescribeAddressesRequest request = DescribeAddressesRequest.builder().build();
        DescribeAddressesResponse response = DescribeAddressesResponse.builder().addresses(addresses).build();

        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        PowerMockito.doReturn(response).when(AwsV2CloudUtil.class, "doDescribeAddressesRequests",
                Mockito.eq(request), Mockito.eq(this.client));

        // exercise
        int usedElasticIp = this.plugin.calculateUsedElasticIp(this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDescribeAddressesRequests(Mockito.eq(request), Mockito.eq(this.client));

        Assert.assertEquals(addresses.size(), usedElasticIp);
    }

    // test case: When calling the calculateTotalQuota method, it must verify that
    // returned the expected values.
    @Test
    public void testCalculateTotalQuota() {
        // set up
        ComputeAllocation computeAllocation = createTotalComputeAllocation();
        Mockito.doReturn(computeAllocation).when(this.plugin).calculateComputeTotalQuota();

        // exercise
        ResourceAllocation totalQuota = this.plugin.calculateTotalQuota();

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).calculateComputeTotalQuota();

        Assert.assertEquals(computeAllocation.getvCPU(), totalQuota.getvCPU());
        Assert.assertEquals(computeAllocation.getRam(), totalQuota.getRam());
        Assert.assertEquals(computeAllocation.getInstances(), totalQuota.getInstances());
        Assert.assertEquals(AwsQuotaPlugin.maximumStorage, totalQuota.getStorage());
        Assert.assertEquals(AwsQuotaPlugin.maximumNetworks, totalQuota.getNetworks());
        Assert.assertEquals(AwsQuotaPlugin.maximumPublicIpAddresses, totalQuota.getPublicIps());
    }

    // test case: When calling the getAllVolumesSize method, it must verify that
    // returned the expected values.
    @Test
    public void testGetAllVolumesSize() {
        // set up
        List<Volume> volumes = this.buildVolumesCollection();

        // exercise
        int size = this.plugin.getAllVolumesSize(volumes);

        // verify
        Assert.assertEquals(TestUtils.DISK_VALUE, size);
    }

    // test case: When calling the calculateComputeTotalQuota method, it must verify that
    // returned the expected values.
    @Test
    public void testCalculateComputeTotalQuota() {
        // set up
        Map<String, ComputeAllocation> instancesAllocated = createTotalComputeAllocationMap();
        Mockito.doReturn(instancesAllocated).when(this.plugin).getTotalComputeAllocationMap();
        ComputeAllocation expectedAllocation = this.createTotalComputeAllocation();

        // exercise
        ComputeAllocation computeAllocation = this.plugin.calculateComputeTotalQuota();

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalComputeAllocationMap();
        Assert.assertEquals(expectedAllocation.getInstances(), computeAllocation.getInstances());
        Assert.assertEquals(expectedAllocation.getvCPU(), computeAllocation.getvCPU());
        Assert.assertEquals(expectedAllocation.getRam(), computeAllocation.getRam());
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

        Assert.assertTrue(this.plugin.getComputeAllocationMap().containsKey(expectedMapKey));
        Assert.assertEquals(allocation, this.plugin.getComputeAllocationMap().get(expectedMapKey));
    }

    // test case: When calling the getInstanceReservations method, it must verify
    // that is call was successful.
    @Test
    public void testGetInstanceReservations() throws FogbowException {
        // set up
        DescribeInstancesResponse response = buildDescribeInstance();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        Mockito.when(AwsV2CloudUtil.doDescribeInstances(Mockito.eq(this.client))).thenReturn(response);

        List<Instance> expected = buildInstancesCollections();

        // exercise
        List<Instance> instances = this.plugin.getInstanceReservations(this.client);

        // verify
        PowerMockito.verifyStatic(AwsV2CloudUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        AwsV2CloudUtil.doDescribeInstances(Mockito.eq(this.client));

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

        Assert.assertTrue(this.plugin.getTotalComputeAllocationMap().containsKey(expectedMapKey));
        Assert.assertEquals(allocation, this.plugin.getTotalComputeAllocationMap().get(expectedMapKey));
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

    // test case: When calling the buildAllocatedInstance method with an instance
    // type by the first time, it must verify that is call was successful and return
    // the expected result.
    @Test
    public void testBuildAllocatedInstanceByFirstTime() throws FogbowException {
        // set up
        Instance instance = buildInstance(InstanceType.T2_MICRO);

        Map<String, ComputeAllocation> totalAllocations = createTotalComputeAllocationMap();
        Mockito.doReturn(totalAllocations).when(this.plugin).getTotalComputeAllocationMap();

        List<Volume> volumes = buildVolumesCollection();
        PowerMockito.mockStatic(AwsV2CloudUtil.class);
        Mockito.when(AwsV2CloudUtil.getInstanceVolumes(Mockito.eq(instance), Mockito.eq(this.client)))
                .thenReturn(volumes);

        int instanceExpected = ONE_VALUE;
        int cpuExpected = TestUtils.CPU_VALUE;
        int ramExpected = TestUtils.MEMORY_VALUE;

        // exercise
        ComputeAllocation allocation = this.plugin.buildAllocatedInstance(instance, this.client);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalComputeAllocationMap();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getComputeAllocationMap();

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

    private List<Address> buildAddressesCollection() {
        Address[] addresses = { Address.builder()
                .publicIp(FAKE_PUBLIC_ID)
                .build()
        };
        return Arrays.asList(addresses);
    }

    private List<Vpc> buildVpcCollection() {
        Vpc[] vpcs = { Vpc.builder()
                .vpcId(FAKE_VPC_ID)
                .build()
        };
        return Arrays.asList(vpcs);
    }

    private List<Volume> buildVolumesCollection() {
        Volume[] volumes = { Volume.builder()
                .volumeId(FAKE_VOLUME_ID)
                .size(TestUtils.DISK_VALUE)
                .build()
        };
        return Arrays.asList(volumes);
    }

    private Map<String, ComputeAllocation> createTotalComputeAllocationMap() {
        Map<String, ComputeAllocation> totalAllocations = new HashMap();
        totalAllocations.put(InstanceType.T1_MICRO.toString(), createComputeAllocation());
        totalAllocations.put(InstanceType.T2_MICRO.toString(), createComputeAllocation());
        return totalAllocations;
    }

    private Map<String, ComputeAllocation> createComputeAllocationMap() {
        Map<String, ComputeAllocation> instancesAllocated = new HashMap();
        instancesAllocated.put(InstanceType.T1_MICRO.toString(), createComputeAllocation());
        return instancesAllocated;
    }

    private ComputeAllocation createTotalComputeAllocation() {
        int vCPU = TestUtils.CPU_VALUE * 2;
        int ram = TestUtils.MEMORY_VALUE * 2;
        int instances = ONE_VALUE * 2;
        return new ComputeAllocation(instances, vCPU, ram);
    }

    private ComputeAllocation createComputeAllocation() {
        int vCPU = TestUtils.CPU_VALUE;
        int ram = TestUtils.MEMORY_VALUE;
        int instances = ONE_VALUE;
        return new ComputeAllocation(instances, vCPU, ram);
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
}
