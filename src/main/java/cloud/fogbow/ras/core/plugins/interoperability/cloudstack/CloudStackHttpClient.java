package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.HttpFogbowGenericRequest;
import cloud.fogbow.ras.util.connectivity.CloudHttpClient;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;

public class CloudStackHttpClient extends CloudHttpClient<CloudStackUser> {

    public CloudStackHttpClient() {
    }

    @Override
    public HttpFogbowGenericRequest prepareRequest(HttpFogbowGenericRequest genericRequest, CloudStackUser cloudUser) {
        try {
            HttpFogbowGenericRequest clonedRequest = (HttpFogbowGenericRequest) genericRequest.clone();
            URIBuilder uriBuilder = new URIBuilder(clonedRequest.getUrl());
            CloudStackUrlUtil.sign(uriBuilder, cloudUser.getToken());

            clonedRequest.setUrl(uriBuilder.toString());
            return clonedRequest;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (UnauthorizedRequestException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
