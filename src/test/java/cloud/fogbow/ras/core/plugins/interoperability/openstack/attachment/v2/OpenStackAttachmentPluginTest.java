package cloud.fogbow.ras.core.plugins.interoperability.openstack.attachment.v2;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DatabaseManager.class, SharedOrderHolders.class })
public class OpenStackAttachmentPluginTest extends BaseUnitTests {

    private static final String FAKE_ENDPOINT = "fake-endpoint";
    private static final String FAKE_SERVER_ID = "fake-server-id";
    private static final String FAKE_POST_REQUEST_BODY = "{\"volumeAttachment\":{\"volumeId\":\"" + TestUtils.FAKE_INSTANCE_ID + "\"}}";
    private static final String FAKE_GET_REQUEST_BODY = "{\"volumeAttachment\": {\"device\": \"" + TestUtils.FAKE_DEVICE + "\",\"id\": \""
            + TestUtils.FAKE_INSTANCE_ID + "\",\"serverId\": \"" + FAKE_SERVER_ID + "\",\"volumeId\": \"" + TestUtils.FAKE_VOLUME_ID + "\"}}";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String EMPTY_STRING = "";

    private OpenStackAttachmentPlugin plugin;
    private OpenStackHttpClient client;
    private ArgumentCaptor<String> argString = ArgumentCaptor.forClass(String.class);
    private ArgumentCaptor<OpenStackV3User> argToken = ArgumentCaptor.forClass(OpenStackV3User.class);

    @Before
    public void setUp() throws UnexpectedException {
        this.testUtils.mockReadOrdersFromDataBase();
        this.client = Mockito.mock(OpenStackHttpClient.class);

        String openstackCloudConfPath = HomeDir.getPath() 
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator 
                + TestUtils.DEFAULT_CLOUD_NAME 
                + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.plugin = Mockito.spy(new OpenStackAttachmentPlugin(openstackCloudConfPath));
        this.plugin.setClient(this.client);
    }
    
    // test case: Check if requestInstance is returning the instanceId from Json
    // response properly.
    @Ignore // FIXME posteriorly...
    @Test
    public void testCloudInitUserDataBuilder() throws FogbowException, HttpResponseException {
        // set up
        AttachmentOrder attachmentOrder = createAttachmentOrder();
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);

        Mockito.doReturn(FAKE_POST_REQUEST_BODY).when(this.client).doPostRequest(Mockito.anyString(),
                Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        // exercise
        String instanceId = this.plugin.requestInstance(attachmentOrder, cloudUser);

        // verify
        Assert.assertEquals(TestUtils.FAKE_INSTANCE_ID, instanceId);
    }

    //test case: Check if requestInstance is properly forwarding UnexpectedException thrown by doPostRequest.
    @Ignore // FIXME posteriorly...
    @Test(expected = UnexpectedException.class)
    public void testRequestInstanceThrowsUnexpectedException()
            throws FogbowException, HttpResponseException {
        //set up
        AttachmentOrder attachmentOrder = createAttachmentOrder();
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        
        int unknownStatusCode = -1;
        HttpResponseException httpResponseException = new HttpResponseException(unknownStatusCode, EMPTY_STRING);
        Mockito.doThrow(httpResponseException).when(this.client).doPostRequest(Mockito.anyString(),
                Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        //exercise/verify
        this.plugin.requestInstance(attachmentOrder, cloudUser);
    }

    //test case: Check if HttpDeleteRequest parameters are correct according to the deleteInstance call parameters
    @Ignore // FIXME Fix this test...
    @Test
    public void testDeleteInstance() throws FogbowException, IOException, SecurityException, IllegalArgumentException {
        //set up
        AttachmentOrder attachmentOrder = createAttachmentOrder();
        OpenStackV3User cloudUser = new OpenStackV3User(TestUtils.FAKE_USER_ID, TestUtils.FAKE_USER_NAME, FAKE_TOKEN_VALUE,
                FAKE_PROJECT_ID);
        
        Mockito.doNothing().when(this.client).doDeleteRequest(this.argString.capture(), this.argToken.capture());
        String expectedEndpoint = FAKE_ENDPOINT + "/v2/" + FAKE_PROJECT_ID + "/servers/" + FAKE_SERVER_ID
                + "/os-volume_attachments" + "/" + TestUtils.FAKE_VOLUME_ID;

        //exercise
        this.plugin.deleteInstance(attachmentOrder, cloudUser);

        //verify
        Assert.assertEquals(expectedEndpoint, this.argString.getValue());
        Assert.assertEquals(cloudUser.toString(), this.argToken.getValue().toString());
    }

    //test case: Check if requestInstance is properly forwarding UnauthorizedRequestException thrown by deleteInstance when Forbidden (403).
    @Test(expected = FogbowException.class)
    public void testDeleteInstanceThrowsUnauthorizedRequestExceptionWhenForbidden() throws HttpResponseException, FogbowException {
        //set up
        AttachmentOrder attachmentOrder = createAttachmentOrder();
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        
        HttpResponseException httpResponseException = new HttpResponseException(HttpStatus.SC_FORBIDDEN, EMPTY_STRING);
        Mockito.doThrow(httpResponseException).when(this.client).doDeleteRequest(Mockito.any(), Mockito.any());

        //exercise/verify
        this.plugin.deleteInstance(attachmentOrder, cloudUser);
    }

    //test case: Check if an attachment is correctly built according to the JSON returned by the getRequest
    @Ignore // FIXME posteriorly...
    @Test
    public void testGetInstance()
            throws FogbowException, HttpResponseException {
        //setup
        AttachmentOrder attachmentOrder = createAttachmentOrder();
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        
        Mockito.doReturn(FAKE_GET_REQUEST_BODY).when(this.client).doGetRequest(Mockito.anyString(),
                Mockito.eq(cloudUser));
        String openStackState = EMPTY_STRING;

        //exercise
        AttachmentInstance attachmentInstance = this.plugin.getInstance(attachmentOrder, cloudUser);

        //verify
        Assert.assertEquals(TestUtils.FAKE_DEVICE, attachmentInstance.getDevice());
        Assert.assertEquals(FAKE_SERVER_ID, attachmentInstance.getComputeId());
        Assert.assertEquals(TestUtils.FAKE_VOLUME_ID, attachmentInstance.getVolumeId());
        Assert.assertEquals(TestUtils.FAKE_INSTANCE_ID, attachmentInstance.getId());
        Assert.assertEquals(openStackState, attachmentInstance.getCloudState());
    }

    //test case: Check if getInstance is properly forwarding UnexpectedException thrown by getInstance.
    @Ignore // FIXME posteriorly...
    @Test(expected = UnexpectedException.class)
    public void testGetInstanceThrowsUnexpectedException() throws FogbowException, HttpResponseException {
        //set up
        AttachmentOrder attachmentOrder = createAttachmentOrder();
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        
        Mockito.doThrow(UnexpectedException.class).when(this.client)
                .doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        //exercise/verify
        this.plugin.getInstance(attachmentOrder, cloudUser);
    }

    //test case: check if generateJsonAttach is generating a correct Json according to
    // a random volumeId generated by VolumeOrder
    @Test
    public void testGenerateJsonToAttach() throws FogbowException {
        // setup
        VolumeOrder volumeOrder = new VolumeOrder();
        String volumeId = volumeOrder.getId();
        String expected = "{\"volumeAttachment\":{\"volumeId\":\"" + volumeId + "\",\"device\":\"\"}}";

        //exercise
        String json = this.plugin.generateJsonRequest(volumeId, "");

        //verify
        Assert.assertNotNull(json);
        Assert.assertEquals(expected, json);
    }

    private AttachmentOrder createAttachmentOrder() {
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(computeOrder.getId(), computeOrder);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);
        return this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);
    }
    
}
