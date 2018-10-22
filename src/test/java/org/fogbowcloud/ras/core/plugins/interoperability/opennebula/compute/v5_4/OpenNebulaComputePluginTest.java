package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.Properties;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.junit.Before;
import org.junit.Test;

public class OpenNebulaComputePluginTest {

	private OpenNebulaClientFactory factory;
	private OpenNebulaComputePlugin plugin;
	private Properties properties;
	
	@Before
	public void setUp() {
		this.properties = PropertiesUtil
				.readProperties(HomeDir.getPath() + DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME);

		this.factory = new OpenNebulaClientFactory();
		this.plugin = new OpenNebulaComputePlugin();
	}
	
	@Test
	public void test() {
		
	}
}
