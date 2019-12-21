package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.*;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9.GetAllDiskOfferingsResponse;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class CloudStackCloudUtils {
    private static final Logger LOGGER = Logger.getLogger(CloudStackCloudUtils.class);

    public static final String CLOUDSTACK_URL_CONFIG = "cloudstack_api_url";
    public static final String NETWORK_OFFERING_ID_CONFIG = "network_offering_id";
    public static final String ZONE_ID_CONFIG = "zone_id";

    public static final String FOGBOW_TAG_SEPARATOR = ":";
    public static final double ONE_GB_IN_BYTES = Math.pow(1024, 3);
    public static final int JOB_STATUS_COMPLETE = 1;
    public static final int JOB_STATUS_PENDING = 0;
    public static final int JOB_STATUS_FAILURE = 2;
    public static final String PENDING_STATE = "pending";
    public static final String FAILURE_STATE = "failure";

    protected static final int ONE_SECOND_IN_MILIS = 1000;
    protected static final int MAX_TRIES = 30;

    /**
     * Request HTTP operations to Cloudstack and treat a possible FogbowException when
     * It is thrown by the cloudStackHttpClient.
     * @throws HttpResponseException
     **/
    @NotNull
    public static String doRequest(@NotNull CloudStackHttpClient cloudStackHttpClient,
                                   String url,
                                   @NotNull CloudStackUser cloudStackUser) throws HttpResponseException {

        try {
            LOGGER.debug(String.format(Messages.Info.REQUESTING_TO_CLOUD, cloudStackUser.getId(), url));
            return cloudStackHttpClient.doGetRequest(url, cloudStackUser);
        } catch (FogbowException e) {
            throw new HttpResponseException(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    // TODO(chico) - Cloudstack compute must use this methods.
    @NotNull
    public static List<GetAllDiskOfferingsResponse.DiskOffering> getDisksOffering(
            @NotNull CloudStackHttpClient httpClient,
            @NotNull CloudStackUser cloudStackUser,
            String cloudStackUrl) throws FogbowException {

        GetAllDiskOfferingsRequest request = new GetAllDiskOfferingsRequest.Builder()
                .build(cloudStackUrl);

        URIBuilder uriRequest = request.getUriBuilder();
        CloudStackUrlUtil.sign(uriRequest, cloudStackUser.getToken());

        try {
            String jsonResponse = CloudStackCloudUtils.doRequest(
                    httpClient, uriRequest.toString(), cloudStackUser);
            GetAllDiskOfferingsResponse response = GetAllDiskOfferingsResponse.fromJson(jsonResponse);
            return response.getDiskOfferings();
        } catch (HttpResponseException e) {
            throw CloudStackHttpToFogbowExceptionMapper.get(e);
        }
    }

    public static String generateInstanceName() {
        String randomSuffix = UUID.randomUUID().toString();
        return SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + randomSuffix;
    }

    public static int convertToGigabyte(long sizeInBytes) {
        return (int) (sizeInBytes / CloudStackCloudUtils.ONE_GB_IN_BYTES);
    }

    /**
     * Wait and process the Cloudstack asynchronous response in its asynchronous life cycle.
     * @throws FogbowException
     */
    @NotNull
    public static String waitForResult(@NotNull CloudStackHttpClient client,
                                       String cloudStackUrl,
                                       String jobId,
                                       @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        int countTries = 0;
        CloudStackQueryAsyncJobResponse response = getAsyncJobResponse(
                client, cloudStackUrl, jobId, cloudStackUser);
        while(response.getJobStatus() == CloudStackQueryJobResult.PROCESSING) {
            if (countTries >= MAX_TRIES) {
                throw new CloudStackTimeoutException(String.format(Messages.Exception.JOB_TIMEOUT, jobId));
            }
            sleepThread();
            response = getAsyncJobResponse(client, cloudStackUrl, jobId, cloudStackUser);
            countTries++;
        }

        return processJobResult(response, jobId);
    }

    @NotNull
    @VisibleForTesting
    static String processJobResult(@NotNull CloudStackQueryAsyncJobResponse response,
                                   String jobId)
            throws FogbowException {

        switch (response.getJobStatus()){
            case CloudStackQueryJobResult.SUCCESS:
                return response.getJobInstanceId();
            case CloudStackQueryJobResult.FAILURE:
                throw new FogbowException(String.format(Messages.Exception.JOB_HAS_FAILED, jobId));
            default:
                throw new UnexpectedException(Messages.Error.UNEXPECTED_JOB_STATUS);
        }
    }

    @NotNull
    @VisibleForTesting
    public static CloudStackQueryAsyncJobResponse getAsyncJobResponse(@NotNull CloudStackHttpClient client,
                                                                      String cloudStackUrl,
                                                                      String jobId,
                                                                      @NotNull CloudStackUser cloudStackUser)
            throws FogbowException {

        String jsonResponse = CloudStackQueryJobResult.getQueryJobResult(
                client, cloudStackUrl, jobId, cloudStackUser);
        return CloudStackQueryAsyncJobResponse.fromJson(jsonResponse);
    }

    @VisibleForTesting
    static void sleepThread() {
        try {
            Thread.sleep(ONE_SECOND_IN_MILIS);
        } catch (InterruptedException e) {
            LOGGER.warn(Messages.Warn.SLEEP_THREAD_INTERRUPTED, e);
        }
    }

    public static class CloudStackTimeoutException extends FogbowException {

        public CloudStackTimeoutException(String message) {
            super(message);
        }

    }
}
