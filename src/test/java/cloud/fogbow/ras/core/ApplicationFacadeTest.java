package cloud.fogbow.ras.core;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
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
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_RULE_ID = "fake-rule-id";
	private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
	private static final String FAKE_FEDERATION_TOKEN_VALUE = "federation-token-value";
	private static final String FAKE_CLOUD_NAME = "fake-cloud-name";
	private static final String FAKE_MEMBER_ID = "fake-member-id";
	private static final String FAKE_NAME = "fake-name";
	private static final String FAKE_GATEWAY = "fake-gateway";
	private static final String FAKE_ADDRESS = "fake-address";
	private static final String FAKE_VOLUME_NAME = "fake-volume-name";
	private static final String FAKE_IMAGE_NAME = "fake-image-name";
	private static final String FAKE_PUBLIC_KEY = "fake-public-key";
	private static final String FAKE_SOURCE_ID = "fake-source-id";
	private static final String FAKE_TARGET_ID = "fake-target-id";
	private static final String FAKE_DEVICE_MOUNT_POINT = "fake-device-mount-point";
	private static final String VALID_PATH_CONF = "ras.conf";
	private static final String VALID_PATH_CONF_WITHOUT_BUILD_PROPERTY = "ras-without-build-number.conf";
	private static final String DEFAULT_BUILD_NUMBER_FORMAT = "%s-abcd";
	private static final String TESTING_MODE_BUILD_NUMBER_FORMAT = "%s-[testing mode]";
	private static final String ID_KEY = "id";
	private static final String FAKE_USER_ID_VALUE = "fake-user-id";

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

	// test case: calling createCompute with a too long public key throws an
	// InvalidParameterException.
	@Test(expected = InvalidParameterException.class) // verify
	public void testCreateComputeWithTooLongPrivateKey() throws Exception {
		// set up
		String publicKey = generateVeryLongPublicKey();
		FederationUser federationUser = null;
		ArrayList<UserData> userData = null;
		List<String> networkIds = null;

		ComputeOrder order = spyComputeOrder(federationUser, publicKey, userData, networkIds);

		// exercise
		this.facade.createCompute(order, FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: calling createCompute with a too long extra user data file content
	// throws an InvalidParameterException.
	@Test(expected = InvalidParameterException.class)
	public void testCreateComputeWithTooLongExtraUserDataFileContent() throws Exception {
		// set up
		String publicKey = FAKE_PUBLIC_KEY;
		FederationUser federationUser = null;
		ArrayList<UserData> userData = generateVeryLongUserDataFileContent();
		List<String> networkIds = null;

		ComputeOrder order = spyComputeOrder(federationUser, publicKey, userData, networkIds);

		// exercise
		this.facade.createCompute(order, FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the createCompute method the new Order passed by
	// parameter without state, it must return set to Open OrderState after its
	// activation.
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

		Assert.assertEquals(OrderState.OPEN, order.getOrderState());
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
		
		ComputeInstance computeInstance = new ComputeInstance(order.getId());
		order.setInstanceId(computeInstance.getId());
		
		Mockito.when(cloudConnector.getInstance(Mockito.eq(order))).thenReturn(computeInstance);

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
		
		Assert.assertSame(computeInstance, instance);
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

	private ComputeOrder spyComputeOrder(FederationUser federationUser, String publicKey, 
			ArrayList<UserData> userData, List<String> networkIds) {

		ComputeOrder order = Mockito.spy(
				new ComputeOrder(federationUser, 
						FAKE_MEMBER_ID, 
						FAKE_FEDERATION_TOKEN_VALUE, 
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

	// TODO on verification ...

	// test case: Check if getCompute is properly forwarding the exception thrown by
	// getFederationUser.
	@Test(expected = InvalidTokenException.class) // verify
	public void testGetComputeOrderWhenGetFederationUserThrowsAnException() throws Exception {

		// set up
		ComputeOrder order = createComputeOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doNothing().when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// exercise
		this.facade.getCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the getCompute() method with an operation not
	// authorized, it must
	// expected a UnauthorizedRequestException.
	@Test(expected = UnauthorizedRequestException.class) // verify
	public void testGetComputeOrderWithOperationNotAuthorized() throws Exception {

		// set up
		ComputeOrder order = createComputeOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doNothing().when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.getCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the method deleteCompute(), the Order passed as
	// parameter must have its state changed to Closed.
	@Test
	public void testDeleteComputeOrder() throws Exception {

		// set up
		Order order = createComputeOrder();
		order.setRequester(getLocalMemberId());
		order.setProvider(getLocalMemberId());
		OrderStateTransitioner.activateOrder(order);

		CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
		PowerMockito.when(CloudConnectorFactory.getInstance()).thenReturn(cloudConnectorFactory);
		Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(localCloudConnector);

		// exercise
		this.facade.deleteCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.localCloudConnector, Mockito.times(1)).deleteInstance(Mockito.any(Order.class));
		Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
	}

	// test case: When try calling the deleteCompute() method without
	// authentication, it must throw UnauthenticatedUserException and the Order
	// remains in the same state.
	@Test
	public void testDeleteComputeOrderWithoutAuthentication() throws Exception {

		// FIXME: improve both the name of the test and the case. I think we are testing
		// how be behave when the authentication method throws an exception not
		// "WithoutAuthentication"

		// set up
		Order order = createComputeOrder();
		OrderStateTransitioner.activateOrder(order);

		try {
			// exercise
			this.facade.deleteCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthenticatedUserException e) {
			// verify
			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: Check if deleteCompute is properly forwarding the exception thrown
	// by getFederationUser, in this case the Order must remain in the same state.
	@Test
	public void testDeleteComputeOrderWithUnauthenticatedUserExceptionInGetFederationUser() throws Exception {

		// set up
		Order order = createComputeOrder();
		OrderStateTransitioner.activateOrder(order);

		try {
			// exercise
			this.facade.deleteCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (InvalidTokenException e) {
			// verify
			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: When try calling the deleteCompute() method with an operation not
	// authorized, it must throw UnauthorizedRequestException and the Order remains
	// in the same state.
	@Test
	public void testDeleteComputeOrderWithOperationNotAuthorizedException() throws Exception {

		// set up
		Order order = createComputeOrder();
		OrderStateTransitioner.activateOrder(order);

		try {
			// exercise
			this.facade.deleteCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthorizedRequestException e) {
			// verify
			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: When calling the createVolume() method the new Order passed by
	// parameter without
	// state, it must return set to Open OrderState after its activation.
	@Test
	public void testCreateVolumeOrder() throws Exception {

		// set up
		VolumeOrder order = createVolumeOrder();
		// verifying that the created order is null
		Assert.assertNull(order.getOrderState());

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doNothing().when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

		// exercise
		this.facade.createVolume(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		// Mockito.verify(this.aaaController, Mockito.times(1)).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class),
		// Mockito.any(ResourceType.class));

		Assert.assertEquals(OrderState.OPEN, order.getOrderState());
	}

	// test case: When try calling the createVolume() method without
	// authentication, it must throw UnauthenticatedUserException.
	@Test
	public void testCreateVolumeOrderWithoutAuthentication() throws Exception {

		// set up
		VolumeOrder order = createVolumeOrder();

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

		try {
			// exercise
			this.facade.createVolume(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthenticatedUserException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: Check if createVolume is properly forwarding the exception thrown
	// by
	// getFederationUser.
	@Test
	public void testCreateVolumeOrderWhenGetFederationUserThrowsAnException() throws Exception {

		// set up
		VolumeOrder order = createVolumeOrder();
		Assert.assertNull(order.getOrderState());

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		try {
			// exercise
			this.facade.createVolume(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (InvalidTokenException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: When try calling the createVolume() method with operation not
	// authorized, it must
	// throw UnauthorizedRequestException.
	@Test
	public void testCreateVolumeOrderWithOperationNotAuthorized() throws Exception {

		// set up
		VolumeOrder order = createVolumeOrder();
		Assert.assertNull(order.getOrderState());

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class),
		// Mockito.any(ResourceType.class));

		try {
			// exercise
			this.facade.createVolume(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthorizedRequestException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1)).
			// authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class),
			// Mockito.any(ResourceType.class));

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: When calling the getVolume() method, it must return the
	// VolumeInstance of the
	// OrderID passed per parameter.
	@Test
	public void testGetVolumeOrder() throws Exception {

		// set up
		VolumeOrder volumeOrder = createVolumeOrder();
		OrderStateTransitioner.activateOrder(volumeOrder);

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doReturn(volumeOrder.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doNothing().when(this.aaaController)
		// .authorize(Mockito.anyString(), Mockito.any(FederationUser.class),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

		// exercise
		VolumeInstance volumeInstance = this.facade.getVolume(volumeOrder.getId(), "");

		// verify
		Assert.assertSame(volumeOrder.getInstanceId(), volumeInstance.getId());
	}

	// test case: When calling the getVolume() method without authentication, it
	// must
	// throw a UnauthenticatedUserException.
	@Test(expected = UnauthenticatedUserException.class) // verify
	public void testGetVolumeOrderWithoutAuthentication() throws Exception {

		// set up
		VolumeOrder order = createVolumeOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.getVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: Check if getVolume is properly forwarding the exception thrown by
	// getFederationUser.
	@Test(expected = InvalidTokenException.class) // verify
	public void testGetVolumeOrderWhenGetFederationUserThrowsAnException() throws Exception {

		// set up
		VolumeOrder order = createVolumeOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// exercise
		this.facade.getVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the getVolume() method with operation not authorized,
	// it must
	// expected a UnauthorizedRequestException.
	@Test(expected = UnauthorizedRequestException.class) // verify
	public void testGetVolumeOrderWithOperationNotAuthorized() throws Exception {

		// set up
		VolumeOrder order = createVolumeOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.getVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the method deleteVolume(), the Order passed per
	// parameter must return
	// its state to Closed.
	@Test
	public void testDeleteVolumeOrder() throws Exception {

		// set up
		VolumeOrder order = createVolumeOrder();
		OrderStateTransitioner.activateOrder(order);
		order.setRequester(getLocalMemberId());
		order.setProvider(getLocalMemberId());

		CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
		PowerMockito.when(CloudConnectorFactory.getInstance()).thenReturn(cloudConnectorFactory);
		Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(localCloudConnector);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doNothing().when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.deleteVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		// Mockito.verify(this.aaaController, Mockito.times(1)).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
	}

	// test case: When try calling the deleteVolume() method without user
	// authentication, it must
	// throw UnauthenticatedUserException and the Order remains in the same state.
	@Test
	public void testDeleteVolumeOrderWithoutAuthentication() throws Exception {

		// set up
		VolumeOrder order = createVolumeOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		try {
			// exercise
			this.facade.deleteVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthenticatedUserException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
			// Mockito.any(Order.class));

			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: Check if DeleteVolume is properly forwarding the exception thrown
	// by
	// getFederationUser, in this case the Order must remains in the same state.
	@Test
	public void testDeleteVolumeOrderWhenGetFederationUserThrowsAnException() throws Exception {

		// set up
		VolumeOrder order = createVolumeOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		try {
			// exercise
			this.facade.deleteVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (InvalidTokenException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: When try calling the deleteVolume() method with operation not
	// authorized, it must
	// throw UnauthorizedRequestException and the Order remains in the same state.
	@Test
	public void testDeleteVolumeOrderWithOperationNotAuthorized() throws Exception {

		// set up
		VolumeOrder order = createVolumeOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		try {
			// exercise
			this.facade.deleteVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthorizedRequestException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1)).
			// authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
			// Mockito.any(Order.class));

			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: When calling the createNetwork() method the new Order passed by
	// parameter without
	// state, it must return set to Open OrderState after its activation.
	@Test
	public void testCreateNetworkOrder() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();
		// verifying that the created order is null
		Assert.assertNull(order.getOrderState());

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doNothing().when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

		// exercise
		this.facade.createNetwork(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		// Mockito.verify(this.aaaController, Mockito.times(1)).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class),
		// Mockito.any(ResourceType.class));

		Assert.assertEquals(OrderState.OPEN, order.getOrderState());
	}

	// test case: When try calling the createNetwork() method without
	// authentication, it must throw UnauthenticatedUserException.
	@Test
	public void testCreateNetworkOrderWithoutAuthentication() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

		try {
			// exercise
			this.facade.createNetwork(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthenticatedUserException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: Check if createNetwork is properly forwarding the exception thrown
	// by
	// getFederationUser.
	@Test
	public void testCreateNetworkOrderWhenGetFederationUserThrowsAnException() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();
		Assert.assertNull(order.getOrderState());

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		try {
			// exercise
			this.facade.createNetwork(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (InvalidTokenException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: When try calling the createNetwork() method with an operation not
	// authorized, it
	// must throw UnauthorizedRequestException.
	@Test
	public void testCreateNetworkOrderWithOperationNotAuthorized() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();
		Assert.assertNull(order.getOrderState());

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doNothing().when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class),
		// Mockito.any(ResourceType.class));

		try {
			// exercise
			this.facade.createNetwork(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthorizedRequestException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1)).
			// authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class),
			// Mockito.any(ResourceType.class));

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: When calling the getNetwork() method, it must return the
	// NetworkInstance of the
	// OrderID passed per parameter.
	@Test
	public void testGetNetworkOrder() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		NetworkInstance networkInstanceExcepted = new NetworkInstance("");
		Mockito.doReturn(networkInstanceExcepted).when(this.orderController)
				.getResourceInstance(Mockito.eq(order.getId()));

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doNothing().when(this.aaaController)
		// .authorize(Mockito.anyString(), Mockito.any(FederationUser.class),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

		// exercise
		NetworkInstance actualInstance = this.facade.getNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.orderController, Mockito.times(1)).getResourceInstance(Mockito.eq(order.getId()));

		Assert.assertSame(networkInstanceExcepted, actualInstance);
	}

	// test case: When calling the getNetwork() method without authentication, it
	// must
	// expected a UnauthenticatedUserException.
	@Test(expected = UnauthenticatedUserException.class) // verify
	public void testGetNetworkOrderWithoutAuthentication() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.getNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: Check if getNetwork is properly forwarding the exception thrown by
	// getFederationUser.
	@Test(expected = InvalidTokenException.class) // verify
	public void testGetNetworkOrderWhenGetFederationUserThrowsAnException() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// exercise
		this.facade.getNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the getNetwork() method with an operation not
	// authorized, it must
	// expected a UnauthorizedRequestException.
	@Test(expected = UnauthorizedRequestException.class) // verify
	public void testGetNetworkOrderWithOperationNotAuthorized() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.getNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the method deleteNetwork(), the Order passed per
	// parameter must
	// return its state to Closed.
	@Test
	public void testDeleteNetworkOrder() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();
		OrderStateTransitioner.activateOrder(order);
		order.setRequester(getLocalMemberId());
		order.setProvider(getLocalMemberId());

		CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
		PowerMockito.when(CloudConnectorFactory.getInstance()).thenReturn(cloudConnectorFactory);
		Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(localCloudConnector);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doNothing().when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.deleteNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		// Mockito.verify(this.aaaController, Mockito.times(1)).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
	}

	// test case: When try calling the deleteNetwork() method without user
	// authentication, it must
	// throw UnauthenticatedUserException and the Order remains in the same state.
	@Test
	public void testDeleteNetworkOrderWithoutAuthentication() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		try {
			// exercise
			this.facade.deleteNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthenticatedUserException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
			// Mockito.any(Order.class));

			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: Check if deleteNetwork is properly forwarding the exception thrown
	// by
	// getFederationUser, in this case the Order must remains in the same state.
	@Test
	public void testDeleteNetworkOrderWhenGetFederationUserThrowsAnException() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		try {
			// exercise
			this.facade.deleteNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (InvalidTokenException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: When try calling the deleteNetwork() method performing an
	// operation without
	// authorization, it must throw UnauthorizedRequestException and the Order
	// remains in the same
	// state.
	@Test
	public void testDeleteNetworkOrderWithOperationNotAuthorized() throws Exception {

		// set up
		NetworkOrder order = createNetworkOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		try {
			// exercise
			this.facade.deleteNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthorizedRequestException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1)).
			// authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
			// Mockito.any(Order.class));

			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: When try calling the createAttachment() method without
	// authentication, it must throw UnauthenticatedUserException.
	@Test
	public void testCreateAttachmentOrderWithoutAuthentication() throws Exception {

		// set up
		AttachmentOrder order = createAttachmentOrder();

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

		try {
			// exercise
			this.facade.createAttachment(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthenticatedUserException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: Check if createAttachment is properly forwarding the exception
	// thrown by
	// getFederationUser.
	@Test
	public void testCreateAttachmentOrderWhenGetFederationUserThrowsAnException() throws Exception {

		// set up
		AttachmentOrder order = createAttachmentOrder();

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		try {
			// exercise
			this.facade.createAttachment(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (InvalidTokenException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: When try calling the createAttachment() method with an operation
	// not authorized,
	// it
	// must throw UnauthorizedRequestException.
	@Test
	public void testCreateAttachmentOrderWithOperationNotAuthorized() throws Exception {

		// set up
		AttachmentOrder order = createAttachmentOrder();
		Assert.assertNull(order.getOrderState());

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class),
		// Mockito.any(ResourceType.class));

		try {
			// exercise
			this.facade.createAttachment(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthorizedRequestException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1)).
			// authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class),
			// Mockito.any(ResourceType.class));

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: When calling the getAttachment() method, it must return the
	// AttachmentInstance of
	// the
	// OrderID passed per parameter.
	@Test
	public void testGetAttachmentOrder() throws Exception {

		// set up
		AttachmentOrder order = createAttachmentOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doNothing().when(this.aaaController)
		// .authorize(Mockito.anyString(), Mockito.any(FederationUser.class),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

		// exercise
		AttachmentInstance actualInstance = this.facade.getAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Assert.assertNotNull(actualInstance);
	}

	// test case: When calling the getAttachment() method without authentication, it
	// must
	// expected a UnauthenticatedUserException.
	@Test
	// verify
	(expected = UnauthenticatedUserException.class)
	public void testGetAttachmentOrderWithoutAuthentication() throws Exception {

		// set up
		AttachmentOrder order = createAttachmentOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.getAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: Check if getAttachment is properly forwarding the exception thrown
	// by
	// getFederationUser.
	@Test(expected = InvalidTokenException.class) // verify
	public void testGetAttachmentOrderWhenGetFederationUserThrowsAnException() throws Exception {

		// set up
		AttachmentOrder order = createAttachmentOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doNothing().when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// exercise
		this.facade.getAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the getAttachment() method performing an operation
	// without
	// authorization, it must expected a UnauthorizedRequestException.
	@Test(expected = UnauthorizedRequestException.class) // verify
	public void testGetAttachmentOrderWithOperationNotAuthorized() throws Exception {

		// set up
		AttachmentOrder order = createAttachmentOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.getAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the method deleteAttachment(), the Order passed per
	// parameter must
	// return its state to Closed.
	@Test
	public void testDeleteAttachmentOrder() throws Exception {

		// set up
		AttachmentOrder order = createAttachmentOrder();
		OrderStateTransitioner.activateOrder(order);
		order.setRequester(getLocalMemberId());
		order.setProvider(getLocalMemberId());

		CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
		PowerMockito.when(CloudConnectorFactory.getInstance()).thenReturn(cloudConnectorFactory);
		Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(localCloudConnector);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doNothing().when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.deleteAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		// Mockito.verify(this.aaaController, Mockito.times(1)).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
	}

	// test case: When try calling the deleteAttachment() method without user
	// authentication, it
	// must throw UnauthenticatedUserException and the Order remains in the same
	// state.
	@Test
	public void testDeleteAttachmentOrderWithoutAuthentication() throws Exception {

		// set up
		AttachmentOrder order = createAttachmentOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		try {
			// exercise
			this.facade.deleteAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthenticatedUserException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
			// Mockito.any(Order.class));

			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: Check if deleteAttachment is properly forwarding the exception
	// thrown by
	// getFederationUser, in this case the Order must remains in the same state.
	@Test
	public void testDeleteAttachmentOrderWhenGetFederationUserThrowsAnException() throws Exception {

		// set up
		AttachmentOrder order = createAttachmentOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		try {
			// exercise
			this.facade.deleteAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (InvalidTokenException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);
			// Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: When try calling the deleteAttachment() method with operation not
	// authorized, it
	// must throw UnauthorizedRequestException and the Order remains in the same
	// state.
	@Test
	public void testDeleteAttachmentOrderWithOperationNotAuthorized() throws Exception {

		// set up
		AttachmentOrder order = createAttachmentOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		try {
			// exercise
			this.facade.deleteAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthorizedRequestException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1)).
			// authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
			// Mockito.any(Order.class));

			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: When trying to call the deletePublicIp() method without privileges
	// to do the operation
	// UnauthorizedRequestException must be thrown and the Order must remain in the
	// same state.
	@Test
	public void testDeletePublicIpOrderWithOperationNotAuthorized() throws Exception {
		// set up
		PublicIpOrder order = createPublicIpOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		try {
			// exercise
			this.facade.deletePublicIp(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthorizedRequestException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1)).
			// authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
			// Mockito.any(Order.class));

			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: Check if deletePublicIp is properly forwarding the exception
	// thrown by
	// getFederationUser, in this case the Order must remain in the same state.
	@Test
	public void testDeletePublicIpOrderWhenGetFederationUserThrowsAnException() throws Exception {
		// set up
		PublicIpOrder order = createPublicIpOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		try {
			// exercise
			this.facade.deletePublicIp(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (InvalidTokenException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);
			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: When trying to call the deletePublicIp() method with no user
	// authenticated
	// UnauthenticatedUserException must be thrown and the Order must remain in the
	// same state.
	@Test
	public void testDeletePublicIpOrderWithoutAuthentication() throws Exception {
		// set up
		PublicIpOrder order = createPublicIpOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		try {
			// exercise
			this.facade.deletePublicIp(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthenticatedUserException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
			// Mockito.any(Order.class));

			Assert.assertEquals(OrderState.OPEN, order.getOrderState());
		}
	}

	// test case: When calling the method deletePublicIp(), the Order passed per
	// parameter must
	// advance its state to CLOSED.
	@Test
	public void testDeletePublicIpOrder() throws Exception {
		// set up
		PublicIpOrder order = createPublicIpOrder();
		OrderStateTransitioner.activateOrder(order);
		order.setRequester(getLocalMemberId());
		order.setProvider(getLocalMemberId());

		// Mockito.doNothing().when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
		PowerMockito.when(CloudConnectorFactory.getInstance()).thenReturn(cloudConnectorFactory);
		Mockito.when(cloudConnectorFactory.getCloudConnector(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(localCloudConnector);

		// exercise
		this.facade.deletePublicIp(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		// Mockito.verify(this.aaaController, Mockito.times(1)).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		Assert.assertEquals(OrderState.CLOSED, order.getOrderState());
	}

	// test case: When calling the getPublicIp() method performing an operation
	// without
	// authorization, it must throw a UnauthorizedRequestException.
	@Test(expected = UnauthorizedRequestException.class) // verify
	public void testGetPublicIpOrderWithOperationNotAuthorized() throws Exception {
		// set up
		PublicIpOrder order = createPublicIpOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.getPublicIp(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: Checks if getPublicIp is properly forwarding the exception thrown
	// by
	// getFederationUser.
	@Test(expected = InvalidTokenException.class) // verify
	public void testGetPublicIpOrderWhenGetFederationUserThrowsAnException() throws Exception {
		// set up
		PublicIpOrder order = createPublicIpOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doNothing().when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// exercise
		this.facade.getPublicIp(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the getPublicIp() method without authentication, it
	// must
	// throw a UnauthenticatedUserException.
	@Test(expected = UnauthenticatedUserException.class)
	public void testGetPublicIpOrderWithoutAuthentication() throws Exception {
		// set up
		PublicIpOrder order = createPublicIpOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class),
		// Mockito.any(Order.class));

		// exercise
		this.facade.getPublicIp(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);
	}

	// test case: When calling the getPublicIp() method, it must return the
	// PublicIpInstance of
	// the orderId passed as parameter.
	@Test
	public void testGetPublicIpOrder() throws Exception {
		// set up
		PublicIpOrder order = createPublicIpOrder();
		OrderStateTransitioner.activateOrder(order);

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doNothing().when(this.aaaController)
		// .authorize(Mockito.anyString(), Mockito.any(FederationUser.class),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

		// exercise
		PublicIpInstance actualInstance = this.facade.getPublicIp(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Assert.assertNotNull(actualInstance);
	}

	// test case: When calling the createPublicIp() method without
	// authentication, it must throw a UnauthenticatedUserException.
	@Test
	public void testCreatePublicIpOrderWithoutAuthentication() throws Exception {
		// set up
		PublicIpOrder order = createPublicIpOrder();

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthenticatedUserException()).when(this.aaaController)
		// .authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

		try {
			// exercise
			this.facade.createPublicIp(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthenticatedUserException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class), Mockito.any(ResourceType.class));

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: Checks if createPublicIp() is properly forwarding the exception
	// thrown by
	// getFederationUser.
	@Test
	public void testCreatePublicIpOrderWhenGetFederationUserThrowsAnException() throws Exception {
		// set up
		PublicIpOrder order = createPublicIpOrder();

		// Mockito.doNothing().when(this.aaaController)
		// .authenticate(Mockito.anyString(), Mockito.any(FederationUser.class));

		// Mockito.doThrow(new InvalidTokenException()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		try {
			// exercise
			this.facade.createPublicIp(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (InvalidTokenException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1))
			// .getFederationUser(FAKE_FEDERATION_TOKEN_VALUE);

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: When calling the createPublicIp() method with an operation that is
	// not authorized,
	// it must throw a UnauthorizedRequestException.
	@Test
	public void testCreatePublicIpOrderWithOperationNotAuthorized() throws Exception {
		// set up
		PublicIpOrder order = createPublicIpOrder();
		Assert.assertNull(order.getOrderState());

		// Mockito.doReturn(order.getFederationUser()).when(this.aaaController)
		// .getFederationUser(Mockito.anyString());

		// Mockito.doThrow(new UnauthorizedRequestException()).when(this.aaaController).
		// authenticateAndAuthorize(Mockito.anyString(),
		// Mockito.any(FederationUser.class), Mockito.anyString(),
		// Mockito.any(Operation.class),
		// Mockito.any(ResourceType.class));

		try {
			// exercise
			this.facade.createPublicIp(order, FAKE_FEDERATION_TOKEN_VALUE);
			Assert.fail();
		} catch (UnauthorizedRequestException e) {
			// verify
			// Mockito.verify(this.aaaController, Mockito.times(1)).
			// authenticateAndAuthorize(Mockito.anyString(),
			// Mockito.any(FederationUser.class), Mockito.anyString(),
			// Mockito.any(Operation.class),
			// Mockito.any(ResourceType.class));

			Assert.assertNull(order.getOrderState());
		}
	}

	// test case: Creating a security rule for a network via public ip endpoint, it
	// should raise an InstanceNotFoundException.
	@Test
	public void testCreateSecurityRuleForNetworkViaPublicIp() throws Exception {
		// set up
		Mockito.doReturn(createNetworkOrder()).when(orderController).getOrder(Mockito.anyString());

		// exercise
		try {
			facade.createSecurityRule(FAKE_INSTANCE_ID, Mockito.mock(SecurityRule.class), FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.PUBLIC_IP);
			// verify
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// Exception thrown
		}
	}

	// test case: Creating a security rule for a public ip via network endpoint, it
	// should raise an InstanceNotFoundException.
	@Test
	public void testCreateSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		Mockito.doReturn(createPublicIpOrder()).when(orderController).getOrder(Mockito.anyString());

		// exercise
		try {
			facade.createSecurityRule(FAKE_INSTANCE_ID, Mockito.mock(SecurityRule.class), FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.NETWORK);
			// verify
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// Exception thrown
		}
	}

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

	// test case: Get all security rules from a network via public ip endpoint, it
	// should raise an InstanceNotFoundException.
	@Test
	public void testGetSecurityRulesForNetworkViaPublicIp() throws Exception {
		// set up
		Mockito.doReturn(createNetworkOrder()).when(orderController).getOrder(Mockito.anyString());

		// exercise
		try {
			facade.getAllSecurityRules(FAKE_INSTANCE_ID, FAKE_FEDERATION_TOKEN_VALUE, ResourceType.PUBLIC_IP);
			// verify
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// Exception thrown
		}
	}

	// test case: Get all security rules from a public ip via network endpoint, it
	// should raise an InstanceNotFoundException.
	@Test
	public void testGetSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		Mockito.doReturn(createPublicIpOrder()).when(orderController).getOrder(Mockito.anyString());

		// exercise
		try {
			facade.getAllSecurityRules(FAKE_INSTANCE_ID, FAKE_FEDERATION_TOKEN_VALUE, ResourceType.NETWORK);
			// verify
			Assert.fail();
		} catch (InstanceNotFoundException e) {
			// Exception thrown
		}
	}

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

	// test case: Delete a security rule from a network via public ip endpoint, it
	// should raise an InstanceNotFoundException.
	@Test
	public void testDeleteSecurityRulesForNetworkViaPublicIp() throws Exception {
		// set up
		Mockito.doReturn(createNetworkOrder()).when(orderController).getOrder(Mockito.anyString());

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

	// test case: Delete a security rule from a public ip via network endpoint, it
	// should raise an InstanceNotFoundException.
	@Test
	public void testDeleteSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		Mockito.doReturn(createPublicIpOrder()).when(orderController).getOrder(Mockito.anyString());

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

	private VolumeOrder createVolumeOrder() throws Exception {
		FederationUser federationUser = null;
		// new FederationUser(FAKE_TOKEN_PROVIDER,
		// FAKE_FEDERATION_TOKEN_VALUE,
		// FAKE_USER_ID, FAKE_USER_NAME);
		VolumeOrder order = new VolumeOrder(federationUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID, "default", FAKE_VOLUME_NAME,
				1);

		VolumeInstance volumeInstanceExcepted = new VolumeInstance(order.getId());
		Mockito.doReturn(volumeInstanceExcepted).when(this.orderController)
				.getResourceInstance(Mockito.eq(order.getId()));
		order.setInstanceId(volumeInstanceExcepted.getId());

		return order;
	}

	// FIXME fix this method with the testCreateComputeOrder implementation...
	private ComputeOrder createComputeOrder() throws Exception {
		FederationUser federationUser = Mockito.mock(FederationUser.class);

		ComputeOrder order = Mockito.spy(new ComputeOrder(federationUser, FAKE_MEMBER_ID, FAKE_MEMBER_ID, "default",
				FAKE_INSTANCE_NAME, 2, 2, 30, FAKE_IMAGE_NAME, mockUserData(), FAKE_PUBLIC_KEY, null));

		ComputeInstance computeInstance = new ComputeInstance(order.getId());
		order.setInstanceId(computeInstance.getId());
		return order;
	}

	private AttachmentOrder createAttachmentOrder() throws Exception {
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

		AttachmentOrder order = new AttachmentOrder(FAKE_MEMBER_ID, "default", computeOrderId, volumeOrderId,
				FAKE_DEVICE_MOUNT_POINT);

		AttachmentInstance attachmentInstance = new AttachmentInstance(order.getId());

		Mockito.doReturn(attachmentInstance).when(this.orderController).getResourceInstance(Mockito.eq(order.getId()));
		order.setInstanceId(attachmentInstance.getId());

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
