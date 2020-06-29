package cloud.fogbow.ras.core.plugins.interoperability.openstack;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.serializables.v2.GetSecurityGroupsResponse;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.util.serializables.v2.GetSecurityGroupsResponse.SecurityGroup;
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
            LOGGER.error(Messages.Log.UNSPECIFIED_PROJECT_ID);
            throw new InvalidParameterException(Messages.Exception.NO_PROJECT_ID);
        }
        return projectId;
    }

    public static String getSecurityGroupIdFromGetResponse(String json) throws InternalServerErrorException {
        String securityGroupId = null;
        try {
            SecurityGroup securityGroup = GetSecurityGroupsResponse.fromJson(json).getSecurityGroups().iterator().next();
            securityGroupId = securityGroup.getId();
        } catch (JSONException e) {
            LOGGER.error(String.format(Messages.Log.UNABLE_TO_RETRIEVE_NETWORK_ID_S, json), e);
            throw new InternalServerErrorException(String.format(Messages.Exception.UNABLE_TO_RETRIEVE_NETWORK_ID_S, json));
        }
        return securityGroupId;
    }

    public static String getNetworkSecurityGroupName(String networkId) {
        return SystemConstants.PN_SECURITY_GROUP_PREFIX + networkId;
    }

}
