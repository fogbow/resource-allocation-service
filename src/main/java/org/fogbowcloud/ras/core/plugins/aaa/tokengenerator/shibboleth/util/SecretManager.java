package org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.shibboleth.util;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.generic.GenericSignatureAuthenticationHolder;

public class SecretManager {
	
	private static final Logger LOGGER = Logger.getLogger(SecretManager.class);
	
	protected static final int MAXIMUM_MAP_SIZE = 100;
	private Map<String, Long> secrets;
	private Date rasStartingTime;
	
	public SecretManager() {
		this.rasStartingTime = new Date(getNow());
		
		this.secrets = new HashMap<String, Long>();
	}
	
	public synchronized boolean verify(String secret) {
		cleanSecrets();
		
		if (!isValidSecret(secret)) {
			return false;
		}
		
		Long validity = getNow() + GenericSignatureAuthenticationHolder.EXPIRATION_INTERVAL;
		this.secrets.put(secret, validity);
		return true;
	}

	protected long getNow() {
		return System.currentTimeMillis();
	}
	
	protected boolean isValidSecret(String secret) {
		boolean alreadyExists = this.secrets.containsKey(secret);
		if (alreadyExists) {
			return false;
		}
		 
		try {
			Date secretCreationTime = new Date(Long.parseLong(secret));
			if (secretCreationTime.before(this.rasStartingTime)) {
				return false;
			}		
		} catch (NumberFormatException e) {
			LOGGER.warn("Secret format is invalid.", e);
			return false;
		}
		
		return true;
	}
	
	// check when exceed the map size
	protected synchronized void cleanSecrets() {
		if (this.secrets.size() < MAXIMUM_MAP_SIZE) {
			return;
		}
		
		Set<String> keySet = new HashSet<String>(this.secrets.keySet());
		for (String key : keySet) {
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
