package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.HttpGenericRequest;
import cloud.fogbow.ras.util.connectivity.CloudHttpClient;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;

public class CloudStackHttpClient extends CloudHttpClient {

    public CloudStackHttpClient() {
    }

    @Override
    public HttpGenericRequest prepareRequest(HttpGenericRequest genericRequest, CloudToken token) {
        try {
            HttpGenericRequest clonedRequest = (HttpGenericRequest) genericRequest.clone();
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
