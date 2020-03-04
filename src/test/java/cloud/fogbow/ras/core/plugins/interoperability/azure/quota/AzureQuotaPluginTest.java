package cloud.fogbow.ras.core.plugins.interoperability.azure.quota;

import cloud.fogbow.common.constants.AzureConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.models.AzureUser;
import cloud.fogbow.common.util.AzureClientCacheManager;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.*;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.ComputeUsage;
import com.microsoft.azure.management.compute.Disk;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        AzureClientCacheManager.class
})
public class AzureQuotaPluginTest extends TestUtils {

    private AzureQuotaPlugin plugin;
    private String defaultRegionName;
    private AzureUser azureUser;
    private Azure azure;

    @Before
    public void setUp() {
        String azureConfFilePath = HomeDir.getPath() +
                SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + AzureTestUtils.AZURE_CLOUD_NAME + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        Properties properties = PropertiesUtil.readProperties(azureConfFilePath);
        this.defaultRegionName = properties.getProperty(AzureConstants.DEFAULT_REGION_NAME_KEY);
        this.plugin = Mockito.spy(new AzureQuotaPlugin(azureConfFilePath));
        this.azureUser = AzureTestUtils.createAzureUser();
        this.azure = null;
    }

    // test case: When calling getUserQuota method with all secondary methods
    // mocked, it must return a ResourceQuota object
    @Test
    public void testGetUserQuotaSuccessful() throws FogbowException {
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
    public void testGetUsedQuota() throws UnauthenticatedUserException {
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
    public void testGetUsedVolumeAllocation() {
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
    public void testGetStorageUsage() {
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
    public void testGetStorageUsageEmptyList() {
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
    public void testGetUsedNetworkAllocation() {
        // set up
        Map<String, NetworkUsage> networkUsages = new HashMap<>();
        NetworkUsage mockedNetworkUsage = Mockito.mock(NetworkUsage.class);

        long expectedInstances = 2;
        NetworkAllocation expectedAllocation = new NetworkAllocation((int) expectedInstances);
        Mockito.when(mockedNetworkUsage.currentValue()).thenReturn(expectedInstances);

        networkUsages.put(AzureQuotaPlugin.QUOTA_NETWORK_INSTANCES, mockedNetworkUsage);

        // exercise
        NetworkAllocation networkAllocation = this.plugin.getUsedNetworkAllocation(networkUsages);

        // verify
        Mockito.verify(mockedNetworkUsage, Mockito.times(TestUtils.RUN_ONCE)).currentValue();
        Assert.assertEquals(expectedAllocation, networkAllocation);
    }

    // test case: When calling getUsedNetworkAllocation with a map of network usages,
    // if the quota network instances field is not present in the map, it must return
    // AzureQuotaPlugin.NO_USAGE
    @Test
    public void testGetUsedNetworkAllocationUndefinedQuotaField() {
        // set up
        Map<String, NetworkUsage> networkUsages = new HashMap<>();

        int expectedInstances = AzureQuotaPlugin.NO_USAGE;
        NetworkAllocation expectedAllocation = new NetworkAllocation(expectedInstances);

        // exercise
        NetworkAllocation networkAllocation = this.plugin.getUsedNetworkAllocation(networkUsages);

        // verify
        Assert.assertEquals(expectedAllocation, networkAllocation);
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

