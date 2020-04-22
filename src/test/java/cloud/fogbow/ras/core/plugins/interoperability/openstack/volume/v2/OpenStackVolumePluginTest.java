package cloud.fogbow.ras.core.plugins.interoperability.openstack.volume.v2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpToFogbowExceptionMapper;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;

@PrepareForTest({ DatabaseManager.class, OpenStackCloudUtils.class, OpenStackHttpToFogbowExceptionMapper.class })
public class OpenStackVolumePluginTest extends BaseUnitTests {

    private static final String ANY_VALUE = "anything";
    private static final String AVAILABLE_STATE = "available";
    private static final String PREFIX_ENDPOINT = "https://mycloud.domain:8776";
    private static final String FAKE_CAPABILITIES = "fake-capabilities";
    private static final String FAKE_NO_MATCH_CAPABILITIES = "fake-no-match-capabilities";
    private static final String FAKE_JSON_REQUEST = "{\"volume\":{\"name\":\"fake-order-name\",\"size\":\"30\"}}";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_TYPES_JSON_RESPONSE = "{\"volume_types\":[{\"extra_specs\":{\"fake-capabilities\":\"fake-value\" },\"id\":\"fake-volume-type-id\",\"name\":\"fake-volume-type-name\"}]}";
    private static final String FAKE_VALUE = "fake-value";
    private static final String FAKE_VOLUME_JSON_RESPONSE = "{\"volume\":{\"id\":\"fake-instance-id\",\"name\":\"fake-order-name\",\"size\":\"30\",\"status\":\"available\"}}";
    private static final String FAKE_VOLUME_TYPE_ID = "fake-volume-type-id";
    private static final String JSON_MALFORMED = "{anything:}";
    private static final String MAP_METHOD = "map";
    private static final String MESSAGE_STATUS_CODE = "Internal server error.";
    
    private static final int ERROR_STATUS_CODE = 500;
    
    private OpenStackVolumePlugin plugin;
    private OpenStackHttpClient client;

    @Before
    public void setUp() throws Exception {
        this.testUtils.mockReadOrdersFromDataBase();
        this.client = Mockito.mock(OpenStackHttpClient.class);

        String openstackCloudConfPath = HomeDir.getPath() 
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator 
                + TestUtils.DEFAULT_CLOUD_NAME 
                + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.plugin = Mockito.spy(new OpenStackVolumePlugin(openstackCloudConfPath));
        this.plugin.setClient(this.client);
    }
    
    // test case: When calling the isReady method with the cloud states available,
    // this means that the state of volume is READY and it must return true.
    @Test
    public void testIsReady() {
        // set up
        String cloudState = OpenStackStateMapper.AVAILABLE_STATUS;

        // exercise
        boolean status = this.plugin.isReady(cloudState);

        // verify
        Assert.assertTrue(status);
    }
    
    // test case: When calling the isReady method with the cloud states different
    // than available, this means that the state of volume is not READY and it must
    // return false.
    @Test
    public void testNotIsReady() {
        // set up
        String[] cloudStates = { OpenStackStateMapper.CREATING_STATUS, OpenStackStateMapper.ERROR_STATUS,
                OpenStackStateMapper.IN_USE_STATUS };

        for (String cloudState : cloudStates) {
            // exercise
            boolean status = this.plugin.isReady(cloudState);

            // verify
            Assert.assertFalse(status);
        }
    }
    
    // test case: When calling the hasFailed method with any cloud states errors,
    // this means that the state of the volume is FAILED and it must return true.
    @Test
    public void testHasFailed() {
        // set up
        String[] cloudStates = { OpenStackStateMapper.ERROR_BACKING_UP_STATUS,
                OpenStackStateMapper.ERROR_DELETING_STATUS, OpenStackStateMapper.ERROR_EXTENDING_STATUS,
                OpenStackStateMapper.ERROR_RESTORING_STATUS, OpenStackStateMapper.ERROR_STATUS };

        for (String cloudState : cloudStates) {
            // exercise
            boolean status = this.plugin.hasFailed(cloudState);

            // verify
            Assert.assertTrue(status);
        }
    }
    
    // test case: When calling the hasFailed method with the cloud states different
    // than any error, this means that the state of the volume is not FAILED and it
    // must return false.
    @Test
    public void testNotHasFailed() {
        // set up
        String[] cloudStates = { OpenStackStateMapper.CREATING_STATUS, OpenStackStateMapper.AVAILABLE_STATUS,
                OpenStackStateMapper.IN_USE_STATUS };

        for (String cloudState : cloudStates) {
            // exercise
            boolean status = this.plugin.hasFailed(cloudState);

            // verify
            Assert.assertFalse(status);
        }
    }
    
    // test case: When invoking the requestInstance method with a valid volume
    // request and a cloud user, it must verify that the call was successful.
    @Test
    public void testRequestInstance() throws FogbowException {
        // set up
        VolumeOrder order = this.testUtils.createLocalVolumeOrder();
        OpenStackV3User cloudUser = createOpenStackUser();

        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.when(OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser))).thenCallRealMethod();

        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.VOLUMES, null);
        GetVolumeResponse response = GetVolumeResponse.fromJson(FAKE_VOLUME_JSON_RESPONSE);
        Mockito.doReturn(response).when(this.plugin).doRequestInstance(Mockito.eq(endpoint),
                Mockito.eq(FAKE_JSON_REQUEST), Mockito.eq(cloudUser));

        Mockito.doNothing().when(this.plugin).setAllocationToOrder(Mockito.eq(order));

        // exercise
        this.plugin.requestInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).findVolumeTypeId(
                Mockito.eq(order.getRequirements()), Mockito.eq(cloudUser.getProjectId()), Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getPrefixEndpoint(Mockito.eq(cloudUser.getProjectId()));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).generateJsonRequest(
                Mockito.eq(String.valueOf(order.getVolumeSize())), Mockito.eq(order.getName()), Mockito.anyString());
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(endpoint),
                Mockito.eq(FAKE_JSON_REQUEST), Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).setAllocationToOrder(Mockito.eq(order));
    }

    // test case: Verify if allocation is set to the order properly
    @Test
    public void testSetAllocationToOrder() {
        // set up
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        int diskSize = 1;
        VolumeAllocation volumeAllocation = new VolumeAllocation(diskSize);

        Mockito.doReturn(diskSize).when(volumeOrder).getVolumeSize();
        Mockito.doCallRealMethod().when(volumeOrder).setActualAllocation(Mockito.any(VolumeAllocation.class));
        Mockito.doReturn(volumeAllocation).when(volumeOrder).getActualAllocation();

        // exercise
        this.plugin.setAllocationToOrder(volumeOrder);

        // verify
        Mockito.verify(volumeOrder, Mockito.times(testUtils.RUN_ONCE))
                .setActualAllocation(Mockito.any());

        Assert.assertEquals(volumeAllocation.getStorage(), volumeOrder.getActualAllocation().getStorage());
    }
    
    // test case: When invoking the getInstance method with a valid volume
    // request and a cloud user, it must verify that the call was successful.
    @Test
    public void testGetInstance() throws FogbowException {
        // set up
        VolumeOrder order = this.testUtils.createLocalVolumeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);

        OpenStackV3User cloudUser = createOpenStackUser();
        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.when(OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser)))
                .thenCallRealMethod();

        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.VOLUMES,
                order.getInstanceId());
        
        VolumeInstance instance = createVolumeInstance();
        Mockito.doReturn(instance).when(this.plugin).doGetInstance(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        // exercise
        this.plugin.getInstance(order, cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenStackCloudUtils.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .getPrefixEndpoint(Mockito.eq(cloudUser.getProjectId()));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
    }
    
    // test case: When invoking the deleteInstance method with a valid volume
    // request and a cloud user, it must verify that the call was successful.
    @Test
    public void testDeleteInstance() throws FogbowException {
        // set up
        VolumeOrder order = this.testUtils.createLocalVolumeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);

        OpenStackV3User cloudUser = createOpenStackUser();
        PowerMockito.mockStatic(OpenStackCloudUtils.class);
        PowerMockito.when(OpenStackCloudUtils.getProjectIdFrom(Mockito.eq(cloudUser)))
                .thenCallRealMethod();

        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.VOLUMES,
                order.getInstanceId());
        
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
    
    // test case: When calling the doDeleteInstance method and an unexpected error
    // occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoDeleteInstanceFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.VOLUMES,
                TestUtils.FAKE_INSTANCE_ID);

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
        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.VOLUMES,
                TestUtils.FAKE_INSTANCE_ID);

        Mockito.doNothing().when(this.client).doDeleteRequest(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        // exercise
        this.plugin.doDeleteInstance(endpoint, cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doDeleteRequest(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
    }
    
    // test case: When calling the doGetInstance method, it must verify that the
    // call was successful.
    @Test
    public void testDoGetInstance() throws FogbowException {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.VOLUMES,
                TestUtils.FAKE_INSTANCE_ID);

        String json = FAKE_VOLUME_JSON_RESPONSE;
        Mockito.doReturn(json).when(this.plugin).doGetResponseFromCloud(Mockito.eq(endpoint), Mockito.eq(cloudUser));
        
        GetVolumeResponse response = GetVolumeResponse.fromJson(json);
        Mockito.doReturn(response).when(this.plugin).doGetVolumeResponseFrom(json);

        // exercise
        this.plugin.doGetInstance(endpoint, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetResponseFromCloud(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetVolumeResponseFrom(Mockito.eq(json));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .buildVolumeInstanceFrom(Mockito.eq(response));
    }
    
    // test case: When calling the doRequestInstance method and an unexpected error
    // occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoRequestInstanceFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String jsonRequest = FAKE_JSON_REQUEST;
        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.VOLUMES,
                TestUtils.FAKE_INSTANCE_ID);

        HttpResponseException expectedException = new HttpResponseException(ERROR_STATUS_CODE, MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(jsonRequest), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, MAP_METHOD, Mockito.any());

        try {
            // exercise
            this.plugin.doRequestInstance(endpoint, jsonRequest, cloudUser);
            Assert.fail();
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
        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.VOLUMES,
                TestUtils.FAKE_INSTANCE_ID);

        String jsonResponse = FAKE_VOLUME_JSON_RESPONSE;
        Mockito.doReturn(jsonResponse).when(this.client).doPostRequest(Mockito.eq(endpoint), Mockito.eq(jsonRequest),
                Mockito.eq(cloudUser));

        // exercise
        this.plugin.doRequestInstance(endpoint, jsonRequest, cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doPostRequest(Mockito.eq(endpoint),
                Mockito.eq(jsonRequest), Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetVolumeResponseFrom(jsonResponse);
    }
    
    // test case: When calling the doGetVolumeResponseFrom method with a JSON
    // malformed, it must verify that a UnexpectedException was throw.
    @Test
    public void testDoGetVolumeResponseFromJsonMalformed() {
        // set up
        String json = JSON_MALFORMED;
        String expected = Messages.Error.ERROR_WHILE_GETTING_VOLUME_INSTANCE;

        try {
            // exercise
            this.plugin.doGetVolumeResponseFrom(json);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetResponseFromCloud method and an unexpected error
    // occurs, it must verify that the map method of the
    // OpenStackHttpToFogbowExceptionMapper class has been called.
    @Test
    public void testDoGetResponseFromCloudFail() throws Exception {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.VOLUMES,
                TestUtils.FAKE_INSTANCE_ID);

        HttpResponseException expectedException = new HttpResponseException(ERROR_STATUS_CODE, MESSAGE_STATUS_CODE);
        Mockito.doThrow(expectedException).when(this.client).doGetRequest(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        PowerMockito.mockStatic(OpenStackHttpToFogbowExceptionMapper.class);
        PowerMockito.doCallRealMethod().when(OpenStackHttpToFogbowExceptionMapper.class, MAP_METHOD, Mockito.any());
        
        try {
            // exercise
            this.plugin.doGetResponseFromCloud(endpoint, cloudUser);
            Assert.fail();
        } catch (Exception e) {
            // verify
            PowerMockito.verifyStatic(OpenStackHttpToFogbowExceptionMapper.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenStackHttpToFogbowExceptionMapper.map(Mockito.eq(expectedException));
        }
    }
    
    // test case: When calling the findVolumeTypeId method, it must verify
    // that the call was successful.
    @Test
    public void testFindVolumeTypeId() throws FogbowException {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String projectId = FAKE_PROJECT_ID;
        Map requirements = new HashMap();
        requirements.put(FAKE_CAPABILITIES, FAKE_VALUE);

        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.TYPES, null);
        String json = FAKE_TYPES_JSON_RESPONSE;
        Mockito.doReturn(json).when(this.plugin).doGetResponseFromCloud(Mockito.eq(endpoint), Mockito.eq(cloudUser));
        
        String expected = FAKE_VOLUME_TYPE_ID;

        // exercise
        String volumeTypeId = this.plugin.findVolumeTypeId(requirements, projectId, cloudUser);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getPrefixEndpoint(Mockito.eq(projectId));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetResponseFromCloud(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetAllTypesResponseFrom(Mockito.eq(json));
        
        Assert.assertEquals(expected, volumeTypeId);
    }
    
    // test case: When calling the findVolumeTypeId method with requirements that do
    // not match cloud volume types, a NoAvailableResourcesException must be thrown.
    @Test
    public void testFindVolumeTypeIdNoMatchRequirements() throws FogbowException {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String projectId = FAKE_PROJECT_ID;
        Map requirements = new HashMap();
        requirements.put(FAKE_NO_MATCH_CAPABILITIES, ANY_VALUE);

        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.TYPES, null);
        String json = FAKE_TYPES_JSON_RESPONSE;
        Mockito.doReturn(json).when(this.plugin).doGetResponseFromCloud(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        String expected = Messages.Exception.UNABLE_TO_MATCH_REQUIREMENTS;

        try {
            // exercise
            this.plugin.findVolumeTypeId(requirements, projectId, cloudUser);
            Assert.fail();
        } catch (NoAvailableResourcesException e) {
            // verify
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getPrefixEndpoint(Mockito.eq(projectId));
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetResponseFromCloud(Mockito.eq(endpoint),
                    Mockito.eq(cloudUser));
            Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetAllTypesResponseFrom(Mockito.eq(json));
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetAllTypesResponseFrom method with a JSON
    // malformed, it must verify that a UnexpectedException was throw.
    @Test
    public void testDoGetAllTypesResponseFromJsonMalformed() {
        // set up
        String json = JSON_MALFORMED;
        String expected = Messages.Error.ERROR_WHILE_PROCESSING_VOLUME_REQUIREMENTS;

        try {
            // exercise
            this.plugin.doGetAllTypesResponseFrom(json);
            Assert.fail();
        } catch (UnexpectedException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When calling the doGetResponseFromCloud method, it must verify
    // that the call was successful.
    @Test
    public void testDoGetResponseFromCloud() throws Exception {
        // set up
        OpenStackV3User cloudUser = createOpenStackUser();
        String endpoint = generateEndpoint(cloudUser.getProjectId(), OpenStackVolumePlugin.VOLUMES,
                TestUtils.FAKE_INSTANCE_ID);

        String json = FAKE_VOLUME_JSON_RESPONSE;
        Mockito.doReturn(json).when(this.client).doGetRequest(Mockito.eq(endpoint), Mockito.eq(cloudUser));

        // exercise
        this.plugin.doGetResponseFromCloud(endpoint, cloudUser);

        // verify
        Mockito.verify(this.client, Mockito.times(TestUtils.RUN_ONCE)).doGetRequest(Mockito.eq(endpoint),
                Mockito.eq(cloudUser));
    }
    
    private VolumeInstance createVolumeInstance() {
        String id = TestUtils.FAKE_INSTANCE_ID;
        String cloudState = AVAILABLE_STATE;
        String name = TestUtils.FAKE_ORDER_NAME;
        int size = TestUtils.DISK_VALUE;
        return new VolumeInstance(id, cloudState, name, size);
    }
    
    private String generateEndpoint(String projectId, String resource, String instanceId) {
        String endpoint = PREFIX_ENDPOINT 
                + OpenStackVolumePlugin.V2_API_ENDPOINT 
                + projectId
                + resource;

        if (instanceId != null) {
            return endpoint + OpenStackVolumePlugin.ENDPOINT_SEPARATOR + instanceId;
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

}
