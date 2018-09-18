package org.fogbowcloud.ras.core.plugins.aaa.authorization;

import org.fogbowcloud.ras.core.constants.Operation;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.fogbowcloud.ras.core.plugins.aaa.authorization.AuthorizationPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class DistributedAuthorizationPluginClient implements AuthorizationPlugin<FederationUserToken> {
    public static final String AUTH_ENDPOINT = "/auth";

    public String serverUrl;

    public DistributedAuthorizationPluginClient() {
    }

    @Override
    public boolean isAuthorized(FederationUserToken federationUserToken, Operation operation, ResourceType type) {
        String endpoint = this.serverUrl + AUTH_ENDPOINT + "/" + federationUserToken.getTokenProvider() + "/" +
                federationUserToken.getUserId() + "/" + type + "/" + operation;
        StringBuffer content = null;

        try {
            URL url = new URL(endpoint);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            con.disconnect();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Boolean.valueOf(content.toString());
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
}
