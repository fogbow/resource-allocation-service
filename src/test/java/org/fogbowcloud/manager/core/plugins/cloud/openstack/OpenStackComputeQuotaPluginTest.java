package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;
import org.fogbowcloud.manager.core.models.tokens.OpenStackUserAttributes;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.quota.v2.GetQuotaResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class OpenStackComputeQuotaPluginTest {

    private static final String TENANT_ID = "TENANT_ID";
    private static final String FAKE_TENANT_ID = "fake-tenant-id";

    private static final String FAKE_QUOTA_JSON_RESPONSE =
            "{\"limits\": {\"rate\": [], \"absolute\": {\"maxServerMeta\": 128, \"maxPersonality\": 5, "
                    + "\"totalServerGroupsUsed\": 0, \"maxImageMeta\": 128, \"maxPersonalitySize\": 10240, "
                    + "\"maxTotalKeypairs\": 10, \"maxSecurityGroupRules\": 20, \"maxServerGroups\": 10, "
                    + "\"totalCoresUsed\": 6, \"totalRAMUsed\": 12288, \"totalInstancesUsed\": 6, "
                    + "\"maxSecurityGroups\": 10, \"totalFloatingIpsUsed\": 0, \"maxTotalCores\": 64, "
                    + "\"maxServerGroupMembers\": 10, \"maxTotalFloatingIps\": 10, "
                    + "\"totalSecurityGroupsUsed\": 1, \"maxTotalInstances\": 20, \"maxTotalRAMSize\": 46080}}}";

    private OpenStackComputeQuotaPlugin plugin;
    private OpenStackUserAttributes localUserAttributes;

    @Before
    public void setUp() throws InvalidParameterException {
        HomeDir.getInstance().setPath("src/test/resources/private");
        this.plugin = Mockito.spy(new OpenStackComputeQuotaPlugin());

        this.localUserAttributes = new OpenStackUserAttributes("", FAKE_TENANT_ID);
    }

    // test case: Tests if getTotalQuota(), getUsedQuota() and getAvailableQuota() returns the right
    // quotas from the FAKE_QUOTA_JSON_RESPONSE.
    @Test
    public void testGetUserQuota() throws FogbowManagerException, UnexpectedException {
        // set up
        GetQuotaResponse getQuotaResponse = GetQuotaResponse.fromJson(FAKE_QUOTA_JSON_RESPONSE);

        int maxTotalCores = getQuotaResponse.getMaxTotalCores();
        int maxTotalRamSize = getQuotaResponse.getMaxTotalRamSize();
        int maxTotalInstances = getQuotaResponse.getMaxTotalInstances();
        ComputeAllocation totalQuota = new ComputeAllocation(maxTotalCores,	maxTotalRamSize, maxTotalInstances);

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
    public void testGetUserQuotaWhenGetRequestThrowsAnException() throws UnexpectedException, FogbowManagerException {
        // set up
        Mockito.doThrow(UnexpectedException.class).when(this.plugin).getUserQuota(this.localUserAttributes);

        // exercise
        this.plugin.getUserQuota(this.localUserAttributes);
    }

}
