package cloud.fogbow.ras.core.cloudconnector;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import cloud.fogbow.ras.api.http.response.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.InteroperabilityPluginInstantiator;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.plugins.interoperability.AttachmentPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.ComputePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.ImagePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.NetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.OrderPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.PublicIpPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.QuotaPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.SecurityRulePlugin;
import cloud.fogbow.ras.core.plugins.interoperability.VolumePlugin;
import cloud.fogbow.ras.core.plugins.mapper.SystemToCloudMapperPlugin;

@PrepareForTest({ CloudConnectorFactory.class, DatabaseManager.class })
public class LocalCloudConnectorTest extends BaseUnitTests {

    private static final String ANY_VALUE = "anything";

    private LocalCloudConnector localCloudConnector;
    private AttachmentPlugin attachmentPlugin;
    private ComputePlugin computePlugin;
    private ImagePlugin imagePlugin;
    private SystemToCloudMapperPlugin mapperPlugin;
    private NetworkPlugin networkPlugin;
    private PublicIpPlugin publicIpPlugin;
    private SecurityRulePlugin securityRulePlugin;
    private VolumePlugin volumePlugin;
    private QuotaPlugin quotaPlugin;

    @Before
    public void setUp() throws FogbowException {
        // mocking databaseManager
        this.testUtils.mockReadOrdersFromDataBase();

        // mocking resource plugins
        this.mockResourcePlugins();

        this.localCloudConnector = Mockito
                .spy(new LocalCloudConnector(mockPluginsInstantiator(), TestUtils.DEFAULT_CLOUD_NAME));
    }
    
    // test case: When invoking the requestInstance method with an order it
    // must call the doRequestInstance method, and confirm in auditRequest
    // the CREATE operation.
    @Test
    public void testRequestInstance() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        Mockito.doReturn(TestUtils.FAKE_INSTANCE_ID).when(this.localCloudConnector).doRequestInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));

        // exercise
        this.localCloudConnector.requestInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doRequestInstance(Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.CREATE), Mockito.any(ResourceType.class), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the requestInstance method with a compute order and
    // returning a null instance ID, it must throw an InternalServerErrorException and call
    // the auditRequest method confirming its requested operation and resource type.
    @Test
    public void testRequestInstanceFailWithComputeOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalComputeOrder();

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        String expected = Messages.Exception.NULL_VALUE_RETURNED;

        try {
            // exercise
            this.localCloudConnector.requestInstance(order);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.COMPUTE), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the requestInstance method with a volume order and
    // returning a null instance ID, it must throw an InternalServerErrorException and call
    // the auditRequest method confirming its requested operation and resource type.
    @Test
    public void testRequestInstanceFailWithVolumeOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalVolumeOrder();

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        String expected = Messages.Exception.NULL_VALUE_RETURNED;

        try {
            // exercise
            this.localCloudConnector.requestInstance(order);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.VOLUME), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the requestInstance method with an attachment order
    // and returning a null instance ID, it must throw an InternalServerErrorException and
    // call the auditRequest method confirming its requested operation and resource
    // type.
    @Test
    public void testRequestInstanceFailWithAttachmentOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalAttachmentOrder(this.testUtils.createLocalComputeOrder(),
                this.testUtils.createLocalVolumeOrder());

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        String expected = Messages.Exception.NULL_VALUE_RETURNED;

        try {
            // exercise
            this.localCloudConnector.requestInstance(order);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.ATTACHMENT), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the requestInstance method with a network order and
    // returning a null instance ID, it must throw an InternalServerErrorException and call
    // the auditRequest method confirming its requested operation and resource type.
    @Test
    public void testRequestInstanceFailWithNetworkOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalNetworkOrder();

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        String expected = Messages.Exception.NULL_VALUE_RETURNED;

        try {
            // exercise
            this.localCloudConnector.requestInstance(order);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.NETWORK), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the requestInstance method with a public IP order
    // and returning a null instance ID, it must throw an InternalServerErrorException and
    // call the auditRequest method confirming its requested operation and resource
    // type.
    @Test
    public void testRequestInstanceFailWithPublicIpOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        String expected = Messages.Exception.NULL_VALUE_RETURNED;

        try {
            // exercise
            this.localCloudConnector.requestInstance(order);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.PUBLIC_IP), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the getInstance method with a valid compute order it
    // must call the doGetInstance method, and confirm in auditRequest the GET
    // operation and the COMPUTE resource type.
    @Test
    public void testGetInstanceWithComputeOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalComputeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        OrderInstance instance = new ComputeInstance(TestUtils.FAKE_INSTANCE_ID);
        Mockito.doReturn(instance).when(this.localCloudConnector).doGetInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));

        // exercise
        this.localCloudConnector.getInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.COMPUTE), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getInstance method with a valid volume order it
    // must call the doGetInstance method, and confirm in auditRequest the GET
    // operation and the VOLUME resource type.
    @Test
    public void testGetInstanceWithVolumeOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalVolumeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        OrderInstance instance = new VolumeInstance(TestUtils.FAKE_INSTANCE_ID);
        Mockito.doReturn(instance).when(this.localCloudConnector).doGetInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));

        // exercise
        this.localCloudConnector.getInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doGetInstance(Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.VOLUME), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getInstance method with a valid attachment order
    // it must call the doGetInstance method, and confirm in auditRequest the GET
    // operation and the ATTACHMENT resource type.
    @Test
    public void testGetInstanceWithAttachmentOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalAttachmentOrder(this.testUtils.createLocalComputeOrder(), this.testUtils.createLocalVolumeOrder());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        OrderInstance instance = new AttachmentInstance(TestUtils.FAKE_INSTANCE_ID);
        Mockito.doReturn(instance).when(this.localCloudConnector).doGetInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));

        // exercise
        this.localCloudConnector.getInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doGetInstance(Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.ATTACHMENT), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getInstance method with a valid network order
    // it must call the doGetInstance method, and confirm in auditRequest the GET
    // operation and the NETWORK resource type.
    @Test
    public void testGetInstanceWithNetworkOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalNetworkOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        OrderInstance instance = new NetworkInstance(TestUtils.FAKE_INSTANCE_ID);
        Mockito.doReturn(instance).when(this.localCloudConnector).doGetInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));

        // exercise
        this.localCloudConnector.getInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doGetInstance(Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.NETWORK), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getInstance method with a valid public IP order
    // it must call the doGetInstance method, and confirm in auditRequest the GET
    // operation and the PUBLIC_IP resource type.
    @Test
    public void testGetInstanceWithPublicIpOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        OrderInstance instance = new PublicIpInstance(TestUtils.FAKE_INSTANCE_ID);
        Mockito.doReturn(instance).when(this.localCloudConnector).doGetInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));

        // exercise
        this.localCloudConnector.getInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.PUBLIC_IP), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getInstance method with a valid compute order
    // and an error occurs while getting the instance in the cloud, it must throw an
    // InstanceNotFoundException and call the auditRequest method confirming its requested
    // operation and resource type.
    @Test
    public void testGetInstanceFailWithComputeOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalComputeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.localCloudConnector.getInstance(order);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.GET), Mockito.eq(ResourceType.COMPUTE), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the getInstance method with a valid volume order
    // and an error occurs while getting the instance in the cloud, it must throw an
    // InstanceNotFoundException and call the auditRequest method confirming its requested
    // operation and resource type.
    @Test
    public void testGetInstanceFailWithVolumeOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalVolumeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.localCloudConnector.getInstance(order);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.GET), Mockito.eq(ResourceType.VOLUME), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the getInstance method with a valid attachment order
    // and an error occurs while getting the instance in the cloud, it must throw an
    // InstanceNotFoundException and call the auditRequest method confirming its requested
    // operation and resource type.
    @Test
    public void testGetInstanceFailWithAttachmentOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalAttachmentOrder(this.testUtils.createLocalComputeOrder(),
                this.testUtils.createLocalVolumeOrder());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.localCloudConnector.getInstance(order);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.GET), Mockito.eq(ResourceType.ATTACHMENT), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the getInstance method with a valid network order
    // and an error occurs while getting the instance in the cloud, it must throw an
    // InstanceNotFoundException and call the auditRequest method confirming its requested
    // operation and resource type.
    @Test
    public void testGetInstanceFailWithNetworkOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalNetworkOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.localCloudConnector.getInstance(order);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.GET), Mockito.eq(ResourceType.NETWORK), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the getInstance method with a valid public IP order
    // and an error occurs while getting the instance in the cloud, it must throw an
    // InstanceNotFoundException and call the auditRequest method confirming its requested
    // operation and resource type.
    @Test
    public void testGetInstanceFailWithPublicIpOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.localCloudConnector.getInstance(order);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.GET), Mockito.eq(ResourceType.PUBLIC_IP), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the getInstance method with a compute order with
    // instance ID null it must call the doGetInstance method, and confirm in
    // auditRequest the GET operation and the COMPUTE resource type.
    @Test
    public void testGetInstanceEmptyForComputeOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalComputeOrder();
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.getInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doGetInstance(Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.COMPUTE), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getInstance method with a volume order with
    // instance ID null it must call the doGetInstance method, and confirm in
    // auditRequest the GET operation and the VOLUME resource type.
    @Test
    public void testGetInstanceEmptyForVolumeOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalVolumeOrder();
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.getInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.VOLUME), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getInstance method with an attachment order with
    // instance ID null it must call the doGetInstance method, and confirm in
    // auditRequest the GET operation and the ATTACHMENT resource type.
    @Test
    public void testGetInstanceEmptyForAttachmentOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalAttachmentOrder(this.testUtils.createLocalComputeOrder(),
                this.testUtils.createLocalVolumeOrder());
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.getInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.ATTACHMENT), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getInstance method with a network order with
    // instance ID null it must call the doGetInstance method, and confirm in
    // auditRequest the GET operation and the NETWORK resource type.
    @Test
    public void testGetInstanceEmptyForNetworkOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalNetworkOrder();
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.getInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.NETWORK), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getInstance method with a public IP order with
    // instance ID null it must call the doGetInstance method, and confirm in
    // auditRequest the GET operation and the PUBLIC_IP resource type.
    @Test
    public void testGetInstanceEmptyForPublicIpOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setOrderState(OrderState.FULFILLED);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.getInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.PUBLIC_IP), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the deleteInstance method with a valid compute order
    // it must call the doDeleteInstance method, and confirm in auditRequest the
    // DELETE operation and the COMPUTE resource type.
    @Test
    public void testDeleteInstanceWithComputeOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalComputeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.deleteInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.DELETE), Mockito.eq(ResourceType.COMPUTE), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the deleteInstance method with a valid volume order
    // it must call the doDeleteInstance method, and confirm in auditRequest the
    // DELETE operation and the VOLUME resource type.
    @Test
    public void testDeleteInstanceWithVolumeOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalVolumeOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.deleteInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.DELETE), Mockito.eq(ResourceType.VOLUME), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the deleteInstance method with a valid attachment
    // order it must call the doDeleteInstance method, and confirm in auditRequest
    // the DELETE operation and the ATTACHMENT resource type.
    @Test
    public void testDeleteInstanceWithAttachmentOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalAttachmentOrder(this.testUtils.createLocalComputeOrder(),
                this.testUtils.createLocalVolumeOrder());
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.deleteInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.DELETE), Mockito.eq(ResourceType.ATTACHMENT), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the deleteInstance method with a valid network order
    // it must call the doDeleteInstance method, and confirm in auditRequest the
    // DELETE operation and the NETWORK resource type.
    @Test
    public void testDeleteInstanceWithNetworkOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalNetworkOrder();
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.deleteInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.DELETE), Mockito.eq(ResourceType.NETWORK), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the deleteInstance method with a valid public IP
    // order it must call the doDeleteInstance method, and confirm in auditRequest
    // the DELETE operation and the PUBLIC_IP resource type.
    @Test
    public void testDeleteInstanceWithPublicIpOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        order.setInstanceId(TestUtils.FAKE_INSTANCE_ID);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.deleteInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.DELETE), Mockito.eq(ResourceType.PUBLIC_IP), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the deleteInstance method with an order containing a
    // null instance ID, it must call the doDeleteInstance method, but it should do
    // nothing, confirming the DELETE operation in auditRequest.
    @Test
    public void testDeleteInstanceWithNullInstanceID() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);
        Assert.assertNull(order.getInstanceId());

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        OrderPlugin plugin = Mockito.mock(OrderPlugin.class);
        Mockito.doReturn(plugin).when(this.localCloudConnector).checkOrderCastingAndSetPlugin(Mockito.eq(order),
                Mockito.any(ResourceType.class));

        // exercise
        this.localCloudConnector.deleteInstance(order);

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.eq(order), Mockito.eq(cloudUser));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.DELETE), Mockito.any(ResourceType.class), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the deleteInstance method with an order already
    // removed it must throw an InstanceNotFoundException, and confirm in
    // auditRequest the DELETE operation.
    @Test
    public void testDeleteInstanceWithOrderAlreadyRemoved() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getInstanceId()).thenReturn(TestUtils.FAKE_INSTANCE_ID);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        OrderPlugin plugin = Mockito.mock(OrderPlugin.class);
        Mockito.doReturn(plugin).when(this.localCloudConnector).checkOrderCastingAndSetPlugin(Mockito.eq(order),
                Mockito.any(ResourceType.class));

        Mockito.doThrow(InstanceNotFoundException.class).when(plugin).deleteInstance(Mockito.any(), Mockito.any());

        try {
            // exercise
            this.localCloudConnector.deleteInstance(order);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.DELETE), Mockito.any(ResourceType.class), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the getUserQuota method it must confirm in auditRequest the
    // GET_USER_QUOTA operation of the QUOTA resource type.
    @Test
    public void testGetUserQuota() throws FogbowException {
        // set up
        Mockito.doReturn(this.computePlugin).when(this.localCloudConnector).checkOrderCastingAndSetPlugin(Mockito.any(),
                Mockito.any());

        ResourceQuota resourceQuota = Mockito.mock(ResourceQuota.class);
        Mockito.doReturn(resourceQuota).when(this.quotaPlugin).getUserQuota(Mockito.any(CloudUser.class));

        // exercise
        this.localCloudConnector.getUserQuota(this.testUtils.createSystemUser());

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.QUOTA), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }

    // test case: When invoking the getAllImages method with a valid system user, it
    // must call the doGetAllImages method and confirm in auditRequest the GET_ALL
    // operation of the IMAGE resource type.
    @Test
    public void testGetAllImages() throws FogbowException {
        // exercise
        this.localCloudConnector.getAllImages(this.testUtils.createSystemUser());

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doGetAllImages(Mockito.any(CloudUser.class));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET_ALL), Mockito.eq(ResourceType.IMAGE), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getAllImages method and an error occurs while
    // getting the images in the cloud, it must throw an exception and confirm in
    // auditRequest the GET_ALL operation of the IMAGE resource type.
    @Test
    public void testGetAllImagesFail() throws FogbowException {
        // set up
        Mockito.doThrow(Throwable.class).when(this.localCloudConnector).doGetAllImages(Mockito.any(CloudUser.class));

        try {
            // exercise
            this.localCloudConnector.getAllImages(this.testUtils.createSystemUser());
            Assert.fail();
        } catch (Throwable e) {
            // verify
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                    .doGetAllImages(Mockito.any(CloudUser.class));
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.GET_ALL), Mockito.eq(ResourceType.IMAGE), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the getImage method with a valid system user, it
    // must call the doGetImage method and confirm in auditRequest the GET
    // operation of the IMAGE resource type.
    @Test
    public void testGetImage() throws FogbowException {
        // set up
        ImageInstance imageInstance = Mockito.mock(ImageInstance.class);
        Mockito.doReturn(imageInstance).when(this.localCloudConnector).doGetImage(Mockito.anyString(),
                Mockito.any(CloudUser.class));

        // exercise
        this.localCloudConnector.getImage(TestUtils.FAKE_IMAGE_ID, this.testUtils.createSystemUser());

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doGetImage(Mockito.anyString(),
                Mockito.any(CloudUser.class));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET), Mockito.eq(ResourceType.IMAGE), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getImage method and an error occurs while
    // getting a specific image in the cloud, it must throw an exception and
    // confirm in auditRequest the GET operation of the IMAGE resource type.
    @Test
    public void testGetImageFail() throws FogbowException {
        // set up
        Mockito.doThrow(Throwable.class).when(this.localCloudConnector).doGetImage(Mockito.anyString(),
                Mockito.any(CloudUser.class));
        try {
            // exercise
            this.localCloudConnector.getImage(TestUtils.FAKE_IMAGE_ID, this.testUtils.createSystemUser());
            Assert.fail();
        } catch (Throwable e) {
            // verify
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                    .doGetImage(Mockito.anyString(), Mockito.any(CloudUser.class));
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.GET), Mockito.eq(ResourceType.IMAGE), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }

    // test case: When invoking the getAllSecurityRules method with a valid network
    // order, it must call the doGetAllSecurityRules method and confirm in
    // auditRequest the GET_ALL operation of the NETWORK resource type.
    @Test
    public void testGetAllSecurityRulesWithNetworkOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalNetworkOrder();

        // exercise
        this.localCloudConnector.getAllSecurityRules(order, this.testUtils.createSystemUser());

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doGetAllSecurityRules(Mockito.any(Order.class), Mockito.any(CloudUser.class));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET_ALL), Mockito.eq(ResourceType.NETWORK), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getAllSecurityRules method with a valid public
    // IP order, it must call the doGetAllSecurityRules method and confirm in
    // auditRequest the GET_ALL operation of the PUBLIC_IP resource type.
    @Test
    public void testGetAllSecurityRulesWithPublicIpOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);

        // exercise
        this.localCloudConnector.getAllSecurityRules(order, this.testUtils.createSystemUser());

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doGetAllSecurityRules(Mockito.any(Order.class), Mockito.any(CloudUser.class));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.GET_ALL), Mockito.eq(ResourceType.PUBLIC_IP), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the getAllSecurityRules method with a network order
    // and an error occurs while getting the security rules in the cloud, it must
    // throw an exception and confirm in auditRequest the GET_ALL operation and the
    // NETWORK resource type.
    @Test
    public void testGetAllSecurityRulesFailWithNetworkOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalNetworkOrder();

        Mockito.doThrow(Throwable.class).when(this.localCloudConnector).doGetAllSecurityRules(Mockito.any(Order.class),
                Mockito.any(CloudUser.class));
        try {
            // exercise
            this.localCloudConnector.getAllSecurityRules(order, this.testUtils.createSystemUser());
            Assert.fail();
        } catch (Throwable e) {
            // verify
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                    .doGetAllSecurityRules(Mockito.any(Order.class), Mockito.any(CloudUser.class));
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.GET_ALL), Mockito.eq(ResourceType.NETWORK), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the getAllSecurityRules method with a public IP order
    // and an error occurs while getting the security rules in the cloud, it must
    // throw an exception and confirm in auditRequest the GET_ALL operation and the
    // PUBLIC_IP resource type.
    @Test
    public void testGetAllSecurityRulesFailWithPublicIpOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);

        Mockito.doThrow(Throwable.class).when(this.localCloudConnector).doGetAllSecurityRules(Mockito.any(Order.class),
                Mockito.any(CloudUser.class));
        try {
            // exercise
            this.localCloudConnector.getAllSecurityRules(order, this.testUtils.createSystemUser());
            Assert.fail();
        } catch (Throwable e) {
            // verify
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                    .doGetAllSecurityRules(Mockito.any(Order.class), Mockito.any(CloudUser.class));
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.GET_ALL), Mockito.eq(ResourceType.PUBLIC_IP), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the requestSecurityRule method with a valid network
    // order, it must call the doRequestSecurityRule method and confirm in
    // auditRequest the CREATE operation of the NETWORK resource type.
    @Test
    public void testRequestSecurityRuleWithNetworkOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalNetworkOrder();
        SecurityRule securityRule = Mockito.mock(SecurityRule.class);

        // exercise
        this.localCloudConnector.requestSecurityRule(order, securityRule, this.testUtils.createSystemUser());

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doRequestSecurityRule(
                Mockito.any(Order.class), Mockito.any(SecurityRule.class), Mockito.any(CloudUser.class));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.NETWORK), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the requestSecurityRule method with a valid public
    // IP order, it must call the doRequestSecurityRule method and confirm in
    // auditRequest the CREATE operation of the PUBLIC_IP resource type.
    @Test
    public void testRequestSecurityRuleWithPublicIpOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        SecurityRule securityRule = Mockito.mock(SecurityRule.class);

        // exercise
        this.localCloudConnector.requestSecurityRule(order, securityRule, this.testUtils.createSystemUser());

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doRequestSecurityRule(
                Mockito.any(Order.class), Mockito.any(SecurityRule.class), Mockito.any(CloudUser.class));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.PUBLIC_IP), Mockito.any(SystemUser.class),
                Mockito.anyString());
    }
    
    // test case: When invoking the requestSecurityRule method with a network
    // order and an error occurs while creating the security rule in the cloud, it
    // must throw an exception and confirm in auditRequest the CREATE operation and
    // the NETWORK resource type.
    @Test
    public void testRequestSecurityRuleFailWithNetworkOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalNetworkOrder();
        SecurityRule securityRule = Mockito.mock(SecurityRule.class);

        Mockito.doThrow(Throwable.class).when(this.localCloudConnector).doRequestSecurityRule(Mockito.any(Order.class),
                Mockito.any(SecurityRule.class), Mockito.any(CloudUser.class));

        // exercise
        try {
            this.localCloudConnector.requestSecurityRule(order, securityRule, this.testUtils.createSystemUser());
            Assert.fail();
        } catch (Throwable e) {
            // verify
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doRequestSecurityRule(
                    Mockito.any(Order.class), Mockito.any(SecurityRule.class), Mockito.any(CloudUser.class));
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.NETWORK), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the requestSecurityRule method with a public IP
    // order and an error occurs while creating the security rule in the cloud, it
    // must throw an exception and confirm in auditRequest the CREATE operation and
    // the PUBLIC_IP resource type.
    @Test
    public void testRequestSecurityRuleFailWithPublicIpOrder() throws FogbowException {
        // set up
        Order order = this.testUtils.createLocalPublicIpOrder(TestUtils.FAKE_COMPUTE_ID);
        SecurityRule securityRule = Mockito.mock(SecurityRule.class);

        Mockito.doThrow(Throwable.class).when(this.localCloudConnector).doRequestSecurityRule(Mockito.any(Order.class),
                Mockito.any(SecurityRule.class), Mockito.any(CloudUser.class));

        // exercise
        try {
            this.localCloudConnector.requestSecurityRule(order, securityRule, this.testUtils.createSystemUser());
            Assert.fail();
        } catch (Throwable e) {
            // verify
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).doRequestSecurityRule(
                    Mockito.any(Order.class), Mockito.any(SecurityRule.class), Mockito.any(CloudUser.class));
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.CREATE), Mockito.eq(ResourceType.PUBLIC_IP), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the deleteSecurityRule method with a valid security
    // rule ID, it must call the doDeleteSecurityRule method and confirm in
    // auditRequest the DELETE operation of the SECURITY_RULE resource type.
    @Test
    public void testDeleteSecurityRule() throws FogbowException {
        // exercise
        this.localCloudConnector.deleteSecurityRule(TestUtils.FAKE_SECURITY_RULE_ID, this.testUtils.createSystemUser());

        // verify
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteSecurityRule(Mockito.anyString(), Mockito.any(CloudUser.class));
        Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                Mockito.eq(Operation.DELETE), Mockito.eq(ResourceType.SECURITY_RULE), Mockito.any(SystemUser.class),
                Mockito.anyString());

    }
    
    // test case: When invoking the deleteSecurityRule method and an error occurs
    // while deleting the security rule in the cloud, it must throw an exception and
    // confirm in auditRequest the DELETE operation and the SECURITY_RULE resource
    // type.
    @Test
    public void testDeleteSecurityRuleFail() throws FogbowException {
        // set up
        Mockito.doThrow(Throwable.class).when(this.localCloudConnector).doDeleteSecurityRule(Mockito.anyString(),
                Mockito.any(CloudUser.class));
        try {
            // exercise
            this.localCloudConnector.deleteSecurityRule(TestUtils.FAKE_SECURITY_RULE_ID, this.testUtils.createSystemUser());
            Assert.fail();
        } catch (Throwable e) {
            // verify
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE))
                    .doDeleteSecurityRule(Mockito.anyString(), Mockito.any(CloudUser.class));
            Mockito.verify(this.localCloudConnector, Mockito.times(TestUtils.RUN_ONCE)).auditRequest(
                    Mockito.eq(Operation.DELETE), Mockito.eq(ResourceType.SECURITY_RULE), Mockito.any(SystemUser.class),
                    Mockito.anyString());
        }
    }
    
    // test case: When invoking the createEmptyInstance method with an order
    // containing an invalid resource type for this context, it must throw an
    // InternalServerErrorException.
    @Test
    public void testCreateEmptyInstanceWithInvalidResource() {
        // set up
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getType()).thenReturn(ResourceType.INVALID_RESOURCE);
        
        String expected = Messages.Exception.UNSUPPORTED_REQUEST_TYPE_S;

        try {
            // exercise
            EmptyOrderInstanceGenerator.createEmptyInstance(order);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When invoking the checkOrderCastingAndSetPlugin method with an
    // order without resource type defined and a resource type valid, it must throw
    // an InternalServerErrorException.
    @Test
    public void testCheckOrderCastingAndSetPluginWithAnOrderWithouResourceType() {
        // set up
        Order order = Mockito.mock(Order.class);
        String expected = Messages.Exception.MISMATCHING_RESOURCE_TYPE;

        try {
            // exercise
            this.localCloudConnector.checkOrderCastingAndSetPlugin(order, ResourceType.COMPUTE);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When invoking the checkOrderCastingAndSetPlugin method with an
    // order without resource type defined and an invalid resource type for this
    // context, it must throw an InternalServerErrorException too.
    @Test
    public void CheckOrderCastingAndSetPluginWithAnInvalidResourceType() {
        // set up
        Order order = Mockito.mock(Order.class);
        String expected = String.format(Messages.Exception.UNSUPPORTED_REQUEST_TYPE_S, order.getType());
        
        try {
            // exercise
            this.localCloudConnector.checkOrderCastingAndSetPlugin(order, ResourceType.INVALID_RESOURCE);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(expected, e.getMessage());
        }
    }
    
    // test case: When invoking the doRequestInstance method, it must execute the
    // requestInstance method of the corresponding resource plug-in to return the
    // instance ID of this resource.
    @Test
    public void testDoRequestInstance() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        OrderPlugin plugin = Mockito.mock(OrderPlugin.class);
        Mockito.doReturn(plugin).when(this.localCloudConnector).checkOrderCastingAndSetPlugin(Mockito.eq(order),
                Mockito.any(ResourceType.class));

        Mockito.when(plugin.requestInstance(Mockito.eq(order), Mockito.eq(cloudUser))).thenReturn(TestUtils.FAKE_INSTANCE_ID);

        // exercise
        this.localCloudConnector.doRequestInstance(order, cloudUser);

        // verify
        Mockito.verify(plugin, Mockito.times(TestUtils.RUN_ONCE)).requestInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));
    }
    
    // test case: When invoking the doDeleteInstance method, it must execute the
    // deleteInstance method of the corresponding resource plug-in.
    @Test
    public void testDoDeleteInstance() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getInstanceId()).thenReturn(TestUtils.FAKE_INSTANCE_ID);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        OrderPlugin plugin = Mockito.mock(OrderPlugin.class);
        Mockito.doReturn(plugin).when(this.localCloudConnector).checkOrderCastingAndSetPlugin(Mockito.eq(order),
                Mockito.any(ResourceType.class));

        // exercise
        this.localCloudConnector.doDeleteInstance(order, cloudUser);

        // verify
        Mockito.verify(plugin, Mockito.times(TestUtils.RUN_ONCE)).deleteInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));
    }
    
    // test case: When invoking the doGetInstance method, it must execute the
    // getInstance method of the corresponding resource plug-in to return the
    // instance of this resource.
    @Test
    public void testDoGetInstance() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getInstanceId()).thenReturn(TestUtils.FAKE_INSTANCE_ID);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        OrderPlugin plugin = Mockito.mock(OrderPlugin.class);
        Mockito.doReturn(plugin).when(this.localCloudConnector).checkOrderCastingAndSetPlugin(Mockito.eq(order),
                Mockito.any(ResourceType.class));
        
        OrderInstance instance = Mockito.mock(OrderInstance.class);
        Mockito.when(plugin.getInstance(Mockito.eq(order), Mockito.eq(cloudUser))).thenReturn(instance);

        // exercise
        this.localCloudConnector.doGetInstance(order, cloudUser);

        // verify
        Mockito.verify(plugin, Mockito.times(TestUtils.RUN_ONCE)).getInstance(Mockito.eq(order),
                Mockito.eq(cloudUser));
    }

    // test case: When invoking the doGetInstance method with order in the state CHECKING_DELETION,
    // it must throw an InstanceNotFoundException.
    @Test(expected = InstanceNotFoundException.class)
    public void testDoGetInstanceWhenCheckingDeletionState() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);
        Mockito.when(order.getOrderState()).thenReturn(OrderState.CHECKING_DELETION);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);

        // exercise
        this.localCloudConnector.doGetInstance(order, cloudUser);
    }

    // test case: When invoking the doGetAllImages method, it must execute the
    // getAllImages method of the image plug-in.
    @Test
    public void testDoGetAllImages() throws FogbowException {
        // set up
        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.doGetAllImages(cloudUser);

        // verify
        Mockito.verify(this.imagePlugin, Mockito.times(TestUtils.RUN_ONCE)).getAllImages(Mockito.eq(cloudUser));
    }
    
    // test case: When invoking the doGetImage method, it must execute the
    // getImage method of the image plug-in.
    @Test
    public void testDoGetImage() throws FogbowException {
        // set up
        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.doGetImage(TestUtils.FAKE_IMAGE_ID, cloudUser);

        // verify
        Mockito.verify(this.imagePlugin, Mockito.times(TestUtils.RUN_ONCE)).getImage(Mockito.anyString(),
                Mockito.eq(cloudUser));
    }

    // test case: When invoking the doGetAllSecurityRules method, it must execute
    // the getSecurityRules method of the security rule plug-in.
    @Test
    public void testDoGetAllSecurityRules() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.doGetAllSecurityRules(order, cloudUser);

        // verify
        Mockito.verify(this.securityRulePlugin, Mockito.times(TestUtils.RUN_ONCE))
                .getSecurityRules(Mockito.eq(order), Mockito.eq(cloudUser));
    }
    
    // test case: When invoking the doRequestSecurityRule method, it must execute
    // the requestSecurityRule method of the security rule plug-in.
    @Test
    public void testDoRequestSecurityRule() throws FogbowException {
        // set up
        Order order = Mockito.mock(Order.class);
        SecurityRule securityRule = Mockito.mock(SecurityRule.class);

        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.doRequestSecurityRule(order, securityRule, cloudUser);

        // verify
        Mockito.verify(this.securityRulePlugin, Mockito.times(TestUtils.RUN_ONCE))
                .requestSecurityRule(Mockito.eq(securityRule), Mockito.eq(order), Mockito.eq(cloudUser));
    }
    
    // test case: When invoking the doDeleteSecurityRule method, it must execute
    // the deleteSecurityRule method of the security rule plug-in.
    @Test
    public void testDoDeleteSecurityRule() throws FogbowException {
        // set up
        CloudUser cloudUser = Mockito.mock(CloudUser.class);
        Mockito.when(this.mapperPlugin.map(Mockito.any(SystemUser.class))).thenReturn(cloudUser);

        // exercise
        this.localCloudConnector.doDeleteSecurityRule(TestUtils.FAKE_SECURITY_RULE_ID, cloudUser);

        // verify
        Mockito.verify(this.securityRulePlugin, Mockito.times(TestUtils.RUN_ONCE))
                .deleteSecurityRule(Mockito.anyString(), Mockito.eq(cloudUser));
    }
    
    private InteroperabilityPluginInstantiator mockPluginsInstantiator() {
        InteroperabilityPluginInstantiator instantiator = Mockito.mock(InteroperabilityPluginInstantiator.class);
        Mockito.when(instantiator.getAttachmentPlugin(Mockito.anyString())).thenReturn(this.attachmentPlugin);
        Mockito.when(instantiator.getComputePlugin(Mockito.anyString())).thenReturn(this.computePlugin);
        Mockito.when(instantiator.getImagePlugin(Mockito.anyString())).thenReturn(this.imagePlugin);
        Mockito.when(instantiator.getSystemToCloudMapperPlugin(Mockito.anyString())).thenReturn(this.mapperPlugin);
        Mockito.when(instantiator.getNetworkPlugin(Mockito.anyString())).thenReturn(this.networkPlugin);
        Mockito.when(instantiator.getPublicIpPlugin(Mockito.anyString())).thenReturn(this.publicIpPlugin);
        Mockito.when(instantiator.getVolumePlugin(Mockito.anyString())).thenReturn(this.volumePlugin);
        Mockito.when(instantiator.getSecurityRulePlugin(Mockito.anyString())).thenReturn(this.securityRulePlugin);
        Mockito.when(instantiator.getQuotaPlugin(Mockito.anyString())).thenReturn(this.quotaPlugin);
        return instantiator;
    }
    
    private void mockResourcePlugins() {
        this.computePlugin = Mockito.mock(ComputePlugin.class);
        this.attachmentPlugin = Mockito.mock(AttachmentPlugin.class);
        this.networkPlugin = Mockito.mock(NetworkPlugin.class);
        this.volumePlugin = Mockito.mock(VolumePlugin.class);
        this.imagePlugin = Mockito.mock(ImagePlugin.class);
        this.publicIpPlugin = Mockito.mock(PublicIpPlugin.class);
        this.mapperPlugin = Mockito.mock(SystemToCloudMapperPlugin.class);
        this.securityRulePlugin = Mockito.mock(SecurityRulePlugin.class);
        this.quotaPlugin = Mockito.mock(QuotaPlugin.class);
    }

}
