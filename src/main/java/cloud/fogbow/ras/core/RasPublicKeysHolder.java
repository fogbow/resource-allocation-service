package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.PublicKeysHolder;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;

import java.security.interfaces.RSAPublicKey;

public class RasPublicKeysHolder {
    private RSAPublicKey asPublicKey;
    private RSAPublicKey msPublicKey;
    
    private static RasPublicKeysHolder instance;

    private RasPublicKeysHolder() {
    }

    public static synchronized RasPublicKeysHolder getInstance() {
        if (instance == null) {
            instance = new RasPublicKeysHolder();
        }
        return instance;
    }

    public static void reset() {
        instance = null;
    }
    
    public RSAPublicKey getAsPublicKey() throws FogbowException {
        if (this.asPublicKey == null) {
            String asAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_URL_KEY);
            String asPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_PORT_KEY);
            this.asPublicKey = PublicKeysHolder.getPublicKey(asAddress, asPort, cloud.fogbow.as.api.http.request.PublicKey.PUBLIC_KEY_ENDPOINT);
        }
        return this.asPublicKey;
    }

    public RSAPublicKey getMsPublicKey() throws FogbowException {
        if (this.msPublicKey == null) {
            String msAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MS_URL_KEY);
            String msPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.MS_PORT_KEY);
            this.asPublicKey = PublicKeysHolder.getPublicKey(msAddress, msPort, cloud.fogbow.ms.api.http.request.PublicKey.PUBLIC_KEY_ENDPOINT);
        }
        return this.asPublicKey;
    }
}
