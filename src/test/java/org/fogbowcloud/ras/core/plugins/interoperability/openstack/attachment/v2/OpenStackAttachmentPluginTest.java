package org.fogbowcloud.ras.core.plugins.interoperability.openstack.attachment.v2;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.ResourceType;
import org.fogbowcloud.ras.core.models.instances.AttachmentInstance;
import org.fogbowcloud.ras.core.models.instances.InstanceState;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.orders.VolumeOrder;
import org.fogbowcloud.ras.core.models.tokens.OpenStackV3Token;
import org.fogbowcloud.ras.core.models.tokens.Token;
import org.fogbowcloud.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Properties;

public class OpenStackAttachmentPluginTest {

    private static final String COMPUTE_NOVAV2_NETWORK_KEY = "compute_novav2_network_id";
    private static final String FAKE_ENDPOINT = "fake-endpoint";
    private static final String FAKE_NET_ID = "fake-net-id";
    private static final String FAKE_SERVER_ID = "fake-server-id";
    private static final String FAKE_VOLUME_ID = "fake-volume-id";
    private static final String MOUNT_POINT = "/dev/vdd";
    private static final String SEPARATOR_ID = " ";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_POST_REQUEST_BODY = "{\"volumeAttachment\":{\"volumeId\":\"" + FAKE_INSTANCE_ID + "\"}}";
    private static final String FAKE_DEVICE = "/dev/sdd";
    private static final String FAKE_GET_REQUEST_BODY = "{\"volumeAttachment\": {\"device\": \"" + FAKE_DEVICE + "\",\"id\": \""
            + FAKE_INSTANCE_ID + "\",\"serverId\": \"" + FAKE_SERVER_ID + "\",\"volumeId\": \"" + FAKE_VOLUME_ID + "\"}}";
    private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_PROJECT_NAME = "fake-project-name";
    private AttachmentOrder attachmentOrder;
    private OpenStackNovaV2AttachmentPlugin openStackAttachmentPlugin;
    private OpenStackV3Token localUserAttributes;
    private HttpRequestClientUtil client;
    private ArgumentCaptor<String> argString = ArgumentCaptor.forClass(String.class);
    private ArgumentCaptor<Token> argToken = ArgumentCaptor.forClass(Token.class);
    private String instanceId = FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID;

    @Before
    public void setUp() throws InvalidParameterException {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put(OpenStackNovaV2AttachmentPlugin.COMPUTE_NOVAV2_URL_KEY, FAKE_ENDPOINT);
        properties.put(COMPUTE_NOVAV2_NETWORK_KEY, FAKE_NET_ID);

        this.localUserAttributes = new OpenStackV3Token(FAKE_TOKEN_PROVIDER, FAKE_TOKEN_VALUE, FAKE_USER_ID, FAKE_NAME, FAKE_PROJECT_ID, FAKE_PROJECT_NAME);
        this.attachmentOrder =
                new AttachmentOrder(null, null, null, FAKE_SERVER_ID, FAKE_VOLUME_ID, MOUNT_POINT);

        this.openStackAttachmentPlugin = new OpenStackNovaV2AttachmentPlugin();
        this.openStackAttachmentPlugin.setProperties(properties);
        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.openStackAttachmentPlugin.setClient(this.client);
    }

    //test case: Check if requestInstance is returning the instanceId from Json response properly.
    @Test
    public void testRequestInstance() throws FogbowRasException, HttpResponseException, UnexpectedException {
        //set up
        Mockito.doReturn(FAKE_POST_REQUEST_BODY).when(this.client).doPostRequest(
                Mockito.anyString(), Mockito.any(Token.class), Mockito.anyString());

        //exercise
        String instanceId = this.openStackAttachmentPlugin.requestInstance(this.attachmentOrder, this.localUserAttributes);

        //verify
        Assert.assertEquals(FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID, instanceId);
    }

    //test case: Check if requestInstance is properly forwarding UnexpectedException thrown by doPostRequest.
    @Test(expected = UnexpectedException.class)
    public void testRequestInstanceThrowsUnexpectedException()
            throws FogbowRasException, HttpResponseException, UnexpectedException {
        //set up
        int unknownStatusCode = -1;
        HttpResponseException httpResponseException = new HttpResponseException(unknownStatusCode, "");
        Mockito.doThrow(httpResponseException).when(this.client).doPostRequest(Mockito.anyString(),
                Mockito.any(Token.class), Mockito.anyString());

        //exercise/verify
        this.openStackAttachmentPlugin.requestInstance(this.attachmentOrder, this.localUserAttributes);
    }

    //test case: Check if HttpDeleteRequest parameters are correct according to the deleteInstance call parameters
    @Test
    public void testDeleteInstance() throws FogbowRasException, ClientProtocolException,
            IOException, UnexpectedException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        //set up
        Mockito.doNothing().when(this.client).doDeleteRequest(this.argString.capture(), this.argToken.capture());
        String expectedEndpoint = FAKE_ENDPOINT + "/v2/" + FAKE_PROJECT_ID + "/servers/" + FAKE_SERVER_ID
                + "/os-volume_attachments" + "/" + FAKE_VOLUME_ID;

        //exercise
        this.openStackAttachmentPlugin.deleteInstance(this.instanceId, this.localUserAttributes);

        //verify
        Assert.assertEquals(expectedEndpoint, this.argString.getValue());
        Assert.assertEquals(this.localUserAttributes.toString(), this.argToken.getValue().toString());
    }

    //test case: Check if requestInstance is properly forwarding UnauthorizedRequestException thrown by deleteInstance when Forbidden (403).
    @Test(expected = FogbowRasException.class)
    public void testDeleteInstanceThrowsUnauthorizedRequestExceptionWhenForbidden() throws HttpResponseException, FogbowRasException, UnexpectedException {
        //set up
        HttpResponseException httpResponseException = new HttpResponseException(HttpStatus.SC_FORBIDDEN, "");
        Mockito.doThrow(httpResponseException).when(this.client).doDeleteRequest(Mockito.any(), Mockito.any());

        //exercise/verify
        this.openStackAttachmentPlugin.deleteInstance(instanceId, this.localUserAttributes);
    }

    //test case: Check if an attachment is correctly built according to the JSON returned by the getRequest
    @Test
    public void testGetInstance()
            throws FogbowRasException, HttpResponseException, UnexpectedException {
        //setup
        String instanceId = FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID;
        Mockito.doReturn(FAKE_GET_REQUEST_BODY).when(this.client).doGetRequest(Mockito.anyString(),
                Mockito.eq(this.localUserAttributes));
        String openStackState = "";
        InstanceState expectedFogbowState = OpenStackStateMapper.map(ResourceType.ATTACHMENT, openStackState);

        //exercise
        AttachmentInstance attachmentInstance = this.openStackAttachmentPlugin.getInstance(instanceId, this.localUserAttributes);

        //verify
        Assert.assertEquals(FAKE_DEVICE, attachmentInstance.getDevice());
        Assert.assertEquals(FAKE_SERVER_ID, attachmentInstance.getServerId());
        Assert.assertEquals(FAKE_VOLUME_ID, attachmentInstance.getVolumeId());
        Assert.assertEquals(FAKE_INSTANCE_ID, attachmentInstance.getId());
        Assert.assertEquals(expectedFogbowState, attachmentInstance.getState());
    }

    //test case: Check if getInstance is properly forwarding UnexpectedException thrown by getInstance.
    @Test(expected = UnexpectedException.class)
    public void testGetInstanceThrowsUnexpectedException()
            throws FogbowRasException, HttpResponseException, UnexpectedException {
        //set up
        Mockito.doThrow(UnexpectedException.class).when(this.client)
                .doGetRequest(Mockito.anyString(), Mockito.any(Token.class));
        String instanceId = FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID;

        //exercise/verify
        this.openStackAttachmentPlugin.getInstance(instanceId, this.localUserAttributes);
    }

    //test case: check if generateJsonAttach is generating a correct Json according to a random volumeId generated by VolumeOrder
    @Test
    public void testGenerateJsonToAttach() {
        // setup
        VolumeOrder volumeOrder = new VolumeOrder();
        String volumeId = volumeOrder.getId();
        String expected = "{\"volumeAttachment\":{\"volumeId\":\"" + volumeId + "\"}}";

        //exercise
        String json = this.openStackAttachmentPlugin.generateJsonToAttach(volumeId);

        //verify
        Assert.assertNotNull(json);
        Assert.assertEquals(expected, json);
    }

}
