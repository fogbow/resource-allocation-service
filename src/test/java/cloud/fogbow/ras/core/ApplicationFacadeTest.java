package cloud.fogbow.ras.core;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.as.core.util.AuthenticationUtil;
import cloud.fogbow.common.constants.HttpMethod;
import cloud.fogbow.common.exceptions.DependencyDetectedException;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.RemoteCommunicationException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.common.util.CryptoUtil;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.ServiceAsymmetricKeysHolder;
import cloud.fogbow.common.util.connectivity.FogbowGenericRequest;
import cloud.fogbow.common.util.connectivity.FogbowGenericResponse;
import cloud.fogbow.common.util.connectivity.HttpRequest;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.api.http.response.InstanceStatus;
import cloud.fogbow.ras.api.http.response.NetworkInstance;
import cloud.fogbow.ras.api.http.response.PublicIpInstance;
import cloud.fogbow.ras.api.http.response.SecurityRuleInstance;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.parameters.SecurityRule;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.intercomponent.xmpp.PacketSenderHolder;
import cloud.fogbow.ras.core.intercomponent.xmpp.requesters.RemoteGetCloudNamesRequest;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.NetworkOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.models.orders.PublicIpOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.authorization.DefaultAuthorizationPlugin;

@PrepareForTest({ 
    AuthenticationUtil.class, 
    CryptoUtil.class, 
    PacketSenderHolder.class, 
    RasPublicKeysHolder.class, 
    RemoteGetCloudNamesRequest.class, 
    ServiceAsymmetricKeysHolder.class, 
    SharedOrderHolders.class 
})
public class ApplicationFacadeTest extends BaseUnitTests {

	private static final String BUILD_NUMBER_FORMAT = "%s-abcd";
	private static final String BUILD_NUMBER_FORMAT_FOR_TESTING = "%s-[testing mode]";
	private static final String FAKE_CLOUD_NAME = "fake-cloud-name";
	private static final String FAKE_CONTENT = "fooBar";
	private static final String FAKE_LOCAL_IDENTITY_PROVIDER = "fake-localidentity-provider";
	private static final String FAKE_PROVIDER_ID = "fake-provider-id";
	private static final String FAKE_OWNER_USER_ID_VALUE = "fake-owner-user-id";
	private static final String FAKE_RULE_ID = "fake-rule-id";
	private static final String FAKE_SOURCE_ID = "fake-source-id";
	private static final String FAKE_URL = "https://www.foo.bar";
	private static final String SYSTEM_USER_TOKEN_VALUE = "system-user-token-value";
	private static final String VALID_PATH_CONF = "ras.conf";
	private static final String VALID_PATH_CONF_WITHOUT_BUILD_PROPERTY = "ras-without-build-number.conf";

	private ApplicationFacade facade;
	private OrderController orderController;

	@Before
	public void setUp() throws UnexpectedException {
		super.mockReadOrdersFromDataBase();
		super.mockLocalCloudConnectorFromFactory();
		this.orderController = new OrderController();
		this.facade = Mockito.spy(ApplicationFacade.getInstance());
		this.facade.setOrderController(this.orderController);
	}

	// test case: When calling the setBuildNumber method, it must generate correct
	// build number version.
	@Test
	public void testVersion() throws Exception {
		// set up
		this.facade.setBuildNumber(HomeDir.getPath() + VALID_PATH_CONF);

		// exercise
		String build = this.facade.getVersionNumber();

		// verify
		String expected = String.format(BUILD_NUMBER_FORMAT, SystemConstants.API_VERSION_NUMBER);
		Assert.assertEquals(expected, build);
	}

	// test case: When calling the setBuildNumber method without a build property
	// valid in the configuration file, it must generate a testing mode build number
	// version.
	@Test
	public void testVersionWithoutBuildProperty() throws Exception {
		// set up
		this.facade.setBuildNumber(HomeDir.getPath() + VALID_PATH_CONF_WITHOUT_BUILD_PROPERTY);

		// exercise
		String build = this.facade.getVersionNumber();

		// verify
		String expected = String.format(BUILD_NUMBER_FORMAT_FOR_TESTING, SystemConstants.API_VERSION_NUMBER);
		Assert.assertEquals(expected, build);
	}
	
	// test case: When calling the getPublicKey method with a null Public Key File
	// Path, it must throw an UnauthorizedRequestException.
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
	
	// test case: When calling the getPublicKey method, verify that this call was
	// successful.
	@Test
	public void testGetPublicKeySuccessfully() throws Exception {
		// set up
		PowerMockito.mockStatic(ServiceAsymmetricKeysHolder.class);
		ServiceAsymmetricKeysHolder sakHolder = Mockito.mock(ServiceAsymmetricKeysHolder.class);
		PowerMockito.when(ServiceAsymmetricKeysHolder.getInstance()).thenReturn(sakHolder);

		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.when(sakHolder.getPublicKey()).thenReturn(keyRSA);

		PowerMockito.mockStatic(CryptoUtil.class);
		PowerMockito.when(CryptoUtil.toBase64(keyRSA)).thenReturn(FAKE_PUBLIC_KEY);

		// exercise
		String publicKey = this.facade.getPublicKey();

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getPublicKey();

		PowerMockito.verifyStatic(ServiceAsymmetricKeysHolder.class, Mockito.times(1));
		ServiceAsymmetricKeysHolder.getInstance();

		PowerMockito.verifyStatic(CryptoUtil.class, Mockito.times(1));
		CryptoUtil.toBase64(Mockito.eq(keyRSA));

		Assert.assertNotNull(publicKey);
	}	
	
	// test case: When calling the getAsPublicKey method, verify that this call was
	// successful.
	@Test
	public void testGetAuthenticationServicePublicKeySuccessfully() throws Exception {
		// set up
		PowerMockito.mockStatic(RasPublicKeysHolder.class);
		RasPublicKeysHolder pkHolder = Mockito.mock(RasPublicKeysHolder.class);
		PowerMockito.when(RasPublicKeysHolder.getInstance()).thenReturn(pkHolder);

		RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
		Mockito.when(pkHolder.getAsPublicKey()).thenReturn(keyRSA);

		// exercise
		this.facade.getAsPublicKey();

		// verify
		PowerMockito.verifyStatic(RasPublicKeysHolder.class, Mockito.times(1));
		RasPublicKeysHolder.getInstance();

		Mockito.verify(pkHolder, Mockito.times(1)).getAsPublicKey();
	}	
	
	// test case: When calling the authorizeOrder method with a different resource
	// type of the order, it must throw a InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testAuthorizeOrderThrowsInstanceNotFoundException() throws Exception {
		// set up
		SystemUser systemUser = null;
		String cloudName = null;
		Operation operation = null;
		ResourceType resourceType = ResourceType.VOLUME;
		ComputeOrder order = new ComputeOrder();
		
		// exercise
		this.facade.authorizeOrder(systemUser, cloudName, operation, resourceType, order);
	}
	
	// test case: When calling the authorizeOrder method with a system user
	// different from the order requester, it must throw an
	// UnauthorizedRequestException.
	@Test(expected = UnauthorizedRequestException.class) // verify
	public void testAuthorizeOrderThrowsUnauthorizedRequestException() throws Exception {
		// set up
		SystemUser owner = new SystemUser(FAKE_OWNER_USER_ID_VALUE, null, null);
		String cloudName = null;
		Operation operation = null;
		ResourceType resourceType = ResourceType.COMPUTE;
		ComputeOrder order = createLocalComputeOrder();

		// exercise
		this.facade.authorizeOrder(owner, cloudName, operation, resourceType, order);
	}

	// test case: When calling a resource with a too long extra user data file
	// content throws an InvalidParameterException.
	@Test(expected = InvalidParameterException.class) // verify
	public void testCreateResourceWithTooLongExtraUserDataFileContent() throws Exception {
		// set up
		ComputeOrder order = createLocalComputeOrder();
		order.setUserData(generateVeryLongUserDataFileContent());
		
		// exercise
		this.facade.createCompute(order, SYSTEM_USER_TOKEN_VALUE);
	}

	// test case: When calling the createCompute method with a new order passed by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreateComputeOrder() throws Exception {
		// set up
	    RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		
		ComputeOrder order = createLocalComputeOrder();
		order.setSystemUser(systemUser);
		order.setUserData(null);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.COMPUTE,
				BaseUnitTests.DEFAULT_CLOUD_NAME,
				order);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		CloudListController cloudListController = Mockito.mock(CloudListController.class);
		this.facade.setCloudListController(cloudListController);
		
		ComputeInstance computeInstance = new ComputeInstance(order.getId());
		order.setInstanceId(computeInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createCompute(order, SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getCompute method, it must return the
	// ComputeInstance of the Order ID passed per parameter.
	@Test
	public void testGetComputeOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		
		ComputeOrder order = createLocalComputeOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.COMPUTE,
				BaseUnitTests.DEFAULT_CLOUD_NAME,
				order);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		ComputeInstance instanceExpected = new ComputeInstance(order.getId());
		order.setInstanceId(instanceExpected.getId());
		Mockito.when(super.localCloudConnector.getInstance(Mockito.eq(order))).thenReturn(instanceExpected);

		// exercise
		ComputeInstance instance = this.facade.getCompute(order.getId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertSame(instanceExpected, instance);
	}

	// test case: When calling the method deleteCompute, the Order passed as a
	// parameter must return its state changed to Closed.
	@Test
	public void testDeleteComputeOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		
		ComputeOrder order = createLocalComputeOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.DELETE,
				ResourceType.COMPUTE,
				BaseUnitTests.DEFAULT_CLOUD_NAME,
				order);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		ComputeInstance computeInstance = new ComputeInstance(order.getId());
		order.setInstanceId(computeInstance.getId());
		Mockito.when(this.localCloudConnector.getInstance(Mockito.eq(order))).thenReturn(computeInstance);

		// checking that the order has a state and is not null
		Assert.assertNotNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deleteCompute(order.getId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the computeOrders.
	@Test
	public void testGetAllComputeOrdersInstancesStatus() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		
		ComputeOrder order = createLocalComputeOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(Operation.GET_ALL, ResourceType.COMPUTE);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		List<InstanceStatus> expectedInstancesStatus = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(SYSTEM_USER_TOKEN_VALUE,
				ResourceType.COMPUTE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedInstancesStatus, instancesStatus);
	}

	// test case: When calling the createVolume method with a new order passed by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreateVolumeOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		VolumeOrder order = createLocalVolumeOrder();
		order.setSystemUser(systemUser);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.VOLUME,
				BaseUnitTests.DEFAULT_CLOUD_NAME,
				order);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		VolumeInstance volumeInstance = new VolumeInstance(order.getId());
		order.setInstanceId(volumeInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createVolume(order, SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getVolume method, it must return the
	// VolumeInstance of the Order ID passed per parameter.
	@Test
	public void testGetVolumeOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		VolumeOrder order = createLocalVolumeOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.VOLUME,
				DEFAULT_CLOUD_NAME,
				order);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		VolumeInstance instanceExpected = new VolumeInstance(order.getId());
		order.setInstanceId(instanceExpected.getId());
		Mockito.when(super.localCloudConnector.getInstance(Mockito.eq(order))).thenReturn(instanceExpected);

		// exercise
		VolumeInstance instance = this.facade.getVolume(order.getId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertSame(instanceExpected, instance);
	}

	// test case: When calling the method deleteVolume, the Order passed as a
	// parameter must return its state changed to Closed.
	@Test
	public void testDeleteVolumeOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		VolumeOrder order = createLocalVolumeOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.DELETE,
				ResourceType.VOLUME,
				BaseUnitTests.DEFAULT_CLOUD_NAME,
				order);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		VolumeInstance volumeInstance = new VolumeInstance(order.getId());
		order.setInstanceId(volumeInstance.getId());
		Mockito.when(super.localCloudConnector.getInstance(Mockito.eq(order))).thenReturn(volumeInstance);

		// checking that the order has a state and is not null
		Assert.assertNotNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deleteVolume(order.getId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the volumeOrders.
	@Test
	public void testGetAllVolumeOrdersInstancesStatus() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		
		VolumeOrder order = createLocalVolumeOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(Operation.GET_ALL, ResourceType.VOLUME);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		List<InstanceStatus> expectedInstancesStatus = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(SYSTEM_USER_TOKEN_VALUE,
				ResourceType.VOLUME);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedInstancesStatus, instancesStatus);
	}

	// test case: When calling the createNetwork method with a new order passed by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreateNetworkOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		NetworkOrder order = createLocalNetworkOrder();
		order.setSystemUser(systemUser);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.NETWORK,
				BaseUnitTests.DEFAULT_CLOUD_NAME,
				order);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		NetworkInstance networkInstance = new NetworkInstance(order.getId());
		order.setInstanceId(networkInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createNetwork(order, SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

    // test case: When calling the getNetwork method, it must return the
	// NetworkInstance of the Order ID passed per parameter.
	@Test
	public void testGetNetworkOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		NetworkOrder order = createLocalNetworkOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.NETWORK,
				BaseUnitTests.DEFAULT_CLOUD_NAME,
				order);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		NetworkInstance instanceExpected = new NetworkInstance(order.getId());
		order.setInstanceId(instanceExpected.getId());
		Mockito.when(super.localCloudConnector.getInstance(Mockito.eq(order))).thenReturn(instanceExpected);

		// exercise
		NetworkInstance instance = this.facade.getNetwork(order.getId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertSame(instanceExpected, instance);
	}

	// test case: When calling the method deleteNetwork, the Order passed as a
	// parameter must return its state changed to Closed.
	@Test
	public void testDeleteNetworkOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		NetworkOrder order = createLocalNetworkOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.DELETE,
				ResourceType.NETWORK,
				BaseUnitTests.DEFAULT_CLOUD_NAME,
				order);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		NetworkInstance networkInstance = new NetworkInstance(order.getId());
		order.setInstanceId(networkInstance.getId());
		Mockito.when(super.localCloudConnector.getInstance(Mockito.eq(order))).thenReturn(networkInstance);

		// checking that the order has a state and is not null
		Assert.assertNotNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deleteNetwork(order.getId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the deleteNetwork method passing a network order with a compute dependency,
	// a DependencyDetectedException should be thrown
	@Test(expected = DependencyDetectedException.class) // verify
	public void testDeleteNetworkOrderWithComputeDependency() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		NetworkOrder networkOrder1 = createLocalNetworkOrder();
		networkOrder1.setSystemUser(systemUser);
		networkOrder1.setInstanceId(FAKE_INSTANCE_ID);
		
		NetworkOrder networkOrder2 = createLocalNetworkOrder();
		networkOrder2.setSystemUser(systemUser);
		networkOrder2.setInstanceId(FAKE_INSTANCE_ID);

		this.orderController.activateOrder(networkOrder1);
		this.orderController.activateOrder(networkOrder2);

		List<String> networkIds = new LinkedList<>();
		networkIds.add(networkOrder1.getId());
		networkIds.add(networkOrder2.getId());

		ComputeOrder computeOrder = new ComputeOrder(
                        createSystemUser(),
                        BaseUnitTests.LOCAL_MEMBER_ID,
                        BaseUnitTests.LOCAL_MEMBER_ID,
                        BaseUnitTests.DEFAULT_CLOUD_NAME, 
                        BaseUnitTests.FAKE_INSTANCE_NAME,
                        BaseUnitTests.CPU_VALUE,
                        BaseUnitTests.MEMORY_VALUE,
                        BaseUnitTests.DISK_VALUE,
                        BaseUnitTests.FAKE_IMAGE_ID,
                        super.mockUserData(),
                        BaseUnitTests.FAKE_PUBLIC_KEY,
                        networkIds);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.COMPUTE,
				BaseUnitTests.DEFAULT_CLOUD_NAME,
				computeOrder);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		ComputeInstance computeInstance = new ComputeInstance(computeOrder.getId());
		computeOrder.setInstanceId(computeInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(computeOrder.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createCompute(computeOrder, SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		
		Assert.assertEquals(expectedOrderState, computeOrder.getOrderState());

		// exercise
		this.facade.deleteNetwork(networkOrder2.getId(), SYSTEM_USER_TOKEN_VALUE);
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the networkOrders.
	@Test
	public void testGetAllNetworkOrdersInstancesStatus() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		
		NetworkOrder order = createLocalNetworkOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(Operation.GET_ALL, ResourceType.NETWORK);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		List<InstanceStatus> expectedInstancesStatus = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(SYSTEM_USER_TOKEN_VALUE,
				ResourceType.NETWORK);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedInstancesStatus, instancesStatus);
	}

	// test case: When calling the createAttachment method with a new order passed
	// by parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreateAttachmentOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		
		ComputeOrder computeOrder = createLocalComputeOrder();
        computeOrder.setInstanceId(computeOrder.getId());
        this.orderController.activateOrder(computeOrder);
        
        VolumeOrder volumeOrder = createLocalVolumeOrder();
        volumeOrder.setInstanceId(volumeOrder.getId());
        this.orderController.activateOrder(volumeOrder);

		AttachmentOrder attachmentOrder = createLocalAttachmentOrder(computeOrder, volumeOrder);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.ATTACHMENT,
				BaseUnitTests.DEFAULT_CLOUD_NAME,
				attachmentOrder);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		AttachmentInstance attachmentInstance = new AttachmentInstance(attachmentOrder.getId());
		attachmentOrder.setInstanceId(attachmentInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(attachmentOrder.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createAttachment(attachmentOrder, SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, attachmentOrder.getOrderState());
	}

	// test case: When calling the getAttachment method, it must return the
	// AttachmentInstance of the Order ID passed per parameter.
	@Test
	public void testGetAttachmentOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		ComputeOrder computeOrder = createLocalComputeOrder();
        computeOrder.setInstanceId(computeOrder.getId());
        this.orderController.activateOrder(computeOrder);
        
        VolumeOrder volumeOrder = createLocalVolumeOrder();
        volumeOrder.setInstanceId(volumeOrder.getId());
        this.orderController.activateOrder(volumeOrder);

        AttachmentOrder attachmentOrder = createLocalAttachmentOrder(computeOrder, volumeOrder);
		this.orderController.activateOrder(attachmentOrder);

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.ATTACHMENT,
				BaseUnitTests.DEFAULT_CLOUD_NAME,
				attachmentOrder);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		AttachmentInstance instanceExpected = new AttachmentInstance(attachmentOrder.getId());
		attachmentOrder.setInstanceId(instanceExpected.getId());
		Mockito.when(super.localCloudConnector.getInstance(Mockito.eq(attachmentOrder))).thenReturn(instanceExpected);

		// exercise
		AttachmentInstance instance = this.facade.getAttachment(attachmentOrder.getId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertSame(instanceExpected, instance);
	}

	// test case: When calling the method deleteAttachment, the Order passed as a
	// parameter must return its state changed to Closed.
	@Test
	public void testDeleteAttachmentOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		ComputeOrder computeOrder = createLocalComputeOrder();
        computeOrder.setInstanceId(computeOrder.getId());
        this.orderController.activateOrder(computeOrder);
        
        VolumeOrder volumeOrder = createLocalVolumeOrder();
        volumeOrder.setInstanceId(volumeOrder.getId());
        this.orderController.activateOrder(volumeOrder);

        AttachmentOrder attachmentOrder = createLocalAttachmentOrder(computeOrder, volumeOrder);
		this.orderController.activateOrder(attachmentOrder);

		RasOperation operation = new RasOperation(
				Operation.DELETE,
				ResourceType.ATTACHMENT,
				DEFAULT_CLOUD_NAME,
				attachmentOrder);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		AttachmentInstance attachmentInstance = new AttachmentInstance(attachmentOrder.getId());
		attachmentOrder.setInstanceId(attachmentInstance.getId());
		Mockito.when(super.localCloudConnector.getInstance(Mockito.eq(attachmentOrder))).thenReturn(attachmentInstance);

		// checking that the order has a state and is not null
		Assert.assertNotNull(attachmentOrder.getOrderState());
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deleteAttachment(attachmentOrder.getId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, attachmentOrder.getOrderState());
	}

	// test case: When calling the deleteCompute method passing a compute order with an attachment dependency,
	// a DependencyDetectedException should be thrown
	@Test(expected = DependencyDetectedException.class) // verify
	public void testDeleteComputeOrderWithAttachmentDependency() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		ComputeOrder computeOrder = createLocalComputeOrder();
        computeOrder.setInstanceId(computeOrder.getId());
        this.orderController.activateOrder(computeOrder);
        
        VolumeOrder volumeOrder = createLocalVolumeOrder();
        volumeOrder.setInstanceId(volumeOrder.getId());
        this.orderController.activateOrder(volumeOrder);

        AttachmentOrder attachmentOrder = createLocalAttachmentOrder(computeOrder, volumeOrder);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.ATTACHMENT,
				DEFAULT_CLOUD_NAME,
				attachmentOrder);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		AttachmentInstance attachmentInstance = new AttachmentInstance(attachmentOrder.getId());
		attachmentOrder.setInstanceId(attachmentInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(attachmentOrder.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createAttachment(attachmentOrder, SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, attachmentOrder.getOrderState());

		// exercise
		this.facade.deleteCompute(attachmentOrder.getComputeOrderId(), SYSTEM_USER_TOKEN_VALUE);
	}

	// test case: When calling the deleteVolume method passing a volume order with an attachment dependency,
	// a DependencyDetectedException should be thrown
	@Test(expected = DependencyDetectedException.class) // verify
	public void testDeleteVolumeOrderWithAttachmentDependency() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		ComputeOrder computeOrder = createLocalComputeOrder();
        computeOrder.setInstanceId(computeOrder.getId());
		this.orderController.activateOrder(computeOrder);
        
        VolumeOrder volumeOrder = createLocalVolumeOrder();
        volumeOrder.setInstanceId(volumeOrder.getId());
        this.orderController.activateOrder(volumeOrder);

        AttachmentOrder attachmentOrder = createLocalAttachmentOrder(computeOrder, volumeOrder);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.ATTACHMENT,
				DEFAULT_CLOUD_NAME,
				attachmentOrder);
		
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		AttachmentInstance attachmentInstance = new AttachmentInstance(attachmentOrder.getId());
		attachmentOrder.setInstanceId(attachmentInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(attachmentOrder.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createAttachment(attachmentOrder, SYSTEM_USER_TOKEN_VALUE);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, attachmentOrder.getOrderState());

		// exercise
		this.facade.deleteVolume(attachmentOrder.getVolumeOrderId(), SYSTEM_USER_TOKEN_VALUE);
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the attachmentOrders.
	@Test
	public void testGetAllAttachmentOrdersInstancesStatus() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		ComputeOrder computeOrder = createLocalComputeOrder();
        computeOrder.setInstanceId(computeOrder.getId());
        this.orderController.activateOrder(computeOrder);
        
        VolumeOrder volumeOrder = createLocalVolumeOrder();
        volumeOrder.setInstanceId(volumeOrder.getId());
        this.orderController.activateOrder(volumeOrder);

        AttachmentOrder attachmentOrder = createLocalAttachmentOrder(computeOrder, volumeOrder);
		this.orderController.activateOrder(attachmentOrder);
		
		RasOperation operation = new RasOperation(Operation.GET_ALL, ResourceType.ATTACHMENT);

		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		List<InstanceStatus> expectedInstancesStatus = generateInstancesStatus(attachmentOrder);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(SYSTEM_USER_TOKEN_VALUE,
				ResourceType.ATTACHMENT);

		// verify
		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();
		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedInstancesStatus, instancesStatus);
	}
	
	// TODO continue verification from here...

	// test case: When calling the createPublicIp method with a new order passed by
	// parameter, it must return its OrderState OPEN after the activation.
	@Test
	public void testCreatePublicIpOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		PublicIpOrder order = spyPublicIpOrder(systemUser);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.PUBLIC_IP,
				DEFAULT_CLOUD_NAME,
				order
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		PublicIpInstance publicIpInstance = new PublicIpInstance(order.getId());
		order.setInstanceId(publicIpInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createPublicIp(order, SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the getPublicIp method, it must return the
	// PublicIpInstance of the OrderID passed per parameter.
	@Test
	public void testGetPublicIpOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		PublicIpOrder order = spyPublicIpOrder(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.PUBLIC_IP,
				DEFAULT_CLOUD_NAME,
				order
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		PublicIpInstance instanceExpected = new PublicIpInstance(order.getId());
		order.setInstanceId(instanceExpected.getId());
		Mockito.when(super.localCloudConnector.getInstance(Mockito.eq(order))).thenReturn(instanceExpected);

		// exercise
		PublicIpInstance instance = this.facade.getPublicIp(order.getId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertSame(instanceExpected, instance);
	}

	// test case: When calling the method deletePublicIp, the Order passed as a
	// parameter must return its state changed to Closed.
	@Test
	public void testDeletePublicIpOrder() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		PublicIpOrder order = spyPublicIpOrder(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.DELETE,
				ResourceType.PUBLIC_IP,
				DEFAULT_CLOUD_NAME,
				order
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		PublicIpInstance publicIpInstance = new PublicIpInstance(order.getId());
		order.setInstanceId(publicIpInstance.getId());
		Mockito.when(super.localCloudConnector.getInstance(Mockito.eq(order))).thenReturn(publicIpInstance);

		// checking that the order has a state and is not null
		Assert.assertNotNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.CLOSED;

		// exercise
		this.facade.deletePublicIp(order.getId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());
	}

	// test case: When calling the deleteCompute method with a PublicIp associated with the compute that is to be
	// deleted, a DependencyDetectedException should be thrown
	@Test(expected = DependencyDetectedException.class) // verify
	public void testDeleteComputeOrderWithPublicIpDependency() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		PublicIpOrder order = spyPublicIpOrder(systemUser);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.PUBLIC_IP,
				DEFAULT_CLOUD_NAME,
				order
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		PublicIpInstance publicIpInstance = new PublicIpInstance(order.getId());
		order.setInstanceId(publicIpInstance.getId());

		// checking if the order has no state and is null
		Assert.assertNull(order.getOrderState());
		OrderState expectedOrderState = OrderState.OPEN;

		// exercise
		this.facade.createPublicIp(order, SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedOrderState, order.getOrderState());

		// exercise
		this.facade.deleteCompute(order.getComputeOrderId(), SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.orderController, Mockito.times(1)).deleteOrder(null);
	}

	// test case: When calling the getAllInstancesStatus method, it must return a
	// list of the instances status of the publiIpOrders.
	@Test
	public void testGetAllPublicIpOrdersInstancesStatus() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		RasOperation operation = new RasOperation(
				Operation.GET_ALL,
				ResourceType.PUBLIC_IP
		);

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		PublicIpOrder order = spyPublicIpOrder(systemUser);
		this.orderController.activateOrder(order);

		List<InstanceStatus> expectedInstancesStatus = generateInstancesStatus(order);

		// exercise
		List<InstanceStatus> instancesStatus = this.facade.getAllInstancesStatus(SYSTEM_USER_TOKEN_VALUE,
				ResourceType.PUBLIC_IP);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));

		Assert.assertEquals(expectedInstancesStatus, instancesStatus);
	}

	// test case: Creating a security rule for a network via a public IP's
	// endpoint, it must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testCreateSecurityRuleForNetworkViaPublicIp() throws Exception {
		// set up
		NetworkOrder order = createLocalNetworkOrder();
		order.setSystemUser(null);
		this.orderController.activateOrder(order);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);

		// exercise
		this.facade.createSecurityRule(FAKE_INSTANCE_ID, securityRule, SYSTEM_USER_TOKEN_VALUE,
				ResourceType.PUBLIC_IP);
	}

	// test case: Creating a security rule for a public IP via a network's
	// endpoint, it must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testCreateSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		SystemUser systemUser = null;
		PublicIpOrder order = spyPublicIpOrder(systemUser);
		this.orderController.activateOrder(order);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);

		// exercise
		this.facade.createSecurityRule(FAKE_INSTANCE_ID, securityRule, SYSTEM_USER_TOKEN_VALUE,
				ResourceType.NETWORK);
	}

	// test case: Get all security rules from a network via a public IP's endpoint,
	// it must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testGetSecurityRulesForNetworkViaPublicIp() throws Exception {
		// set up
		NetworkOrder order = createLocalNetworkOrder();
		order.setSystemUser(null);
		this.orderController.activateOrder(order);

		// exercise
		this.facade.getAllSecurityRules(FAKE_INSTANCE_ID, SYSTEM_USER_TOKEN_VALUE, ResourceType.PUBLIC_IP);
	}

	// test case: Get all security rules from a public IP via a network's endpoint,
	// it must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testGetSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		SystemUser systemUser = null;
		PublicIpOrder order = spyPublicIpOrder(systemUser);
		this.orderController.activateOrder(order);

		// exercise
		this.facade.getAllSecurityRules(FAKE_INSTANCE_ID, SYSTEM_USER_TOKEN_VALUE, ResourceType.NETWORK);
	}
	
	// test case: Delete a security rule from a network via public IP's endpoint, it
	// must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testDeleteSecurityRulesForNetworkViaPublicIp() throws Exception {
		// set up
		NetworkOrder order = createLocalNetworkOrder();
		order.setSystemUser(null);
		this.orderController.activateOrder(order);

		// exercise
		this.facade.deleteSecurityRule(FAKE_INSTANCE_ID, FAKE_RULE_ID, SYSTEM_USER_TOKEN_VALUE,
				ResourceType.PUBLIC_IP);
	}

	// test case: Delete a security rule from a public IP via a network's endpoint,
	// it must raise an InstanceNotFoundException.
	@Test(expected = InstanceNotFoundException.class) // verify
	public void testDeleteSecurityRuleForPublicIpViaNetwork() throws Exception {
		// set up
		SystemUser systemUser = null;
		PublicIpOrder order = spyPublicIpOrder(systemUser);
		this.orderController.activateOrder(order);

		// exercise
		this.facade.deleteSecurityRule(FAKE_INSTANCE_ID, FAKE_RULE_ID, SYSTEM_USER_TOKEN_VALUE,
				ResourceType.NETWORK);
	}
	
	// test case: Creating a security rule for a public IP via its endpoint, it must
	// return the rule id, if the createSecurityRule method does not throw an
	// exception.
	@Test
	public void testCreateSecurityRuleForPublicIp() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		PublicIpOrder order = spyPublicIpOrder(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.SECURITY_RULE,
				DEFAULT_CLOUD_NAME,
				order
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);
		Mockito.doReturn(FAKE_INSTANCE_ID).when(super.localCloudConnector).requestSecurityRule(order, securityRule,
				systemUser);

		// exercise
		try {
			this.facade.createSecurityRule(order.getId(), securityRule, SYSTEM_USER_TOKEN_VALUE,
					ResourceType.PUBLIC_IP);
		} catch (InstanceNotFoundException e) {
			// verify
			Assert.fail();
		}

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(super.localCloudConnector, Mockito.times(1)).requestSecurityRule(Mockito.any(), Mockito.eq(securityRule),
				Mockito.eq(systemUser));
	}

	// test case: Creating a security rule for a network via its endpoint, it must
	// return the rule id, if the createSecurityRule method does not throw an
	// exception.
	@Test
	public void testCreateSecurityRuleForNetwork() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		NetworkOrder order = createLocalNetworkOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.CREATE,
				ResourceType.SECURITY_RULE,
				DEFAULT_CLOUD_NAME,
				order
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		SecurityRule securityRule = Mockito.mock(SecurityRule.class);
		Mockito.doReturn(FAKE_INSTANCE_ID).when(super.localCloudConnector).requestSecurityRule(order, securityRule,
				systemUser);

		// exercise
		try {
			this.facade.createSecurityRule(order.getId(), securityRule, SYSTEM_USER_TOKEN_VALUE,
					ResourceType.NETWORK);
		} catch (InstanceNotFoundException e) {
			// verify
			Assert.fail();
		}

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(super.localCloudConnector, Mockito.times(1)).requestSecurityRule(Mockito.any(), Mockito.eq(securityRule),
				Mockito.eq(systemUser));
	}

	// test case: Get all security rules for a public IP via its endpoint, it must
	// return a list of rules if the getAllSecurityRules method does not throw an
	// exception.
	@Test
	public void testGetSecurityRuleForPublicIp() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		PublicIpOrder order = spyPublicIpOrder(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.GET_ALL,
				ResourceType.SECURITY_RULE,
				DEFAULT_CLOUD_NAME,
				order
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		SecurityRuleInstance securityRuleInstance = Mockito.mock(SecurityRuleInstance.class);
		securityRuleInstance.setId(FAKE_INSTANCE_ID);

		List<SecurityRuleInstance> expectedSecurityRuleInstances = new ArrayList<>();
		expectedSecurityRuleInstances.add(securityRuleInstance);
		
		Mockito.doReturn(expectedSecurityRuleInstances).when(super.localCloudConnector).getAllSecurityRules(order, systemUser);

		// exercise
		List<SecurityRuleInstance> securityRuleInstances = null;
		try {
			securityRuleInstances = this.facade.getAllSecurityRules(order.getId(), SYSTEM_USER_TOKEN_VALUE,
					ResourceType.PUBLIC_IP);
		} catch (InstanceNotFoundException e) {
			Assert.fail();
		}

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(super.localCloudConnector, Mockito.times(1)).getAllSecurityRules(Mockito.any(), Mockito.eq(systemUser));

		Assert.assertEquals(expectedSecurityRuleInstances, securityRuleInstances);
	}

	// test case: Get all security rules for a network via its endpoint, it must
	// return a list of rules if the getAllSecurityRules method does not throw an
	// exception.
	@Test
	public void testGetSecurityRuleForNetwork() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		NetworkOrder order = createLocalNetworkOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.GET_ALL,
				ResourceType.SECURITY_RULE,
				DEFAULT_CLOUD_NAME,
				order
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		SecurityRuleInstance securityRuleInstance = Mockito.mock(SecurityRuleInstance.class);
		securityRuleInstance.setId(FAKE_INSTANCE_ID);

		List<SecurityRuleInstance> expectedSecurityRuleInstances = new ArrayList<>();
		expectedSecurityRuleInstances.add(securityRuleInstance);
		
		Mockito.doReturn(expectedSecurityRuleInstances).when(super.localCloudConnector).getAllSecurityRules(order, systemUser);

		// exercise
		List<SecurityRuleInstance> securityRuleInstances = null;
		try {
			securityRuleInstances = this.facade.getAllSecurityRules(order.getId(), SYSTEM_USER_TOKEN_VALUE,
					ResourceType.NETWORK);
		} catch (InstanceNotFoundException e) {
			Assert.fail();
		}

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(super.localCloudConnector, Mockito.times(1)).getAllSecurityRules(Mockito.any(), Mockito.eq(systemUser));

		Assert.assertEquals(expectedSecurityRuleInstances, securityRuleInstances);
	}
	
	// test case: Delete a security rule for a public IP through its endpoint, if the 
	// deleteSecurityRule method does not throw an exception it is because the
	// removal succeeded.
	@Test
	public void testDeleteSecurityRuleForPublicIp() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		PublicIpOrder order = spyPublicIpOrder(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.DELETE,
				ResourceType.SECURITY_RULE,
				DEFAULT_CLOUD_NAME,
				order
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		SecurityRuleInstance securityRuleInstance = Mockito.mock(SecurityRuleInstance.class);
		securityRuleInstance.setId(FAKE_INSTANCE_ID);

		Mockito.doNothing().when(super.localCloudConnector).deleteSecurityRule(Mockito.anyString(),
				Mockito.any(SystemUser.class));

		// exercise
		try {
			this.facade.deleteSecurityRule(order.getId(), securityRuleInstance.getId(), SYSTEM_USER_TOKEN_VALUE,
					ResourceType.PUBLIC_IP);
		} catch (InstanceNotFoundException e) {
			Assert.fail();
		}

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(super.localCloudConnector, Mockito.times(1)).deleteSecurityRule(Mockito.anyString(),
				Mockito.eq(systemUser));
	}
	
	// test case: Delete a security rule for a network through its endpoint, if the
	// deleteSecurityRule method does not throw an exception it is because the
	// removal succeeded.
	@Test
	public void testDeleteSecurityRuleForNetwork() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		NetworkOrder order = createLocalNetworkOrder();
		order.setSystemUser(systemUser);
		this.orderController.activateOrder(order);

		RasOperation operation = new RasOperation(
				Operation.DELETE,
				ResourceType.SECURITY_RULE,
				DEFAULT_CLOUD_NAME,
				order
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		SecurityRuleController securityRuleController = Mockito.spy(new SecurityRuleController());
		this.facade.setSecurityRuleController(securityRuleController);

		SecurityRuleInstance securityRuleInstance = Mockito.mock(SecurityRuleInstance.class);
		securityRuleInstance.setId(FAKE_INSTANCE_ID);

		Mockito.doNothing().when(super.localCloudConnector).deleteSecurityRule(Mockito.anyString(),
				Mockito.any(SystemUser.class));

		// exercise
		try {
			this.facade.deleteSecurityRule(order.getId(), securityRuleInstance.getId(), SYSTEM_USER_TOKEN_VALUE,
					ResourceType.NETWORK);
		} catch (InstanceNotFoundException e) {
			Assert.fail();
		}

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(super.localCloudConnector, Mockito.times(1)).deleteSecurityRule(Mockito.anyString(),
				Mockito.eq(systemUser));
	}
	
	// test case: When calling the genericRequest method, it must return a generic
	// request response.
	@Test
	public void testGenericRequestSuccessfully() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		String url = FAKE_URL;
		HashMap<String, String> headers = new HashMap<>();
		HashMap<String, String> body = new HashMap<>();
		FogbowGenericRequest httpGenericRequest = new HttpRequest(HttpMethod.GET, url, body, headers);
		String serializedGenericRequest = GsonHolder.getInstance().toJson(httpGenericRequest);

		String responseContent = FAKE_CONTENT;
		FogbowGenericResponse expectedResponse = new FogbowGenericResponse(responseContent);

		RasOperation operation = new RasOperation(
				Operation.GENERIC_REQUEST,
				ResourceType.GENERIC_RESOURCE,
				FAKE_CLOUD_NAME,
				serializedGenericRequest
		);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		Mockito.when(super.localCloudConnector.genericRequest(Mockito.eq(serializedGenericRequest), Mockito.eq(systemUser)))
				.thenReturn(expectedResponse);

		String cloudName = FAKE_CLOUD_NAME;
		
		// exercise
		FogbowGenericResponse fogbowGenericResponse = this.facade.genericRequest(cloudName,
				FAKE_PROVIDER_ID, serializedGenericRequest, SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(super.localCloudConnector, Mockito.times(1)).genericRequest(Mockito.eq(serializedGenericRequest),
				Mockito.eq(systemUser));

		Assert.assertEquals(expectedResponse, fogbowGenericResponse);
	}

	// test case: When calling the getCloudNames method with a local provider ID, it
	// must verify that this call was successful.
	@Test
	public void testGetCloudNamesWithLocalMemberId() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.CLOUD_NAMES
		);

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		List<String> cloudNames = new ArrayList<>();

		CloudListController cloudListController = Mockito.mock(CloudListController.class);
		Mockito.doReturn(cloudNames).when(cloudListController).getCloudNames();
		this.facade.setCloudListController(cloudListController);

		// exercise
		this.facade.getCloudNames(FAKE_LOCAL_IDENTITY_PROVIDER, SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(cloudListController, Mockito.times(1)).getCloudNames();
	}

	// test case: When calling the getCloudNames method with a remote provider ID, it
	// must verify that this call was successful.
	@Test
	public void testGetCloudNamesWithRemoteMemberId() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.CLOUD_NAMES
		);

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		List<String> cloudNames = new ArrayList<>();
		RemoteGetCloudNamesRequest remoteGetCloudNamesRequest = Mockito.mock(RemoteGetCloudNamesRequest.class);
		Mockito.when(this.facade.getCloudNamesFromRemoteRequest(FAKE_PROVIDER_ID, systemUser)).thenReturn(remoteGetCloudNamesRequest);
		Mockito.when(remoteGetCloudNamesRequest.send()).thenReturn(cloudNames);

		// exercise
			this.facade.getCloudNames(FAKE_PROVIDER_ID, SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(remoteGetCloudNamesRequest, Mockito.times(1)).send();
	}
	
	// test case: When calling the getCloudNames method and can not get remote
	// communication, it must throw a RemoteCommunicationException.
	@Test(expected = RemoteCommunicationException.class) // verify
	public void testGetCloudNamesThrowsRemoteCommunicationException() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();
		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);

		AuthorizationPlugin<RasOperation> authorization = Mockito.mock(DefaultAuthorizationPlugin.class);
		Mockito.when(authorization.isAuthorized(Mockito.eq(systemUser), Mockito.eq(new RasOperation(Operation.GET, ResourceType.COMPUTE)))).thenReturn(true);

		this.facade.setAuthorizationPlugin(authorization);

		// exercise
		this.facade.getCloudNames(FAKE_PROVIDER_ID, SYSTEM_USER_TOKEN_VALUE);
	}
	
	// test case: When calling the getComputeAllocation method, verify that this
	// call was successful.
	@Test
	public void testGetComputeAllocationSuccessfully() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		RasOperation operation = new RasOperation(
				Operation.GET_USER_ALLOCATION,
				ResourceType.COMPUTE,
				DEFAULT_CLOUD_NAME
		);

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		String cloudName = DEFAULT_CLOUD_NAME;

		// exercise
		this.facade.getComputeAllocation(FAKE_PROVIDER_ID, cloudName, SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
	}
	
	// test case: When calling the getComputeQuota method, verify that this call was
	// successful.
	@Test
	public void testGetComputeQuotaSuccessfully() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		RasOperation operation = new RasOperation(
				Operation.GET_USER_QUOTA,
				ResourceType.COMPUTE,
				DEFAULT_CLOUD_NAME
		);

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		ComputeQuota quota = Mockito.mock(ComputeQuota.class);
		Mockito.when(super.localCloudConnector.getUserQuota(Mockito.eq(systemUser), Mockito.eq(ResourceType.COMPUTE)))
				.thenReturn(quota);

		String cloudName = DEFAULT_CLOUD_NAME;
		
		// exercise
		this.facade.getComputeQuota(FAKE_PROVIDER_ID, cloudName, SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(super.localCloudConnector, Mockito.times(1)).getUserQuota(Mockito.eq(systemUser),
				Mockito.eq(ResourceType.COMPUTE));
	}
	
	// test case: When calling the getAllImages method, verify that this call was
	// successful.
	@Test
	public void testGetAllImagesSuccessfully() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		RasOperation operation = new RasOperation(
				Operation.GET_ALL,
				ResourceType.IMAGE,
				DEFAULT_CLOUD_NAME
		);

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		List<ImageSummary> imageSummaryList = new ArrayList<>();
		Mockito.when(super.localCloudConnector.getAllImages(Mockito.eq(systemUser))).thenReturn(imageSummaryList);
		
		String providerId = null;
		String cloudName = DEFAULT_CLOUD_NAME;
		
		// exercise
		this.facade.getAllImages(providerId, cloudName, SYSTEM_USER_TOKEN_VALUE);
		
		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(super.localCloudConnector, Mockito.times(1)).getAllImages(Mockito.eq(systemUser));
	}
	
	// test case: When calling the getImage method, verify that this call was
	// successful.
	@Test
	public void testGetImageSuccessfully() throws Exception {
		RSAPublicKey keyRSA = mockRSAPublicKey();

		RasOperation operation = new RasOperation(
				Operation.GET,
				ResourceType.IMAGE,
				DEFAULT_CLOUD_NAME
		);

		SystemUser systemUser = createFederationUserAuthenticate(keyRSA);
		AuthorizationPlugin<RasOperation> authorization = mockAuthorizationPlugin(systemUser, operation);

		ImageInstance imageInstance = Mockito.mock(ImageInstance.class);
		Mockito.when(super.localCloudConnector.getImage(Mockito.anyString(), Mockito.eq(systemUser))).thenReturn(imageInstance);

		String providerId = FAKE_PROVIDER_ID;
		String cloudName = DEFAULT_CLOUD_NAME;
		String imageId = FAKE_IMAGE_ID;

		// exercise
		this.facade.getImage(providerId, cloudName, imageId, SYSTEM_USER_TOKEN_VALUE);

		// verify
		Mockito.verify(this.facade, Mockito.times(1)).getAsPublicKey();

		PowerMockito.verifyStatic(AuthenticationUtil.class, Mockito.times(1));
		AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString());

		Mockito.verify(authorization, Mockito.times(1)).isAuthorized(Mockito.eq(systemUser), Mockito.eq(operation));
		Mockito.verify(super.localCloudConnector, Mockito.times(1)).getImage(Mockito.anyString(), Mockito.eq(systemUser));
	}
	
	private PublicIpOrder spyPublicIpOrder(SystemUser systemUser) {
		String localMemberId = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.LOCAL_PROVIDER_ID_KEY);

		ComputeOrder computeOrder = new ComputeOrder();
		computeOrder.setSystemUser(systemUser);
		computeOrder.setOrderStateInTestMode(OrderState.FULFILLED);
		computeOrder.setRequester(localMemberId);
		computeOrder.setProvider(localMemberId);
		computeOrder.setCloudName(DEFAULT_CLOUD_NAME);
		ComputeInstance computeInstance = new ComputeInstance(FAKE_SOURCE_ID);
		computeOrder.setInstanceId(computeInstance.getId());
		
		SharedOrderHolders sharedOrderHolders = SharedOrderHolders.getInstance();
		Map<String, Order> activeOrdersMap = Mockito.spy(sharedOrderHolders.getActiveOrdersMap());
		activeOrdersMap.put(computeOrder.getId(), computeOrder);
		
		String computeOrderId = computeOrder.getId();
		PublicIpOrder order = Mockito.spy(
				new PublicIpOrder(systemUser,
						localMemberId,
						localMemberId,
						DEFAULT_CLOUD_NAME, 
						computeOrderId));

		return order;
	}

	private List<InstanceStatus> generateInstancesStatus(Order order) throws InstanceNotFoundException {
		List<InstanceStatus> instancesExpected = new ArrayList<>();
		InstanceStatus instanceStatus = new InstanceStatus(
				order.getId(), 
				null, 
				order.getProvider(),
				order.getCloudName(), 
				InstanceStatus.mapInstanceStateFromOrderState(order.getOrderState()));

		instancesExpected.add(instanceStatus);
		return instancesExpected;
	}

	private SystemUser createFederationUserAuthenticate(RSAPublicKey keyRSA) throws FogbowException {
		SystemUser systemUser = createSystemUser();
		PowerMockito.mockStatic(AuthenticationUtil.class);
		PowerMockito.when(AuthenticationUtil.authenticate(Mockito.eq(keyRSA), Mockito.anyString()))
				.thenReturn(systemUser);
		
		return systemUser;
	}

    private RSAPublicKey mockRSAPublicKey() throws FogbowException {
        RSAPublicKey keyRSA = Mockito.mock(RSAPublicKey.class);
        Mockito.doReturn(keyRSA).when(this.facade).getAsPublicKey();
        return keyRSA;
    }

	private AuthorizationPlugin<RasOperation> mockAuthorizationPlugin(SystemUser systemUser, RasOperation rasOperation)
			throws UnexpectedException, UnauthorizedRequestException {

		AuthorizationPlugin<RasOperation> authorization = Mockito.mock(DefaultAuthorizationPlugin.class);
		Mockito.when(authorization.isAuthorized(Mockito.eq(systemUser), Mockito.eq(rasOperation))).thenReturn(true);

		this.facade.setAuthorizationPlugin(authorization);
		return authorization;
	}

	private ArrayList<UserData> generateVeryLongUserDataFileContent() {
	    char[] value = new char[UserData.MAX_EXTRA_USER_DATA_FILE_CONTENT + 1];
		String extraUserDataFileContent = new String(value);

		UserData userData = new UserData();
		userData.setExtraUserDataFileContent(extraUserDataFileContent);

		ArrayList<UserData> userDataScripts = new ArrayList<>();
		userDataScripts.add(userData);

		return userDataScripts;
	}

}
