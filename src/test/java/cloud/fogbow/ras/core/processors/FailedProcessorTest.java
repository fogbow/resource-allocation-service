package cloud.fogbow.ras.core.processors;

import cloud.fogbow.common.constants.FogbowConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.instances.ComputeInstance;
import cloud.fogbow.ras.core.models.instances.Instance;
import cloud.fogbow.ras.core.models.instances.InstanceState;
import cloud.fogbow.ras.core.models.linkedlists.ChainedList;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudConnectorFactory.class)
public class FailedProcessorTest extends BaseUnitTests {

	private static final int CPU_VALUE = 8;
	private static final long DEFAULT_SLEEP_TIME = 500;
	private static final int DISK_VALUE = 30;
	private static final int MEMORY_VALUE = 1024;
	
	private static final String CLOUD_NAME = "default";
	private static final String FAKE_IMAGE_NAME = "fake-image-name";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
	private static final String FAKE_FEDERATION_USER_TOKEN_VALUE = "fake-token-value";
	private static final String FAKE_PUBLIC_KEY = "fake-public-key";
	private static final String FAKE_REMOTE_MEMBER_ID = "fake-intercomponent-member";
	private static final String FAKE_TOKEN_PROVIDER = "fake-token-provider";
	private static final String FAKE_USER_ID = "fake-user-id";
	private static final String FAKE_USER_NAME = "fake-user-name";
	
	private ChainedList failedOrderList;
    private ChainedList fulfilledOrderList;
    private FailedProcessor failedProcessor;
    private LocalCloudConnector localCloudConnector;
    private Properties properties;
    private Thread thread;
    
    @Before
    public void setUp() throws UnexpectedException {
    	super.mockReadOrdersFromDataBase();
    	this.localCloudConnector = Mockito.mock(LocalCloudConnector.class);
        this.thread = null;
    	
    	PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.put(ConfigurationPropertyKeys.XMPP_JID_KEY, BaseUnitTests.LOCAL_MEMBER_ID);

        SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
        this.fulfilledOrderList = sharedOrderHolders.getFulfilledOrdersList();
        this.failedOrderList = sharedOrderHolders.getFailedAfterSuccessfulRequestOrdersList();
    }
    
    @After
    public void tearDown() {
        if (this.thread != null) {
            this.thread.interrupt();
        }
        super.tearDown();
    }
    
	// test case: When running thread in the FailedProcessor with a remote member
	// ID, the processFailedOrder method must not change its state, remaining in the
	// failed list.
	@Test
	public void testRunProcessLocalOrderWithRemoteMember()
			throws FogbowException, InterruptedException {

		// set up
		Order order = this.createOrder();
		order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
		this.failedOrderList.addItem(order);
		Assert.assertNull(this.fulfilledOrderList.getNext());

		this.failedProcessor = new FailedProcessor(FAKE_REMOTE_MEMBER_ID,
				ConfigurationPropertyDefaults.FAILED_ORDERS_SLEEP_TIME);

		// exercise
		this.thread = new Thread(this.failedProcessor);
		this.thread.start();
		Thread.sleep(DEFAULT_SLEEP_TIME);

		// verify
		Assert.assertEquals(order, this.failedOrderList.getNext());
		Assert.assertNull(this.fulfilledOrderList.getNext());
	}
	
	// test case: When calling the processFulfilledOrder method for any requesting
	// state other than failed after a successful request, it must not transition
	// states by keeping the request in its source list.
	@Test
	public void testRunProcessLocalOrderNotFailed() throws FogbowException, InterruptedException {

		// set up
		Order order = this.createOrder();
		order.setOrderStateInTestMode(OrderState.FULFILLED);
		this.fulfilledOrderList.addItem(order);
		Assert.assertNull(this.failedOrderList.getNext());

		// exercise
		spyFailedProcessor();
		this.failedProcessor.processFailedOrder(order);

		// verify
		Assert.assertEquals(order, this.fulfilledOrderList.getNext());
		Assert.assertNull(this.failedOrderList.getNext());
	}
    
	// test case: When executing the thread in FailedProcessor, if the instance
	// state is still Failed after a successful request, the processFailedOrder
	// method should not change its state and it must remain in the list of
	// failures.
	@Test
	public void testRunProcessLocalOrderWithInstanceFailed()
			throws FogbowException, InterruptedException {
		
		// set up
		Order order = createOrder();
		order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
		this.failedOrderList.addItem(order);
		Assert.assertNull(this.fulfilledOrderList.getNext());

		Instance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
		orderInstance.setState(InstanceState.FAILED);
		order.setInstanceId(FAKE_INSTANCE_ID);

		mockCloudConnectorFactory(orderInstance);

		// exercise
		this.thread = new Thread(this.failedProcessor);
		this.thread.start();
		Thread.sleep(DEFAULT_SLEEP_TIME);

		// verify
		Assert.assertNotNull(this.failedOrderList.getNext());
		Assert.assertNull(this.fulfilledOrderList.getNext());
	}
	
	// test case: When executing the thread in the FailedProcessor, if the instance
	// is back to the Ready state, the processFailedOrder method must change
	// OrderState from Failed to Fulfilled and the order must be removed from the
	// failed list and put in the fulfilled list.
	@Test
	public void testRunProcessLocalOrderWithInstanceReady()
			throws InterruptedException, FogbowException {

		// set up
		Order order = createOrder();
		order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
		this.failedOrderList.addItem(order);
		Assert.assertNull(this.fulfilledOrderList.getNext());

		Instance orderInstance = new ComputeInstance(FAKE_INSTANCE_ID);
		orderInstance.setState(InstanceState.READY);
		order.setInstanceId(FAKE_INSTANCE_ID);

		mockCloudConnectorFactory(orderInstance);

		// exercise
		this.thread = new Thread(this.failedProcessor);
		this.thread.start();
		Thread.sleep(DEFAULT_SLEEP_TIME);

		// verify
		Assert.assertNotNull(this.fulfilledOrderList.getNext());
		Assert.assertNull(this.failedOrderList.getNext());
	}
	
	// test case: During a thread running in FailedProcessor, if any errors occur
	// when attempting to get a cloud provider instance, the processFailedOrder
	// method will catch an exception.
	@Test
	public void testRunProcessLocalOrderToCatchExceptionWhileTryingToGetInstance()
			throws InterruptedException, FogbowException {

		// set up
		Order order = createOrder();
		order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
		this.failedOrderList.addItem(order);

		CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);

		PowerMockito.mockStatic(CloudConnectorFactory.class);
		BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);

		Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(this.localCloudConnector);

		Mockito.doThrow(new RuntimeException()).when(this.localCloudConnector).getInstance(Mockito.any(Order.class));

		spyFailedProcessor();

		// exercise
		this.thread = new Thread(this.failedProcessor);
		this.thread.start();
		Thread.sleep(DEFAULT_SLEEP_TIME);

		// verify
		Mockito.verify(this.failedProcessor, Mockito.times(1)).processFailedOrder(order);
	}
	
	// test case: Check the throw of UnexpectedException when running the thread in
	// the FailedProcessor, while running a local order.
	@Test
	public void testRunProcessLocalOrderThrowsUnexpectedException()
			throws InterruptedException, FogbowException {

		// set up
		Order order = createOrder();
		order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
		this.failedOrderList.addItem(order);

		spyFailedProcessor();

		Mockito.doThrow(new UnexpectedException()).when(this.failedProcessor).processFailedOrder(order);

		// exercise
		this.thread = new Thread(this.failedProcessor);
		this.thread.start();
		Thread.sleep(DEFAULT_SLEEP_TIME);

		// verify
		Mockito.verify(this.failedProcessor, Mockito.times(1)).processFailedOrder(order);
	}
	
	// test case: Check the throw of RuntimeException when running the thread in
	// the FailedProcessor, while running a local order.
    @Test
    public void testRunProcessLocalOrderThrowsRuntimeException()
            throws InterruptedException, UnexpectedException {

        // set up
		Order order = createOrder();
		order.setOrderStateInTestMode(OrderState.FAILED_AFTER_SUCCESSUL_REQUEST);
		this.failedOrderList.addItem(order);

        spyFailedProcessor();

        // exercise
        Mockito.doThrow(new RuntimeException()).when(this.failedProcessor)
                .processFailedOrder(order);

        this.thread = new Thread(this.failedProcessor);
        this.thread.start();
        Thread.sleep(DEFAULT_SLEEP_TIME);

        // verify
        Mockito.verify(this.failedProcessor, Mockito.times(1)).processFailedOrder(order);
    }
    
	private void mockCloudConnectorFactory(Instance orderInstance) throws FogbowException {
		CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
		
		PowerMockito.mockStatic(CloudConnectorFactory.class);
		BDDMockito.given(CloudConnectorFactory.getInstance()).willReturn(cloudConnectorFactory);
		
		Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(this.localCloudConnector);

		Mockito.doReturn(orderInstance).when(this.localCloudConnector).getInstance(Mockito.any(Order.class));

		spyFailedProcessor();
	}

	private void spyFailedProcessor() {
		this.failedProcessor = Mockito.spy(new FailedProcessor(BaseUnitTests.LOCAL_MEMBER_ID,
				ConfigurationPropertyDefaults.FAILED_ORDERS_SLEEP_TIME));
	}

	private Order createOrder() {
		Map<String, String> attributes = new HashMap<>();
		attributes.put(FogbowConstants.PROVIDER_ID_KEY, FAKE_TOKEN_PROVIDER);
		attributes.put(FogbowConstants.USER_ID_KEY, FAKE_USER_ID);
		attributes.put(FogbowConstants.USER_NAME_KEY, FAKE_USER_NAME);
		attributes.put(FogbowConstants.TOKEN_VALUE_KEY, FAKE_FEDERATION_USER_TOKEN_VALUE);
		FederationUser federationUser = new FederationUser(attributes);


		String requestingMember = String.valueOf(this.properties.get(ConfigurationPropertyKeys.XMPP_JID_KEY));
		String providingMember = String.valueOf(this.properties.get(ConfigurationPropertyKeys.XMPP_JID_KEY));
		ArrayList<UserData> userData = super.mockUserData();
		List<String> networkIds = null;

		Order order = new ComputeOrder(
				federationUser, 
				requestingMember, 
				providingMember, 
				CLOUD_NAME,
				FAKE_INSTANCE_NAME, 
				CPU_VALUE, 
				MEMORY_VALUE, 
				DISK_VALUE, 
				FAKE_IMAGE_NAME, 
				userData, 
				FAKE_PUBLIC_KEY, 
				networkIds);
		
		return order;
	}
}
