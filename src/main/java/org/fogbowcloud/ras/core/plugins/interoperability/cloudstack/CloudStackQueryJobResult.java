package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack;

import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.publicip.v4_9.QueryAsyncJobResultRequest;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;

public class CloudStackQueryJobResult {

    public static final int PROCESSING = 0;
    public static final int SUCCESS = 1;
    public static final int FAILURE = 2;

    public static String getQueryJobResult(HttpRequestClientUtil client, String jobId, Token cloudStackToken)
            throws FogbowRasException {
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
            CloudStackHttpToFogbowRasExceptionMapper.map(e);
        }

        return jsonResponse;
    }
}
