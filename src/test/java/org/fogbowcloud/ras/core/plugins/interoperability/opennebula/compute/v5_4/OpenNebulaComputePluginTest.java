package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.compute.v5_4;

import java.util.Properties;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.junit.Before;
import org.junit.Test;
import org.opennebula.client.ClientConfigurationException;

public class OpenNebulaComputePluginTest {

	private static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
	
	private OpenNebulaClientFactory factory;
	private OpenNebulaComputePlugin plugin;
	private Properties properties;
	private String networkId;
	
	@Before
	public void setUp() {
		this.properties = PropertiesUtil
				.readProperties(HomeDir.getPath() + DefaultConfigurationConstants.OPENNEBULA_CONF_FILE_NAME);

		this.factory = new OpenNebulaClientFactory();
		this.plugin = new OpenNebulaComputePlugin();
		this.networkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
	}
	
	@Test
	public void test() throws UnexpectedException, ClientConfigurationException {

		//		Mockito.doNothing().when(this.factory.createClient(Mockito.anyString()));

		CreateComputeRequest request = new CreateComputeRequest.Builder()
				.contextEncoding("base64")
				.contextUserdata("")
				.contextNetwork("YES")
				.cpu("1")
				.graphicsListen("0.0.0.0")
				.graphicsType("vnc")
				.imageId("2")
				.volumeSize("4")
				.volumeType("fs")
				.memory("2048")
				.networkId(this.networkId)
				.build();

		String template = request.getVirtualMachine().generateTemplate();
		
		System.out.println(template);
		
		//		Mockito.verify(this.factory, Mockito.times(1)).createClient(anyString());
	}
}
