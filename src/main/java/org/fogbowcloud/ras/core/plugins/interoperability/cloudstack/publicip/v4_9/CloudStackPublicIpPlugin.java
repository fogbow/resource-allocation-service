package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.instances.PublicIpInstance;
import org.fogbowcloud.ras.core.models.orders.PublicIpOrder;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackHttpToFogbowRasExceptionMapper;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.AssociateIpAddressResponse.IpAddress;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

public class CloudStackPublicIpPlugin implements PublicIpPlugin<CloudStackToken> {

	private static final Logger LOGGER = Logger.getLogger(CloudStackPublicIpPlugin.class);
	
	private static final String DEFAULT_START_PORT = "22";
	private static final String DEFAULT_END_PORT = "60000";
	private static final String DEFAULT_PROTOCOL = "TCP";
	
	private HttpRequestClientUtil client;
	
	public CloudStackPublicIpPlugin() {
		this.client = new HttpRequestClientUtil();
	}
	
	@Override
	public String requestInstance(PublicIpOrder publicIpOrder, CloudStackToken cloudStackToken) throws FogbowRasException, UnexpectedException {
		LOGGER.info("");
		
		String computeInstanceId = publicIpOrder.getComputeInstanceId();
		
		// TODO use getInstance of the CloudStackComputePlugin
		String networkId = "";
		
		// asynchronous operation
		String ipAdressId = associateIpAdress(networkId, cloudStackToken);
		
		// TODO wait asynchronous operation and check the success
		checkOperation();
		
		try {
			enableStaticNat(computeInstanceId, ipAdressId, cloudStackToken);			
		} catch (Exception e) {
			deleteInstance(computeInstanceId, cloudStackToken);
		}
		
		try {
			// asynchronous operation
			createFirewallRule(ipAdressId, cloudStackToken);
		} catch (Exception e) {
			deleteInstance(computeInstanceId, cloudStackToken);
		}
		
		// TODO wait asynchronous operation and check the success
		checkOperation();
		
		return ipAdressId;
	}

	@Override
	public void deleteInstance(String ipAddressId, CloudStackToken cloudStackToken) throws FogbowRasException, UnexpectedException {
		LOGGER.info("");
		
		DisassocioateIpAddressRequest disassocioateIpAddressRequest = new DisassocioateIpAddressRequest
				.Builder()
				.id(ipAddressId)
				.build();
		
		CloudStackUrlUtil.sign(disassocioateIpAddressRequest.getUriBuilder(), cloudStackToken.getTokenValue());
		
        try {
            this.client.doGetRequest(disassocioateIpAddressRequest.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }     
        
		// TODO wait asynchronous operation and check the success
		checkOperation();
	}
	
	@Override
	public PublicIpInstance getInstance(String publicIpOrderId, CloudStackToken token)
			throws FogbowRasException, UnexpectedException {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected String associateIpAdress(String networkId, CloudStackToken cloudStackToken) throws FogbowRasException {
		AssociateIpAddressRequest associateIpAddressRequest = new AssociateIpAddressRequest.Builder()
				.networkId(networkId)
				.build();
		
        CloudStackUrlUtil.sign(associateIpAddressRequest.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        try {
            jsonResponse = this.client.doGetRequest(associateIpAddressRequest.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }        
		
        AssociateIpAddressResponse associateIpAddressResponse = AssociateIpAddressResponse.fromJson(jsonResponse);
        
		IpAddress ipAddress = associateIpAddressResponse.getIpAddress();
		return ipAddress.getId();
	}
	
	protected void enableStaticNat(String computeInstanceId, String ipAdressId, CloudStackToken cloudStackToken) 
			throws FogbowRasException {
		
		EnableStaticNatRequest enableStaticNatRequest = new EnableStaticNatRequest.Builder()
				.ipAddressId(ipAdressId)
				.virtualMachineId(computeInstanceId)
				.build();
		
        CloudStackUrlUtil.sign(enableStaticNatRequest.getUriBuilder(), cloudStackToken.getTokenValue());

        try {
            this.client.doGetRequest(enableStaticNatRequest.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }        
	}
	
	protected void createFirewallRule(String ipAdressId, CloudStackToken cloudStackToken) throws FogbowRasException {
		CreateFirewallRequest createFirewallRequest = new CreateFirewallRequest.Builder()
				.protocol(DEFAULT_PROTOCOL)
				.startPort(DEFAULT_START_PORT)
				.endPort(DEFAULT_END_PORT)
				.ipAddress(ipAdressId)
				.build();
		
		CloudStackUrlUtil.sign(createFirewallRequest.getUriBuilder(), cloudStackToken.getTokenValue());
		
        try {
            this.client.doGetRequest(createFirewallRequest.getUriBuilder().toString(), cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }     
	}	
	
	// TODO
	private void checkOperation() {}

}
