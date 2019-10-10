package cloud.fogbow.ras.core.plugins.interoperability.openstack;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.constants.Messages;
import org.apache.log4j.Logger;

public class OpenStackCloudUtils {

    private static final Logger LOGGER = Logger.getLogger(OpenStackCloudUtils.class);
    
    public static final String ACTION = "action";
    public static final String COMPUTE_NOVA_V2_URL_KEY = "openstack_nova_v2_url";
    public static final String COMPUTE_V2_API_ENDPOINT = "/v2/";
    public static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
    public static final String ENDPOINT_SEPARATOR = "/";
    public static final String EXTERNAL_NETWORK_ID_KEY = "external_gateway_info";
    public static final String INGRESS_DIRECTION = "ingress";
    public static final String NETWORK_NEUTRON_V2_URL_KEY = "openstack_neutron_v2_url";
    public static final String NETWORK_PORTS_RESOURCE = "Network Ports";
    public static final String NETWORK_V2_API_ENDPOINT = "/v2.0";
    public static final String SECURITY_GROUP_RESOURCE = "Security Group";
    public static final String SECURITY_GROUP_RULES = "/security-group-rules";
    public static final String SECURITY_GROUPS = "/security-groups";
    public static final String SERVERS = "/servers";

    public static String getProjectIdFrom(OpenStackV3User cloudUser) throws InvalidParameterException {
        String projectId = cloudUser.getProjectId();
        if (projectId == null) {
            LOGGER.error(Messages.Error.UNSPECIFIED_PROJECT_ID);
            throw new InvalidParameterException(Messages.Exception.NO_PROJECT_ID);
        }
        return projectId;
    }
}
