package cloud.fogbow.ras.core.plugins.interoperability.aws.genericrequest.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.ras.core.plugins.interoperability.GenericRequestPlugin;

public class AwsV2GenericRequestPlugin implements GenericRequestPlugin<AwsV2User> {

    @Override
    public FogbowGenericResponse redirectGenericRequest(String genericRequest, AwsV2User cloudUser) throws FogbowException {
    	throw new UnsupportedOperationException("This feature has not been implemented for aws cloud, yet.");
    }
}
