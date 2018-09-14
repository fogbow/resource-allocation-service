package org.fogbowcloud.ras.core.plugins.aaa.authentication.openstack;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.authentication.AuthenticationPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.openstack.v3.OpenStackTokenGeneratorPlugin;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

import java.util.Properties;

public class OpenStackAuthenticationPlugin implements AuthenticationPlugin {
    private static final Logger LOGGER = Logger.getLogger(OpenStackTokenGeneratorPlugin.class);

    private String v3TokensEndpoint;
    private HttpRequestClientUtil client;
    private String localProviderId;

    public OpenStackAuthenticationPlugin() {
        Properties properties = PropertiesUtil.readProperties(HomeDir.getPath() +
                DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);

        String identityUrl = properties.getProperty(OpenStackTokenGeneratorPlugin.OPENSTACK_KEYSTONE_V3_URL);
        if (isUrlValid(identityUrl)) {
            this.v3TokensEndpoint = identityUrl + OpenStackTokenGeneratorPlugin.V3_TOKENS_ENDPOINT_PATH;
        }
        this.client = new HttpRequestClientUtil();
        this.localProviderId = PropertiesHolder.getInstance().getProperty(ConfigurationConstants.LOCAL_MEMBER_ID);
    }

    @Override
    public boolean isAuthentic(FederationUserToken federationToken) throws UnavailableProviderException {
        if (federationToken.getTokenProvider().equals(this.localProviderId)) {
            try {
                this.client.doGetRequest(this.v3TokensEndpoint, federationToken);
                return true;
            } catch (HttpResponseException e) {
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean isUrlValid(String url) throws FatalErrorException {
        if (url == null || url.trim().isEmpty()) {
            throw new FatalErrorException(String.format(Messages.Fatal.INVALID_KEYSTONE_URL,
                    OpenStackTokenGeneratorPlugin.OPENSTACK_KEYSTONE_V3_URL));
        }
        return true;
    }

    // Used in testing
    protected void setClient(HttpRequestClientUtil client) {
        this.client = client;
    }
}
