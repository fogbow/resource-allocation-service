package cloud.fogbow.ras.core.plugins.mapper.one2one;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import cloud.fogbow.ras.core.plugins.mapper.FederationToLocalMapperPlugin;
import cloud.fogbow.ras.core.plugins.mapper.all2one.OpenStackAllToOneMapper;

public class OpenStackOneToOneMapper extends GenericOneToOneFederationToLocalMapper
        implements FederationToLocalMapperPlugin {
    public OpenStackOneToOneMapper(String mapperConfFilePath) {
        super(new OpenStackAllToOneMapper(mapperConfFilePath));
    }

    @Override
    public OpenStackV3Token map(FederationUser federationUser) throws FogbowException {
        OpenStackV3Token openStackV3Token;
        CloudToken mappedToken = super.map(federationUser);
        if (federationUser.getTokenProviderId().equals(super.getMemberId())) {
            openStackV3Token = new OpenStackV3Token(federationUser);
        } else {
            openStackV3Token = (OpenStackV3Token) mappedToken;
        }
        return openStackV3Token;
    }
}
