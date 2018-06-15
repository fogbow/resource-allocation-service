package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import java.util.Properties;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.OpenStackNovaV2AttachmentPlugin;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class OpenStackAttachmentPluginTest {

    private static final String COMPUTE_NOVAV2_NETWORK_KEY = "compute_novav2_network_id";
    private static final String COMPUTE_NOVAV2_URL_KEY = "compute_novav2_url";
    private static final String FAKE_ENDPOINT = "fake-endpoint";
    private static final String FAKE_NET_ID = "fake-net-id";
    private static final String FAKE_SERVER_ID = "fake-server-id";
    private static final String FAKE_VOLUME_ID = "fake-volume-id";
    private static final String MOUNT_POINT = "/dev/vdd";
    private static final String SEPARATOR_ID = " ";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_POST_REQUEST_BODY = "{\"volumeAttachment\":{\"volumeId\":\"" + FAKE_INSTANCE_ID + "\"}}";
    private static final String FAKE_GET_RESQUEST_BODY = "{\"volumeAttachment\": {\"device\": \"/dev/sdd\",\"id\": \"" 
            + FAKE_INSTANCE_ID + "\",\"serverId\": \"" + FAKE_SERVER_ID + "\",\"volumeId\": \"" + FAKE_VOLUME_ID + "\"}}";
    private static final String FAKE_TENANT_ID = "fake-tenant-id";
    private static final String FAKE_FEDERATION_TOKEN_VALUE = "fake_federation_token_value";
    
    private AttachmentOrder attachmentOrder;
    private OpenStackNovaV2AttachmentPlugin openStackAttachmentPlugin;
    private Token localToken;

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put(COMPUTE_NOVAV2_URL_KEY, FAKE_ENDPOINT);
        properties.put(COMPUTE_NOVAV2_NETWORK_KEY, FAKE_NET_ID);

        this.localToken = mock(Token.class);
        this.attachmentOrder = new AttachmentOrder(null, null, null, FAKE_SERVER_ID, FAKE_VOLUME_ID, MOUNT_POINT);
        this.openStackAttachmentPlugin = Mockito.spy(new OpenStackNovaV2AttachmentPlugin());
    }
    
    @Test
    public void requestInstanceTest() throws RequestException {
        doReturn(FAKE_POST_REQUEST_BODY).when(this.openStackAttachmentPlugin).
        doPostRequest(Mockito.anyString(), Mockito.anyString(), Mockito.any(JSONObject.class));

        String instanceId = this.openStackAttachmentPlugin.requestInstance(this.attachmentOrder, this.localToken);
        Assert.assertEquals(FAKE_INSTANCE_ID, instanceId);
    }
    
    @Test
    public void deleteInstance() throws RequestException {
      Mockito.doReturn(FAKE_TENANT_ID).when(this.openStackAttachmentPlugin).getTenantId(this.localToken);
      Mockito.doReturn(FAKE_FEDERATION_TOKEN_VALUE).when(this.localToken).getAccessId();
      
      String instanceId = FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID;
      
      doNothing().when(this.openStackAttachmentPlugin).deleteInstance(instanceId, this.localToken);
      this.openStackAttachmentPlugin.deleteInstance(instanceId, this.localToken);
      // returned on successful request response codes 202
    }
    
    @Test
    public void getInstanceTest() throws RequestException {
        String instanceId = FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID;
        
        doReturn(FAKE_GET_RESQUEST_BODY).when(this.openStackAttachmentPlugin).
        doGetRequest(Mockito.anyString(), eq(this.localToken));
        
        AttachmentInstance attachmentInstance = Mockito.mock(AttachmentInstance.class);
        
        doReturn(attachmentInstance).when(this.openStackAttachmentPlugin).getInstanceFromJson(FAKE_GET_RESQUEST_BODY);
        
        attachmentInstance = this.openStackAttachmentPlugin.getInstance(instanceId, this.localToken);
        Assert.assertNotNull(attachmentInstance);
    }
    
    @Test
    public void generateJsonToAttachTest() {
        VolumeOrder volumeOrder = Mockito.spy(new VolumeOrder());
        String volumeId = volumeOrder.getId();
        
        JSONObject json = this.openStackAttachmentPlugin.generateJsonToAttach(volumeId);
        
        String expected = "{\"volumeAttachment\":{\"volumeId\":\"" + volumeId + "\"}}";
        Assert.assertNotNull(json);
        Assert.assertEquals(expected, json.toString());        
    }
}
