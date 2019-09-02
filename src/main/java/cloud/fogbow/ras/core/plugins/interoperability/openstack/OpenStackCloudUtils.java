package cloud.fogbow.ras.core.plugins.interoperability.openstack;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenStackCloudUtils {

    private static final Logger LOGGER = Logger.getLogger(OpenStackCloudUtils.class);

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
            JSONObject response = new JSONObject(json);
            JSONArray securityGroupJSONArray = response.getJSONArray(OpenStackConstants.Network.SECURITY_GROUPS_KEY_JSON);
            JSONObject securityGroup = securityGroupJSONArray.optJSONObject(0);
            securityGroupId = securityGroup.getString(OpenStackConstants.Network.ID_KEY_JSON);
        } catch (JSONException e) {
            String message = String.format(Messages.Error.UNABLE_TO_RETRIEVE_NETWORK_ID, json);
            LOGGER.error(message, e);
            throw new UnexpectedException(message, e);
        }
        return securityGroupId;
    }

    public static String getSGNameForPrivateNetwork(String networkId) {
        return SystemConstants.PN_SECURITY_GROUP_PREFIX + networkId;
    }

}
