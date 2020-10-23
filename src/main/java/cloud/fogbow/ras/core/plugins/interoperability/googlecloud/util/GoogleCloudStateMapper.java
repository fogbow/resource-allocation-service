package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util;

import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.ResourceType;
import org.apache.log4j.Logger;

public class GoogleCloudStateMapper {

    private static final Logger LOGGER = Logger.getLogger(GoogleCloudStateMapper.class);

    public static InstanceState map(ResourceType type, String state) {
        state = state.toLowerCase();

        switch (type){
            case PUBLIC_IP:
                switch (state) {
                    // TODO: Implements states
                    default:
                        break;
                }
            default:
                LOGGER.error(Messages.Log.INSTANCE_TYPE_NOT_DEFINED);
                return InstanceState.INCONSISTENT;
        }

    }
}
