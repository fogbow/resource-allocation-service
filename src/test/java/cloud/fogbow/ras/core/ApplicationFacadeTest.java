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
import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.exceptions.UnauthenticatedUserException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationController;
import cloud.fogbow.common.util.AuthenticationUtil;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.RSAUtil;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.ras.core.cloudconnector.CloudConnector;
import cloud.fogbow.ras.core.cloudconnector.CloudConnectorFactory;
import cloud.fogbow.ras.core.constants.SystemConstants;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteGetCloudNamesRequest;
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
@PrepareForTest({ PublicKeysHolder.class, AuthenticationUtil.class, CloudConnectorFactory.class, 
	DatabaseManager.class, RemoteGetCloudNamesRequest.class, RSAUtil.class, ServiceAsymmetricKeysHolder.class, 
	SharedOrderHolders.class })
public class ApplicationFacadeTest extends BaseUnitTests {

	private static final String DEFAULT_CLOUD_NAME = "default";
	private static final String DEFAULT_BUILD_NUMBER_FORMAT = "%s-abcd";
	private static final String FAKE_ADDRESS = "fake-address";
	private static final String FAKE_PUBLIC_KEY = "fake-public-key";
	private static final String FAKE_CLOUD_NAME = "fake-cloud-name";
	private static final String FAKE_CONTENT = "fooBar";
	private static final String FAKE_DEVICE_MOUNT_POINT = "fake-device-mount-point";
	private static final String FAKE_FEDERATION_TOKEN_VALUE = "federation-token-value";
	private static final String FAKE_GATEWAY = "fake-gateway";
	private static final String FAKE_IMAGE_NAME = "fake-image-name";
	private static final String FAKE_INSTANCE_ID = "fake-instance-id";
	private static final String FAKE_INSTANCE_NAME = "fake-instance-name";
	private static final String FAKE_LOCAL_IDENTITY_MEMBER = "fake-localidentity-member";
	private static final String FAKE_MEMBER_ID_VALUE = "fake-member-id";
	private static final String FAKE_NAME_VALUE = "fake-name";
	private static final String FAKE_RULE_ID = "fake-rule-id";
	private static final String FAKE_SOURCE_ID = "fake-source-id";
	private static final String FAKE_TARGET_ID = "fake-target-id";
	private static final String FAKE_URL = "https://www.foo.bar";
	private static final String FAKE_USER_ID_VALUE = "fake-user-id";
	private static final String FAKE_VOLUME_NAME = "fake-volume-name";
	private static final String GET_METHOD = "GET";
	private static final String ID_KEY = "id";
	private static final String NAME_KEY = "name";
	private static final String PROVIDER_KEY = "provider";
	private static final String TESTING_MODE_BUILD_NUMBER_FORMAT = "%s-[testing mode]";
	private static final String TOKEN_KEY = "token";
	private static final String VALID_PATH_CONF = "ras.conf";
	private static final String VALID_PATH_CONF_WITHOUT_BUILD_PROPERTY = "ras-without-build-number.conf";

	private static final int CPU_VALUE = 2;
	private static final int DISK_VALUE = 30;
	private static final int MEMORY_VALUE = 2;
	
	private ApplicationFacade facade;
	private OrderController orderController;

	@Before
	public void setUp() throws UnexpectedException {
		super.mockReadOrdersFromDataBase();
		this.orderController = Mockito.spy(new OrderController());
		this.facade = Mockito.spy(ApplicationFacade.getInstance());
		this.facade.setOrderController(this.orderController);
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
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String publicKey = FAKE_PUBLIC_KEY;
		ArrayList<UserData> userData = super.mockUserData();
		List<String> networkIds = null;

		ComputeOrder order = spyComputeOrder(federationUser, publicKey, userData, networkIds);

		ComputeInstance computeInstance = new ComputeInstance(order.getId());
		order.setInstanceId(computeInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createCompute(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getCompute method, it must return the
	// ComputeInstance of the Order ID passed per parameter.
	@Test
	public void testGetComputeOrder() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
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
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

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
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
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
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deleteCompute(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the computeOrders.
	@Test
	public void testGetAllComputeOrdersInstancesStatus() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String publicKey = FAKE_PUBLIC_KEY;
		ArrayList<UserData> userData = super.mockUserData();
		List<String> networkIds = null;

		ComputeOrder order = spyComputeOrder(federationUser, publicKey, userData, networkIds);
		OrderStateTransitioner.activateOrder(order);

		List<InstanceStatus> expectedInstancesStatus = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.COMPUTE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(1)).getInstancesStatus(Mockito.eq(federationUser),
				Mockito.eq(ResourceType.COMPUTE));

		Assert.assertEquals(expectedInstancesStatus, instancesStatus);
	}

	// test case: When calling the createVolume method with a new order passed by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreateVolumeOrder() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		VolumeOrder order = spyVolumeOrder(federationUser);

		VolumeInstance volumeInstance = new VolumeInstance(order.getId());
		order.setInstanceId(volumeInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createVolume(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getVolume method, it must return the
	// VolumeInstance of the Order ID passed per parameter.
	@Test
	public void testGetVolumeOrder() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
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
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

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
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
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
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deleteVolume(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the volumeOrders.
	@Test
	public void testGetAllVolumeOrdersInstancesStatus() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		VolumeOrder order = spyVolumeOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		List<InstanceStatus> expectedInstancesStatus = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.VOLUME);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(1)).getInstancesStatus(Mockito.eq(federationUser),
				Mockito.eq(ResourceType.VOLUME));

		Assert.assertEquals(expectedInstancesStatus, instancesStatus);
	}

	// test case: When calling the createNetwork method with a new order passed by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreateNetworkOrder() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		NetworkOrder order = spyNetworkOrder(federationUser);

		NetworkInstance networkInstance = new NetworkInstance(order.getId());
		order.setInstanceId(networkInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createNetwork(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getNetwork method, it must return the
	// NetworkInstance of the Order ID passed per parameter.
	@Test
	public void testGetNetworkOrder() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
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
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

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
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
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
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deleteNetwork(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the networkOrders.
	@Test
	public void testGetAllNetworkOrdersInstancesStatus() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		NetworkOrder order = spyNetworkOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		List<InstanceStatus> expectedInstancesStatus = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.NETWORK);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(1)).getInstancesStatus(Mockito.eq(federationUser),
				Mockito.eq(ResourceType.NETWORK));

		Assert.assertEquals(expectedInstancesStatus, instancesStatus);
	}

	// test case: When calling the createAttachment method with a new order passed
	// by parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreateAttachmentOrder() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		AttachmentOrder order = spyAttachmentOrder(federationUser);

		AttachmentInstance attachmentInstance = new AttachmentInstance(order.getId());
		order.setInstanceId(attachmentInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createAttachment(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getAttachment method, it must return the
	// AttachmentInstance of the Order ID passed per parameter.
	@Test
	public void testGetAttachmentOrder() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
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
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

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
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
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
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deleteAttachment(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(2)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.orderController, Mockito.times(1)).getCloudConnector(Mockito.eq(order));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the attachmentOrders.
	@Test
	public void testGetAllAttachmentOrdersInstancesStatus() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		AttachmentOrder order = spyAttachmentOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		List<InstanceStatus> expectedInstancesStatus = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.ATTACHMENT);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(1)).getInstancesStatus(Mockito.eq(federationUser),
				Mockito.eq(ResourceType.ATTACHMENT));

		Assert.assertEquals(expectedInstancesStatus, instancesStatus);
	}

	// test case: When calling the createPublicIp method with a new order passed by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreatePublicIpOrder() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		PublicIpOrder order = spyPublicIpOrder(federationUser);

		PublicIpInstance publicIpInstance = new PublicIpInstance(order.getId());
		order.setInstanceId(publicIpInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createPublicIp(order, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getPublicIp method, it must return the
	// PublicIpInstance of the OrderID passed per parameter.
	@Test
	public void testGetPublicIpOrder() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
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
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

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
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
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
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deletePublicIp(order.getId(), FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the publiIpOrders.
	@Test
	public void testGetAllPublicIpOrdersInstancesStatus() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
		
		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		PublicIpOrder order = spyPublicIpOrder(federationUser);
		OrderStateTransitioner.activateOrder(order);

		List<InstanceStatus> expectedInstancesStatus = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.PUBLIC_IP);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());

		Mockito.verify(this.orderController, Mockito.times(1)).getInstancesStatus(Mockito.eq(federationUser),
				Mockito.eq(ResourceType.PUBLIC_IP));

		Assert.assertEquals(expectedInstancesStatus, instancesStatus);
	}

	// test case: Creating a security rule for a network via a public IP's
	// endpoint, it must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testCreateSecurityRuleForNetworkViaPublicIp() throws Exception {
		// set up
		FederationUser federationUser = null;
		NetworkOrder order = spyNetworkOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);

		// exercise
		this.facade.createSecurityRule(FAKE_INSTANCE_ID, securityRule, FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.PUBLIC_IP);
	}

	// test case: Creating a security rule for a public IP via a network's
	// endpoint, it must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testCreateSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		FederationUser federationUser = null;
		PublicIpOrder order = spyPublicIpOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);

		// exercise
		this.facade.createSecurityRule(FAKE_INSTANCE_ID, securityRule, FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.NETWORK);
	}

	// test case: Get all security rules from a network via a public IP's endpoint,
	// it must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testGetSecurityRulesForNetworkViaPublicIp() throws Exception {
		// set up
		FederationUser federationUser = null;
		NetworkOrder order = spyNetworkOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		// exercise
		this.facade.getAllSecurityRules(FAKE_INSTANCE_ID, FAKE_FEDERATION_TOKEN_VALUE, ResourceType.PUBLIC_IP);
	}

	// test case: Get all security rules from a public IP via a network's endpoint,
	// it must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testGetSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		FederationUser federationUser = null;
		PublicIpOrder order = spyPublicIpOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		// exercise
		this.facade.getAllSecurityRules(FAKE_INSTANCE_ID, FAKE_FEDERATION_TOKEN_VALUE, ResourceType.NETWORK);
	}
	
	// test case: Delete a security rule from a network via public IP's endpoint, it
	// must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testDeleteSecurityRulesForNetworkViaPublicIp() throws Exception {
		// set up
		FederationUser federationUser = null;
		NetworkOrder order = spyNetworkOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		// exercise
		this.facade.deleteSecurityRule(FAKE_INSTANCE_ID, FAKE_RULE_ID, FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.PUBLIC_IP);
	}

	// test case: Delete a security rule from a public IP via a network's endpoint,
	// it must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testDeleteSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		FederationUser federationUser = null;
		PublicIpOrder order = spyPublicIpOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		// exercise
		this.facade.deleteSecurityRule(FAKE_INSTANCE_ID, FAKE_RULE_ID, FAKE_FEDERATION_TOKEN_VALUE,
				ResourceType.NETWORK);
	}
	
	// test case: Creating a security rule for a public IP via its endpoint, it must
	// return the rule id, if the createSecurityRule method does not throw an
	// exception.
	@Test
	public void testCreateSecurityRuleForPublicIp() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();

		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		PublicIpOrder order = spyPublicIpOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);
		Mockito.doReturn(FAKE_INSTANCE_ID).when(cloudConnector).requestSecurityRule(order, securityRule,
				federationUser);

		// exercise
		try {
			this.facade.createSecurityRule(order.getId(), securityRule, FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.PUBLIC_IP);
		} catch (InstanceNotFoundException e) {
			// verify
			Assert.fail();
		}

		// verify
		Mockito.verify(this.orderController, Mockito.times(1)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(cloudConnector, Mockito.times(1)).requestSecurityRule(Mockito.any(), Mockito.eq(securityRule),
				Mockito.eq(federationUser));
	}

	// test case: Creating a security rule for a network via its endpoint, it must
	// return the rule id, if the createSecurityRule method does not throw an
	// exception.
	@Test
	public void testCreateSecurityRuleForNetwork() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();

		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);
		
		NetworkOrder order = spyNetworkOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);
		
		SecurityRule securityRule = Mockito.mock(SecurityRule.class);
		Mockito.doReturn(FAKE_INSTANCE_ID).when(cloudConnector).requestSecurityRule(order, securityRule,
				federationUser);

		// exercise
		try {
			this.facade.createSecurityRule(order.getId(), securityRule, FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.NETWORK);
		} catch (InstanceNotFoundException e) {
			// verify
			Assert.fail();
		}

		// verify
		Mockito.verify(this.orderController, Mockito.times(1)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(cloudConnector, Mockito.times(1)).requestSecurityRule(Mockito.any(), Mockito.eq(securityRule),
				Mockito.eq(federationUser));
	}

	// test case: Get all security rules for a public IP via its endpoint, it must
	// return a list of rules if the getAllSecurityRules method does not throw an
	// exception.
	@Test
	public void testGetSecurityRuleForPublicIp() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();

		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		PublicIpOrder order = spyPublicIpOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);
		securityRule.setInstanceId(FAKE_INSTANCE_ID);

		List<SecurityRule> expectedSecurityRules = new ArrayList<>();
		expectedSecurityRules.add(securityRule);

		Mockito.doReturn(expectedSecurityRules).when(cloudConnector).getAllSecurityRules(order, federationUser);

		// exercise
		List<SecurityRule> securityRules = null;
		try {
			securityRules = this.facade.getAllSecurityRules(order.getId(), FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.PUBLIC_IP);
		} catch (InstanceNotFoundException e) {
			Assert.fail();
		}

		// verify
		Mockito.verify(this.orderController, Mockito.times(1)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(cloudConnector, Mockito.times(1)).getAllSecurityRules(Mockito.any(), Mockito.eq(federationUser));

		Assert.assertEquals(expectedSecurityRules, securityRules);
	}

	// test case: Get all security rules for a network via its endpoint, it must
	// return a list of rules if the getAllSecurityRules method does not throw an
	// exception.
	@Test
	public void testGetSecurityRuleForNetwork() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();

		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		NetworkOrder order = spyNetworkOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);
		securityRule.setInstanceId(FAKE_INSTANCE_ID);

		List<SecurityRule> expectedSecurityRules = new ArrayList<>();
		expectedSecurityRules.add(securityRule);

		Mockito.doReturn(expectedSecurityRules).when(cloudConnector).getAllSecurityRules(order, federationUser);

		// exercise
		List<SecurityRule> securityRules = null;
		try {
			securityRules = this.facade.getAllSecurityRules(order.getId(), FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.NETWORK);
		} catch (InstanceNotFoundException e) {
			Assert.fail();
		}

		// verify
		Mockito.verify(this.orderController, Mockito.times(1)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(cloudConnector, Mockito.times(1)).getAllSecurityRules(Mockito.any(), Mockito.eq(federationUser));

		Assert.assertEquals(expectedSecurityRules, securityRules);
	}
	
	// test case: Delete a security rule for a public IP through its endpoint, if the 
	// deleteSecurityRule method does not throw an exception it is because the
	// removal succeeded.
	@Test
	public void testDeleteSecurityRuleForPublicIp() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();

		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		PublicIpOrder order = spyPublicIpOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);
		securityRule.setInstanceId(FAKE_INSTANCE_ID);

		Mockito.doNothing().when(cloudConnector).deleteSecurityRule(Mockito.anyString(),
				Mockito.any(FederationUser.class));

		// exercise
		try {
			this.facade.deleteSecurityRule(order.getId(), securityRule.getInstanceId(), FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.PUBLIC_IP);
		} catch (InstanceNotFoundException e) {
			Assert.fail();
		}

		// verify
		Mockito.verify(this.orderController, Mockito.times(1)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(cloudConnector, Mockito.times(1)).deleteSecurityRule(Mockito.anyString(),
				Mockito.eq(federationUser));
	}
	
	// test case: Delete a security rule for a network through its endpoint, if the
	// deleteSecurityRule method does not throw an exception it is because the
	// removal succeeded.
	@Test
	public void testDeleteSecurityRuleForNetwork() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();

		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		NetworkOrder order = spyNetworkOrder(federationUser);
		Mockito.doReturn(order).when(this.orderController).getOrder(Mockito.anyString());

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);
		securityRule.setInstanceId(FAKE_INSTANCE_ID);

		Mockito.doNothing().when(cloudConnector).deleteSecurityRule(Mockito.anyString(),
				Mockito.any(FederationUser.class));

		// exercise
		try {
			this.facade.deleteSecurityRule(order.getId(), securityRule.getInstanceId(), FAKE_FEDERATION_TOKEN_VALUE,
					ResourceType.NETWORK);
		} catch (InstanceNotFoundException e) {
			Assert.fail();
		}

		// verify
		Mockito.verify(this.orderController, Mockito.times(1)).getOrder(Mockito.eq(order.getId()));
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(cloudConnector, Mockito.times(1)).deleteSecurityRule(Mockito.anyString(),
				Mockito.eq(federationUser));
	}
	
	// test case: When calling the genericRequest method, it must return a generic
	// request response.
	@Test
	public void testGenericRequest() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();

		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		String method = GET_METHOD;
		String url = FAKE_URL;
		Map<String, String> headers = new HashMap<>();
		Map<String, String> body = new HashMap<>();
		GenericRequest genericRequest = new GenericRequest(method, url, headers, body);

		String responseContent = FAKE_CONTENT;
		GenericRequestResponse expectedResponse = new GenericRequestResponse(responseContent);

		CloudConnectorFactory cloudConnectorFactory = mockCloudConnectorFactory();
		CloudConnector cloudConnector = mockCloudConnector(cloudConnectorFactory);

		Mockito.when(cloudConnector.genericRequest(Mockito.eq(genericRequest), Mockito.eq(federationUser)))
				.thenReturn(expectedResponse);

		// exercise
		GenericRequestResponse genericRequestResponse = this.facade.genericRequest(FAKE_CLOUD_NAME,
				FAKE_MEMBER_ID_VALUE, genericRequest, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		Mockito.verify(cloudConnector, Mockito.times(1)).genericRequest(Mockito.eq(genericRequest),
				Mockito.eq(federationUser));

		Assert.assertEquals(expectedResponse, genericRequestResponse);
	}

	private PublicIpOrder spyPublicIpOrder(FederationUser federationUser) {
		ComputeOrder computeOrder = new ComputeOrder();
		ComputeInstance computeInstance = new ComputeInstance(FAKE_SOURCE_ID);
		computeOrder.setInstanceId(computeInstance.getId());
		
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		Map<String, Order> activeOrdersMap = Mockito.spy(sharedOrderHolders.getActiveOrdersMap());
		activeOrdersMap.put(computeOrder.getId(), computeOrder);
		
		String computeOrderId = computeOrder.getId();
		PublicIpOrder order = Mockito.spy(
				new PublicIpOrder(federationUser, 
						FAKE_MEMBER_ID_VALUE, 
						FAKE_FEDERATION_TOKEN_VALUE,
						DEFAULT_CLOUD_NAME, 
						computeOrderId));

		return order;
	}

	private AttachmentOrder spyAttachmentOrder(FederationUser federationUser) {
		ComputeOrder computeOrder = new ComputeOrder();
		ComputeInstance computeInstance = new ComputeInstance(FAKE_SOURCE_ID);
		computeOrder.setInstanceId(computeInstance.getId());
		
		VolumeOrder volumeOrder = new VolumeOrder();
		VolumeInstance volumeInstance = new VolumeInstance(FAKE_TARGET_ID);
		volumeOrder.setInstanceId(volumeInstance.getId());
		
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		Map<String, Order> activeOrdersMap = Mockito.spy(sharedOrderHolders.getActiveOrdersMap());
		activeOrdersMap.put(computeOrder.getId(), computeOrder);
		activeOrdersMap.put(volumeOrder.getId(), volumeOrder);
		
		String computeOrderId = computeOrder.getId();
		String volumeOrderId = volumeOrder.getId();
		AttachmentOrder order = Mockito.spy(
				new AttachmentOrder(federationUser, 
						FAKE_MEMBER_ID_VALUE, 
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
						FAKE_MEMBER_ID_VALUE, 
						FAKE_FEDERATION_TOKEN_VALUE,
						DEFAULT_CLOUD_NAME, 
						FAKE_NAME_VALUE, 
						FAKE_GATEWAY, 
						FAKE_ADDRESS, 
						NetworkAllocationMode.STATIC));

		return order;
	}

	private VolumeOrder spyVolumeOrder(FederationUser federationUser) {
		VolumeOrder order = Mockito.spy(
				new VolumeOrder(federationUser, 
						FAKE_MEMBER_ID_VALUE, 
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
		PowerMockito.mockStatic(CloudConnectorFactory.class);
		CloudConnectorFactory cloudConnectorFactory = Mockito.mock(CloudConnectorFactory.class);
		PowerMockito.when(CloudConnectorFactory.getInstance()).thenReturn(cloudConnectorFactory);
		return cloudConnectorFactory;
	}

	private FederationUser createFederationUserAuthenticate(RSAPublicKey keyRSA, Map<String, String> attributes)
			throws UnauthenticatedUserException, InvalidTokenException {

		FederationUser federationUser = new FederationUser(attributes);
		PowerMockito.mockStatic(AuthenticationUtil.class);
		PowerMockito.when(AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString()))
				.thenReturn(federationUser);
		
		return federationUser;
	}

	private Map<String, String> putUserIdAttribute() {
		Map<String, String> attributes = new HashMap<>();
		attributes.put(PROVIDER_KEY, FAKE_MEMBER_ID_VALUE);
		attributes.put(ID_KEY, FAKE_USER_ID_VALUE);
		attributes.put(NAME_KEY, FAKE_NAME_VALUE);
		attributes.put(TOKEN_KEY, FAKE_FEDERATION_TOKEN_VALUE);
		return attributes;
	}

	private ComputeOrder spyComputeOrder(FederationUser federationUser, String publicKey, ArrayList<UserData> userData,
			List<String> networkIds) {

		ComputeOrder order = Mockito.spy(
				new ComputeOrder(federationUser, 
						FAKE_MEMBER_ID_VALUE, 
						FAKE_MEMBER_ID_VALUE,
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

	// TODO Performing coverage tests...
	
	// test case: ...
	@Test
	public void testGetPublicKeySuccessfully() throws Exception {
		// set up
		PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
		ServiceAsymmetricKeysHolder sakHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
		PowerMockito.when(ServiceAsymmetricKeysHolder.getInstance()).thenReturn(sakHolder);

		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.when(sakHolder.getPublicKey()).thenReturn(keyRSA);

		PowerMockito.mockStatic(RSAUtil.class);
		PowerMockito.when(RSAUtil.savePublicKey(keyRSA)).thenReturn(FAKE_PUBLIC_KEY);

		// exercise
		String publicKey = this.facade.getPublicKey();

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getPublicKey();

		PowerMockito.verifyStatic(ServiceAsymmetricKeysHolder.class, Mockito.times(1));
		ServiceAsymmetricKeysHolder.getInstance();

		PowerMockito.verifyStatic(RSAUtil.class, Mockito.times(1));
		RSAUtil.savePublicKey(Mockito.eq(keyRSA));

		Assert.assertNotNull(publicKey);
	}
	
	// test case: ...
	@Test(expected = UnexpectedException.class) // verify
	public void testGetPublicKeyThrowsUnexpectedException() throws Exception {
		// set up
		PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
		ServiceAsymmetricKeysHolder sakHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
		PowerMockito.when(ServiceAsymmetricKeysHolder.getInstance()).thenReturn(sakHolder);
		sakHolder.setPublicKeyFilePath(null);

		// exercise
		this.facade.getPublicKey();
	}
	
	// test case: ...
	@Test
	public void testGetCloudNamesWithLocalMemberId() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();

		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		List<String> cloudNames = new ArrayList<>();

		CloudListController cloudListController = Mockito.mock(CloudListController.class);
		Mockito.doReturn(cloudNames).when(cloudListController).getCloudNames();
		this.facade.setCloudListController(cloudListController);

		// exercise
		this.facade.getCloudNames(FAKE_LOCAL_IDENTITY_MEMBER, FAKE_FEDERATION_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());

		Mockito.verify(cloudListController, Mockito.times(1)).getCloudNames();
	}
	
	// test case: ...
	@Test(expected = RemoteCommunicationException.class) // verify
	public void testGetCloudNamesThrowsRemoteCommunicationException() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();

		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);

		AuthorizationController authorization = Mockito.mock(AuthorizationController.class);
		Mockito.doNothing().when(authorization).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString(), Mockito.anyString());

		this.facade.setAuthorizationController(authorization);

		// exercise
		this.facade.getCloudNames(FAKE_MEMBER_ID_VALUE, FAKE_FEDERATION_TOKEN_VALUE);
	}
	
	@Ignore
	// test case: ...
	@Test
	public void testGetCloudNamesWithRemoteMemberId() throws Exception {
		// set up
		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();

		Map<String, String> attributes = putUserIdAttribute();
		FederationUser federationUser = createFederationUserAuthenticate(keyRSA, attributes);
		AuthorizationController authorization = mockAuthorizationController(federationUser);

		List<String> cloudNames = new ArrayList<>();

		RemoteGetCloudNamesRequest remoteGetCloudNamesRequest = 
				Mockito.spy(new RemoteGetCloudNamesRequest(FAKE_MEMBER_ID_VALUE, federationUser));
		Mockito.doReturn(cloudNames).when(remoteGetCloudNamesRequest).send();

		// exercise
		try {
			this.facade.getCloudNames(FAKE_MEMBER_ID_VALUE, FAKE_FEDERATION_TOKEN_VALUE);
		} catch (RemoteCommunicationException e) {
			Assert.fail();
		}

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).authorize(Mockito.eq(federationUser), Mockito.anyString(),
				Mockito.anyString());
	}
	
}
