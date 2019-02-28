package cloud.fogbow.ras.core.plugins.interoperability.openstack.quota.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2.OpenStackComputePlugin;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.HashMap;

public class OpenStackComputeQuotaPluginTest {

    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";

    private static final String FAKE_QUOTA_JSON_RESPONSE =
            "{\"limits\": {\"rate\": [], \"absolute\": {\"maxServerMeta\": 128, \"maxPersonality\": 5, "
                    + "\"totalServerGroupsUsed\": 0, \"maxImageMeta\": 128, \"maxPersonalitySize\": 10240, "
                    + "\"maxTotalKeypairs\": 10, \"maxSecurityGroupRules\": 20, \"maxServerGroups\": 10, "
                    + "\"totalCoresUsed\": 6, \"totalRAMUsed\": 12288, \"totalInstancesUsed\": 6, "
                    + "\"maxSecurityGroups\": 10, \"totalFloatingIpsUsed\": 0, \"maxTotalCores\": 64, "
                    + "\"maxServerGroupMembers\": 10, \"maxTotalFloatingIps\": 10, "
                    + "\"totalSecurityGroupsUsed\": 1, \"maxTotalInstances\": 20, \"maxTotalRAMSize\": 46080}}}";

    private OpenStackComputeQuotaPlugin plugin;
    private OpenStackV3Token localUserAttributes;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        String cloudConfPath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.plugin = Mockito.spy(new OpenStackComputeQuotaPlugin(cloudConfPath));

        HashMap<String, String> extraAttributes = new HashMap<>();
        extraAttributes.put(OpenStackConstants.Identity.PROJECT_KEY_JSON, FAKE_PROJECT_ID);
        FederationUser federationUser = new FederationUser(FAKE_TOKEN_PROVIDER, FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, extraAttributes);
        this.localUserAttributes = new OpenStackV3Token(federationUser);
    }

    // test case: Tests if getTotalQuota(), getUsedQuota() and getAvailableQuota() returns the right
    // quotas from the FAKE_QUOTA_JSON_RESPONSE.
    @Test
    public void testGetUserQuota() throws FogbowException {
        // set up
        GetQuotaResponse getQuotaResponse = GetQuotaResponse.fromJson(FAKE_QUOTA_JSON_RESPONSE);

        int maxTotalCores = getQuotaResponse.getMaxTotalCores();
        int maxTotalRamSize = getQuotaResponse.getMaxTotalRamSize();
        int maxTotalInstances = getQuotaResponse.getMaxTotalInstances();
        ComputeAllocation totalQuota = new ComputeAllocation(maxTotalCores, maxTotalRamSize, maxTotalInstances);

        int totalCoresUsed = getQuotaResponse.getTotalCoresUsed();
        int totalRamUsed = getQuotaResponse.getTotalRamUsed();
        int totalInstancesUsed = getQuotaResponse.getTotalInstancesUsed();
        ComputeAllocation usedQuota = new ComputeAllocation(totalCoresUsed, totalRamUsed, totalInstancesUsed);

        ComputeQuota computeQuota = new ComputeQuota(totalQuota, usedQuota);
        Mockito.doReturn(computeQuota).when(this.plugin).getUserQuota(this.localUserAttributes);

        // exercise
        ComputeAllocation retrievedTotalQuota = this.plugin.getUserQuota(this.localUserAttributes).getTotalQuota();
        ComputeAllocation retrievedUsedQuota = this.plugin.getUserQuota(this.localUserAttributes).getUsedQuota();
        ComputeAllocation retrievedAvailableQuota = this.plugin.getUserQuota(this.localUserAttributes).getAvailableQuota();

        // verify
        Assert.assertEquals(totalQuota.getvCPU(), retrievedTotalQuota.getvCPU());
        Assert.assertEquals(totalQuota.getRam(), retrievedTotalQuota.getRam());
        Assert.assertEquals(totalQuota.getInstances(), retrievedTotalQuota.getInstances());

        Assert.assertEquals(usedQuota.getvCPU(), retrievedUsedQuota.getvCPU());
        Assert.assertEquals(usedQuota.getRam(), retrievedUsedQuota.getRam());
        Assert.assertEquals(usedQuota.getInstances(), retrievedUsedQuota.getInstances());

        Assert.assertEquals(computeQuota.getAvailableQuota().getvCPU(), retrievedAvailableQuota.getvCPU());
        Assert.assertEquals(computeQuota.getAvailableQuota().getRam(), retrievedAvailableQuota.getRam());
        Assert.assertEquals(computeQuota.getAvailableQuota().getInstances(), retrievedAvailableQuota.getInstances());
    }

    @Test(expected = UnexpectedException.class)
    public void testGetUserQuotaWhenGetRequestThrowsAnException() throws FogbowException {
        // set up
        Mockito.doThrow(UnexpectedException.class).when(this.plugin).getUserQuota(this.localUserAttributes);

        // exercise
        this.plugin.getUserQuota(this.localUserAttributes);
    }

}
