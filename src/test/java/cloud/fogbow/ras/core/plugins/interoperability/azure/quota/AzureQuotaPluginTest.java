package cloud.fogbow.ras.core.plugins.interoperability.azure.quota;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.*;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.*;
import com.microsoft.azure.management.network.NetworkUsage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AzureClientCacheManager.class
})
public class AzureQuotaPluginTest extends TestUtils {

    private static final String VM_SIZE_A1 = "basic_A1";
    private static final String VM_SIZE_A2 = "basic_A2";
    private static final String VM_SIZE_A3 = "basic_A3";
    private static final String VM_SIZE_A4 = "basic_A4";

    private AzureQuotaPlugin plugin;
    private AzureUser azureUser;
    private Azure azure;

    @Before
    public void setUp() {
        String azureConfFilePath = HomeDir.getPath() +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.plugin = Mockito.spy(new AzureQuotaPlugin(azureConfFilePath));
        this.azureUser = AzureTestUtils.createAzureUser();
        this.azure = null;
    }

    // test case: When calling getUserQuota method with all secondary methods
    // mocked, it must return a ResourceQuota object
    @Test
    public void testGetUserQuotaSuccessfully() throws FogbowException {
        // set up
        mockGetAzureClient();

        Map<String, ComputeUsage> computeUsageMap = Mockito.mock(HashMap.class);
        Map<String, NetworkUsage> networkUsageMap = Mockito.mock(HashMap.class);
        PagedList<Disk> disks = (PagedList<Disk>) Mockito.mock(PagedList.class);

        Mockito.doReturn(computeUsageMap).when(this.plugin).getComputeUsageMap(Mockito.eq(this.azure));
        Mockito.doReturn(networkUsageMap).when(this.plugin).getNetworkUsageMap(Mockito.eq(this.azure));
        Mockito.doReturn(disks).when(this.plugin).getDisks(Mockito.eq(this.azure));

        ResourceAllocation totalQuota = createTotalQuota();
        ResourceAllocation usedQuota = createUsedQuota();

        Mockito.doReturn(totalQuota).when(this.plugin).getTotalQuota(Mockito.eq(computeUsageMap),
                Mockito.eq(networkUsageMap));

        Mockito.doReturn(usedQuota).when(this.plugin).getUsedQuota(Mockito.eq(computeUsageMap),
                Mockito.eq(networkUsageMap), Mockito.eq(disks), Mockito.eq(this.azure));


        // exercise
        ResourceQuota quota = this.plugin.getUserQuota(this.azureUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworkUsageMap(Mockito.eq(this.azure));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getComputeUsageMap(Mockito.eq(this.azure));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getDisks(Mockito.eq(this.azure));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getUsedQuota(Mockito.eq(computeUsageMap),
                Mockito.eq(networkUsageMap), Mockito.eq(disks), Mockito.eq(this.azure));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalQuota(Mockito.eq(computeUsageMap),
                Mockito.eq(networkUsageMap));

        Assert.assertNotNull(quota);
        Assert.assertEquals(totalQuota, quota.getTotalQuota());
        Assert.assertEquals(usedQuota, quota.getUsedQuota());
    }

    // test case: When calling getUsedQuota with all secondary methods mocked,
    // it must return a ResourceAllocation object
    @Test
    public void testGetUsedQuotaSuccessfully() throws UnauthenticatedUserException {
        // set up
        mockGetAzureClient();

        Map<String, ComputeUsage> computeUsageMap = Mockito.mock(HashMap.class);
        Map<String, NetworkUsage> networkUsageMap = Mockito.mock(HashMap.class);
        PagedList<Disk> disks = (PagedList<Disk>) Mockito.mock(PagedList.class);

        ComputeAllocation computeAllocation = Mockito.mock(ComputeAllocation.class);
        NetworkAllocation networkAllocation = Mockito.mock(NetworkAllocation.class);
        VolumeAllocation volumeAllocation = Mockito.mock(VolumeAllocation.class);
        PublicIpAllocation publicIpAllocation = Mockito.mock(PublicIpAllocation.class);

        Mockito.doReturn(computeAllocation).when(this.plugin).getUsedComputeAllocation(Mockito.eq(computeUsageMap),
                Mockito.eq(azure));
        Mockito.doReturn(networkAllocation).when(this.plugin).getUsedNetworkAllocation(Mockito.eq(networkUsageMap));
        Mockito.doReturn(publicIpAllocation).when(this.plugin).getUsedPublicIpAllocation(Mockito.eq(networkUsageMap));
        Mockito.doReturn(volumeAllocation).when(this.plugin).getUsedVolumeAllocation(Mockito.eq(disks));

        ResourceAllocation expectedUsedQuota = createUsedQuota();
        Mockito.doReturn(expectedUsedQuota).when(this.plugin).buildQuota(Mockito.eq(computeAllocation),
                Mockito.eq(networkAllocation), Mockito.eq(publicIpAllocation), Mockito.eq(volumeAllocation));

        // exercise
        ResourceAllocation usedQuota = this.plugin.getUsedQuota(computeUsageMap, networkUsageMap, disks, this.azure);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getUsedComputeAllocation(Mockito.eq(computeUsageMap), Mockito.eq(azure));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getUsedNetworkAllocation(Mockito.eq(networkUsageMap));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getUsedPublicIpAllocation(Mockito.eq(networkUsageMap));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getUsedVolumeAllocation(Mockito.eq(disks));
        Mockito.verify(this.plugin, Mockito.times(RUN_ONCE)).buildQuota(Mockito.eq(computeAllocation),
                Mockito.eq(networkAllocation), Mockito.eq(publicIpAllocation), Mockito.eq(volumeAllocation));

        Assert.assertEquals(expectedUsedQuota, usedQuota);
    }

    // test case: When calling getUsedVolumeAllocation with all secondary methods mocked,
    // it must return a VolumeAllocation object
    @Test
    public void testGetUsedVolumeAllocationSuccessfully() {
        // set up
        PagedList<Disk> disks = (PagedList<Disk>) Mockito.mock(PagedList.class);

        int expectedVolumes = 3;
        int expectedStorage = 30;
        VolumeAllocation expectedAllocation = new VolumeAllocation(expectedVolumes, expectedStorage);

        Mockito.when(disks.size()).thenReturn(expectedVolumes);
        Mockito.doReturn(expectedStorage).when(this.plugin).getStorageUsage(Mockito.eq(disks));

        // exercise
        VolumeAllocation volumeAllocation = this.plugin.getUsedVolumeAllocation(disks);

        // verify
        Mockito.verify(disks, Mockito.times(TestUtils.RUN_ONCE)).size();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getStorageUsage(Mockito.eq(disks));
        Assert.assertEquals(expectedAllocation, volumeAllocation);
    }

    // test case: When calling getStorageUsage method with given a paged list of disks,
    // it must return the total storage used by all disks
    @Test
    public void testGetStorageUsageSuccessfully() {
        // set up
        PagedList<Disk> disks = (PagedList<Disk>) Mockito.mock(PagedList.class);

        int diskSize1 = 2;
        int diskSize2 = 4;
        int diskSize3 = 8;
        int expectedTotal = diskSize1 + diskSize2 + diskSize3;

        Disk disk1 = createDisk(diskSize1);
        Disk disk2 = createDisk(diskSize2);
        Disk disk3 = createDisk(diskSize3);
        List<Disk> diskList = Arrays.asList(disk1, disk2, disk3);

        Mockito.when(disks.stream()).thenReturn(diskList.stream());

        // exercise
        int actualTotal = this.plugin.getStorageUsage(disks);

        // verify
        Assert.assertEquals(expectedTotal, actualTotal);
    }

    // test case: When calling getStorageUsage method with a empty paged list of disks,
    // it must return zero (no usage)
    @Test
    public void testGetStorageUsageWithEmptyList() {
        // set up
        PagedList<Disk> disks = (PagedList<Disk>) Mockito.mock(PagedList.class);

        int expectedTotal = 0;
        List<Disk> diskList = new ArrayList<>();

        Mockito.when(disks.stream()).thenReturn(diskList.stream());

        // exercise
        int actualTotal = this.plugin.getStorageUsage(disks);

        // verify
        Assert.assertEquals(expectedTotal, actualTotal);
    }

    // test case: When calling getUsedNetworkAllocation with a map of network usages,
    // it must return a NetworkAllocation object with the number of used instances
    @Test
    public void testGetUsedNetworkAllocationSuccessfully() {
        // set up
        Map<String, NetworkUsage> networkUsages = new HashMap<>();
        NetworkUsage mockedNetworkUsage = Mockito.mock(NetworkUsage.class);

        long expectedInstances = 2;
        Mockito.when(mockedNetworkUsage.currentValue()).thenReturn(expectedInstances);

        networkUsages.put(AzureQuotaPlugin.QUOTA_NETWORK_INSTANCES, mockedNetworkUsage);

        // exercise
        NetworkAllocation networkAllocation = this.plugin.getUsedNetworkAllocation(networkUsages);

        // verify
        Mockito.verify(mockedNetworkUsage, Mockito.times(TestUtils.RUN_ONCE)).currentValue();
        Assert.assertEquals(Math.toIntExact(expectedInstances), networkAllocation.getInstances());
    }

    // test case: When calling getUsedNetworkAllocation with a empty map of network usages,
    // it must return a NetworkAllocation object with instances set to AzureQuotaPlugin.NO_USAGE
    @Test
    public void testGetUsedNetworkAllocationWithEmptyMap() {
        // set up
        Map<String, NetworkUsage> networkUsages = new HashMap<>();

        // exercise
        NetworkAllocation networkAllocation = this.plugin.getUsedNetworkAllocation(networkUsages);

        // verify
        Assert.assertEquals(AzureQuotaPlugin.NO_USAGE, networkAllocation.getInstances());
    }

    // test case: When calling getUsedComputeAllocation with a map of compute usages,
    // it must return a ComputeAllocation object with the right instances, vCPUs and
    // memory values.
    @Test
    public void testGetUsedComputeAllocationSuccessfully() throws UnauthenticatedUserException {
        // set up
        mockGetAzureClient();

        Map<String, ComputeUsage> computeUsageMap = new HashMap<>();

        int expectedInstances = 2;
        int expectedCores = 4;
        int expectedRam = 2048;

        ComputeUsage mockedCoresUsage = Mockito.mock(ComputeUsage.class);
        Mockito.when(mockedCoresUsage.currentValue()).thenReturn(expectedCores);
        computeUsageMap.put(AzureQuotaPlugin.QUOTA_VM_CORES_KEY, mockedCoresUsage);

        ComputeUsage mockedVmUsage = Mockito.mock(ComputeUsage.class);
        Mockito.when(mockedVmUsage.currentValue()).thenReturn(expectedInstances);
        computeUsageMap.put(AzureQuotaPlugin.QUOTA_VM_INSTANCES_KEY, mockedVmUsage);

        Mockito.doReturn(expectedRam).when(this.plugin).getMemoryUsage(Mockito.eq(this.azure));

        ComputeAllocation expectedAllocation = new ComputeAllocation(expectedInstances, expectedCores, expectedRam);

        // exercise
        ComputeAllocation computeAllocation = this.plugin.getUsedComputeAllocation(computeUsageMap, this.azure);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getMemoryUsage(Mockito.eq(this.azure));
        Assert.assertEquals(expectedAllocation, computeAllocation);
    }

    // test case: When calling getUsedComputeAllocation with a empty map of network usages,
    // it must return a ComputeAllocation with instances and cores set to AzureQuotaPlugin.NO_USAGE
    @Test
    public void testGetUsedComputeAllocationWithEmptyMap() throws UnauthenticatedUserException {
        // set up
        mockGetAzureClient();
        Map<String, ComputeUsage> computeUsageMap = new HashMap<>();
        int expectedRam = 1024;
        Mockito.doReturn(expectedRam).when(this.plugin).getMemoryUsage(Mockito.eq(this.azure));

        // exercise
        ComputeAllocation computeAllocation = this.plugin.getUsedComputeAllocation(computeUsageMap, this.azure);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getMemoryUsage(Mockito.eq(this.azure));
        Assert.assertEquals(AzureQuotaPlugin.NO_USAGE, computeAllocation.getvCPU());
        Assert.assertEquals(AzureQuotaPlugin.NO_USAGE, computeAllocation.getInstances());
    }

    // test case: When calling getUsedPublicIpAllocation with a map of network usages,
    // it must return a PublicIpAllocation object with the number of used instances
    @Test
    public void testGetUsedPublicIpAllocationSuccessfully() {
        // set up
        Map<String, NetworkUsage> networkUsages = new HashMap<>();
        NetworkUsage mockedNetworkUsage = Mockito.mock(NetworkUsage.class);

        long expectedInstances = 2;
        Mockito.when(mockedNetworkUsage.currentValue()).thenReturn(expectedInstances);

        networkUsages.put(AzureQuotaPlugin.QUOTA_PUBLIC_IP_ADDRESSES, mockedNetworkUsage);

        // exercise
        PublicIpAllocation networkAllocation = this.plugin.getUsedPublicIpAllocation(networkUsages);

        // verify
        Mockito.verify(mockedNetworkUsage, Mockito.times(TestUtils.RUN_ONCE)).currentValue();
        Assert.assertEquals(expectedInstances, networkAllocation.getInstances());
    }

    // test case: When calling getUsedPublicIpAllocation with a empty map of network usages,
    // it must return a PublicIpAllocation object with instances set to AzureQuotaPlugin.NO_USAGE
    @Test
    public void testGetUsedPublicIpAllocationWithEmptyMap() {
        // set up
        Map<String, NetworkUsage> networkUsages = new HashMap<>();

        // exercise
        NetworkAllocation networkAllocation = this.plugin.getUsedNetworkAllocation(networkUsages);

        // verify
        Assert.assertEquals(AzureQuotaPlugin.NO_USAGE, networkAllocation.getInstances());
    }

    // test case: When calling getTotalQuota method with all secondary methods mocked
    // it should return a ResourceAllocation object
    @Test
    public void testGetTotalQuotaSuccessfully() {
        // set up
        Map<String, ComputeUsage> computeUsageMap = Mockito.mock(HashMap.class);
        Map<String, NetworkUsage> networkUsageMap = Mockito.mock(HashMap.class);

        ComputeAllocation computeAllocation = Mockito.mock(ComputeAllocation.class);
        NetworkAllocation networkAllocation = Mockito.mock(NetworkAllocation.class);
        VolumeAllocation volumeAllocation = Mockito.mock(VolumeAllocation.class);
        PublicIpAllocation publicIpAllocation = Mockito.mock(PublicIpAllocation.class);

        Mockito.doReturn(computeAllocation).when(this.plugin).getTotalComputeAllocation(Mockito.eq(computeUsageMap));
        Mockito.doReturn(networkAllocation).when(this.plugin).getTotalNetworkAllocation(Mockito.eq(networkUsageMap));
        Mockito.doReturn(publicIpAllocation).when(this.plugin).getTotalPublicIpAllocation(Mockito.eq(networkUsageMap));
        Mockito.doReturn(volumeAllocation).when(this.plugin).getTotalVolumeAllocation();

        ResourceAllocation expectedUsedQuota = createUsedQuota();
        Mockito.doReturn(expectedUsedQuota).when(this.plugin).buildQuota(Mockito.eq(computeAllocation),
                Mockito.eq(networkAllocation), Mockito.eq(publicIpAllocation), Mockito.eq(volumeAllocation));

        // exercise
        ResourceAllocation totalQuota = this.plugin.getTotalQuota(computeUsageMap, networkUsageMap);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalComputeAllocation(Mockito.eq(computeUsageMap));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalNetworkAllocation(Mockito.eq(networkUsageMap));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalPublicIpAllocation(Mockito.eq(networkUsageMap));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalVolumeAllocation();
        Mockito.verify(this.plugin, Mockito.times(RUN_ONCE)).buildQuota(Mockito.eq(computeAllocation),
                Mockito.eq(networkAllocation), Mockito.eq(publicIpAllocation), Mockito.eq(volumeAllocation));

        Assert.assertEquals(expectedUsedQuota, totalQuota);
    }

    // test case: When calling getTotalVolumeAllocation method, it must return
    // a VolumeAllocation object with instances set to FogbowConstants.UNLIMITED_RESOURCE
    // and storage set to AzureQuotaPlugin.MAXIMUM_STORAGE_ACCOUNT_CAPACITY
    @Test
    public void testGetTotalVolumeAllocationSuccessfully() {
        // set up
        int expectedInstances = FogbowConstants.UNLIMITED_RESOURCE;
        int expectedStorage = AzureQuotaPlugin.MAXIMUM_STORAGE_ACCOUNT_CAPACITY;

        // exercise
        VolumeAllocation totalAllocation = this.plugin.getTotalVolumeAllocation();

        // verify
        Assert.assertEquals(expectedInstances, totalAllocation.getInstances());
        Assert.assertEquals(expectedStorage, totalAllocation.getStorage());
    }

    // test case: When calling getTotalNetworkAllocation with a map of network usages,
    // it must return a NetworkAllocation object with the number of total instances
    @Test
    public void testGetTotalNetworkAllocationSuccessfully() {
        // set up
        Map<String, NetworkUsage> networkUsages = new HashMap<>();
        NetworkUsage mockedNetworkUsage = Mockito.mock(NetworkUsage.class);

        long expectedInstances = 10;
        Mockito.when(mockedNetworkUsage.limit()).thenReturn(expectedInstances);

        networkUsages.put(AzureQuotaPlugin.QUOTA_NETWORK_INSTANCES, mockedNetworkUsage);

        // exercise
        NetworkAllocation networkAllocation = this.plugin.getTotalNetworkAllocation(networkUsages);

        // verify
        Mockito.verify(mockedNetworkUsage, Mockito.times(TestUtils.RUN_ONCE)).limit();
        Assert.assertEquals(Math.toIntExact(expectedInstances), networkAllocation.getInstances());
    }

    // test case: When calling getTotalNetworkAllocation with a empty map of network usages,
    // it must return a NetworkAllocation object with instances set to AzureQuotaPlugin.NO_USAGE
    @Test
    public void testGetTotalNetworkAllocationWithEmptyMap() {
        // set up
        Map<String, NetworkUsage> networkUsages = new HashMap<>();

        // exercise
        NetworkAllocation networkAllocation = this.plugin.getTotalNetworkAllocation(networkUsages);

        // verify
        Assert.assertEquals(AzureQuotaPlugin.NO_USAGE, networkAllocation.getInstances());
    }

    // test case: When calling getTotalComputeAllocation with a map of compute usages,
    // it must return a ComputeAllocation object with the right instances, vCPUs and
    // memory values.
    @Test
    public void testGetTotalComputeAllocationSuccessfully() throws UnauthenticatedUserException {
        // set up
        mockGetAzureClient();

        Map<String, ComputeUsage> computeUsageMap = new HashMap<>();

        long expectedInstances = 25000;
        long expectedCores = 10;
        int expectedRam = FogbowConstants.UNLIMITED_RESOURCE;

        ComputeUsage mockedCoresUsage = Mockito.mock(ComputeUsage.class);
        Mockito.when(mockedCoresUsage.limit()).thenReturn(expectedCores);
        computeUsageMap.put(AzureQuotaPlugin.QUOTA_VM_CORES_KEY, mockedCoresUsage);

        ComputeUsage mockedVmUsage = Mockito.mock(ComputeUsage.class);
        Mockito.when(mockedVmUsage.limit()).thenReturn(expectedInstances);
        computeUsageMap.put(AzureQuotaPlugin.QUOTA_VM_INSTANCES_KEY, mockedVmUsage);

        // exercise
        ComputeAllocation computeAllocation = this.plugin.getTotalComputeAllocation(computeUsageMap);

        // verify
        Assert.assertEquals(expectedCores, computeAllocation.getvCPU());
        Assert.assertEquals(expectedInstances, computeAllocation.getInstances());
        Assert.assertEquals(expectedRam, computeAllocation.getRam());
    }

    // test case: When calling getTotalComputeAllocation with a empty map of network usages,
    // it must return a ComputeAllocation with instances and cores set to AzureQuotaPlugin.NO_USAGE
    @Test
    public void testGetTotalComputeAllocationWithEmptyMap() throws UnauthenticatedUserException {
        // set up
        mockGetAzureClient();
        Map<String, ComputeUsage> computeUsageMap = new HashMap<>();
        int expectedRam = FogbowConstants.UNLIMITED_RESOURCE;
        Mockito.doReturn(expectedRam).when(this.plugin).getMemoryUsage(Mockito.eq(this.azure));

        // exercise
        ComputeAllocation computeAllocation = this.plugin.getTotalComputeAllocation(computeUsageMap);

        // verify
        Assert.assertEquals(AzureQuotaPlugin.NO_USAGE, computeAllocation.getvCPU());
        Assert.assertEquals(AzureQuotaPlugin.NO_USAGE, computeAllocation.getInstances());
        Assert.assertEquals(expectedRam, computeAllocation.getRam());
    }

    // test case: When calling getTotalPublicIpAllocation with a map of network usages,
    // it must return a PublicIpAllocation object with the number of total instances
    @Test
    public void testGetTotalPublicIpAllocationSuccessfully() {
        // set up
        Map<String, NetworkUsage> networkUsages = new HashMap<>();
        NetworkUsage mockedNetworkUsage = Mockito.mock(NetworkUsage.class);

        long expectedInstances = 1000;
        Mockito.when(mockedNetworkUsage.limit()).thenReturn(expectedInstances);

        networkUsages.put(AzureQuotaPlugin.QUOTA_PUBLIC_IP_ADDRESSES, mockedNetworkUsage);

        // exercise
        PublicIpAllocation networkAllocation = this.plugin.getTotalPublicIpAllocation(networkUsages);

        // verify
        Mockito.verify(mockedNetworkUsage, Mockito.times(TestUtils.RUN_ONCE)).limit();
        Assert.assertEquals(expectedInstances, networkAllocation.getInstances());
    }

    // test case: When calling getTotalPublicIpAllocation with a empty map of network usages,
    // it must return a PublicIpAllocation object with instances set to AzureQuotaPlugin.NO_USAGE
    @Test
    public void testGetTotalPublicIpAllocationWithEmptyMap() {
        // set up
        Map<String, NetworkUsage> networkUsages = new HashMap<>();

        // exercise
        NetworkAllocation networkAllocation = this.plugin.getTotalNetworkAllocation(networkUsages);

        // verify
        Assert.assertEquals(AzureQuotaPlugin.NO_USAGE, networkAllocation.getInstances());
    }

    // test case: When calling buildQuota method with: ComputeAllocation, VolumeAllocation,
    // NetworkAllocation and PublicIpAllocation as parameters, it should return a
    // ResourceAllocation with the allocations information
    @Test
    public void testBuildQuotaSuccessfully() {
        // set up
        int expectedInstances = 1;
        int expectedCores = 2;
        int expectedRam = 1024;
        int expectedVolumes = 2;
        int expectedStorage = 30;
        int expectedNetworks = 1;
        int expectedPublicIps = 1;

        ComputeAllocation computeAllocation = new ComputeAllocation(expectedInstances, expectedCores, expectedRam);
        VolumeAllocation volumeAllocation = new VolumeAllocation(expectedVolumes, expectedStorage);
        NetworkAllocation networkAllocation = new NetworkAllocation(expectedNetworks);
        PublicIpAllocation publicIpAllocation = new PublicIpAllocation(expectedPublicIps);

        // exercise
        ResourceAllocation resourceAllocation = this.plugin.buildQuota(computeAllocation,
                networkAllocation, publicIpAllocation, volumeAllocation);

        // verify
        Assert.assertEquals(expectedInstances, resourceAllocation.getInstances());
        Assert.assertEquals(expectedCores, resourceAllocation.getvCPU());
        Assert.assertEquals(expectedRam, resourceAllocation.getRam());
        Assert.assertEquals(expectedStorage, resourceAllocation.getStorage());
        Assert.assertEquals(expectedVolumes, resourceAllocation.getVolumes());
        Assert.assertEquals(expectedNetworks, resourceAllocation.getNetworks());
        Assert.assertEquals(expectedPublicIps, resourceAllocation.getPublicIps());
    }

    // test case: When calling getComputeUsageMap method with all secondary methods
    // mocked it must return a map of ComputeUsage with only valid usages:
    // AzureQuotaPlugin.QUOTA_VM_INSTANCES_KEY and AzureQuotaPlugin.QUOTA_VM_CORES_KEY
    @Test
    public void testGetComputeUsageMapSuccessfully() throws UnauthenticatedUserException {
        // set up
        mockGetAzureClient();

        PagedList<ComputeUsage> computeUsages = (PagedList<ComputeUsage>) Mockito.mock(PagedList.class);

        ComputeUsage computeUsage1 = this.createComputeUsage(AzureQuotaPlugin.QUOTA_VM_INSTANCES_KEY);
        ComputeUsage computeUsage2 = this.createComputeUsage(AzureQuotaPlugin.QUOTA_VM_CORES_KEY);

        List<ComputeUsage> computeUsageList = Arrays.asList(computeUsage1, computeUsage2);
        Stream<ComputeUsage> mockStream = computeUsageList.stream();

        Mockito.when(computeUsages.stream()).thenReturn(mockStream);
        Mockito.doReturn(computeUsages).when(this.plugin).getComputeUsage(Mockito.eq(this.azure));

        // exercise
        Map<String, ComputeUsage> computeUsageMap = this.plugin.getComputeUsageMap(this.azure);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getComputeUsage(Mockito.eq(this.azure));

        ComputeUsage vmUsage = computeUsageMap.get(AzureQuotaPlugin.QUOTA_VM_INSTANCES_KEY);
        ComputeUsage coresUsage = computeUsageMap.get(AzureQuotaPlugin.QUOTA_VM_CORES_KEY);
        Assert.assertNotNull(vmUsage);
        Assert.assertNotNull(coresUsage);
    }

    // test case: When calling getNetworkUsageMap method with all secondary methods
    // mocked it must return a map of NetworkUsage with only valid usages:
    // AzureQuotaPlugin.QUOTA_QUOTA_NETWORK_INSTANCES and AzureQuotaPlugin.QUOTA_PUBLIC_IP_ADDRESSES
    @Test
    public void testGetNetworkUsageMapSuccessfully() throws UnauthenticatedUserException {
        // set up
        mockGetAzureClient();

        PagedList<NetworkUsage> networkUsages = (PagedList<NetworkUsage>) Mockito.mock(PagedList.class);

        NetworkUsage expectedPublicIpUsage = this.createNetworkUsage(AzureQuotaPlugin.QUOTA_PUBLIC_IP_ADDRESSES);
        NetworkUsage expectedNetworkUsage = this.createNetworkUsage(AzureQuotaPlugin.QUOTA_NETWORK_INSTANCES);

        List<NetworkUsage> networkUsageList = Arrays.asList(expectedPublicIpUsage, expectedNetworkUsage);
        Stream<NetworkUsage> mockStream = networkUsageList.stream();

        Mockito.when(networkUsages.stream()).thenReturn(mockStream);
        Mockito.doReturn(networkUsages).when(this.plugin).getNetworkUsage(Mockito.eq(this.azure));

        // exercise
        Map<String, NetworkUsage> networkUsageMap = this.plugin.getNetworkUsageMap(this.azure);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworkUsage(Mockito.eq(this.azure));

        NetworkUsage publicIpUsage = networkUsageMap.get(AzureQuotaPlugin.QUOTA_PUBLIC_IP_ADDRESSES);
        NetworkUsage networkUsage = networkUsageMap.get(AzureQuotaPlugin.QUOTA_NETWORK_INSTANCES);
        Assert.assertNotNull(publicIpUsage);
        Assert.assertNotNull(networkUsage);
    }

    // test case: When calling getMemoryUsage method with all secondary methods mocked
    // it must return the total memory in use
    @Test
    public void testGetMemoryUsageSuccessfully() throws UnauthenticatedUserException {
        // set up
        mockGetAzureClient();
        int expectedMemory = 4096;

        List<String> sizeNamesInUse = (List<String>) Mockito.mock(List.class);
        Mockito.doReturn(sizeNamesInUse).when(this.plugin).getVirtualMachineSizeNamesInUse(Mockito.eq(this.azure));

        Map<String, VirtualMachineSize> virtualMachineSizes = (HashMap<String, VirtualMachineSize>) Mockito.mock(HashMap.class);
        Mockito.doReturn(virtualMachineSizes).when(this.plugin).getVirtualMachineSizesInUse(Mockito.eq(sizeNamesInUse),
                Mockito.eq(this.azure));

        Mockito.doReturn(expectedMemory).when(this.plugin).doGetMemoryUsage(Mockito.eq(sizeNamesInUse),
                Mockito.eq(virtualMachineSizes));

        // exercise
        int memoryUsage = this.plugin.getMemoryUsage(this.azure);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getVirtualMachineSizeNamesInUse(Mockito.eq(this.azure));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getVirtualMachineSizesInUse(Mockito.eq(sizeNamesInUse),
                Mockito.eq(this.azure));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetMemoryUsage(Mockito.eq(sizeNamesInUse),
                Mockito.eq(virtualMachineSizes));

        Assert.assertEquals(expectedMemory, memoryUsage);
    }

    // test case: When calling doGetMemoryUsage method with the right parameters
    // it must return the sum of all memory used by the virtual machine sizes passed
    // by parameter.
    @Test
    public void testDoGetMemoryUsageSuccessfully() {
        // set up
        String sizeName1 = "basic_A1";
        String sizeName2 = "basic_A2";

        int memorySize1 = 1024;
        int memorySize2 = 512;
        int expectedSum = memorySize1 + memorySize1 + memorySize2;

        VirtualMachineSize virtualMachineSize1 = this.createVirtualMachineSize(sizeName1, memorySize1);
        VirtualMachineSize virtualMachineSize2 = this.createVirtualMachineSize(sizeName2, memorySize2);

        List<String> sizeNamesInUse = Arrays.asList(sizeName1, sizeName1, sizeName2);

        Map<String, VirtualMachineSize> virtualMachineSizes = new HashMap<>();
        virtualMachineSizes.put(sizeName1, virtualMachineSize1);
        virtualMachineSizes.put(sizeName2, virtualMachineSize2);

        // exercise
        int memoryUsage = this.plugin.doGetMemoryUsage(sizeNamesInUse, virtualMachineSizes);

        // verify
        Assert.assertEquals(expectedSum, memoryUsage);
    }

    // test case: When calling doGetMemoryUsage method with the list of size names in use empty
    // it must return AzureQuotaPlugin.NO_USAGE (zero)
    @Test
    public void testDoGetMemoryUsageNoSizeInUseSuccessfully() {
        // set up
        int expectedSum = 0;
        List<String> sizeNamesInUse = new ArrayList<>();
        Map<String, VirtualMachineSize> virtualMachineSizes = new HashMap<>();

        // exercise
        int memoryUsage = this.plugin.doGetMemoryUsage(sizeNamesInUse, virtualMachineSizes);

        // verify
        Assert.assertEquals(expectedSum, memoryUsage);
    }

    // test case: When calling GetVirtualMachineSizes method with secondary methods mocked,
    // a list of size names (in use) and a azure client as parameter it must return
    // a map of VirtualMachineSize with only the virtual machine sizes that the name is
    // contained in the list of size names parameter
    @Test
    public void testGetVirtualMachineSizesInUseSuccessfully() throws UnauthenticatedUserException {
        // set up
        mockGetAzureClient();
        List<String> sizeNames = Arrays.asList(VM_SIZE_A1, VM_SIZE_A2);

        PagedList<VirtualMachineSize> mockedSizes = (PagedList<VirtualMachineSize>) Mockito.mock(PagedList.class);
        Stream<VirtualMachineSize> mockStream = this.createVirtualMachineSizeStream();
        Mockito.when(mockedSizes.stream()).thenReturn(mockStream);

        Mockito.doReturn(mockedSizes).when(this.plugin).getVirtualMachineSizes(Mockito.eq(this.azure));

        // exercise
        Map<String, VirtualMachineSize> virtualMachineSizes = this.plugin.getVirtualMachineSizesInUse(sizeNames, this.azure);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getVirtualMachineSizes(Mockito.eq(this.azure));
        for (String sizeName : virtualMachineSizes.keySet()) {
            Assert.assertTrue(sizeNames.contains(sizeName));
        }
    }

    // test case: When calling getVirtualMachineSizeNamesInUse method with secondary methods mocked
    // it must return a list of virtual machine size names that is in use
    @Test
    public void testGetVirtualMachineSizeNamesInUseSuccessfully() throws UnauthenticatedUserException {
        // set up
        mockGetAzureClient();

        PagedList<VirtualMachine> virtualMachines = (PagedList<VirtualMachine>) Mockito.mock(PagedList.class);

        VirtualMachineSizeTypes vm1SizeType = VirtualMachineSizeTypes.BASIC_A1;
        VirtualMachineSizeTypes vm2SizeType = VirtualMachineSizeTypes.BASIC_A3;

        VirtualMachine vm1 = this.createVirtualMachine(vm1SizeType);
        VirtualMachine vm2 = this.createVirtualMachine(vm2SizeType);

        List<VirtualMachine> virtualMachineList = Arrays.asList(vm1, vm2);
        Stream<VirtualMachine> mockStream = virtualMachineList.stream();

        Mockito.when(virtualMachines.stream()).thenReturn(mockStream);

        Mockito.doReturn(virtualMachines).when(this.plugin).getVirtualMachines(Mockito.eq(this.azure));

        // exercise
        List<String> sizeNamesInUse = this.plugin.getVirtualMachineSizeNamesInUse(this.azure);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getVirtualMachines(Mockito.eq(this.azure));
        Assert.assertTrue(sizeNamesInUse.contains(vm1SizeType.toString()));
        Assert.assertTrue(sizeNamesInUse.contains(vm2SizeType.toString()));
    }

    private VirtualMachine createVirtualMachine(VirtualMachineSizeTypes sizeType) {
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Mockito.when(virtualMachine.size()).thenReturn(sizeType);
        return virtualMachine;
    }

    private Stream<VirtualMachineSize> createVirtualMachineSizeStream() {
        ArrayList<VirtualMachineSize> virtualMachineSizes = new ArrayList<>();

        virtualMachineSizes.add(this.createVirtualMachineSize(VM_SIZE_A1, 512));
        virtualMachineSizes.add(this.createVirtualMachineSize(VM_SIZE_A2, 1024));
        virtualMachineSizes.add(this.createVirtualMachineSize(VM_SIZE_A3, 2048));
        virtualMachineSizes.add(this.createVirtualMachineSize(VM_SIZE_A4, 4096));

        return virtualMachineSizes.stream();
    }

    private VirtualMachineSize createVirtualMachineSize(String name, int memorySize) {
        VirtualMachineSize size = Mockito.mock(VirtualMachineSize.class);
        Mockito.when(size.name()).thenReturn(name);
        Mockito.when(size.memoryInMB()).thenReturn(memorySize);
        return size;
    }

    private NetworkUsage createNetworkUsage(String name) {
        NetworkUsage networkUsage = Mockito.mock(NetworkUsage.class);
        com.microsoft.azure.management.network.UsageName usageName =
                Mockito.mock(com.microsoft.azure.management.network.UsageName.class);

        Mockito.when(networkUsage.name()).thenReturn(usageName);
        Mockito.when(networkUsage.name().value()).thenReturn(name);
        return networkUsage;
    }

    private ComputeUsage createComputeUsage(String name) {
        ComputeUsage computeUsage = Mockito.mock(ComputeUsage.class);
        Mockito.when(computeUsage.name()).thenReturn(Mockito.mock(UsageName.class));
        Mockito.when(computeUsage.name().value()).thenReturn(name);
        return computeUsage;
    }

    private Disk createDisk(int sizeInGB) {
        Disk disk = Mockito.mock(Disk.class);
        Mockito.when(disk.sizeInGB()).thenReturn(sizeInGB);
        return disk;
    }

    private void mockGetAzureClient() throws UnauthenticatedUserException {
        PowerMockito.mockStatic(AzureClientCacheManager.class);
        PowerMockito.when(AzureClientCacheManager.getAzure(Mockito.eq(this.azureUser)))
                .thenReturn(azure);
    }
}

