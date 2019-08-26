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
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
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
@PrepareForTest({ DatabaseManager.class, OpenStackHttpToFogbowExceptionMapper.class })
public class OpenStackAttachmentPluginTest extends BaseUnitTests {

    private static final String FAKE_JSON_REQUEST = "{\"volumeAttachment\":{\"volumeId\":\"fake-volume-id\",\"device\":\"fake-device\"}}";
    private static final String FAKE_JSON_RESPONSE_FROM_GET_REQUEST = "{\"volumeAttachment\":{\"device\":\"/dev/sdd\","
            + "\"id\":\"fake-instance-id\",\"serverId\":\"fake-compute-id\",\"volumeId\":\"fake-volume-id\"}}";
    private static final String FAKE_JSON_RESPONSE_FROM_POST_REQUEST = "{\"volumeAttachment\": {\"volumeId\": \"fake-volume-id\"}}";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_SERVER_ID = "fake-server-id";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String MAP_METHOD = "map";
    private static final String MESSAGE_STATUS_CODE = "Internal server error.";
    private static final String PREFIX_ENDPOINT = "https://mycloud.domain:8774";
    
    private static final int ERROR_STATUS_CODE = 500;
    
    // FIXME remove this constants...
    private static final String FAKE_ENDPOINT = "fake-endpoint";
    private static final String FAKE_GET_REQUEST_BODY = "{\"volumeAttachment\": {\"device\": \"" + TestUtils.FAKE_DEVICE + "\",\"id\": \""
            + TestUtils.FAKE_INSTANCE_ID + "\",\"serverId\": \"" + FAKE_SERVER_ID + "\",\"volumeId\": \"" + TestUtils.FAKE_VOLUME_ID + "\"}}";
    private static final String FAKE_POST_REQUEST_BODY = "{\"volumeAttachment\":{\"volumeId\":\"" + TestUtils.FAKE_INSTANCE_ID + "\"}}";

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
    
    // test case: When invoking the requestInstance method with a valid attachment
    // request and a cloud user, it must verify that the call was successful.
    @Test
    public void testRequestInstance() throws FogbowException {
        // set up
        AttachmentOrder order = createAttachmentOrder();
        OpenStackV3User cloudUser = createOpenStackUser();
        
        String endpoint = generateEndpoint(cloudUser.getProjectId(),order.getComputeId(), null);
        CreateAttachmentResponse response = CreateAttachmentResponse.fromJson(FAKE_JSON_RESPONSE_FROM_POST_REQUEST);
        Mockito.doReturn(response).when(this.plugin).doRequestInstance(Mockito.eq(endpoint),
                Mockito.eq(FAKE_JSON_REQUEST), Mockito.eq(cloudUser));

        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getProjectIdFrom(Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getPrefixEndpoint(Mockito.eq(cloudUser.getProjectId()));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .generateJsonRequest(Mockito.eq(order.getVolumeId()), Mockito.eq(order.getDevice()));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(endpoint),
                Mockito.eq(FAKE_JSON_REQUEST), Mockito.eq(cloudUser));
    }

    // test case: When invoking the deleteInstance method with a valid attachment
    // request and a cloud user, it must verify that the call was successful.
    @Test
    public void testDeleteInstance() throws FogbowException {
        // set up
        AttachmentOrder order = createAttachmentOrder();
        OpenStackV3User cloudUser = createOpenStackUser();

        String endpoint = generateEndpoint(cloudUser.getProjectId(),order.getComputeId(), order.getVolumeId());
        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        // exercise
        this.plugin.deleteInstance(order, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getProjectIdFrom(Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getPrefixEndpoint(Mockito.eq(cloudUser.getProjectId()));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
    }
    
    // test case: When invoking the getInstance method with a valid attachment
    // request and a cloud user, it must verify that the call was successful.
    @Test
    public void testGetInstance() throws FogbowException {
        // set up
        AttachmentOrder order = createAttachmentOrder();
        OpenStackV3User cloudUser = createOpenStackUser();

        String endpoint = generateEndpoint(cloudUser.getProjectId(),order.getComputeId(), order.getVolumeId());
        GetAttachmentResponse response = GetAttachmentResponse.fromJson(FAKE_JSON_RESPONSE_FROM_GET_REQUEST);
        Mockito.doReturn(response).when(this.plugin).doGetInstance(Mockito.eq(endpoint), Mockito.eq(cloudUser));
        
        // exercise
        this.plugin.getInstance(order, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getProjectIdFrom(Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getPrefixEndpoint(Mockito.eq(cloudUser.getProjectId()));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .buildAttachmentInstanceFrom(Mockito.eq(response));
    }
    

    // test case: When calling the getInstance method and an unexpected error
    // occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoGetInstanceFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String endpoint = generateEndpoint(cloudUser.getProjectId(), TestUtils.FAKE_COMPUTE_ID,
                TestUtils.FAKE_VOLUME_ID);

        HttpResponseException expectedException = new HttpResponseException(ERROR_STATUS_CODE, MESSAGE_STATUS_CODE);
        Mockito.when(this.client.doGetRequest(Mockito.anyString(), Mockito.eq(cloudUser))).thenThrow(expectedException);

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, MAP_METHOD, Mockito.any());

        try {
            // exercise
            this.plugin.doGetInstance(endpoint, cloudUser);
            Assert.fail();
        } catch (FogbowException e) {
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    private String generateEndpoint(String projectId, String serverId, String volumeId) {
        String endpoint = PREFIX_ENDPOINT + OpenStackAttachmentPlugin.COMPUTE_V2_API_ENDPOINT + projectId
                + OpenStackAttachmentPlugin.SERVERS + serverId 
                + OpenStackAttachmentPlugin.OS_VOLUME_ATTACHMENTS;
        
        if (volumeId != null) {
            return endpoint + OpenStackAttachmentPlugin.ENDPOINT_SEPARATOR + volumeId; 
        }
        return endpoint;
    }
    
    private OpenStackV3User createOpenStackUser() {
        String userId = TestUtils.FAKE_USER_ID;
        String userName = TestUtils.FAKE_USER_NAME;
        String tokenValue = FAKE_TOKEN_VALUE;
        String projectId = FAKE_PROJECT_ID;
        return new OpenStackV3User(userId, userName, tokenValue, projectId);
    }
    
    private AttachmentOrder createAttachmentOrder() {
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(TestUtils.FAKE_COMPUTE_ID);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(computeOrder.getId(), computeOrder);
        
        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();
        volumeOrder.setInstanceId(TestUtils.FAKE_VOLUME_ID);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);
        
        AttachmentOrder attachmentOrder = this.testUtils.createLocalAttachmentOrder(computeOrder, volumeOrder);
        return attachmentOrder;
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
        HttpResponseException httpResponseException = new HttpResponseException(unknownStatusCode, OpenStackAttachmentPlugin.EMPTY_STRING);
        Mockito.doThrow(httpResponseException).when(this.client).doPostRequest(Mockito.anyString(),
                Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        //exercise/verify
        this.plugin.requestInstance(attachmentOrder, cloudUser);
    }

    //test case: Check if HttpDeleteRequest parameters are correct according to the deleteInstance call parameters
    @Ignore // FIXME Fix this test...
    @Test
    public void testDeleteInstanceOld() throws FogbowException, IOException, SecurityException, IllegalArgumentException {
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
        
        HttpResponseException httpResponseException = new HttpResponseException(HttpStatus.SC_FORBIDDEN, OpenStackAttachmentPlugin.EMPTY_STRING);
        Mockito.doThrow(httpResponseException).when(this.client).doDeleteRequest(Mockito.any(), Mockito.any());

        //exercise/verify
        this.plugin.deleteInstance(attachmentOrder, cloudUser);
    }

    //test case: Check if an attachment is correctly built according to the JSON returned by the getRequest
    @Ignore // FIXME posteriorly...
    @Test
    public void testGetInstanceOld()
            throws FogbowException, HttpResponseException {
        //setup
        AttachmentOrder attachmentOrder = createAttachmentOrder();
        OpenStackV3User cloudUser = Mockito.mock(OpenStackV3User.class);
        
        Mockito.doReturn(FAKE_GET_REQUEST_BODY).when(this.client).doGetRequest(Mockito.anyString(),
                Mockito.eq(cloudUser));
        String openStackState = OpenStackAttachmentPlugin.EMPTY_STRING;

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
    
}
