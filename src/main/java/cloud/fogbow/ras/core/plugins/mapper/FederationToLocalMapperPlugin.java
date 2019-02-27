package cloud.fogbow.ras.core.plugins.mapper;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;

public interface FederationToLocalMapperPlugin<T extends CloudToken> {
    public T map(FederationUser federationUser) throws FogbowException;
}

