package cloud.fogbow.ras.core.plugins.interoperability.openstack.quota.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;

@PrepareForTest({ GetQuotaResponse.class, DatabaseManager.class,
        OpenStackHttpToFogbowExceptionMapper.class })
public class OpenStackComputeQuotaPluginTest extends BaseUnitTests {

    private static final String ANY_JSON = "{\"foo\": \"bar\"}";
    private static final String ANY_URL = "http://localhost:5056";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String MAP_METHOD = "map";

    private static final int ANY_STATUS_CODE = 400;
    private static final int FAKE_MAX_TOTAL_CORES = 16;
    private static final int FAKE_MAX_TOTAL_INSTANCES = 4;
    private static final int FAKE_MAX_TOTAL_MEMORY = 16384;
    private static final int FAKE_TOTAL_CORES = 8;
    private static final int FAKE_TOTAL_INSTANCES = 2;
    private static final int FAKE_TOTAL_MEMORY = 8192;
    private OpenStackComputeQuotaPlugin plugin;
    private OpenStackHttpClient client;
    private OpenStackV3User cloudUser;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        testUtils.mockReadOrdersFromDataBase();

        String cloudConfPath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.client = Mockito.mock(OpenStackHttpClient.class);
        this.plugin = Mockito.spy(new OpenStackComputeQuotaPlugin(cloudConfPath));
        this.plugin.setClient(client);
        this.cloudUser = new OpenStackV3User(TestUtils.FAKE_USER_ID, TestUtils.FAKE_USER_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);
    }

    // test case: Tests if getTotalQuota(), getUsedQuota() and getAvailableQuota() returns the right
    // quotas from the mocked response.
    @Test
    public void testGetUserQuota() throws FogbowException {
        // set up
        Mockito.doReturn(ANY_JSON).when(this.plugin).doGetQuota(Mockito.anyString(), Mockito.eq(cloudUser));

        GetQuotaResponse getQuotaResponse = getQuotaResponseMock();

        PowerMockito.mockStatic(GetQuotaResponse.class);
        BDDMockito.given(GetQuotaResponse.fromJson(Mockito.anyString()))
                .willReturn(getQuotaResponse);

        int maxTotalCores = getQuotaResponse.getMaxTotalCores();
        int maxTotalRamSize = getQuotaResponse.getMaxTotalRamSize();
        int maxTotalInstances = getQuotaResponse.getMaxTotalInstances();
        ComputeAllocation totalQuota = new ComputeAllocation(maxTotalCores, maxTotalRamSize, maxTotalInstances);

        int totalCoresUsed = getQuotaResponse.getTotalCoresUsed();
        int totalRamUsed = getQuotaResponse.getTotalRamUsed();
        int totalInstancesUsed = getQuotaResponse.getTotalInstancesUsed();
        ComputeAllocation usedQuota = new ComputeAllocation(totalCoresUsed, totalRamUsed, totalInstancesUsed);

        // exercise
        ComputeQuota quota = this.plugin.getUserQuota(this.cloudUser);
        ComputeAllocation retrievedTotalQuota = quota.getTotalQuota();
        ComputeAllocation retrievedUsedQuota = quota.getUsedQuota();

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetQuota(Mockito.anyString(), Mockito.eq(cloudUser));

        Assert.assertEquals(totalQuota.getvCPU(), retrievedTotalQuota.getvCPU());
        Assert.assertEquals(totalQuota.getRam(), retrievedTotalQuota.getRam());
        Assert.assertEquals(totalQuota.getInstances(), retrievedTotalQuota.getInstances());

        Assert.assertEquals(usedQuota.getvCPU(), retrievedUsedQuota.getvCPU());
        Assert.assertEquals(usedQuota.getRam(), retrievedUsedQuota.getRam());
        Assert.assertEquals(usedQuota.getInstances(), retrievedUsedQuota.getInstances());
    }

    // test case: When calling the doGetQuota method, it must verify
    // that the call was successful.
    @Test
    public void testDoGetQuotaSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        Mockito.when(this.client.doGetRequest(Mockito.eq(ANY_URL), Mockito.eq(cloudUser)))
                .thenReturn(ANY_JSON);

        // exercise
        String jsoResponse = this.plugin.doGetQuota(ANY_URL, this.cloudUser);

        // verify
        Assert.assertEquals(ANY_JSON, jsoResponse);
    }

    // test case: When calling the doGetQuota method and an unexpected
    // error occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoGetQuotaUnsuccessfully() throws Exception {
        // set up
        HttpResponseException exception = new HttpResponseException(ANY_STATUS_CODE, Messages.Exception.INVALID_PARAMETER);
        Mockito.when(this.client.doGetRequest(Mockito.eq(ANY_URL), Mockito.eq(cloudUser)))
                .thenThrow(exception);

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, MAP_METHOD, Mockito.any());

        GetQuotaResponse getQuotaResponse = getQuotaResponseMock();

        PowerMockito.mockStatic(GetQuotaResponse.class);
        BDDMockito.given(GetQuotaResponse.fromJson(Mockito.anyString()))
                .willReturn(getQuotaResponse);

        try {
            // exercise
            this.plugin.doGetQuota(ANY_URL, this.cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            Assert.assertEquals(exception.getMessage(), e.getMessage());

            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(exception));
        }
    }

    private GetQuotaResponse getQuotaResponseMock() {
        GetQuotaResponse mock = Mockito.mock(GetQuotaResponse.class);
        Mockito.when(mock.getMaxTotalCores()).thenReturn(FAKE_MAX_TOTAL_CORES);
        Mockito.when(mock.getMaxTotalInstances()).thenReturn(FAKE_MAX_TOTAL_INSTANCES);
        Mockito.when(mock.getMaxTotalRamSize()).thenReturn(FAKE_MAX_TOTAL_MEMORY);
        Mockito.when(mock.getTotalCoresUsed()).thenReturn(FAKE_TOTAL_CORES);
        Mockito.when(mock.getTotalInstancesUsed()).thenReturn(FAKE_TOTAL_INSTANCES);
        Mockito.when(mock.getTotalRamUsed()).thenReturn(FAKE_TOTAL_MEMORY);

        return mock;
    }
}
