package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.quotas.ComputeQuota;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class OpenStackComputeQuotaPluginTest {
	private OpenStackComputeQuotaPlugin plugin;
	private Token token;
	
	public void setUp() {
		HomeDir.getInstance().setPath("src/test/resources/private");
		
		PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
		Properties properties = propertiesHolder.getProperties();
		properties.put("", "");
		
		plugin = new OpenStackComputeQuotaPlugin();
		
		token = new Token();
		Map<String, String> attributes = new HashMap<String, String>();
		
		attributes.put("tenantId", Mockito.anyString());
		token.setAttributes(attributes);
		token.setAccessId(Mockito.anyString());
	}
	
    /**
     * TODO The test must be redone using mock.
     * @throws FogbowManagerException
     */
    @Ignore
	@Test
	public void testGetUserQuota() throws FogbowManagerException, UnexpectedException {		
    	Mockito.doReturn(null).when(this.plugin.getJson(this.token));
		ComputeQuota quota = plugin.getUserQuota(token);
	}
	
}
