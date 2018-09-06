package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.models.tokens.Token;

public interface PublicIpPlugin<T extends Token> {

	public String allocatePublicIp(String computeInstanceId, T localUserAttributes) throws Exception;
	
	public void releasePublicIp(String computeInstanceId, T localUserAttributes) throws Exception;
	
}
