package org.fogbowcloud.ras.core.plugins.aaa.identity.ldap;

import org.apache.log4j.Logger;
import org.fogbowcloud.ras.core.exceptions.FatalErrorException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.models.tokens.LdapToken;
import org.fogbowcloud.ras.core.plugins.aaa.identity.FederationIdentityPlugin;
import org.fogbowcloud.ras.core.plugins.aaa.tokengenerator.ldap.LdapTokenGeneratorPlugin;

public class LdapFederationIdentityPlugin implements FederationIdentityPlugin<LdapToken> {
    private static final Logger LOGGER = Logger.getLogger(LdapFederationIdentityPlugin.class);

    public LdapFederationIdentityPlugin() throws FatalErrorException {
    }

    @Override
    public LdapToken createToken(String federationTokenValue) throws InvalidParameterException {

        String split[] = federationTokenValue.split(LdapTokenGeneratorPlugin.TOKEN_VALUE_SEPARATOR);
        if (split == null || split.length < 5) {
            LOGGER.error("Invalid token value: " + federationTokenValue);
            throw new InvalidParameterException();
        }

        String tokenProvider = split[0];
        String uuid = split[1];
        String name = split[2];
        String expirationDate = split[3];
        LdapToken ldapToken = new LdapToken(tokenProvider, federationTokenValue, uuid, name, expirationDate);
        return ldapToken;
    }
}