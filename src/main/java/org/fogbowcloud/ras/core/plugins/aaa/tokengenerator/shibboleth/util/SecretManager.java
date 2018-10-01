package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth.ShibbolethTokenGenerator;

public class SecretManager {
	
	protected static final int MAXIMUM_MAP_SIZE = 100;
	private Map<String, Long> secrets;
	
	public SecretManager() {
		this.secrets = new HashMap<String, Long>();
	}
	
	public synchronized boolean verify(String secret) {
		cleanSecrets();
		
		boolean alreadyExists = this.secrets.containsKey(secret);
		if (alreadyExists) {
			return false;
		}
		
		Long validity = getNow() + ShibbolethTokenGenerator.EXPIRATION_INTERVAL;
		this.secrets.put(secret, validity);
		return true;
	}

	protected long getNow() {
		return System.currentTimeMillis();
	}
	
	// check when exceed the map size
	protected void cleanSecrets() {
		if (this.secrets.size() < MAXIMUM_MAP_SIZE) {
			return;
		}
		
		for (String key : this.secrets.keySet()) {
			Date secretValidity = new Date(this.secrets.get(key));
			Date now = new Date(getNow());
			
			boolean invalidValidity = secretValidity.before(now);
			if (invalidValidity) {
				this.secrets.remove(key);
			}
		}
	}
	
	protected void setSecrets(Map<String, Long> secrets) {
		this.secrets = secrets;
	}
	
	protected Map<String, Long> getSecrets() {
		return secrets;
	}
	
}
