package org.fogbowcloud.manager.core.plugins.cloud.openstack;


import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import com.google.gson.Gson;

public class OpenStackImagePluginTest {

    /**
     * TODO The test must be redone using mock.
     * @throws FogbowManagerException
     */
    @Ignore
    @Test
    public void testGetAllImages() throws FogbowManagerException, UnexpectedException {
        HomeDir.getInstance().setPath("src/test/resources/private");

        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put("", "");

        OpenStackImagePlugin plugin = new OpenStackImagePlugin();

        Token token = new Token();

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("tenantId", "");
        token.setAttributes(attributes);
        token.setAccessId("");
        Map<String, String> s = plugin.getAllImages(token);
        JSONObject json = new JSONObject(s);
        System.out.println(json);

        Image image = plugin.getImage("", token);
        Gson gson = new Gson();
        String userJSONString = gson.toJson(image);
        System.out.println(userJSONString);
    }

}
