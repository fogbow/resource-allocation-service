package org.fogbowcloud.manager.core.plugins.cloud.quota.compute;

import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.QuotaException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.token.Token;
import org.junit.Test;

import com.google.gson.Gson;

public class OpenStackComputeQuotaPluginTest {
	
	@Test
	public void testGetUserQuota() throws QuotaException {
		//Properties properties = new Properties();
		
		//properties.put(OpenStackConfigurationConstants.COMPUTE_NOVAV2_URL_KEY, "https://cloud.lsd.ufcg.edu.br:8774");
		OpenStackComputeQuotaPlugin plugin = new OpenStackComputeQuotaPlugin();
		
		Token token = new Token();
		
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("tenantId", "3324431f606d4a74a060cf78c16fcb21");
		token.setAttributes(attributes);
		token.setAccessId("gAAAAABbGtM0Eqd3aINFw6lX1Q91k4RDOS4nTd3hkUhymh81JfYa5UGJjkNfbN3WWLHPa-1oSofJG75H9RCMv3Mm1kLzrfdZLErL-rF2ZTOnAxT_iI6qzPyU2A7GmXl9JP12_WHuhVdxLZysxO3UTHryTCJjy-jOWNdvbXV5E6JfWp-SW49rmrH5WXvSLNxEK80Sn7oJgFMjZ3VmyiNh8-skfM01Uo0_8B8dCRfkFskpdu6QV2LzeZg");
		
		ComputeQuota quota = plugin.getUserQuota(token);
		Gson gson = new Gson();
		String userJSONString = gson.toJson(quota);
		System.out.println(userJSONString);
	}
	
}
