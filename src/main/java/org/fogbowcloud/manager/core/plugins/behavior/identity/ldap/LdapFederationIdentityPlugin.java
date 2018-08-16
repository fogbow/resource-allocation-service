package org.fogbowcloud.manager.core.plugins.behavior.identity.ldap;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.models.tokens.LdapToken;
import org.fogbowcloud.manager.core.models.tokens.generators.ldap.LdapTokenGenerator;
import org.fogbowcloud.manager.core.plugins.behavior.identity.FederationIdentityPlugin;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.exceptions.*;

public class LdapFederationIdentityPlugin implements FederationIdentityPlugin<LdapToken> {
    private static final Logger LOGGER = Logger.getLogger(LdapFederationIdentityPlugin.class);

    public LdapFederationIdentityPlugin() throws FatalErrorException {
    }

    @Override
    public LdapToken createToken(String federationTokenValue) throws InvalidParameterException {

        String split[] = federationTokenValue.split(LdapTokenGenerator.TOKEN_VALUE_SEPARATOR);
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