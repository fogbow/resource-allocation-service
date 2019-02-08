package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.RSAUtil;
import cloud.fogbow.common.util.connectivity.HttpRequestClientUtil;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import org.apache.http.client.HttpResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPublicKey;

public class PublicKeysHolder {
    private HttpRequestClientUtil client;
    private RSAPublicKey asPublicKey;

    private static PublicKeysHolder instance;

    private PublicKeysHolder() {
        String timeoutStr = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.HTTP_REQUEST_TIMEOUT_KEY,
                ConfigurationPropertyDefaults.HTTP_REQUEST_TIMEOUT);
        this.client = new HttpRequestClientUtil(new Integer(timeoutStr));
        this.asPublicKey = null;
    }

    public static synchronized PublicKeysHolder getInstance() {
        if (instance == null) {
            instance = new PublicKeysHolder();
        }
        return instance;
    }

    public RSAPublicKey getAsPublicKey() throws UnavailableProviderException, UnexpectedException, ConfigurationErrorException {
        if (this.asPublicKey == null) {
            String asAddress = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_URL_KEY);
            String asPort = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.AS_PORT_KEY);
            this.asPublicKey = getPublicKey(asAddress, asPort, cloud.fogbow.as.api.http.PublicKey.PUBLIC_KEY_ENDPOINT);
        }
        return this.asPublicKey;
    }

    private RSAPublicKey getPublicKey(String serviceAddress, String servicePort, String suffix)
            throws UnavailableProviderException, UnexpectedException, ConfigurationErrorException {
        RSAPublicKey publicKey = null;

        URI uri = null;
        try {
            uri = new URI(serviceAddress);
        } catch (URISyntaxException e) {
            throw new ConfigurationErrorException(String.format(Messages.Exception.INVALID_URL, serviceAddress));
        }
        uri = UriComponentsBuilder.fromUri(uri).port(servicePort).path(suffix).build(true).toUri();

        String endpoint = uri.toString();
        String responseStr = null;
        try {
            responseStr = this.client.doGetRequest(endpoint, new CloudToken("", "", ""));
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
