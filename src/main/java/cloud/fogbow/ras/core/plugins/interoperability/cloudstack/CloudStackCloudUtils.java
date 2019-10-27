package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryAsyncJobResponse;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackQueryJobResult;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.constants.Messages;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;

import javax.validation.constraints.NotNull;

public class CloudStackCloudUtils {
    private static final Logger LOGGER = Logger.getLogger(CloudStackUrlUtil.class);

    public static final String CLOUDSTACK_URL_CONFIG = "cloudstack_api_url";
    public static final String NETWORK_OFFERING_ID_CONFIG = "network_offering_id";
    public static final String ZONE_ID_CONFIG = "zone_id";

    public static final int JOB_STATUS_COMPLETE = 1;
    public static final int JOB_STATUS_PENDING = 0;
    public static final int JOB_STATUS_FAILURE = 2;
    public static final int JOB_STATUS_INCONSISTENT = 3;
    public static final String PENDING_STATE = "pending";
    public static final String FAILURE_STATE = "failure";

    public static final int ONE_SECOND_IN_MILIS = 1000;
    public static final int MAX_TRIES = 30;

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
                throw new TimeoutCloudstackAsync(String.format(Messages.Exception.JOB_TIMEOUT, jobId));
            }
            response = getAsyncJobResponse(client, cloudStackUrl, jobId, cloudStackUser);
            countTries++;
            sleepThread();
        }

        return processJobResult(response, jobId);
    }

    private static void sleepThread() {
        try {
            Thread.sleep(ONE_SECOND_IN_MILIS);
        } catch (InterruptedException e) {
            LOGGER.warn("", e);
        }
    }

    public static String processJobResult(CloudStackQueryAsyncJobResponse queryAsyncJobResult,
                                      String jobId)
            throws FogbowException {

        switch (queryAsyncJobResult.getJobStatus()){
            case CloudStackQueryJobResult.SUCCESS:
                return queryAsyncJobResult.getJobInstanceId();
            case CloudStackQueryJobResult.FAILURE:
                throw new FogbowException(String.format(Messages.Exception.JOB_HAS_FAILED, jobId));
            default:
                throw new UnexpectedException(Messages.Error.UNEXPECTED_JOB_STATUS);
        }
    }

    public static CloudStackQueryAsyncJobResponse getAsyncJobResponse(CloudStackHttpClient client,
                                                                      String cloudStackUrl,
                                                                      String jobId,
                                                                      CloudStackUser cloudStackUser)
            throws FogbowException {

        String jsonResponse = CloudStackQueryJobResult.getQueryJobResult(
                client, cloudStackUrl, jobId, cloudStackUser);
        return CloudStackQueryAsyncJobResponse.fromJson(jsonResponse);
    }

    public static class TimeoutCloudstackAsync extends FogbowException {
        public TimeoutCloudstackAsync(String message) {
            super(message);
        }
    }

}
