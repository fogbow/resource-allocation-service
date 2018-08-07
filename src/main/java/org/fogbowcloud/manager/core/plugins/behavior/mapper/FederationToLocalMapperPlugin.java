package org.fogbowcloud.manager.core.plugins.behavior.mapper;

import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.tokens.FederationUserAttributes;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;

public interface FederationToLocalMapperPlugin {

	LocalUserAttributes map(FederationUserAttributes user) throws UnexpectedException, FogbowManagerException;

}
