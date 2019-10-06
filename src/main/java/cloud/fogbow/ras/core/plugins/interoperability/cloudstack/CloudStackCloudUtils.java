package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;

public class CloudStackCloudUtils {
    private static final Logger LOGGER = Logger.getLogger(CloudStackUrlUtil.class);

    // TODO(chico) - Refactor the CloudstackComputePlugin, because there are duplicate constants
    public static final String CLOUDSTACK_URL_CONFIG = "cloudstack_api_url";
    public static final String NETWORK_OFFERING_ID_CONFIG = "network_offering_id";
    public static final String ZONE_ID_CONFIG = "zone_id";

    /**
     * Request HTTP operations to Cloudstack and treat a possible FogbowException when
     * It is thrown by the cloudStackHttpClient.
     * @throws HttpResponseException
     **/
    // TODO(chico) - It must be used in the ComputePlugin too
    @NotNull
    public static String doGet(@NotNull CloudStackHttpClient cloudStackHttpClient,
                               String url,
                               @NotNull CloudStackUser cloudStackUser) throws HttpResponseException {

        try {
            LOGGER.debug(String.format(
                    "The user %s is requesting to Cloudstack: %s", cloudStackUser.getId(), url));
            return cloudStackHttpClient.doGetRequest(url, cloudStackUser);
        } catch (FogbowException e) {
            throw new HttpResponseException(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}
