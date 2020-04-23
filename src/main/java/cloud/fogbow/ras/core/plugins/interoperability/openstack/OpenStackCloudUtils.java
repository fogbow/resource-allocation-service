package cloud.fogbow.ras.core.plugins.interoperability.openstack;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.GetSecurityGroupsResponse;
import org.apache.log4j.Logger;
import org.json.JSONException;

public class OpenStackCloudUtils {

    private static final Logger LOGGER = Logger.getLogger(OpenStackCloudUtils.class);

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
            LOGGER.error(Messages.Error.UNSPECIFIED_PROJECT_ID);
            throw new InvalidParameterException(Messages.Exception.NO_PROJECT_ID);
        }
        return projectId;
    }

    public static String getSecurityGroupIdFromGetResponse(String json) throws UnexpectedException {
        String securityGroupId = null;
        try {
            GetSecurityGroupsResponse.SecurityGroup securityGroup = GetSecurityGroupsResponse.fromJson(json).getSecurityGroups().iterator().next();
            securityGroupId = securityGroup.getId();
        } catch (JSONException e) {
            String message = String.format(Messages.Error.UNABLE_TO_RETRIEVE_NETWORK_ID, json);
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
        return securityGroupId;
    }

    public static String getNetworkSecurityGroupName(String networkId) {
        return SystemConstants.PN_SECURITY_GROUP_PREFIX + networkId;
    }

}
