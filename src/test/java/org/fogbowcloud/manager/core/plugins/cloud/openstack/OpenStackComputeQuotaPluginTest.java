package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.util.JSONUtil;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

public class OpenStackComputeQuotaPluginTest {

    private static final String TENANT_ID = "tenantId";
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
    private Token token;

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
        this.plugin = Mockito.spy(new OpenStackComputeQuotaPlugin());

        this.token = new Token();
        Map<String, String> attributes = new HashMap<>();
        attributes.put(TENANT_ID, FAKE_TENANT_ID);
        token.setAttributes(attributes);
        token.setAccessId("");
    }

    // test case: Tests if getTotalQuota(), getUsedQuota() and getAvailableQuota() returns the right
    // quotas from the FAKE_QUOTA_JSON_RESPONSE.
    @Test
    public void testgetUserQuota() throws FogbowManagerException, UnexpectedException {
        // set up
        ComputeQuota computeQuota = processJson(FAKE_QUOTA_JSON_RESPONSE);

        Mockito.doReturn(FAKE_QUOTA_JSON_RESPONSE).when(this.plugin).getQuotaJson(this.token);

        // exercise
        ComputeAllocation totalQuota = this.plugin.getUserQuota(this.token).getTotalQuota();
        ComputeAllocation usedQuota = this.plugin.getUserQuota(this.token).getUsedQuota();
        ComputeAllocation availableQuota = this.plugin.getUserQuota(this.token).getAvailableQuota();

        // verify
        Assert.assertEquals(computeQuota.getTotalQuota().getvCPU(), totalQuota.getvCPU());
        Assert.assertEquals(computeQuota.getTotalQuota().getRam(), totalQuota.getRam());
        Assert.assertEquals(computeQuota.getTotalQuota().getInstances(), totalQuota.getInstances());

        Assert.assertEquals(computeQuota.getUsedQuota().getvCPU(), usedQuota.getvCPU());
        Assert.assertEquals(computeQuota.getUsedQuota().getRam(), usedQuota.getRam());
        Assert.assertEquals(computeQuota.getUsedQuota().getInstances(), usedQuota.getInstances());

        Assert.assertEquals(computeQuota.getAvailableQuota().getvCPU(), availableQuota.getvCPU());
        Assert.assertEquals(computeQuota.getAvailableQuota().getRam(), availableQuota.getRam());
        Assert.assertEquals(computeQuota.getAvailableQuota().getInstances(), availableQuota.getInstances());
    }

    @Test(expected = UnexpectedException.class)
    public void testGetUserQuotaWhenGetRequestThrowsAnException() throws UnexpectedException, FogbowManagerException {
        // set up
        Mockito.doThrow(UnexpectedException.class).when(this.plugin).getQuotaJson(this.token);

        // exercise
        this.plugin.getUserQuota(this.token);
    }

    private ComputeQuota processJson(String jsonStr) {
        JSONObject jsonObject = (JSONObject) JSONUtil.getValue(jsonStr, "limits", "absolute");

        ComputeAllocation totalQuota = new ComputeAllocation(jsonObject.getInt(OpenStackComputeQuotaPlugin.MAX_TOTAL_CORES_JSON),
                jsonObject.getInt(OpenStackComputeQuotaPlugin.MAX_TOTAL_RAM_SIZE_JSON),
                jsonObject.getInt(OpenStackComputeQuotaPlugin.MAX_TOTAL_INSTANCES_JSON));

        ComputeAllocation usedQuota = new ComputeAllocation(jsonObject.getInt(OpenStackComputeQuotaPlugin.TOTAL_CORES_USED_JSON),
                jsonObject.getInt(OpenStackComputeQuotaPlugin.TOTAL_RAM_USED_JSON),
                jsonObject.getInt(OpenStackComputeQuotaPlugin.TOTAL_INSTANCES_USED_JSON));

        return new ComputeQuota(totalQuota, usedQuota);
    }
}
