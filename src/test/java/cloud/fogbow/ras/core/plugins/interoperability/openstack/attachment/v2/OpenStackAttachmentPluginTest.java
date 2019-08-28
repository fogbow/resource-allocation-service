package cloud.fogbow.ras.core.plugins.interoperability.openstack.attachment.v2;

import java.io.File;

import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DatabaseManager.class, GetAttachmentResponse.class, OpenStackCloudUtils.class, OpenStackHttpToFogbowExceptionMapper.class })
public class OpenStackAttachmentPluginTest extends BaseUnitTests {

    private static final String FAKE_JSON_REQUEST = "{\"volumeAttachment\":{\"volumeId\":\"fake-volume-id\",\"device\":\"fake-device\"}}";
    private static final String FAKE_JSON_RESPONSE_FROM_GET_REQUEST = "{\"volumeAttachment\":{\"device\":\"/dev/sdd\","
            + "\"id\":\"fake-instance-id\",\"serverId\":\"fake-compute-id\",\"volumeId\":\"fake-volume-id\"}}";
    private static final String FAKE_JSON_RESPONSE_FROM_POST_REQUEST = "{\"volumeAttachment\": {\"volumeId\": \"fake-volume-id\"}}";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String MAP_METHOD = "map";
    private static final String MESSAGE_STATUS_CODE = "Internal server error.";
    private static final String PREFIX_ENDPOINT = "https://mycloud.domain:8774";
    private static final String JSON_MALFORMED = "{anything:}";
    
    private static final int ERROR_STATUS_CODE = 500;
    
    private OpenStackAttachmentPlugin plugin;
    private OpenStackHttpClient client;

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

        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.when(OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser)))
                .thenReturn(cloudUser.getProjectId());

        String endpoint = generateEndpoint(cloudUser.getProjectId(), order.getComputeId(), null);
        CreateAttachmentResponse response = CreateAttachmentResponse.fromJson(FAKE_JSON_RESPONSE_FROM_POST_REQUEST);
        Mockito.doReturn(response).when(this.plugin).doRequestInstance(Mockito.eq(endpoint),
                Mockito.eq(FAKE_JSON_REQUEST), Mockito.eq(cloudUser));

        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser));

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
        
        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.when(OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser)))
                .thenReturn(cloudUser.getProjectId());

        String endpoint = generateEndpoint(cloudUser.getProjectId(),order.getComputeId(), order.getVolumeId());
        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        // exercise
        this.plugin.deleteInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser));
        
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
        
        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.when(OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser)))
                .thenReturn(cloudUser.getProjectId());

        String endpoint = generateEndpoint(cloudUser.getProjectId(),order.getComputeId(), order.getVolumeId());
        GetAttachmentResponse response = GetAttachmentResponse.fromJson(FAKE_JSON_RESPONSE_FROM_GET_REQUEST);
        Mockito.doReturn(response).when(this.plugin).doGetInstance(Mockito.eq(endpoint), Mockito.eq(cloudUser));
        
        // exercise
        this.plugin.getInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser));
        
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
        } catch (Exception e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the doGetInstance method, it must verify that the
    // call was successful.
    @Test
    public void testDoGetInstance() throws Exception {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String endpoint = generateEndpoint(cloudUser.getProjectId(), TestUtils.FAKE_COMPUTE_ID,
                TestUtils.FAKE_VOLUME_ID);
        
        String json = FAKE_JSON_RESPONSE_FROM_GET_REQUEST;
        Mockito.when(this.client.doGetRequest(Mockito.eq(endpoint), Mockito.eq(cloudUser))).thenReturn(json);
        
        // exercise
        this.plugin.doGetInstance(endpoint, cloudUser);
        
        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(endpoint), Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetAttachmentResponseFrom(json);
    }
    
    // test case: When calling the doGetAttachmentResponseFrom method with a JSON
    // malformed, it must verify that a UnexpectedException was throw.
    @Test
    public void testDoGetAttachmentResponseFromJsonMalformed() {
        // set up
        String json = JSON_MALFORMED;
        String expected = Messages.Error.UNABLE_TO_GET_ATTACHMENT_INSTANCE;

        try {
            // exercise
            this.plugin.doGetAttachmentResponseFrom(json);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doDeleteInstance method and an unexpected error
    // occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoDeleteInstanceFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String endpoint = generateEndpoint(cloudUser.getProjectId(), TestUtils.FAKE_COMPUTE_ID,
                TestUtils.FAKE_VOLUME_ID);

        HttpResponseException expectedException = new HttpResponseException(ERROR_STATUS_CODE, MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doDeleteRequest(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, MAP_METHOD, Mockito.any());

        try {
            // exercise
            this.plugin.doDeleteInstance(endpoint, cloudUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the doDeleteInstance method, it must verify that the
    // call was successful.
    @Test
    public void testDoDeleteInstance() throws Exception {
        /// set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String endpoint = generateEndpoint(cloudUser.getProjectId(), TestUtils.FAKE_COMPUTE_ID,
                TestUtils.FAKE_VOLUME_ID);

        Mockito.doNothing().when(this.client).doDeleteRequest(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        // exercise
        this.plugin.doDeleteInstance(endpoint, cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doDeleteRequest(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
    }
    
    // test case: When calling the doRequestInstance method and an unexpected error
    // occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoRequestInstanceFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String jsonRequest = FAKE_JSON_REQUEST;
        String endpoint = generateEndpoint(cloudUser.getProjectId(), TestUtils.FAKE_COMPUTE_ID,
                TestUtils.FAKE_VOLUME_ID);
        
        HttpResponseException expectedException = new HttpResponseException(ERROR_STATUS_CODE, MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doPostRequest(Mockito.eq(endpoint), Mockito.eq(jsonRequest),
                Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, MAP_METHOD, Mockito.any());
        
        try {
            // exercise
            this.plugin.doRequestInstance(endpoint, jsonRequest, cloudUser);
        } catch (Exception e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the doRequestInstance method, it must verify that the
    // call was successful.
    @Test
    public void testDoRequestInstance() throws Exception {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String jsonRequest = FAKE_JSON_REQUEST;
        String endpoint = generateEndpoint(cloudUser.getProjectId(), TestUtils.FAKE_COMPUTE_ID,
                TestUtils.FAKE_VOLUME_ID);
        
        String jsonResponse = FAKE_JSON_RESPONSE_FROM_POST_REQUEST;
        Mockito.doReturn(jsonResponse).when(this.client).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(jsonRequest), Mockito.eq(cloudUser));

        // exercise
        this.plugin.doRequestInstance(endpoint, jsonRequest, cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(jsonRequest), Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doCreateAttachmentResponseFrom(jsonResponse);
    }
    
    // test case: When calling the doCreateAttachmentResponseFrom method with a JSON
    // malformed, it must verify that a UnexpectedException was throw.
    @Test
    public void testdoCreateAttachmentResponseFromJsonMalformed() {
        // set up
        String json = JSON_MALFORMED;
        String expected = Messages.Error.UNABLE_TO_CREATE_ATTACHMENT;

        try {
            // exercise
            this.plugin.doCreateAttachmentResponseFrom(json);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    private String generateEndpoint(String projectId, String serverId, String volumeId) {
        String endpoint = PREFIX_ENDPOINT + OpenStackAttachmentPlugin.V2_API_ENDPOINT + projectId
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
    
}
