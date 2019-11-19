package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryAsyncJobResponse;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryJobResult;
import cloud.fogbow.ras.constants.Messages;
import com.google.common.annotations.VisibleForTesting;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;

public class CloudStackCloudUtils {
    private static final Logger LOGGER = Logger.getLogger(CloudStackCloudUtils.class);

    public static final String CLOUDSTACK_URL_CONFIG = "cloudstack_api_url";
    public static final String NETWORK_OFFERING_ID_CONFIG = "network_offering_id";
    public static final String ZONE_ID_CONFIG = "zone_id";

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
            LOGGER.warn(Messages.Warn.THREAD_INTERRUPTED, e);
        }
    }

    public static class CloudStackTimeoutException extends FogbowException {

        public CloudStackTimeoutException(String message) {
            super(message);
        }

    }

}
