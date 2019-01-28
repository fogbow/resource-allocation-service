package cloud.fogbow.ras.core.plugins.interoperability.openstack.attachment.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackV3Token;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.constants.SystemConstants;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.instances.AttachmentInstance;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.util.connectivity.AuditableHttpRequestClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
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
    private AttachmentOrder attachmentOrder;
    private OpenStackAttachmentPlugin openStackAttachmentPlugin;
    private OpenStackV3Token localUserAttributes;
    private AuditableHttpRequestClient client;
    private ArgumentCaptor<String> argString = ArgumentCaptor.forClass(String.class);
    private ArgumentCaptor<CloudToken> argToken = ArgumentCaptor.forClass(CloudToken.class);
    private String instanceId = FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID;

    @Before
    public void setUp() throws InvalidParameterException {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put(OpenStackAttachmentPlugin.COMPUTE_NOVAV2_URL_KEY, FAKE_ENDPOINT);
        properties.put(COMPUTE_NOVAV2_NETWORK_KEY, FAKE_NET_ID);

        this.localUserAttributes = new OpenStackV3Token(FAKE_TOKEN_PROVIDER, FAKE_USER_ID, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);
        this.attachmentOrder = new AttachmentOrder(null, "default", FAKE_SERVER_ID, FAKE_VOLUME_ID, MOUNT_POINT);

        String cloudConfPath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + "default" + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.openStackAttachmentPlugin = new OpenStackAttachmentPlugin(cloudConfPath);
        this.openStackAttachmentPlugin.setProperties(properties);
        this.client = Mockito.mock(AuditableHttpRequestClient.class);
        this.openStackAttachmentPlugin.setClient(this.client);
    }

    //test case: Check if requestInstance is returning the instanceId from Json response properly.
    @Test
    public void testRequestInstance() throws FogbowException, HttpResponseException {
        //set up
        Mockito.doReturn(FAKE_POST_REQUEST_BODY).when(this.client).doPostRequest(
                Mockito.anyString(), Mockito.any(CloudToken.class), Mockito.anyString());

        //exercise
        String instanceId = this.openStackAttachmentPlugin.requestInstance(this.attachmentOrder, this.localUserAttributes);

        //verify
        Assert.assertEquals(FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID, instanceId);
    }

    //test case: Check if requestInstance is properly forwarding UnexpectedException thrown by doPostRequest.
    @Test(expected = UnexpectedException.class)
    public void testRequestInstanceThrowsUnexpectedException()
            throws FogbowException, HttpResponseException {
        //set up
        int unknownStatusCode = -1;
        HttpResponseException httpResponseException = new HttpResponseException(unknownStatusCode, "");
        Mockito.doThrow(httpResponseException).when(this.client).doPostRequest(Mockito.anyString(),
                Mockito.any(CloudToken.class), Mockito.anyString());

        //exercise/verify
        this.openStackAttachmentPlugin.requestInstance(this.attachmentOrder, this.localUserAttributes);
    }

    //test case: Check if HttpDeleteRequest parameters are correct according to the deleteInstance call parameters
    @Test
    public void testDeleteInstance() throws FogbowException, ClientProtocolException,
            IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
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
    @Test(expected = FogbowException.class)
    public void testDeleteInstanceThrowsUnauthorizedRequestExceptionWhenForbidden() throws HttpResponseException, FogbowException {
        //set up
        HttpResponseException httpResponseException = new HttpResponseException(HttpStatus.SC_FORBIDDEN, "");
        Mockito.doThrow(httpResponseException).when(this.client).doDeleteRequest(Mockito.any(), Mockito.any());

        //exercise/verify
        this.openStackAttachmentPlugin.deleteInstance(instanceId, this.localUserAttributes);
    }

    //test case: Check if an attachment is correctly built according to the JSON returned by the getRequest
    @Test
    public void testGetInstance()
            throws FogbowException, HttpResponseException {
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
        Assert.assertEquals(FAKE_SERVER_ID, attachmentInstance.getComputeId());
        Assert.assertEquals(FAKE_VOLUME_ID, attachmentInstance.getVolumeId());
        Assert.assertEquals(FAKE_INSTANCE_ID, attachmentInstance.getId());
        Assert.assertEquals(expectedFogbowState, attachmentInstance.getState());
    }

    //test case: Check if getInstance is properly forwarding UnexpectedException thrown by getInstance.
    @Test(expected = UnexpectedException.class)
    public void testGetInstanceThrowsUnexpectedException()
            throws FogbowException, HttpResponseException {
        //set up
        Mockito.doThrow(UnexpectedException.class).when(this.client)
                .doGetRequest(Mockito.anyString(), Mockito.any(CloudToken.class));
        String instanceId = FAKE_SERVER_ID + SEPARATOR_ID + FAKE_VOLUME_ID;

        //exercise/verify
        this.openStackAttachmentPlugin.getInstance(instanceId, this.localUserAttributes);
    }

    //test case: check if generateJsonAttach is generating a correct Json according to
    // a random volumeId generated by VolumeOrder
    @Test
    public void testGenerateJsonToAttach() {
        // setup
        VolumeOrder volumeOrder = new VolumeOrder();
        String volumeId = volumeOrder.getId();
        String expected = "{\"volumeAttachment\":{\"volumeId\":\"" + volumeId + "\",\"device\":\"\"}}";

        //exercise
        String json = this.openStackAttachmentPlugin.generateJsonToAttach(volumeId, "");

        //verify
        Assert.assertNotNull(json);
        Assert.assertEquals(expected, json);
    }

}
