package cloud.fogbow.ras.core;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.InvalidTokenException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnavailableProviderException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationController;
import cloud.fogbow.common.util.AuthenticationUtil;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.cloudconnector.LocalCloudConnector;
import cloud.fogbow.ras.core.constants.SystemConstants;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.InstanceStatus;
import cloud.fogbow.ras.core.models.NetworkAllocationMode;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.instances.AttachmentInstance;
import cloud.fogbow.ras.core.models.instances.ComputeInstance;
import cloud.fogbow.ras.core.models.instances.NetworkInstance;
import cloud.fogbow.ras.core.models.instances.PublicIpInstance;
import cloud.fogbow.ras.core.models.instances.VolumeInstance;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.models.securityrules.SecurityRule;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequest;
import cloud.fogbow.ras.core.plugins.interoperability.genericrequest.GenericRequestResponse;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ApplicationFacade.class, PublicKeysHolder.class, AuthenticationUtil.class, DatabaseManager.class,
		SharedOrderHolders.class, CloudConnectorFactory.class })
public class ApplicationFacadeTest extends BaseUnitTests {

	private static final String DEFAULT_CLOUD_NAME = "default";
	private static final String DEFAULT_BUILD_NUMBER_FORMAT = "%s-abcd";
	private static final String FAKE_ADDRESS = "fake-address";
	private static final String FAKE_PUBLIC_KEY = "fake-public-key";
	private static final String FAKE_CLOUD_NAME = "fake-cloud-name";
	private static final String FAKE_DEVICE_MOUNT_POINT = "fake-device-mount-point";
	private static final String FAKE_FEDERATION_TOKEN_VALUE = "federation-token-value";
	private static final String FAKE_GATEWAY = "fake-gateway";
	private static final String FAKE_IMAGE_NAME = "fake-image-name";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
	private static final String FAKE_MEMBER_ID = "fake-member-id";
	private static final String FAKE_NAME = "fake-name";
	private static final String FAKE_RULE_ID = "fake-rule-id";
	private static final String FAKE_SOURCE_ID = "fake-source-id";
	private static final String FAKE_TARGET_ID = "fake-target-id";
	private static final String FAKE_USER_ID_VALUE = "fake-user-id";
	private static final String FAKE_VOLUME_NAME = "fake-volume-name";
	private static final String ID_KEY = "id";
	private static final String TESTING_MODE_BUILD_NUMBER_FORMAT = "%s-[testing mode]";
	private static final String VALID_PATH_CONF = "ras.conf";
	private static final String VALID_PATH_CONF_WITHOUT_BUILD_PROPERTY = "ras-without-build-number.conf";

	private static final int CPU_VALUE = 2;
	private static final int DISK_VALUE = 30;
	private static final int MEMORY_VALUE = 2;

	private ApplicationFacade facade;
	private LocalCloudConnector localCloudConnector;
	private Map<String, Order> activeOrdersMap;
	private OrderController orderController;
	private SecurityRuleController securityRuleController;

	@Before
	public void setUp() throws UnexpectedException {
		super.mockReadOrdersFromDataBase();

		this.facade = ApplicationFacade.getInstance();

		this.orderController = Mockito.spy(new OrderController());
		this.facade.setOrderController(this.orderController);

		this.securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(this.securityRuleController);

		PowerMockito.mockStatic(CloudConnectorFactory.class);

		this.localCloudConnector = Mockito.mock(LocalCloudConnector.class);

		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		this.activeOrdersMap = Mockito.spy(sharedOrderHolders.getActiveOrdersMap());
	}

	// test case: When calling the setBuildNumber method, it must generate correct
	// build number version.
	@Test
	public void testVersion() throws Exception {
		// Setup
		this.facade.setBuildNumber(HomeDir.getPath() + VALID_PATH_CONF);

		// Exercise
		String build = this.facade.getVersionNumber();

		// Verify
		String expected = String.format(DEFAULT_BUILD_NUMBER_FORMAT, SystemConstants.API_VERSION_NUMBER);
		Assert.assertEquals(expected, build);
	}

	// test case: When calling the setBuildNumber method without a build property
	// valid in the configuration file, it must generate a testing mode build number
	// version.
	@Test
	public void testVersionWithoutBuildProperty() throws Exception {
		// Setup
		this.facade.setBuildNumber(HomeDir.getPath() + VALID_PATH_CONF_WITHOUT_BUILD_PROPERTY);

		// Exercise
		String build = this.facade.getVersionNumber();

		// Verify
		String expected = String.format(TESTING_MODE_BUILD_NUMBER_FORMAT, SystemConstants.API_VERSION_NUMBER);
		Assert.assertEquals(expected, build);
	}

	// test case: When calling a resource with a too long public key throws an
	// InvalidParameterException.
	@Test(expected = InvalidParameterException.class) // verify
	public void testCreateResourceWithTooLongPrivateKey() throws Exception {
		// set up
		String publicKey = generateVeryLongPublicKey();
		FederationUser federationUser = null;
		ArrayList<UserData> userData = null;
		List<String> networkIds = null;

		ComputeOrder order = spyComputeOrder(federationUser, publicKey, userData, networkIds);

		// exercise
		this.facade.createCompute(order, FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling a resource with a too long extra user data file
	// content
	// throws an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testCreateResourceWithTooLongExtraUserDataFileContent() throws Exception {
		// set up
		String publicKey = FAKE_PUBLIC_KEY;
		FederationUser federationUser = null;
		ArrayList<UserData> userData = generateVeryLongUserDataFileContent();
		List<String> networkIds = null;

		ComputeOrder order = spyComputeOrder(federationUser, publicKey, userData, networkIds);

		// exercise
		this.facade.createCompute(order, FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the createCompute method with a new order passed by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreateComputeOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		FederationUser federationUser = mockFederationUserAuthenticate(keyRSA);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String publicKey = FAKE_PUBLIC_KEY;
		ArrayList<UserData> userData = super.mockUserData();
		List<String> networkIds = null;

		ComputeOrder order = spyComputeOrder(federationUser, publicKey, userData, networkIds);

		ComputeInstance computeInstance = new ComputeInstance(order.getId());
		order.setInstanceId(computeInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState orderStateExpected = OrderState.OPEN;

		// exercise
		this.facade.createCompute(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(orderStateExpected, order.getOrderState());
	}

	// test case: When calling the getCompute method, it must return the
	// ComputeInstance of the Order ID passed per parameter.
	@Test
	public void testGetComputeOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String publicKey = FAKE_PUBLIC_KEY;
		ArrayList<UserData> userData = super.mockUserData();
		List<String> networkIds = null;

		ComputeOrder order = spyComputeOrder(federationUser, publicKey, userData, networkIds);
		OrderStateTransitioner.activateOrder(order);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		ComputeInstance instanceExpected = new ComputeInstance(order.getId());
		order.setInstanceId(instanceExpected.getId());

		Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenReturn(instanceExpected);

		// exercise
		ComputeInstance instance = this.facade.getCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getResourceInstance(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertSame(instanceExpected, instance);
	}

	// test case: When calling the method deleteCompute, the Order passed as a
	// parameter must return its state changed to Closed.
	@Test
	public void testDeleteComputeOrder() throws Exception {

		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String publicKey = FAKE_PUBLIC_KEY;
		ArrayList<UserData> userData = super.mockUserData();
		List<String> networkIds = null;

		ComputeOrder order = spyComputeOrder(federationUser, publicKey, userData, networkIds);
		OrderStateTransitioner.activateOrder(order);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		ComputeInstance computeInstance = new ComputeInstance(order.getId());
		order.setInstanceId(computeInstance.getId());

		Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenReturn(computeInstance);

		// checking that the order has a state and is not null
		Assert.assertNotNull(order.getOrderState());
		OrderState orderStateExpected = OrderState.CLOSED;

		// exercise
		this.facade.deleteCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertEquals(orderStateExpected, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the computeOrders.
	@Test
	public void testGetAllComputeOrdersInstancesStatus() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String publicKey = FAKE_PUBLIC_KEY;
		ArrayList<UserData> userData = super.mockUserData();
		List<String> networkIds = null;

		ComputeOrder order = spyComputeOrder(federationUser, publicKey, userData, networkIds);
		OrderStateTransitioner.activateOrder(order);

		List<InstanceStatus> instancesStatusExpected = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.COMPUTE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(1)).getInstancesStatus(Mockito.eq(federationUser),
				Mockito.eq(ResourceType.COMPUTE));

		Assert.assertEquals(instancesStatusExpected, instancesStatus);
	}

	// test case: When calling the createVolume method with a new order passed by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreateVolumeOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		FederationUser federationUser = mockFederationUserAuthenticate(keyRSA);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		VolumeOrder order = spyVolumeOrder(federationUser);

		VolumeInstance volumeInstance = new VolumeInstance(order.getId());
		order.setInstanceId(volumeInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState orderStateExpected = OrderState.OPEN;

		// exercise
		this.facade.createVolume(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(orderStateExpected, order.getOrderState());
	}

	// test case: When calling the getVolume method, it must return the
	// VolumeInstance of the Order ID passed per parameter.
	@Test
	public void testGetVolumeOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		VolumeOrder order = spyVolumeOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		VolumeInstance instanceExpected = new VolumeInstance(order.getId());
		order.setInstanceId(instanceExpected.getId());

		Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenReturn(instanceExpected);

		// exercise
		VolumeInstance instance = this.facade.getVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getResourceInstance(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertSame(instanceExpected, instance);
	}

	// test case: When calling the method deleteVolume, the Order passed as a
	// parameter must return its state changed to Closed.
	@Test
	public void testDeleteVolumeOrder() throws Exception {

		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		VolumeOrder order = spyVolumeOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		VolumeInstance volumeInstance = new VolumeInstance(order.getId());
		order.setInstanceId(volumeInstance.getId());

		Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenReturn(volumeInstance);

		// checking that the order has a state and is not null
		Assert.assertNotNull(order.getOrderState());
		OrderState orderStateExpected = OrderState.CLOSED;

		// exercise
		this.facade.deleteVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertEquals(orderStateExpected, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the volumeOrders.
	@Test
	public void testGetAllVolumeOrdersInstancesStatus() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		VolumeOrder order = spyVolumeOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		List<InstanceStatus> instancesStatusExpected = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.VOLUME);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(1)).getInstancesStatus(Mockito.eq(federationUser),
				Mockito.eq(ResourceType.VOLUME));

		Assert.assertEquals(instancesStatusExpected, instancesStatus);
	}

	// test case: When calling the createNetwork method with a new order passed by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreateNetworkOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		FederationUser federationUser = mockFederationUserAuthenticate(keyRSA);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		NetworkOrder order = spyNetworkOrder(federationUser);

		NetworkInstance networkInstance = new NetworkInstance(order.getId());
		order.setInstanceId(networkInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState orderStateExpected = OrderState.OPEN;

		// exercise
		this.facade.createNetwork(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(orderStateExpected, order.getOrderState());
	}

	// test case: When calling the getNetwork method, it must return the
	// NetworkInstance of the Order ID passed per parameter.
	@Test
	public void testGetNetworkOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		NetworkOrder order = spyNetworkOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		NetworkInstance instanceExpected = new NetworkInstance(order.getId());
		order.setInstanceId(instanceExpected.getId());

		Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenReturn(instanceExpected);

		// exercise
		NetworkInstance instance = this.facade.getNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getResourceInstance(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertSame(instanceExpected, instance);
	}

	// test case: When calling the method deleteNetwork, the Order passed as a
	// parameter must return its state changed to Closed.
	@Test
	public void testDeleteNetworkOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		NetworkOrder order = spyNetworkOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		NetworkInstance networkInstance = new NetworkInstance(order.getId());
		order.setInstanceId(networkInstance.getId());

		Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenReturn(networkInstance);

		// checking that the order has a state and is not null
		Assert.assertNotNull(order.getOrderState());
		OrderState orderStateExpected = OrderState.CLOSED;

		// exercise
		this.facade.deleteNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertEquals(orderStateExpected, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the networkOrders.
	@Test
	public void testGetAllNetworkOrdersInstancesStatus() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		NetworkOrder order = spyNetworkOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		List<InstanceStatus> instancesStatusExpected = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.NETWORK);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(1)).getInstancesStatus(Mockito.eq(federationUser),
				Mockito.eq(ResourceType.NETWORK));

		Assert.assertEquals(instancesStatusExpected, instancesStatus);
	}

	// test case: When calling the createAttachment method with a new order passed
	// by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreateAttachmentOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		FederationUser federationUser = mockFederationUserAuthenticate(keyRSA);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		AttachmentOrder order = spyAttachmentOrder(federationUser);

		AttachmentInstance attachmentInstance = new AttachmentInstance(order.getId());
		order.setInstanceId(attachmentInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState orderStateExpected = OrderState.OPEN;

		// exercise
		this.facade.createAttachment(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(orderStateExpected, order.getOrderState());
	}

	// test case: When calling the getAttachment method, it must return the
	// AttachmentInstance of the Order ID passed per parameter.
	@Test
	public void testGetAttachmentOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		AttachmentOrder order = spyAttachmentOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		AttachmentInstance instanceExpected = new AttachmentInstance(order.getId());
		order.setInstanceId(instanceExpected.getId());

		Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenReturn(instanceExpected);

		// exercise
		AttachmentInstance instance = this.facade.getAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getResourceInstance(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertSame(instanceExpected, instance);
	}

	// test case: When calling the method deleteAttachment, the Order passed as a
	// parameter must return its state changed to Closed.
	@Test
	public void testDeleteAttachmentOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		AttachmentOrder order = spyAttachmentOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		AttachmentInstance attachmentInstance = new AttachmentInstance(order.getId());
		order.setInstanceId(attachmentInstance.getId());

		Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenReturn(attachmentInstance);

		// checking that the order has a state and is not null
		Assert.assertNotNull(order.getOrderState());
		OrderState orderStateExpected = OrderState.CLOSED;

		// exercise
		this.facade.deleteAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertEquals(orderStateExpected, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the attachmentOrders.
	@Test
	public void testGetAllAttachmentOrdersInstancesStatus() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		AttachmentOrder order = spyAttachmentOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		List<InstanceStatus> instancesStatusExpected = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.ATTACHMENT);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(1)).getInstancesStatus(Mockito.eq(federationUser),
				Mockito.eq(ResourceType.ATTACHMENT));

		Assert.assertEquals(instancesStatusExpected, instancesStatus);
	}

	// test case: When calling the createPublicIp method with a new order passed by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreatePublicIpOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		FederationUser federationUser = mockFederationUserAuthenticate(keyRSA);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		PublicIpOrder order = spyPublicIpOrder(federationUser);

		PublicIpInstance publicIpInstance = new PublicIpInstance(order.getId());
		order.setInstanceId(publicIpInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState orderStateExpected = OrderState.OPEN;

		// exercise
		this.facade.createPublicIp(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(orderStateExpected, order.getOrderState());
	}

	// test case: When calling the getPublicIp method, it must return the
	// PublicIpInstance of the OrderID passed per parameter.
	@Test
	public void testGetPublicIpOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		PublicIpOrder order = spyPublicIpOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		PublicIpInstance instanceExpected = new PublicIpInstance(order.getId());
		order.setInstanceId(instanceExpected.getId());

		Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenReturn(instanceExpected);

		// exercise
		PublicIpInstance instance = this.facade.getPublicIp(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getResourceInstance(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertSame(instanceExpected, instance);
	}

	// test case: When calling the method deletePublicIp, the Order passed as a
	// parameter must return its state changed to Closed.
	@Test
	public void testDeletePublicIpOrder() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		PublicIpOrder order = spyPublicIpOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		PublicIpInstance publicIpInstance = new PublicIpInstance(order.getId());
		order.setInstanceId(publicIpInstance.getId());

		Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenReturn(publicIpInstance);

		// checking that the order has a state and is not null
		Assert.assertNotNull(order.getOrderState());
		OrderState orderStateExpected = OrderState.CLOSED;

		// exercise
		this.facade.deletePublicIp(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(orderStateExpected, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the publiIpOrders.
	@Test
	public void testGetAllPublicIpOrdersInstancesStatus() throws Exception {
		// set up
		PublicKeysHolder pkHolder = mockPublicKeyHolder();
		RSAPublicKey keyRSA = mockRSAPublicKey(pkHolder);
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = spyFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		PublicIpOrder order = spyPublicIpOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		List<InstanceStatus> instancesStatusExpected = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.PUBLIC_IP);

		// verify
		PowerMockito.verifyStatic(PublicKeysHolder.class, Mockito.times(1));
		PublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(1)).getInstancesStatus(Mockito.eq(federationUser),
				Mockito.eq(ResourceType.PUBLIC_IP));

		Assert.assertEquals(instancesStatusExpected, instancesStatus);
	}

	// test case: Creating a security rule for a network via a public IP's
	// endpoint, it must raise an InstanceNotFoundException.
	@Test
	public void testCreateSecurityRuleForNetworkViaPublicIp() throws Exception {
		// set up
		FederationUser federationUser = null;
		NetworkOrder order = spyNetworkOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);

		// exercise
		try {
			facade.createSecurityRule(FAKE_INSTANCE_ID, securityRule, FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.PUBLIC_IP);

			// verify
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// Exception thrown
		}
	}

	// test case: Creating a security rule for a public IP via a network's
	// endpoint, it must raise an InstanceNotFoundException.
	@Test
	public void testCreateSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		FederationUser federationUser = null;
		PublicIpOrder order = spyPublicIpOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);

		// exercise
		try {
			facade.createSecurityRule(FAKE_INSTANCE_ID, securityRule, FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.NETWORK);
			// verify
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// Exception thrown
		}
	}

	// test case: Get all security rules from a network via a public IP's endpoint,
	// it must raise an InstanceNotFoundException.
	@Test
	public void testGetSecurityRulesForNetworkViaPublicIp() throws Exception {
		// set up
		FederationUser federationUser = null;
		NetworkOrder order = spyNetworkOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		// exercise
		try {
			facade.getAllSecurityRules(FAKE_INSTANCE_ID, FAKE_FEDERATION_TOKEN_VALUE, ResourceType.PUBLIC_IP);

			// verify
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// Exception thrown
		}
	}

	// test case: Get all security rules from a public IP via a network's endpoint,
	// it must raise an InstanceNotFoundException.
	@Test
	public void testGetSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		FederationUser federationUser = null;
		PublicIpOrder order = spyPublicIpOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		// exercise
		try {
			facade.getAllSecurityRules(FAKE_INSTANCE_ID, FAKE_FEDERATION_TOKEN_VALUE, ResourceType.NETWORK);
			
			// verify
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// Exception thrown
		}
	}
	
	// test case: Delete a security rule from a network via public IP's endpoint, it
	// must raise an InstanceNotFoundException.
	@Test
	public void testDeleteSecurityRulesForNetworkViaPublicIp() throws Exception {
		// set up
		FederationUser federationUser = null;
		NetworkOrder order = spyNetworkOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());
		
		// exercise
		try {
			facade.deleteSecurityRule(FAKE_INSTANCE_ID, FAKE_RULE_ID, FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.PUBLIC_IP);
			
			// verify
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// Exception thrown
		}
	}

	// test case: Delete a security rule from a public IP via a network's endpoint,
	// it must raise an InstanceNotFoundException.
	@Test
	public void testDeleteSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		FederationUser federationUser = null;
		PublicIpOrder order = spyPublicIpOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());
		
		// exercise
		try {
			facade.deleteSecurityRule(FAKE_INSTANCE_ID, FAKE_RULE_ID, FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.NETWORK);
			
			// verify
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// Exception thrown
		}
	}
	
	// FIXME put on top the tests verified...

	private PublicIpOrder spyPublicIpOrder(FederationUser federationUser) {
		ComputeOrder computeOrder = new ComputeOrder();
		ComputeInstance computeInstance = new ComputeInstance(FAKE_SOURCE_ID);
		computeOrder.setInstanceId(computeInstance.getId());
		this.activeOrdersMap.put(computeOrder.getId(), computeOrder);
		String computeOrderId = computeOrder.getId();

		PublicIpOrder order = Mockito.spy(
				new PublicIpOrder(federationUser, 
						FAKE_MEMBER_ID, 
						FAKE_FEDERATION_TOKEN_VALUE,
						DEFAULT_CLOUD_NAME, 
						computeOrderId));

		return order;
	}

	private AttachmentOrder spyAttachmentOrder(FederationUser federationUser) {
		ComputeOrder computeOrder = new ComputeOrder();
		ComputeInstance computeInstance = new ComputeInstance(FAKE_SOURCE_ID);
		computeOrder.setInstanceId(computeInstance.getId());
		this.activeOrdersMap.put(computeOrder.getId(), computeOrder);
		String computeOrderId = computeOrder.getId();

		VolumeOrder volumeOrder = new VolumeOrder();
		VolumeInstance volumeInstance = new VolumeInstance(FAKE_TARGET_ID);
		volumeOrder.setInstanceId(volumeInstance.getId());
		this.activeOrdersMap.put(volumeOrder.getId(), volumeOrder);
		String volumeOrderId = volumeOrder.getId();

		AttachmentOrder order = Mockito.spy(
				new AttachmentOrder(federationUser, 
						FAKE_MEMBER_ID, 
						FAKE_FEDERATION_TOKEN_VALUE,
						DEFAULT_CLOUD_NAME, 
						computeOrderId, 
						volumeOrderId, 
						FAKE_DEVICE_MOUNT_POINT));

		return order;
	}

	private NetworkOrder spyNetworkOrder(FederationUser federationUser) {
		NetworkOrder order = Mockito.spy(
				new NetworkOrder(federationUser, 
						FAKE_MEMBER_ID, 
						FAKE_FEDERATION_TOKEN_VALUE,
						DEFAULT_CLOUD_NAME, 
						FAKE_NAME, 
						FAKE_GATEWAY, 
						FAKE_ADDRESS, 
						NetworkAllocationMode.STATIC));

		return order;
	}

	private VolumeOrder spyVolumeOrder(FederationUser federationUser) {
		VolumeOrder order = Mockito.spy(
				new VolumeOrder(federationUser, 
						FAKE_MEMBER_ID, 
						FAKE_FEDERATION_TOKEN_VALUE,
						DEFAULT_CLOUD_NAME, 
						FAKE_VOLUME_NAME, 
						DISK_VALUE));

		return order;
	}

	private List<InstanceStatus> generateInstancesStatus(Order order) {
		List<InstanceStatus> instancesExpected = new ArrayList<>();
		InstanceStatus instanceStatus = new InstanceStatus(
				order.getId(), 
				null, 
				order.getProvider(),
				order.getCloudName(), 
				order.getCachedInstanceState());

		instancesExpected.add(instanceStatus);
		return instancesExpected;
	}

	private CloudConnector mockCloudConnector(CloudConnectorFactory cloudConnectorFactory) {
		CloudConnector cloudConnector = Mockito.mock(CloudConnector.class);
		Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(cloudConnector);
		return cloudConnector;
	}

	private CloudConnectorFactory mockCloudConnectorFactory() {
		CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
		PowerMockito.when(CloudConnectorFactory.getInstance()).thenReturn(cloudConnectorFactory);
		return cloudConnectorFactory;
	}

	private FederationUser spyFederationUserAuthenticate(RSAPublicKey keyRSA, Map<String, String> attributes)
			throws UnauthenticatedUserException, InvalidTokenException {

		FederationUser federationUser = Mockito.spy(new FederationUser(attributes));
		PowerMockito.mockStatic(AuthenticationUtil.class);
		PowerMockito.when(AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString()))
				.thenReturn(federationUser);
		
		return federationUser;
	}

	private Map<String, String> putUserIdAttribute() {
		Map<String, String> attributes = new HashMap<>();
		attributes.put(ID_KEY, FAKE_USER_ID_VALUE);
		return attributes;
	}

	private ComputeOrder spyComputeOrder(FederationUser federationUser, String publicKey, ArrayList<UserData> userData,
			List<String> networkIds) {

		ComputeOrder order = Mockito.spy(
				new ComputeOrder(federationUser, 
						FAKE_MEMBER_ID, 
						FAKE_MEMBER_ID,
						DEFAULT_CLOUD_NAME, 
						FAKE_INSTANCE_NAME, 
						CPU_VALUE, 
						MEMORY_VALUE, 
						DISK_VALUE, 
						FAKE_IMAGE_NAME, 
						userData,
						publicKey, 
						networkIds));

		return order;
	}

	private AuthorizationController mockAuthorizationController(FederationUser federationUser)
			throws UnexpectedException, UnauthorizedRequestException {

		AuthorizationController authorization = Mockito.mock(AuthorizationController.class);
		Mockito.doNothing().when(authorization).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		this.facade.setAuthorizationController(authorization);
		return authorization;
	}

	private FederationUser mockFederationUserAuthenticate(RSAPublicKey keyRSA)
			throws UnauthenticatedUserException, InvalidTokenException {

		FederationUser federationUser = Mockito.mock(FederationUser.class);
		PowerMockito.mockStatic(AuthenticationUtil.class);
		PowerMockito.when(AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString()))
				.thenReturn(federationUser);

		return federationUser;
	}

	private RSAPublicKey mockRSAPublicKey(PublicKeysHolder pkHolder)
			throws UnavailableProviderException, UnexpectedException {

		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.when(pkHolder.getAsPublicKey()).thenReturn(keyRSA);
		return keyRSA;
	}

	private PublicKeysHolder mockPublicKeyHolder() {
		PowerMockito.mockStatic(PublicKeysHolder.class);
		PublicKeysHolder pkHolder = Mockito.mock(PublicKeysHolder.class);
		PowerMockito.when(PublicKeysHolder.getInstance()).thenReturn(pkHolder);
		return pkHolder;
	}

	private ArrayList<UserData> generateVeryLongUserDataFileContent() {
		String extraUserDataFileContent = new String(new char[UserData.MAX_EXTRA_USER_DATA_FILE_CONTENT + 1]);

		UserData userData = new UserData();
		userData.setExtraUserDataFileContent(extraUserDataFileContent);

		ArrayList<UserData> userDataScripts = new ArrayList<>();
		userDataScripts.add(userData);

		return userDataScripts;
	}

	private String generateVeryLongPublicKey() {
		return new String(new char[ComputeOrder.MAX_PUBLIC_KEY_SIZE + 1]);
	}

	// TODO tests in verification...

	@Ignore
	// test case: Creating a security rule for a public ip via its endpoint, it
	// should return the rule id.
	@Test
	public void testCreateSecurityRuleForPublicIp() throws Exception {
		// set up
		Mockito.doReturn(createPublicIpOrder()).when(orderController).getOrder(Mockito.anyString());
		// Mockito.doReturn(Mockito.mock(FederationUser.class)).when(aaaController).getFederationUser(Mockito.anyString());
		// Mockito.doNothing().when(aaaController).authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));
		Mockito.doReturn(FAKE_INSTANCE_ID).when(securityRuleController).createSecurityRule(Mockito.any(Order.class),
				Mockito.any(SecurityRule.class), Mockito.any(FederationUser.class));

		// exercise
		try {
			facade.createSecurityRule(FAKE_INSTANCE_ID, Mockito.mock(SecurityRule.class), FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.PUBLIC_IP);
		} catch (InstanceNotFoundException e) {
			// verify
			Assert.fail();
		}
	}

	@Ignore
	// test case: Creating a security rule for a network via its endpoint, it should
	// return the rule id.
	@Test
	public void testCreateSecurityRuleForNetwork() throws Exception {
		// set up
		Mockito.doReturn(createNetworkOrder()).when(orderController).getOrder(Mockito.anyString());
		// Mockito.doReturn(Mockito.mock(FederationUser.class)).when(aaaController).getFederationUser(Mockito.anyString());
		// Mockito.doNothing().when(aaaController).authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));
		Mockito.doReturn(FAKE_INSTANCE_ID).when(securityRuleController).createSecurityRule(Mockito.any(Order.class),
				Mockito.any(SecurityRule.class), Mockito.any(FederationUser.class));

		// exercise
		try {
			facade.createSecurityRule(FAKE_INSTANCE_ID, Mockito.mock(SecurityRule.class), FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.NETWORK);
		} catch (InstanceNotFoundException e) {
			// verify
			Assert.fail();
		}
	}

	@Ignore
	// test case: Get all security rules for a public ip via its endpoint, it should
	// return the rule id.
	@Test
	public void testGetSecurityRuleForPublicIp() throws Exception {
		// set up
		Mockito.doReturn(createPublicIpOrder()).when(orderController).getOrder(Mockito.anyString());
		// Mockito.doReturn(Mockito.mock(FederationUser.class)).when(aaaController).getFederationUser(Mockito.anyString());
		// Mockito.doNothing().when(aaaController).authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));
		Mockito.doReturn(new ArrayList<SecurityRule>()).when(securityRuleController)
				.getAllSecurityRules(Mockito.any(Order.class), Mockito.any(FederationUser.class));

		// exercise
		try {
			facade.getAllSecurityRules(FAKE_INSTANCE_ID, FAKE_FEDERATION_TOKEN_VALUE, ResourceType.PUBLIC_IP);
		} catch (InstanceNotFoundException e) {
			// verify
			Assert.fail();
		}
	}

	@Ignore
	// test case: Get all security rules for a network via its endpoint, it should
	// return the rule id.
	@Test
	public void testGetSecurityRuleForNetwork() throws Exception {
		// set up
		Mockito.doReturn(createNetworkOrder()).when(orderController).getOrder(Mockito.anyString());
		// Mockito.doReturn(Mockito.mock(FederationUser.class)).when(aaaController).getFederationUser(Mockito.anyString());
		// Mockito.doNothing().when(aaaController).authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));
		Mockito.doReturn(new ArrayList<SecurityRule>()).when(securityRuleController)
				.getAllSecurityRules(Mockito.any(Order.class), Mockito.any(FederationUser.class));

		// exercise
		try {
			facade.getAllSecurityRules(FAKE_INSTANCE_ID, FAKE_FEDERATION_TOKEN_VALUE, ResourceType.NETWORK);
		} catch (InstanceNotFoundException e) {
			// verify
			Assert.fail();
		}
	}

	@Ignore
	// test case: Delete a security rule for a public ip via its endpoint, it should
	// return the rule id.
	@Test
	public void testDeleteSecurityRuleForPublicIp() throws Exception {
		// set up
		Mockito.doReturn(createPublicIpOrder()).when(orderController).getOrder(Mockito.anyString());
		// Mockito.doReturn(Mockito.mock(FederationUser.class)).when(aaaController).getFederationUser(Mockito.anyString());
		// Mockito.doNothing().when(aaaController).authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));
		Mockito.doNothing().when(securityRuleController).deleteSecurityRule(Mockito.anyString(), Mockito.anyString(),
				Mockito.anyString(), Mockito.any(FederationUser.class));

		// exercise
		try {
			facade.deleteSecurityRule(FAKE_INSTANCE_ID, FAKE_RULE_ID, FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.PUBLIC_IP);
		} catch (InstanceNotFoundException e) {
			Assert.fail();
		}

		// verify
	}

	@Ignore
	// test case: Delete a security rule for a network via its endpoint, it should
	// return the rule id.
	@Test
	public void testDeleteSecurityRuleForNetwork() throws Exception {
		// set up
		Mockito.doReturn(createNetworkOrder()).when(orderController).getOrder(Mockito.anyString());
		// Mockito.doReturn(Mockito.mock(FederationUser.class)).when(aaaController).getFederationUser(Mockito.anyString());
		// Mockito.doNothing().when(aaaController).authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));
		Mockito.doNothing().when(securityRuleController).deleteSecurityRule(Mockito.anyString(), Mockito.anyString(),
				Mockito.anyString(), Mockito.any(FederationUser.class));

		// exercise
		try {
			facade.deleteSecurityRule(FAKE_INSTANCE_ID, FAKE_RULE_ID, FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.NETWORK);
		} catch (InstanceNotFoundException e) {
			Assert.fail();
		}

		// verify
	}

	@Ignore
	@Test
	public void testGenericRequest() throws Exception {
		// set up
		String method = "GET";
		String url = "https://www.foo.bar";
		Map<String, String> headers = new HashMap<>();
		Map<String, String> body = new HashMap<>();
		GenericRequest genericRequest = new GenericRequest(method, url, headers, body);

		String fakeResponseContent = "fooBar";
		GenericRequestResponse expectedResponse = new GenericRequestResponse(fakeResponseContent);
		CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
		PowerMockito.when(CloudConnectorFactory.getInstance()).thenReturn(cloudConnectorFactory);
		Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(localCloudConnector);
		Mockito.when(localCloudConnector.genericRequest(Mockito.eq(genericRequest), Mockito.any(FederationUser.class)))
				.thenReturn(expectedResponse);

		// exercise
		GenericRequestResponse genericRequestResponse = facade.genericRequest(FAKE_CLOUD_NAME, FAKE_MEMBER_ID,
				genericRequest, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Assert.assertEquals(expectedResponse, genericRequestResponse);
	}

	private NetworkOrder createNetworkOrder() throws Exception {
		FederationUser federationUser = null;
		// new FederationUser(FAKE_TOKEN_PROVIDER,
		// FAKE_FEDERATION_TOKEN_VALUE,
		// FAKE_USER_ID, FAKE_USER_NAME);
		NetworkOrder order = new NetworkOrder(federationUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID, "default", FAKE_NAME,
				FAKE_GATEWAY, FAKE_ADDRESS, NetworkAllocationMode.STATIC);

		NetworkInstance networtkInstanceExcepted = new NetworkInstance(order.getId());
		Mockito.doReturn(networtkInstanceExcepted).when(this.orderController)
				.getResourceInstance(Mockito.eq(order.getId()));
		order.setInstanceId(networtkInstanceExcepted.getId());

		return order;
	}

	private PublicIpOrder createPublicIpOrder() throws Exception {
		FederationUser federationUser = null;
		// new FederationUser(FAKE_TOKEN_PROVIDER,
		// FAKE_FEDERATION_TOKEN_VALUE,
		// FAKE_USER_ID, FAKE_USER_NAME);

		ComputeOrder computeOrder = new ComputeOrder();
		ComputeInstance computeInstance = new ComputeInstance(FAKE_SOURCE_ID);
		computeOrder.setInstanceId(computeInstance.getId());
		this.activeOrdersMap.put(computeOrder.getId(), computeOrder);

		PublicIpOrder order = new PublicIpOrder(federationUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID, "default",
				computeInstance.getId());

		PublicIpInstance publicIpInstance = new PublicIpInstance(order.getId());

		Mockito.doReturn(publicIpInstance).when(this.orderController).getResourceInstance(Mockito.eq(order.getId()));
		order.setInstanceId(publicIpInstance.getId());

		return order;
	}

}
