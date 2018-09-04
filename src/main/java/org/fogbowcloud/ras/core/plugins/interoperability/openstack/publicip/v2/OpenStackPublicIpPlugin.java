package org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2.CreateFloatingIpResponse.FloatingIp;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.publicip.v2.GetNetworkPortsResponse.Port;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;

public class OpenStackPublicIpPlugin implements PublicIpPlugin<OpenStackV3Token> {

	private static final int MAXIMUM_PORTS_SIZE = 1;

	private static final Logger LOGGER = Logger.getLogger(OpenStackPublicIpPlugin.class);
	
	// FIXME Parameter duplicated. Look the OpenStackV2NetworkPlugin
	private static final String NETWORK_NEUTRONV2_URL_KEY = "openstack_neutron_v2_url";
	
	private static final String EXTERNAL_NETWORK_ID_KEY = "external_network_id";
	
	protected static final String SUFFIX_ENDPOINT_FLOATINGIPS = "/floatingips";	
	protected static final String NETWORK_V2_API_ENDPOINT = "/v2.0";
	protected static final String SUFFIX_ENDPOINT_PORTS = "/ports";

	private static final String DEFAULT_NETWORK_ID_KEY = null;


	private Properties properties;
	private HttpRequestClientUtil client;
	
	public OpenStackPublicIpPlugin() {
        this.properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);	
        
        // TODO check if is necessary check the parameters
        
		initClient();
	}
	
	@Override
	public String allocatePublicIp(String computeInstanceId, OpenStackV3Token openStackV3Token)
			throws HttpResponseException, URISyntaxException, FogbowRasException, UnexpectedException {
		
        LOGGER.info("Creating floating ip in the " + computeInstanceId + " with tokens " + openStackV3Token);

		String portId = getNetworkPortIp(computeInstanceId, openStackV3Token);		
		String floatingNetworkId = getExternalNetworkId();
		String projectId = openStackV3Token.getProjectId();
		
		CreateFloatingIpRequest createBody = new CreateFloatingIpRequest.Builder()
				.floatingNetworkId(floatingNetworkId)
				.projectId(projectId)
				.portId(portId)
				.build();
		String body = createBody.toJson();
		
		String responsePostFloatingIp = null;
        try {
        	String floatingIpEndpoint = getFloatingIpEndpoint();
        	responsePostFloatingIp = this.client.doPostRequest(floatingIpEndpoint, openStackV3Token, body);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }
		CreateFloatingIpResponse createFloatingIpResponse = CreateFloatingIpResponse.fromJson(responsePostFloatingIp);
		
		FloatingIp floatingIp = createFloatingIpResponse.getFloatingIp();
		String floatingIpID = floatingIp.getId();
		return floatingIpID;
	}

	protected String getNetworkPortIp(String computeInstanceId, OpenStackV3Token openStackV3Token)
			throws URISyntaxException, HttpResponseException, FogbowRasException, UnexpectedException {
		LOGGER.debug("Searching the network port of the VM (" 
						+ computeInstanceId + ")  with tokens " + openStackV3Token);
		String defaulNetworkId = getDefaultNetworkId();
		String networkPortsEndpointBase = getNetworkPortsEndpoint();

		GetNetworkPortsResquest getNetworkPortsResquest = new GetNetworkPortsResquest.Builder()
				.url(networkPortsEndpointBase).deviceId(computeInstanceId).networkId(defaulNetworkId).build();

		String responseGetPorts = null;
        try {
        	String networkPortsEndpoint = getNetworkPortsResquest.getUrl();
        	responseGetPorts = this.client.doGetRequest(networkPortsEndpoint, openStackV3Token);
        } catch (HttpResponseException e) {
            OpenStackHttpToFogbowRasExceptionMapper.map(e);
        }

		GetNetworkPortsResponse networkPortsResponse = GetNetworkPortsResponse.fromJson(responseGetPorts);

		String networkPortId = null;
		List<Port> ports = networkPortsResponse.getPorts();
		// One is the maximum threshold of doors in the fogbow for default network
		if (ports != null && ports.size() == MAXIMUM_PORTS_SIZE) {
			networkPortId = ports.get(0).getId();
		}

		LOGGER.debug("The network port found was " + networkPortId + " of the VM (" + computeInstanceId + ")");
		return networkPortId;
	}

	@Override
	public void releasePublicIp(String floatingIpId, OpenStackV3Token openStackV3Token) throws Exception {
        LOGGER.info("Deleting instance " + floatingIpId + " with tokens " + openStackV3Token);
        
		String neutroUrlPrefix = getNetworkPortsEndpoint();
		this.client.doDeleteRequest(neutroUrlPrefix, openStackV3Token);
	}		
	
	protected String getDefaultNetworkId() {
		return this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
	}
	
	protected String getExternalNetworkId() {
		return this.properties.getProperty(EXTERNAL_NETWORK_ID_KEY);
	}
	
	protected String getNetworkPortsEndpoint() {
        return this.properties.getProperty(NETWORK_NEUTRONV2_URL_KEY) + NETWORK_V2_API_ENDPOINT + SUFFIX_ENDPOINT_PORTS;
    }
	
	protected String getFloatingIpEndpoint() {
        return this.properties.getProperty(NETWORK_NEUTRONV2_URL_KEY) + NETWORK_V2_API_ENDPOINT + SUFFIX_ENDPOINT_FLOATINGIPS;
    }
    
    private void initClient() {
        HttpRequestUtil.init();
        this.client = new HttpRequestClientUtil();
    }
    
    protected void setClient(HttpRequestClientUtil client) {
		this.client = client;
	}
    
}
