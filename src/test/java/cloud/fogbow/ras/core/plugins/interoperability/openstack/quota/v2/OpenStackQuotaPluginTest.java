package cloud.fogbow.ras.core.plugins.interoperability.openstack.quota.v2;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;

@PrepareForTest({ DatabaseManager.class })
public class OpenStackQuotaPluginTest extends BaseUnitTests {

    private static final int TOTAL_INSTANCES_VALUE = 10;
    private static final int TOTAL_VCPU_VALUE = 8;
    private static final int TOTAL_RAM_VALUE = 8192;
    private static final int TOTAL_DISK_VALUE = 1000;
    private static final int TOTAL_NETWORK_VALUE = 5;
    private static final int TOTAL_PUBLIC_IP_VALUE = 2;
    private static final int USED_INSTANCES_VALUE = 1;
    private static final int USED_VCPU_VALUE = 2;
    private static final int USED_RAM_VALUE = 2048;
    private static final int USED_DISK_VALUE = 1;
    private static final int USED_NETWORK_VALUE = 1;
    private static final int USED_PUBLIC_IP_VALUE = 1;
    
    private OpenStackHttpClient client;
    private OpenStackQuotaPlugin plugin;
    private OpenStackV3User cloudUser;
    
    @Before
    public void setUp() throws FogbowException {
        this.testUtils.mockReadOrdersFromDataBase();
        
        String openstackConfFilePath = HomeDir.getPath() 
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME 
                + File.separator
                + TestUtils.DEFAULT_CLOUD_NAME 
                + File.separator 
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        
        this.client = Mockito.mock(OpenStackHttpClient.class);
        this.plugin = Mockito.spy(new OpenStackQuotaPlugin(openstackConfFilePath));
        this.plugin.setClient(client);
        this.cloudUser = this.testUtils.createOpenStackUser();
    }
    
    // test case: ...
    @Test
    public void testGetUserQuota() throws FogbowException {
        // set up
        GetComputeQuotasResponse computeQuotas = getComputeQuotaResponse();
        Mockito.doReturn(computeQuotas).when(this.plugin).getComputeQuotas(Mockito.eq(this.cloudUser));

        GetNetworkQuotasResponse networkQuotas = getNetworkQuotasResponse();
        Mockito.doReturn(networkQuotas).when(this.plugin).getNetworkQuotas(Mockito.eq(this.cloudUser));

        GetVolumeQuotasResponse volumeQuotas = getVolumeQuotasResponse();
        Mockito.doReturn(volumeQuotas).when(this.plugin).getVolumeQuotas(Mockito.eq(this.cloudUser));

        ResourceQuota resourceQuota = Mockito.mock(ResourceQuota.class);
        Mockito.doReturn(resourceQuota).when(this.plugin).buildResourceQuota(Mockito.eq(computeQuotas),
                Mockito.eq(networkQuotas), Mockito.eq(volumeQuotas));

        // exercise
        this.plugin.getUserQuota(this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getComputeQuotas(Mockito.eq(this.cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworkQuotas(Mockito.eq(this.cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getVolumeQuotas(Mockito.eq(this.cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildResourceQuota(Mockito.eq(computeQuotas),
                Mockito.eq(networkQuotas), Mockito.eq(volumeQuotas));
    }
    
    // test case: ...
    @Test
    public void testBuildResourceQuota() {
        // set up
        GetComputeQuotasResponse computeQuotas = getComputeQuotaResponse();
        GetNetworkQuotasResponse networkQuotas = getNetworkQuotasResponse();
        GetVolumeQuotasResponse volumeQuotas = getVolumeQuotasResponse();

        ResourceAllocation totalQuota = buildTotalQuota();
        Mockito.doReturn(totalQuota).when(this.plugin).getTotalQuota(Mockito.eq(computeQuotas),
                Mockito.eq(networkQuotas), Mockito.eq(volumeQuotas));

        ResourceAllocation usedQuota = buildUsedQuota();
        Mockito.doReturn(usedQuota).when(this.plugin).getTotalQuota(Mockito.eq(computeQuotas),
                Mockito.eq(networkQuotas), Mockito.eq(volumeQuotas));

        ResourceQuota expected = new ResourceQuota(totalQuota, usedQuota);

        // exercise
        ResourceQuota quota = this.plugin.buildResourceQuota(computeQuotas, networkQuotas, volumeQuotas);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalQuota(Mockito.eq(computeQuotas),
                Mockito.eq(networkQuotas), Mockito.eq(volumeQuotas));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getUsedQuota(Mockito.eq(computeQuotas),
                Mockito.eq(networkQuotas), Mockito.eq(volumeQuotas));

        Assert.assertEquals(expected, quota);
    }
    
    // test case: ...
    @Test
    public void testGetUsedQuota() {
        // set up
        GetComputeQuotasResponse computeQuotas = Mockito.mock(GetComputeQuotasResponse.class);
        GetNetworkQuotasResponse networkQuotas = Mockito.mock(GetNetworkQuotasResponse.class);
        GetVolumeQuotasResponse volumeQuotas = Mockito.mock(GetVolumeQuotasResponse.class);
        
        Mockito.when(computeQuotas.getTotalInstancesUsed()).thenReturn(USED_INSTANCES_VALUE);
        Mockito.when(computeQuotas.getTotalCoresUsed()).thenReturn(USED_VCPU_VALUE);
        Mockito.when(computeQuotas.getTotalRamUsed()).thenReturn(USED_RAM_VALUE);
        Mockito.when(networkQuotas.getNetworkUsed()).thenReturn(USED_NETWORK_VALUE);
        Mockito.when(networkQuotas.getFloatingIpUsed()).thenReturn(USED_PUBLIC_IP_VALUE);
        Mockito.when(volumeQuotas.getTotalGigabytesUsed()).thenReturn(USED_DISK_VALUE);
        
        ResourceAllocation expected = buildUsedQuota();

        // exercise
        ResourceAllocation usedQuota = this.plugin.getUsedQuota(computeQuotas, networkQuotas, volumeQuotas);

        // verify
        Mockito.verify(computeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getTotalInstancesUsed();
        Mockito.verify(computeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getTotalCoresUsed();
        Mockito.verify(computeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getTotalRamUsed();
        Mockito.verify(networkQuotas, Mockito.times(TestUtils.RUN_ONCE)).getNetworkUsed();
        Mockito.verify(networkQuotas, Mockito.times(TestUtils.RUN_ONCE)).getFloatingIpUsed();
        Mockito.verify(volumeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getTotalGigabytesUsed();
        
        Assert.assertEquals(expected, usedQuota);
    }
    
    // test case: ...
    @Test
    public void testGetTotalQuota() {
        // set up
        GetComputeQuotasResponse computeQuotas = Mockito.mock(GetComputeQuotasResponse.class);
        GetNetworkQuotasResponse networkQuotas = Mockito.mock(GetNetworkQuotasResponse.class);
        GetVolumeQuotasResponse volumeQuotas = Mockito.mock(GetVolumeQuotasResponse.class);

        Mockito.when(computeQuotas.getMaxTotalInstances()).thenReturn(TOTAL_INSTANCES_VALUE);
        Mockito.when(computeQuotas.getMaxTotalCores()).thenReturn(TOTAL_VCPU_VALUE);
        Mockito.when(computeQuotas.getMaxTotalRamSize()).thenReturn(TOTAL_RAM_VALUE);
        Mockito.when(networkQuotas.getNetworkLimit()).thenReturn(TOTAL_NETWORK_VALUE);
        Mockito.when(networkQuotas.getFloatingIpLimit()).thenReturn(TOTAL_PUBLIC_IP_VALUE);
        Mockito.when(volumeQuotas.getMaxTotalVolumeGigabytes()).thenReturn(TOTAL_DISK_VALUE);

        ResourceAllocation expected = buildTotalQuota();

        // exercise
        ResourceAllocation usedQuota = this.plugin.getTotalQuota(computeQuotas, networkQuotas, volumeQuotas);

        // verify
        Mockito.verify(computeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getMaxTotalInstances();
        Mockito.verify(computeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getMaxTotalCores();
        Mockito.verify(computeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getMaxTotalRamSize();
        Mockito.verify(networkQuotas, Mockito.times(TestUtils.RUN_ONCE)).getNetworkLimit();
        Mockito.verify(networkQuotas, Mockito.times(TestUtils.RUN_ONCE)).getFloatingIpLimit();
        Mockito.verify(volumeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getMaxTotalVolumeGigabytes();

        Assert.assertEquals(expected, usedQuota);
    }

    // test case: ...
    @Test
    public void test() {
        // set up
        
        // exercise
        
        // verify
        
    }
    
    private ResourceAllocation buildUsedQuota() {
        return ResourceAllocation.builder()
                .instances(USED_INSTANCES_VALUE)
                .vCPU(USED_VCPU_VALUE)
                .ram(USED_RAM_VALUE)
                .disk(USED_DISK_VALUE)
                .networks(USED_NETWORK_VALUE)
                .publicIps(USED_PUBLIC_IP_VALUE)
                .build();
    }
    
    private ResourceAllocation buildTotalQuota() {
        return ResourceAllocation.builder()
                .instances(TOTAL_INSTANCES_VALUE)
                .vCPU(TOTAL_VCPU_VALUE)
                .ram(TOTAL_RAM_VALUE)
                .disk(TOTAL_DISK_VALUE)
                .networks(TOTAL_NETWORK_VALUE)
                .publicIps(TOTAL_PUBLIC_IP_VALUE)
                .build();
    }
    
    private GetVolumeQuotasResponse getVolumeQuotasResponse() {
        String json = "{\"limits\": {"
                + "     \"absolute\": {"
                + "         \"maxTotalVolumeGigabytes\": 1000,"
                + "         \"maxTotalVolumes\": 10,"
                + "         \"totalVolumesUsed\": 1,"
                + "         \"totalGigabytesUsed\": 1"
                + "         }"
                + "     }"
                + " }";
        return GetVolumeQuotasResponse.fromJson(json);
    }

    private GetNetworkQuotasResponse getNetworkQuotasResponse() {
        String json = "{\"quota\": {"
                + "     \"floatingip\": {"
                + "         \"limit\": 2,"
                + "         \"reserved\": 0,"
                + "         \"used\": 1"
                + "         },"
                + "     \"network\": {"
                + "         \"limit\": 5,"
                + "         \"reserved\": 0,"
                + "         \"used\": 1"
                + "         }"
                + "     }"
                + " }";
        return GetNetworkQuotasResponse.fromJson(json);
    }

    private GetComputeQuotasResponse getComputeQuotaResponse() {
        String json = "{\"limits\":{"
                + "     \"absolute\":{"
                + "         \"maxTotalCores\":8," 
                + "         \"maxTotalInstances\":10," 
                + "         \"maxTotalRAMSize\":8192," 
                + "         \"totalCoresUsed\":2," 
                + "         \"totalInstancesUsed\":1," 
                + "         \"totalRAMUsed\":2048"
                + "         }"
                + "     }"
                + " }";
        
        return GetComputeQuotasResponse.fromJson(json);
    }

}
