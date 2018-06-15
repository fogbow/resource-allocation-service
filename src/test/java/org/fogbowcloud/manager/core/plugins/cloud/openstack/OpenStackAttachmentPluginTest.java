package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import static org.mockito.Mockito.mock;
import java.util.Properties;

import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.RequestException;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.cloud.openstack.OpenStackNovaV2AttachmentPlugin;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_TENANT_ID = "fake-tenant-id";
    private static final String FAKE_FEDERATION_TOKEN_VALUE = "fake_federation_token_value";
    
    @SuppressWarnings("unused")
    private AttachmentOrder attachmentOrder;
    private OpenStackNovaV2AttachmentPlugin openStackAttachmentPlugin;
    
    @SuppressWarnings("unused")
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
    
    @Ignore
    @Test
    public void requestInstanceTest() throws RequestException {
        Mockito.doReturn(FAKE_TENANT_ID).when(openStackAttachmentPlugin).getTenantId(localToken);
        Mockito.doReturn(FAKE_FEDERATION_TOKEN_VALUE).when(localToken).getAccessId();
        String instanceId = this.openStackAttachmentPlugin.requestInstance(this.attachmentOrder, this.localToken);
        Assert.assertEquals(FAKE_INSTANCE_ID, instanceId);
    }
    
    @Test
    public void deleteInstance() {
        //TODO
    }
    
    @Test
    public void getInstanceTest() {
        //TODO
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
