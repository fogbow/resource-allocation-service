package cloud.fogbow.ras.core.plugins.interoperability.aws.genericrequest.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.AwsV2User;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.ras.core.plugins.interoperability.GenericRequestPlugin;

public class AwsV2GenericRequestPlugin implements GenericRequestPlugin<AwsV2User> {

    public AwsV2GenericRequestPlugin() {

    }
    public FogbowGenericResponse redirectGenericRequest(String genericRequest, AwsV2User cloudUser) throws FogbowException {
        return null;
    }
}
