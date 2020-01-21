package cloud.fogbow.ras.core.plugins.interoperability.openstack.quota.v2;

import java.io.File;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;

@PrepareForTest({ 
    DatabaseManager.class, 
    GetComputeQuotasResponse.class,
    GetVolumeQuotasResponse.class, 
    GetNetworkQuotasResponse.class, 
    OpenStackCloudUtils.class,
    OpenStackHttpToFogbowExceptionMapper.class
})
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
    
    private static final String FAKE_TENANT_ID = "fake-tenant-id";
    private static final String FAKE_VOLUME_ENDPOINT = "https://mycloud.mydomain:8776/v3/fake-tenant-id/limits";
    private static final String FAKE_NETWORK_ENDPOINT = "https://mycloud.domain:9696/v2.0/quotas/fake-tenant-id/details.json";
    private static final String FAKE_COMPUTE_ENDPOINT = "https://mycloud.domain:8774/v2/limits";
    private static final String METHOD_GET_PROJECT_ID_FROM = "getProjectIdFrom";
    
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
    
    // test case: When calling the getUserQuota method, it must verify
    // that the call was successful.
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
    
    // test case: When calling the buildResourceQuota method, it must verify that
    // the call was successful and returned the expected value.
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
        Mockito.doReturn(usedQuota).when(this.plugin).getUsedQuota(Mockito.eq(computeQuotas), Mockito.eq(networkQuotas),
                Mockito.eq(volumeQuotas));

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
    
    // test case: When calling the getUsedQuota method, it must verify that
    // the call was successful and returned the expected value.
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
    
    // test case: When calling the getTotalQuota method, it must verify that
    // the call was successful and returned the expected value.
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
        ResourceAllocation totalQuota = this.plugin.getTotalQuota(computeQuotas, networkQuotas, volumeQuotas);

        // verify
        Mockito.verify(computeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getMaxTotalInstances();
        Mockito.verify(computeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getMaxTotalCores();
        Mockito.verify(computeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getMaxTotalRamSize();
        Mockito.verify(networkQuotas, Mockito.times(TestUtils.RUN_ONCE)).getNetworkLimit();
        Mockito.verify(networkQuotas, Mockito.times(TestUtils.RUN_ONCE)).getFloatingIpLimit();
        Mockito.verify(volumeQuotas, Mockito.times(TestUtils.RUN_ONCE)).getMaxTotalVolumeGigabytes();

        Assert.assertEquals(expected, totalQuota);
    }
    
    // test case: When calling the getVolumeQuotas method, it must verify that
    // the call was successful.
    @Test
    public void testGetVolumeQuotas() throws FogbowException {
        // set up
        String tenantId = FAKE_TENANT_ID;
        Mockito.doReturn(tenantId).when(this.plugin).getTenantId(Mockito.eq(this.cloudUser));

        String endpoint = FAKE_VOLUME_ENDPOINT;
        Mockito.doReturn(endpoint).when(this.plugin).getVolumeQuotaEndpoint(Mockito.eq(tenantId));

        String response = generateVolumeQuotas();
        Mockito.doReturn(response).when(this.plugin).doGetQuota(Mockito.eq(endpoint), Mockito.eq(this.cloudUser));

        // exercise
        this.plugin.getVolumeQuotas(this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTenantId(Mockito.eq(this.cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getVolumeQuotaEndpoint(Mockito.eq(tenantId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetQuota(Mockito.eq(endpoint),
                Mockito.eq(this.cloudUser));

        PowerMockito.verifyStatic(GetVolumeQuotasResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetVolumeQuotasResponse.fromJson(Mockito.eq(response));
    }
    
    // test case: When calling the getVolumeQuotaEndpoint method, it must verify if
    // returned the expected value.
    @Test
    public void testGetVolumeQuotaEndpoint() {
        // set up
        String expected = FAKE_VOLUME_ENDPOINT;

        // exercise
        String endpoint = this.plugin.getVolumeQuotaEndpoint(FAKE_TENANT_ID);

        // verify
        Assert.assertEquals(expected, endpoint);
    }
    
    // test case: When calling the getNetworkQuotas method, it must verify that
    // the call was successful.
    @Test
    public void testGetNetworkQuotas() throws FogbowException {
        // set up
        String tenantId = FAKE_TENANT_ID;
        Mockito.doReturn(tenantId).when(this.plugin).getTenantId(Mockito.eq(this.cloudUser));

        String endpoint = FAKE_NETWORK_ENDPOINT;
        Mockito.doReturn(endpoint).when(this.plugin).getNetworkQuotaEndpoint(Mockito.eq(tenantId));

        String response = generateNetworkQuotas();
        Mockito.doReturn(response).when(this.plugin).doGetQuota(Mockito.eq(endpoint), Mockito.eq(this.cloudUser));

        // exercise
        this.plugin.getNetworkQuotas(this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTenantId(Mockito.eq(this.cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getNetworkQuotaEndpoint(Mockito.eq(tenantId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetQuota(Mockito.eq(endpoint),
                Mockito.eq(this.cloudUser));

        PowerMockito.verifyStatic(GetNetworkQuotasResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetNetworkQuotasResponse.fromJson(Mockito.eq(response));
    }
    
    // test case: When calling the getNetworkQuotaEndpoint method, it must verify if
    // returned the expected value.
    @Test
    public void testGetNetworkQuotaEndpoint() {
        // set up
        String expected = FAKE_NETWORK_ENDPOINT;

        // exercise
        String endpoint = this.plugin.getNetworkQuotaEndpoint(FAKE_TENANT_ID);

        // verify
        Assert.assertEquals(expected, endpoint);
    }
    
    // test case: When calling the getTenantId method, it must verify that
    // the call was successful.
    @Test
    public void testGetTenantId() throws Exception {
        // set up
        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.doReturn(FAKE_TENANT_ID).when(OpenStackCloudUtils.class, METHOD_GET_PROJECT_ID_FROM,
                Mockito.eq(this.cloudUser));

        // exercise
        this.plugin.getTenantId(this.cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(this.cloudUser));
    }
    
    // test case: When calling the getComputeQuotas method, it must verify that
    // the call was successful.
    @Test
    public void testGetComputeQuotas() throws FogbowException {
        // set up
        String endpoint = FAKE_COMPUTE_ENDPOINT;
        Mockito.doReturn(endpoint).when(this.plugin).getComputeQuotaEndpoint();

        String response = generateComputeQuotas();
        Mockito.doReturn(response).when(this.plugin).doGetQuota(Mockito.eq(endpoint), Mockito.eq(this.cloudUser));

        // exercise
        this.plugin.getComputeQuotas(this.cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getComputeQuotaEndpoint();
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetQuota(Mockito.eq(endpoint),
                Mockito.eq(this.cloudUser));

        PowerMockito.verifyStatic(GetComputeQuotasResponse.class, Mockito.times(TestUtils.RUN_ONCE));
        GetComputeQuotasResponse.fromJson(Mockito.eq(response));
    }
    
    // test case: When calling the getComputeQuotaEndpoint method, it must verify if
    // returned the expected value.
    @Test
    public void testGetComputeQuotaEndpoint() {
        // set up
        String expected = FAKE_COMPUTE_ENDPOINT;

        // exercise
        String endpoint = this.plugin.getComputeQuotaEndpoint();

        // verify
        Assert.assertEquals(expected, endpoint);
    }
    
    // test case: When calling the doGetQuota method, it must verify that
    // the call was successful.
    @Test
    public void testDoGetQuota() throws Exception {
        // set up
        String endpoint = FAKE_COMPUTE_ENDPOINT;
        String response = generateComputeQuotas();
        Mockito.when(this.client.doGetRequest(Mockito.eq(endpoint), Mockito.eq(this.cloudUser))).thenReturn(response);

        // exercise
        this.plugin.doGetQuota(endpoint, cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(endpoint),
                Mockito.eq(this.cloudUser));
    }
    
    // test case: When calling the doGetQuota method and the server returns an
    // error, it must verify that the expected exception has been thrown.
    @Test(expected = InvalidParameterException.class)
    public void testDoGetQuotaFail() throws HttpResponseException, FogbowException {
        // set up
        String endpoint = FAKE_COMPUTE_ENDPOINT;
        HttpResponseException exception = new HttpResponseException(HttpStatus.SC_BAD_REQUEST, TestUtils.ANY_VALUE);
        Mockito.when(this.client.doGetRequest(Mockito.eq(endpoint), Mockito.eq(this.cloudUser))).thenThrow(exception);

        this.plugin.doGetQuota(endpoint, this.cloudUser);
        Assert.fail();
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
        String json = generateVolumeQuotas();
        return GetVolumeQuotasResponse.fromJson(json);
    }

    private String generateVolumeQuotas() {
        String jsonFormat = "{\"limits\": {"
                + "     \"absolute\": {"
                + "         \"maxTotalVolumeGigabytes\": %s,"
                + "         \"totalGigabytesUsed\": %s"
                + "         }"
                + "     }"
                + " }";
        
        return String.format(jsonFormat, TOTAL_DISK_VALUE, USED_DISK_VALUE);
    }

    private GetNetworkQuotasResponse getNetworkQuotasResponse() {
        String json = generateNetworkQuotas();
        return GetNetworkQuotasResponse.fromJson(json);
    }

    private String generateNetworkQuotas() {
        String jsonFormat = "{\"quota\": {"
                + "     \"floatingip\": {"
                + "         \"limit\": %s,"
                + "         \"used\": %s"
                + "         },"
                + "     \"network\": {"
                + "         \"limit\": %s,"
                + "         \"used\": %s"
                + "         }"
                + "     }"
                + " }";
        
        return String.format(jsonFormat, TOTAL_PUBLIC_IP_VALUE, USED_PUBLIC_IP_VALUE, 
                TOTAL_NETWORK_VALUE, USED_NETWORK_VALUE);
    }

    private GetComputeQuotasResponse getComputeQuotaResponse() {
        String json = generateComputeQuotas();
        return GetComputeQuotasResponse.fromJson(json);
    }

    private String generateComputeQuotas() {
        String jsonFormat = "{\"limits\":{"
                + "     \"absolute\":{"
                + "         \"maxTotalCores\":%s," 
                + "         \"maxTotalInstances\":%s," 
                + "         \"maxTotalRAMSize\":%s," 
                + "         \"totalCoresUsed\":%s," 
                + "         \"totalInstancesUsed\":%s," 
                + "         \"totalRAMUsed\":%s"
                + "         }"
                + "     }"
                + " }";
        
        return String.format(jsonFormat, TOTAL_VCPU_VALUE, TOTAL_INSTANCES_VALUE, TOTAL_RAM_VALUE, 
                USED_VCPU_VALUE, USED_INSTANCES_VALUE, USED_RAM_VALUE);
    }

}
