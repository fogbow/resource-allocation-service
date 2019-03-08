package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.genericrequest.v4_9;

import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.connectivity.HttpResponse;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.HttpRequest;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.GenericRequestPlugin;
import com.google.gson.Gson;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.HashMap;

public class CloudStackGenericRequestPlugin implements GenericRequestPlugin<CloudStackUser> {

    private CloudStackHttpClient client;

    public CloudStackGenericRequestPlugin() {
        this.client = new CloudStackHttpClient();
    }

    @Override
    public HttpResponse redirectGenericRequest(String genericRequest, CloudStackUser cloudUser) throws FogbowException {
        HttpRequest httpRequest = new Gson().fromJson(genericRequest, HttpRequest.class);
        try {
            URIBuilder uriBuilder = new URIBuilder(httpRequest.getUrl());
            CloudStackUrlUtil.sign(uriBuilder, cloudUser.getToken());

            String url = uriBuilder.toString();
            HashMap<String, String> headers = httpRequest.getHeaders();
            HashMap<String, String> body = httpRequest.getBody();
            return client.doGenericRequest(HttpMethod.GET, url, headers, body, cloudUser);
        } catch (URISyntaxException e) {
            throw new FogbowException(String.format(Messages.Exception.MALFORMED_GENERIC_REQUEST_URL, httpRequest.getUrl()));
        }
    }

    protected void setClient(CloudStackHttpClient client) {
        this.client = client;
    }
}