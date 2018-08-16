package org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.core.plugins.cloud.cloudstack.volume.v4_9.CloudStackVolumePlugin;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CloudStackVolumePluginTest {

    private static final String TEST_PATH = "src/test/resources/private";
    private static final String LOCAL_USER_ATTRIBUTES = "apiKey:secretKey";
    private static final String POST = "post";
    private static final String GET = "get";
  
    private static final String RESPONSE_LIST_DISK_OFFERINGS = 
          "{\n" + 
          "    \"listdiskofferingsresponse\": {\n" + 
          "        \"id\": \"2bc5cf33-9ae4-4fa8-8e90-f271bdd2dbce\",\n" + 
          "        \"jobid\": \"7aca8c95-6eb4-43be-98ab-320af00bb3e0\",\n" + 
          "        \"diskoffering\": [{\n" + 
          "            \"id\": \"62d5f174-2f1e-42f0-931e-07600a05470e\",\n" + 
          "            \"disksize\": 0,\n" + 
          "            \"iscustomized\": \"true\"\n" + 
          "        }]\n" + 
          "    }\n" + 
          "}";

    private static final String RESPONSE_CREATE_VOLUME = 
          "{\n" + 
          "    \"createvolumeresponse\": {\n" + 
          "        \"id\": \"2bc5cf33-9ae4-4fa8-8e90-f271bdd2dbce\",\n" + 
          "        \"jobid\": \"7aca8c95-6eb4-43be-98ab-320af00bb3e0\"\n" + 
          "    }\n" + 
          "}\n";
  
    private CloudStackVolumePlugin cloudStackVolumePlugin;
    private HttpRequestClientUtil httpResponseClientUtil;
    private Token token;
  
    @Before
    public void setUp() {
        HomeDir.getInstance().setPath(TEST_PATH);
        this.httpResponseClientUtil = Mockito.mock(HttpRequestClientUtil.class);
        this.cloudStackVolumePlugin = new CloudStackVolumePlugin();
        this.cloudStackVolumePlugin.setClient(this.httpResponseClientUtil);
        this.token = new Token(LOCAL_USER_ATTRIBUTES);
    }
  
    @Test
    public void testRequestInstance() throws FogbowManagerException, UnexpectedException {
        VolumeOrder volumeOrder = new VolumeOrder("fake-id", null, "fake-requesting-member",
                "fake-providing-member", 1, "fake-volume-name");
        String orderId = volumeOrder.getId();

        String response = this.cloudStackVolumePlugin.requestInstance(volumeOrder, this.token);
        String expected = String.format(RESPONSE_CREATE_VOLUME, orderId);
        Assert.assertEquals(expected, response);
    }
  
}
