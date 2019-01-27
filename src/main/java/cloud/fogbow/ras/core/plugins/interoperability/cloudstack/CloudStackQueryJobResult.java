package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.QueryAsyncJobResultRequest;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;
import org.apache.http.client.HttpResponseException;

public class CloudStackQueryJobResult {

    public static final int PROCESSING = 0;
    public static final int SUCCESS = 1;
    public static final int FAILURE = 2;

    public static String getQueryJobResult(AuditableHttpRequestClient client, String jobId, CloudToken cloudStackToken)
            throws FogbowException {
        QueryAsyncJobResultRequest queryAsyncJobResultRequest = new QueryAsyncJobResultRequest.Builder()
                .jobId(jobId)
                .build();

        CloudStackUrlUtil
                .sign(queryAsyncJobResultRequest.getUriBuilder(), cloudStackToken.getTokenValue());

        String jsonResponse = null;
        String requestUrl = queryAsyncJobResultRequest.getUriBuilder().toString();

        try {
            jsonResponse = client.doGetRequest(requestUrl, cloudStackToken);
        } catch (HttpResponseException e) {
            CloudStackHttpToFogbowExceptionMapper.map(e);
        }

        return jsonResponse;
    }
}
