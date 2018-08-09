package org.fogbowcloud.manager.core.plugins.behavior.authentication;

import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.UnauthenticatedUserException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;

public interface AuthenticationPlugin {

    /**
     * Based on an access id recreates a Token containing the corresponding access id plus the user
     * name and some arbitrary information regarding the tokens.
     *
     * @return a FederationUserToken for the corresponding federationTokenValue. A federationUser is
     * composed of a unique id (String) and a Map<String, String> of attributes.
     * Any implementation of this plugin MUST include at least the following attribute:
     * "user-name", which is the name of the user (ex. used in the Dashboard after authentication).
     * @throws UnauthenticatedUserException
     */
    public FederationUserToken getFederationUser(String federationTokenValue) throws UnauthenticatedUserException,
            InvalidParameterException;

    /**
     * Verifies if the federationTokenValue is valid against the identity service.
     *
     * @param federationTokenValue
     * @return a boolean stating whether the tokens value is valid or not.
     */
    public boolean isAuthentic(String federationTokenValue);

}
