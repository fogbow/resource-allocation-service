package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpDelete;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.instances.AttachmentInstance;
import org.fogbowcloud.manager.core.models.orders.AttachmentOrder;
import org.fogbowcloud.manager.core.models.orders.VolumeOrder;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
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
    private HttpRequestClientUtil client;

    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put(COMPUTE_NOVAV2_URL_KEY, FAKE_ENDPOINT);
        properties.put(COMPUTE_NOVAV2_NETWORK_KEY, FAKE_NET_ID);

        this.localToken = Mockito.mock(Token.class);
        this.attachmentOrder =
                new AttachmentOrder(null, null, null, FAKE_SERVER_ID, FAKE_VOLUME_ID, MOUNT_POINT);
        this.openStackAttachmentPlugin = Mockito.spy(new OpenStackNovaV2AttachmentPlugin());

        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.openStackAttachmentPlugin.setClient(this.client);
    }
    
    //test case: Check if requestInstance is returning the instanceId from Json response properly.
    @Test
    public void testRequestInstance() throws FogbowManagerException, HttpResponseException, UnexpectedException {
    	//set up
        Mockito.doReturn(FAKE_POST_REQUEST_BODY).when(this.client).doPostRequest(
                Mockito.anyString(), Mockito.any(Token.class), Mockito.any(JSONObject.class));
        
        //exercise
        String instanceId = this.openStackAttachmentPlugin.requestInstance(this.attachmentOrder,
                this.localToken);
        
        //verify
        Assert.assertEquals(FAKE_INSTANCE_ID, instanceId);
    }
    
    //test case: Check if requestInstance throws UnexpectedException when the http requisition throws UnexpectedException.
    @Test(expected = UnexpectedException.class)
    public void testRequestInstanceThrowUnexpectedException()
            throws FogbowManagerException, HttpResponseException, UnexpectedException {
        //set up
        Mockito.doThrow(UnexpectedException.class).when(this.client).doPostRequest(
                Mockito.anyString(), Mockito.any(Token.class), Mockito.any(JSONObject.class));
        //exercise/verify
        this.openStackAttachmentPlugin.requestInstance(this.attachmentOrder, this.localToken);
    }
    
    // FIXME: this method does nothing
    @Test(expected = Test.None.class)
    public void testDeleteInstance() throws FogbowManagerException, ClientProtocolException,
            IOException, UnexpectedException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Mockito.doReturn(FAKE_TENANT_ID).when(this.openStackAttachmentPlugin)
                .getTenantId(this.localToken);
        Mockito.doReturn(FAKE_FEDERATION_TOKEN_VALUE).when(this.localToken).getAccessId();

        String instanceId = FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID;

        Mockito.doNothing().when(this.openStackAttachmentPlugin).deleteInstance(instanceId,
                this.localToken);
        
        HttpClient client = Mockito.mock(HttpClient.class);
       
        HttpResponse response = Mockito.mock(HttpResponse.class);
        response.setStatusCode(204);
        
        Mockito.doReturn(response).when(client).execute(Mockito.any(HttpDelete.class));
        
        Field clientField = HttpRequestClientUtil.class.getDeclaredField("client");
        
        clientField.setAccessible(true);
        clientField.set(this.client, client);
        
        this.openStackAttachmentPlugin.deleteInstance(instanceId, this.localToken);
        
    }
    
    // FIXME: this method does nothing
    @Test(expected = FogbowManagerException.class)
    public void testDeleteInstanceThrowsRequestException()
            throws FogbowManagerException, UnexpectedException {

        String instanceId = FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID;

        Mockito.doThrow(FogbowManagerException.class).when(this.openStackAttachmentPlugin)
                .deleteInstance(instanceId, this.localToken);

        this.openStackAttachmentPlugin.deleteInstance(instanceId, this.localToken);
    }
    
    // FIXME: is this test useful?
    //test case: Check if a attachment returned by getInstance is not null when a valid instance is returned by getInstanceFromJson.
    @Test
    public void testGetInstance()
            throws FogbowManagerException, HttpResponseException, UnexpectedException {
    	//setup
        String instanceId = FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID;
        Mockito.doReturn(FAKE_GET_RESQUEST_BODY).when(this.client).doGetRequest(Mockito.anyString(),
                Mockito.eq(this.localToken));
        AttachmentInstance attachmentInstance = Mockito.mock(AttachmentInstance.class);
        Mockito.doReturn(attachmentInstance).when(this.openStackAttachmentPlugin)
                .getInstanceFromJson(FAKE_GET_RESQUEST_BODY);
        
        //exercise
        attachmentInstance =
                this.openStackAttachmentPlugin.getInstance(instanceId, this.localToken);
        
        //verify
        Assert.assertNotNull(attachmentInstance);
    }
    
    //test case: Check if getInstance throws UnexpectedException when the http requisition throws UnexpectedException
    @Test(expected = UnexpectedException.class)
    public void testGetInstanceThrowsUnexpectedException()
            throws FogbowManagerException, HttpResponseException, UnexpectedException {
    	//set up
        Mockito.doThrow(UnexpectedException.class).when(this.client)
                .doGetRequest(Mockito.anyString(), Mockito.any(Token.class));
        String instanceId = FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID;
        
        //exercise/verify
        this.openStackAttachmentPlugin.getInstance(instanceId, this.localToken);
    }
    
    //test case: check if generateJsonAttach is generating a correct Json according to a random volumeId generated by VolumeOrder
    @Test
    public void testGenerateJsonToAttach() {
    	// setup
        VolumeOrder volumeOrder = Mockito.spy(new VolumeOrder());
        String volumeId = volumeOrder.getId();
        JSONObject json = this.openStackAttachmentPlugin.generateJsonToAttach(volumeId);
        
        //exercise
        String expected = "{\"volumeAttachment\":{\"volumeId\":\"" + volumeId + "\"}}";
        
        //verify
        Assert.assertNotNull(json);
        Assert.assertEquals(expected, json.toString());
    }

}
