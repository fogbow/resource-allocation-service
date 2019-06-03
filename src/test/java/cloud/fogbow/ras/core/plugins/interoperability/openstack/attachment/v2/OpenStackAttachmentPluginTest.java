package cloud.fogbow.ras.core.plugins.interoperability.openstack.attachment.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.common.models.OpenStackV3User;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Properties;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SharedOrderHolders.class})
public class OpenStackAttachmentPluginTest extends BaseUnitTests {

    private static final String COMPUTE_NOVAV2_NETWORK_KEY = "compute_novav2_network_id";
    private static final String FAKE_ENDPOINT = "fake-endpoint";
    private static final String FAKE_NET_ID = "fake-net-id";
    private static final String FAKE_SERVER_ID = "fake-server-id";
    private static final String FAKE_VOLUME_ID = "fake-volume-id";
    private static final String FAKE_INSTANCE_ID = "fake-instance-id";
    private static final String FAKE_POST_REQUEST_BODY = "{\"volumeAttachment\":{\"volumeId\":\"" + FAKE_INSTANCE_ID + "\"}}";
    private static final String FAKE_DEVICE = "/dev/sdd";
    private static final String FAKE_GET_REQUEST_BODY = "{\"volumeAttachment\": {\"device\": \"" + FAKE_DEVICE + "\",\"id\": \""
            + FAKE_INSTANCE_ID + "\",\"serverId\": \"" + FAKE_SERVER_ID + "\",\"volumeId\": \"" + FAKE_VOLUME_ID + "\"}}";
    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";
    private static final String FAKE_ID_PROVIDER = "fake-id-provider";
    private static final String FAKE_PROVIDER = "fake-provider";
    private static final String DEFAULT_CLOUD = "default";
    private AttachmentOrder attachmentOrder;
    private OpenStackAttachmentPlugin openStackAttachmentPlugin;
    private OpenStackV3User localUserAttributes;
    private OpenStackHttpClient client;
    private ArgumentCaptor<String> argString = ArgumentCaptor.forClass(String.class);
    private ArgumentCaptor<OpenStackV3User> argToken = ArgumentCaptor.forClass(OpenStackV3User.class);
    private SharedOrderHolders sharedOrderHolders;

    @Before
    public void setUp() throws InvalidParameterException, UnexpectedException {
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        Properties properties = propertiesHolder.getProperties();
        properties.put(OpenStackAttachmentPlugin.COMPUTE_NOVAV2_URL_KEY, FAKE_ENDPOINT);
        properties.put(COMPUTE_NOVAV2_NETWORK_KEY, FAKE_NET_ID);

        this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);

        Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
                .thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());

        this.localUserAttributes = new OpenStackV3User(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);
        this.attachmentOrder = createAttachmentOrder();

        String cloudConfPath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator
                + DEFAULT_CLOUD + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.openStackAttachmentPlugin = new OpenStackAttachmentPlugin(cloudConfPath);
        this.openStackAttachmentPlugin.setProperties(properties);
        this.client = Mockito.mock(OpenStackHttpClient.class);
        this.openStackAttachmentPlugin.setClient(this.client);
    }

    //test case: Check if requestInstance is returning the instanceId from Json response properly.
    @Test
    public void testRequestInstance() throws FogbowException, HttpResponseException {
        //set up
        Mockito.doReturn(FAKE_POST_REQUEST_BODY).when(this.client).doPostRequest(
                Mockito.anyString(), Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        //exercise
        String instanceId = this.openStackAttachmentPlugin.requestInstance(this.attachmentOrder, this.localUserAttributes);

        //verify
        Assert.assertEquals(FAKE_INSTANCE_ID, instanceId);
    }

    //test case: Check if requestInstance is properly forwarding UnexpectedException thrown by doPostRequest.
    @Test(expected = UnexpectedException.class)
    public void testRequestInstanceThrowsUnexpectedException()
            throws FogbowException, HttpResponseException {
        //set up
        int unknownStatusCode = -1;
        HttpResponseException httpResponseException = new HttpResponseException(unknownStatusCode, "");
        Mockito.doThrow(httpResponseException).when(this.client).doPostRequest(Mockito.anyString(),
                Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        //exercise/verify
        this.openStackAttachmentPlugin.requestInstance(this.attachmentOrder, this.localUserAttributes);
    }

    //test case: Check if HttpDeleteRequest parameters are correct according to the deleteInstance call parameters
    @Test
    public void testDeleteInstance() throws FogbowException, IOException, SecurityException, IllegalArgumentException {
        //set up
        Mockito.doNothing().when(this.client).doDeleteRequest(this.argString.capture(), this.argToken.capture());
        String expectedEndpoint = FAKE_ENDPOINT + "/v2/" + FAKE_PROJECT_ID + "/servers/" + FAKE_SERVER_ID
                + "/os-volume_attachments" + "/" + FAKE_VOLUME_ID;

        //exercise
        this.openStackAttachmentPlugin.deleteInstance(this.attachmentOrder, this.localUserAttributes);

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
        this.openStackAttachmentPlugin.deleteInstance(this.attachmentOrder, this.localUserAttributes);
    }

    //test case: Check if an attachment is correctly built according to the JSON returned by the getRequest
    @Test
    public void testGetInstance()
            throws FogbowException, HttpResponseException {
        //setup
        Mockito.doReturn(FAKE_GET_REQUEST_BODY).when(this.client).doGetRequest(Mockito.anyString(),
                Mockito.eq(this.localUserAttributes));
        String openStackState = "";

        //exercise
        AttachmentInstance attachmentInstance = this.openStackAttachmentPlugin.getInstance(this.attachmentOrder, this.localUserAttributes);

        //verify
        Assert.assertEquals(FAKE_DEVICE, attachmentInstance.getDevice());
        Assert.assertEquals(FAKE_SERVER_ID, attachmentInstance.getComputeId());
        Assert.assertEquals(FAKE_VOLUME_ID, attachmentInstance.getVolumeId());
        Assert.assertEquals(FAKE_INSTANCE_ID, attachmentInstance.getId());
        Assert.assertEquals(openStackState, attachmentInstance.getCloudState());
    }

    //test case: Check if getInstance is properly forwarding UnexpectedException thrown by getInstance.
    @Test(expected = UnexpectedException.class)
    public void testGetInstanceThrowsUnexpectedException() throws FogbowException, HttpResponseException {
        //set up
        Mockito.doThrow(UnexpectedException.class).when(this.client)
                .doGetRequest(Mockito.anyString(), Mockito.any(OpenStackV3User.class));

        //exercise/verify
        this.openStackAttachmentPlugin.getInstance(this.attachmentOrder, this.localUserAttributes);
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

    private AttachmentOrder createAttachmentOrder() {
        String instanceId = FAKE_INSTANCE_ID;
        SystemUser requester = new SystemUser(FAKE_USER_ID, FAKE_NAME, FAKE_ID_PROVIDER);
        ComputeOrder computeOrder = new ComputeOrder();
        VolumeOrder volumeOrder = new VolumeOrder();
        computeOrder.setSystemUser(requester);
        computeOrder.setProvider(FAKE_PROVIDER);
        computeOrder.setCloudName(DEFAULT_CLOUD);
        computeOrder.setInstanceId(FAKE_SERVER_ID);
        computeOrder.setOrderStateInTestMode(OrderState.FULFILLED);
        volumeOrder.setSystemUser(requester);
        volumeOrder.setProvider(FAKE_PROVIDER);
        volumeOrder.setCloudName(DEFAULT_CLOUD);
        volumeOrder.setInstanceId(FAKE_VOLUME_ID);
        volumeOrder.setOrderStateInTestMode(OrderState.FULFILLED);
        this.sharedOrderHolders.getActiveOrdersMap().put(computeOrder.getId(), computeOrder);
        this.sharedOrderHolders.getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);
        AttachmentOrder attachmentOrder = new AttachmentOrder(computeOrder.getId(), volumeOrder.getId(), FAKE_DEVICE);
        attachmentOrder.setInstanceId(instanceId);
        this.sharedOrderHolders.getActiveOrdersMap().put(attachmentOrder.getId(), attachmentOrder);
        return attachmentOrder;
    }
}
