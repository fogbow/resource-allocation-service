package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.util.RSAUtil;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.core.constants.ConfigurationConstants;
import cloud.fogbow.ras.core.constants.Messages;
import org.apache.http.client.HttpResponseException;

import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;

public class PublicKeysHolder {
    private HttpRequestClientUtil client;
    private RSAPublicKey asPublicKey;

    private static PublicKeysHolder instance;

    private PublicKeysHolder() {
        String timeoutStr = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.HTTP_REQUEST_TIMEOUT);
        this.client = new HttpRequestClientUtil(new Integer(timeoutStr));
        this.asPublicKey = null;
    }

    public static synchronized PublicKeysHolder getInstance() {
        if (instance == null) {
            instance = new PublicKeysHolder();
        }
        return instance;
    }

    public RSAPublicKey getAsPublicKey() throws UnavailableProviderException, UnexpectedException {
        if (this.asPublicKey == null) {
            String asAddress = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.AS_URL);
            String asPort = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.AS_PORT);
            this.asPublicKey = getPublicKey(asAddress, asPort, cloud.fogbow.as.api.http.PublicKey.PUBLIC_KEY_ENDPOINT);
        }
        return this.asPublicKey;
    }

    private RSAPublicKey getPublicKey(String serviceAddress, String servicePort, String suffix)
            throws UnavailableProviderException, UnexpectedException {
        RSAPublicKey publicKey = null;
        String endpoint = serviceAddress + ":" + servicePort + "/" + suffix;
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, null);
        } catch (HttpResponseException e) {
            throw new UnavailableProviderException(e.getMessage(), e);
        }
        try {
            publicKey = RSAUtil.getPublicKeyFromString(responseStr);
        } catch (GeneralSecurityException e) {
            throw new UnexpectedException(Messages.Exception.INVALID_PUBLIC_KEY);
        }
        return publicKey;
    }
}
