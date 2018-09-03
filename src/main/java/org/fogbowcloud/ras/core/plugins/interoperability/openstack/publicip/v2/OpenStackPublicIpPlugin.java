package org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;

public class OpenStackPublicIpPlugin implements PublicIpPlugin<OpenStackV3Token> {

	// TODO add logs
	private static final Logger LOGGER = Logger.getLogger(OpenStackPublicIpPlugin.class);
	
	// FIXME Parameter duplicated. Look the OpenStackV2NetworkPlugin
	private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";
	// TODO check this name
	private static final String EXTERNAL_NETWORK_ID_KEY = "external_network_id";
	
	protected static final String SUFFIX_ENDPOINT_FLOATINGIPS = "/floatingips";	
	protected static final String NETWORK_V2_API_ENDPOINT = "/v2.0";
	protected static final String SUFFIX_ENDPOINT_PORTS = "/ports";


	private Properties properties;
	private HttpRequestClientUtil client;
	
	public OpenStackPublicIpPlugin() {
        this.properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);	
        
        // TODO check if is necessary check the parameters
        
		initClient();
	}
	
	@Override
	public boolean associate(String computeInstanceId, OpenStackV3Token openStackV3Token) throws Exception {
		String endpoint = getCreateFloatingIpEndpoint() + NETWORK_V2_API_ENDPOINT + SUFFIX_ENDPOINT_FLOATINGIPS;
		
		// TODO get port id
		
		String floatingNetworkId = getExternalNetworkId();
		String projectId = openStackV3Token.getProjectId();
		
		CreateFloatingIpRequest createBody = new CreateFloatingIpRequest.Builder()
				.floatingNetworkId(floatingNetworkId)
				.projectId(projectId)
				.portId("")
				.build();
		
		String body = createBody.toJson();
		this.client.doPostRequest(endpoint, openStackV3Token, body);
		
		return true;
	}
	
	protected String getNetworkPortIp(String computeInstanceId, OpenStackV3Token openStackV3Token) throws Exception {
		
		String neutroUrlPrefix = getNetworkPortEndpoint();
		GetNetworkPortsResquest getNetworkPortsResquest = new GetNetworkPortsResquest.Builder()
				.url(neutroUrlPrefix)
				.deviceId(computeInstanceId)
				.build();
		
		String endpoint = getNetworkPortsResquest.getUrl();
		String responseDoGetRequest = this.client.doGetRequest(endpoint, openStackV3Token);
		
		// 
		
		return null;
	}
	
	protected String getFloatingIpId(String networkPortId, OpenStackV3Token openStackV3Token) {
		return null;
	}
	
	@Override
	public boolean disassociate(String computeInstanceId, OpenStackV3Token openStackV3Token) throws Exception {
		// TODO get port
		
		// TODO get floating ip ID
		
		// TODO delete floating ip
		
		return false;
	}		
	
	private String getExternalNetworkId() {
		return this.properties.getProperty(EXTERNAL_NETWORK_ID_KEY);
	}
	
    private String getNetworkPortEndpoint() {
        return this.properties.getProperty(NETWORK_NEUTRONV2_URL_KEY) + NETWORK_V2_API_ENDPOINT + SUFFIX_ENDPOINT_PORTS;
    }
	
    private String getCreateFloatingIpEndpoint() {
        return this.properties.getProperty(NETWORK_NEUTRONV2_URL_KEY) + NETWORK_V2_API_ENDPOINT + SUFFIX_ENDPOINT_FLOATINGIPS;
    }
    
    private void initClient() {
        HttpRequestUtil.init();
        this.client = new HttpRequestClientUtil();
    }
    
//	String deviceId = "a264d57b-aa53-4f08-b270-e5bf4e707384";
	
}
