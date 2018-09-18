package org.fogbowcloud.ras.core.plugins.aaa.authorization;

import org.fogbowcloud.ras.core.PluginFactory;
import org.fogbowcloud.ras.core.constants.Messages;
import org.fogbowcloud.ras.core.constants.Operation;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ComposedAuthorizationPlugin implements AuthorizationPlugin<FederationUserToken> {
    List<AuthorizationPlugin> authorizationPlugins;

    public ComposedAuthorizationPlugin(String confPath) {
        List<String> pluginNames = getPluginNames(confPath);
        this.authorizationPlugins = getPlugins(pluginNames);
    }

    @Override
    public boolean isAuthorized(FederationUserToken federationUserToken, Operation operation, ResourceType type) {

        for (AuthorizationPlugin plugin : this.authorizationPlugins) {
            if (!plugin.isAuthorized(federationUserToken, operation, type)) {
                return false;
            }
        }
        return true;
    }

    private List<String> getPluginNames(String confPath) {
        ArrayList<String> authorizationPluginNames = new ArrayList<>();

        File file = new File(confPath);
        Scanner input = null;
        try {
            input = new Scanner(file);
        } catch (FileNotFoundException e) {
            throw new FatalErrorException(
                    String.format(Messages.Fatal.UNABLE_TO_READ_COMPOSED_AUTHORIZATION_PLUGIN_CONF_FILE, confPath));
        }

        while (input.hasNextLine()) {
            String nextLine = input.nextLine().trim();
            if (!nextLine.isEmpty()) {
                authorizationPluginNames.add(nextLine);
            }
        }

        return authorizationPluginNames;
    }

    private List<AuthorizationPlugin> getPlugins(List<String> pluginNames) {
        PluginFactory pluginFactory = new PluginFactory();
        ArrayList<AuthorizationPlugin> authorizationPlugins = new ArrayList<>();
        for (int i = 0; i < pluginNames.size(); i++) {
            authorizationPlugins.add(i, (AuthorizationPlugin) pluginFactory.createPluginInstance(pluginNames.get(i)));
        }
        return authorizationPlugins;
    }
}
