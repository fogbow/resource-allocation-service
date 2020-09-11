package cloud.fogbow.ras.core.plugins.interoperability.openstack.util;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import org.apache.log4j.Logger;

public class OpenStackPluginUtils {

    private static final Logger LOGGER = Logger.getLogger(OpenStackPluginUtils.class);

    public static final String COMPUTE_NOVA_URL_KEY = "openstack_nova_url";
    public static final String NETWORK_NEUTRON_URL_KEY = "openstack_neutron_url";
    public static final String VOLUME_NOVA_URL_KEY = "openstack_cinder_url";
    public static final String VOLUME_CINDER_URL_KEY = "openstack_cinder_url";
    public static final String IMAGE_GLANCE_URL_KEY = "openstack_glance_url";
    public static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";
    public static final String EXTERNAL_NETWORK_ID_KEY = "external_gateway_info";

    public static String getProjectIdFrom(OpenStackV3User cloudUser) throws InvalidParameterException {
        String projectId = cloudUser.getProjectId();
        if (projectId == null) {
            LOGGER.error(Messages.Log.UNSPECIFIED_PROJECT_ID);
            throw new InvalidParameterException(Messages.Exception.NO_PROJECT_ID);
        }
        return projectId;
    }

    public static String getNetworkSecurityGroupName(String networkId) {
        return SystemConstants.PN_SECURITY_GROUP_PREFIX + networkId;
    }

}
