package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.util.connectivity.CloudHttpClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;

public class CloudStackHttpClient extends CloudHttpClient {
    public static final String CLOUDSTACK_HTTP_METHOD = "GET";

    public CloudStackHttpClient(Integer timeout) throws FatalErrorException {
        super(timeout);
    }

    public CloudStackHttpClient(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public GenericRequest includeTokenInRequest(GenericRequest genericRequest, CloudToken token) {
        try {
            GenericRequest clonedRequest = (GenericRequest) genericRequest.clone();
            URIBuilder uriBuilder = new URIBuilder(clonedRequest.getUrl());
            CloudStackUrlUtil.sign(uriBuilder, token.getTokenValue());

            clonedRequest.setUrl(uriBuilder.toString());
            return clonedRequest;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (UnauthorizedRequestException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
