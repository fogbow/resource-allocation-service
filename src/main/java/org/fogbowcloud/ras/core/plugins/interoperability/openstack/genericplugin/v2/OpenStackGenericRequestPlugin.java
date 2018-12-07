package org.fogbowcloud.ras.core.plugins.interoperability.openstack.genericplugin.v2;

import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.plugins.interoperability.genericrequest.HttpBasedGenericRequestPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

public class OpenStackGenericRequestPlugin extends HttpBasedGenericRequestPlugin<OpenStackV3Token> {

    @Override
    public String redirectGenericRequest(String method, String url, Map<String, String> headers, String body, OpenStackV3Token token) throws FogbowRasException {
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod(method.toUpperCase());

            if (!body.isEmpty()) {
                con.setDoOutput(true);
                OutputStream os = con.getOutputStream();
                os.write(body.getBytes());
                os.flush();
                os.close();
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                StringBuffer response = new StringBuffer();

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else {
                // FIXME Retrieve more info about this error
                throw new FogbowRasException(String.format("Response code was <%d>", responseCode));
            }
        } catch (ProtocolException e) {
            throw new FogbowRasException("", e);
        } catch (MalformedURLException e) {
            throw new FogbowRasException("", e);
        } catch (IOException e) {
            throw new FogbowRasException("", e);
        }
    }

}
