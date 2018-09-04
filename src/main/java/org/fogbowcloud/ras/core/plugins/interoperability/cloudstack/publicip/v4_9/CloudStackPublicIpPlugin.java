package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9;

import java.io.File;
import java.util.Properties;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.PublicIpPlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;

public class CloudStackPublicIpPlugin implements PublicIpPlugin<CloudStackToken> {

	private Properties properties;
	
	public CloudStackPublicIpPlugin() {
        String cloudStackConfFilePath = HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;
        
		this.properties = PropertiesUtil.readProperties(cloudStackConfFilePath);        
	}
	
	@Override
	public String allocatePublicIp(String computeInstanceId, CloudStackToken localUserAttributes) throws Exception {
		
		String networkId = "";
		
		String associateIpAdressId = associateIpAdress(networkId);
		
		try {
			enableStaticNat(computeInstanceId, associateIpAdressId);			
		} catch (Exception e) {
			releasePublicIp(computeInstanceId, localUserAttributes);
		}
		
		try {
			createFirewallRule(associateIpAdressId);
		} catch (Exception e) {
			releasePublicIp(computeInstanceId, localUserAttributes);
		}
		
		return null;
	}
	
	@Override
	public void releasePublicIp(String computeInstanceId, CloudStackToken localUserAttributes) throws Exception {
		
	}
	
	protected String associateIpAdress(String networkId) {
		return null;
	}
	
	protected void enableStaticNat(String computeInstanceId, String associateIpAdressId) {
		
	}
	
	protected void createFirewallRule(String associateIpAdressId) {
		
	}	

}
