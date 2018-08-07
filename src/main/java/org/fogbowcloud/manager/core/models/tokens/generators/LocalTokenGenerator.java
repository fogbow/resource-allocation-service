package org.fogbowcloud.manager.core.models.tokens.generators;

import java.util.Map;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;

/**
 *
 */
public interface LocalTokenGenerator<T extends LocalUserAttributes> {

    /**
     * Creates a tokens based on the user's credentials.
     *
     * @param userCredentials
     * @return a LocalUserAttributes with an access ID provided by the identity service.
     * @throws FogbowManagerException
     */
    public T createToken(Map<String, String> userCredentials) throws FogbowManagerException, UnexpectedException;

}
