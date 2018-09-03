package org.fogbowcloud.ras.core.plugins.interoperability;

import org.fogbowcloud.ras.core.models.tokens.Token;

public interface PublicIpPlugin<T extends Token> {

	public boolean associate(String computeInstanceId, T Token) throws Exception;
	
	public boolean disassociate(String computeInstanceId, T Token) throws Exception;
	
}
