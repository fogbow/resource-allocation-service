package org.fogbowcloud.ras.core.plugins.interoperability.opennebula.quota.v5_4;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.SystemConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.quotas.ComputeQuota;
import org.fogbowcloud.ras.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.ras.core.models.tokens.OpenNebulaToken;
import org.fogbowcloud.ras.core.plugins.interoperability.opennebula.OpenNebulaClientFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.group.Group;
import org.opennebula.client.user.User;
import org.opennebula.client.user.UserPool;

import java.io.File;
import java.util.Iterator;

public class OpenNebulaComputeQuotaPluginTest {

	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String FAKE_USER_NAME = "fake-user-name";
	private static final String QUOTA_CPU_USED_PATH = "VM_QUOTA/VM/CPU_USED";
	private static final String QUOTA_MEMORY_USED_PATH = "VM_QUOTA/VM/MEMORY_USED";
	private static final String QUOTA_VMS_USED_PATH = "VM_QUOTA/VM/VMS_USED";
	private static final String QUOTA_CPU_PATH = "VM_QUOTA/VM/CPU";
	private static final String QUOTA_MEMORY_PATH = "VM_QUOTA/VM/MEMORY";
	private static final String QUOTA_VMS_PATH = "VM_QUOTA/VM/VMS";
	private static final String GROUPS_ID_PATH = "GROUPS/ID";
	private static final String UNCHECKED_VALUE = "unchecked";
	private static final String VALUE_DEFAULT_QUOTA = "-1";
	private static final String VALUE_UNLIMITED_QUOTA = "-2";
	private static final String CLOUD_NAME = "opennebula";

	private OpenNebulaClientFactory factory;
	private OpenNebulaComputeQuotaPlugin plugin;

	@Before
	public void setUp() {
		String openenbulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
				File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
		this.factory = Mockito.spy(new OpenNebulaClientFactory(openenbulaConfFilePath));
		this.plugin = Mockito.spy(new OpenNebulaComputeQuotaPlugin(openenbulaConfFilePath));
	}
	
	// test case: When calling the getUserQuota method, if the createClient method
	// in the OpenNebulaClientFactory class can not create a valid client from a
	// token value, it must throw a UnespectedException.
	@Test(expected = UnexpectedException.class) // verify
	public void testGetUserQuotaThrowExceptionWhenCallCreateClientMethod()
			throws UnexpectedException, FogbowRasException {

		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Mockito.doThrow(new UnexpectedException()).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		// exercise
		this.plugin.getUserQuota(token);
	}
	
	// test case: When invoking the getUserQuota method, with a valid client, a
	// collection of users must be loaded next to a group of users, and their data
	// will be collected to calculate the available quotas.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testGetUserQuotaSuccessful() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		UserPool userPool = Mockito.mock(UserPool.class);
		Mockito.doReturn(userPool).when(this.factory).createUserPool(client);

		User user = Mockito.mock(User.class);
		Mockito.doReturn(user).when(this.factory).getUser(userPool, FAKE_USER_NAME);

		Iterator<User> userIterator = Mockito.mock(Iterator.class);
		Mockito.when(userPool.iterator()).thenReturn(userIterator);
		Mockito.when(userIterator.hasNext()).thenReturn(true, false);
		Mockito.when(userIterator.next()).thenReturn(user);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(user.info()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		Mockito.when(user.xpath(QUOTA_CPU_USED_PATH)).thenReturn("2");
		Mockito.when(user.xpath(QUOTA_MEMORY_USED_PATH)).thenReturn("2048");
		Mockito.when(user.xpath(QUOTA_VMS_USED_PATH)).thenReturn("1");
		Mockito.when(user.xpath(QUOTA_CPU_PATH)).thenReturn("8");
		Mockito.when(user.xpath(QUOTA_MEMORY_PATH)).thenReturn("32768");
		Mockito.when(user.xpath(QUOTA_VMS_PATH)).thenReturn("5");

		Mockito.when(user.xpath(GROUPS_ID_PATH)).thenReturn("1");

		Group group = Mockito.mock(Group.class);
		Mockito.doReturn(group).when(this.factory).createGroup(client, 1);

		Mockito.when(group.xpath(QUOTA_CPU_USED_PATH)).thenReturn("6");
		Mockito.when(group.xpath(QUOTA_MEMORY_USED_PATH)).thenReturn("16384");
		Mockito.when(group.xpath(QUOTA_VMS_USED_PATH)).thenReturn("10");
		Mockito.when(group.xpath(QUOTA_CPU_PATH)).thenReturn("16");
		Mockito.when(group.xpath(QUOTA_MEMORY_PATH)).thenReturn("65536");
		Mockito.when(group.xpath(QUOTA_VMS_PATH)).thenReturn("20");

		ComputeAllocation expected = new ComputeAllocation(6, 30720, 4);
		
		// exercise
		ComputeQuota computeQuota = this.plugin.getUserQuota(token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).getUser(Mockito.eq(userPool), Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).getUserQuota(Mockito.eq(token));
		Mockito.verify(user, Mockito.times(7)).xpath(Mockito.anyString());
		
		Assert.assertEquals(expected.getvCPU(), computeQuota.getAvailableQuota().getvCPU());
		Assert.assertEquals(expected.getRam(), computeQuota.getAvailableQuota().getRam());
		Assert.assertEquals(expected.getInstances(), computeQuota.getAvailableQuota().getInstances());
	}
	
	// test case: the computation of the remaining value of computing quotas must
	// remain accessible even with the return null or of data as default value quota
	// for users resource.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testGetUserQuotaWithDefaultValuesInUserResources() throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		UserPool userPool = Mockito.mock(UserPool.class);
		Mockito.doReturn(userPool).when(this.factory).createUserPool(client);

		User user = Mockito.mock(User.class);
		Mockito.doReturn(user).when(this.factory).getUser(userPool, FAKE_USER_NAME);

		Iterator<User> userIterator = Mockito.mock(Iterator.class);
		Mockito.when(userPool.iterator()).thenReturn(userIterator);
		Mockito.when(userIterator.hasNext()).thenReturn(true, false);
		Mockito.when(userIterator.next()).thenReturn(user);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(user.info()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		Mockito.when(user.xpath(QUOTA_CPU_USED_PATH)).thenReturn(null);
		Mockito.when(user.xpath(QUOTA_MEMORY_USED_PATH)).thenReturn(null);
		Mockito.when(user.xpath(QUOTA_VMS_USED_PATH)).thenReturn(null);
		Mockito.when(user.xpath(QUOTA_CPU_PATH)).thenReturn(VALUE_DEFAULT_QUOTA);
		Mockito.when(user.xpath(QUOTA_MEMORY_PATH)).thenReturn(VALUE_DEFAULT_QUOTA);
		Mockito.when(user.xpath(QUOTA_VMS_PATH)).thenReturn("1");

		Mockito.when(user.xpath(GROUPS_ID_PATH)).thenReturn("1");

		Group group = Mockito.mock(Group.class);
		Mockito.doReturn(group).when(this.factory).createGroup(client, 1);

		Mockito.when(group.xpath(QUOTA_CPU_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_MEMORY_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_VMS_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_CPU_PATH)).thenReturn(VALUE_UNLIMITED_QUOTA);
		Mockito.when(group.xpath(QUOTA_MEMORY_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_VMS_PATH)).thenReturn(null);

		// exercise
		this.plugin.getUserQuota(token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).getUser(Mockito.eq(userPool), Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).getUserQuota(Mockito.eq(token));
		Mockito.verify(user, Mockito.times(7)).xpath(Mockito.anyString());
	}
	
	// test case: the computation of the remaining value of computing quotas must
	// remain accessible even with the return null or of data as unlimited value
	// quota for groups resource.
	@Test
	@SuppressWarnings(UNCHECKED_VALUE)
	public void testGetUserQuotaWithUnlimitedValuesInGroupResources()
			throws UnexpectedException, FogbowRasException {
		// set up
		OpenNebulaToken token = createOpenNebulaToken();
		Client client = this.factory.createClient(token.getTokenValue());
		Mockito.doReturn(client).when(this.factory).createClient(token.getTokenValue());
		this.plugin.setFactory(this.factory);

		UserPool userPool = Mockito.mock(UserPool.class);
		Mockito.doReturn(userPool).when(this.factory).createUserPool(client);

		User user = Mockito.mock(User.class);
		Mockito.doReturn(user).when(this.factory).getUser(userPool, FAKE_USER_NAME);

		Iterator<User> userIterator = Mockito.mock(Iterator.class);
		Mockito.when(userPool.iterator()).thenReturn(userIterator);
		Mockito.when(userIterator.hasNext()).thenReturn(true, false);
		Mockito.when(userIterator.next()).thenReturn(user);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(user.info()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		Mockito.when(user.xpath(QUOTA_CPU_USED_PATH)).thenReturn(null);
		Mockito.when(user.xpath(QUOTA_MEMORY_USED_PATH)).thenReturn(null);
		Mockito.when(user.xpath(QUOTA_VMS_USED_PATH)).thenReturn(null);
		Mockito.when(user.xpath(QUOTA_CPU_PATH)).thenReturn(null);
		Mockito.when(user.xpath(QUOTA_MEMORY_PATH)).thenReturn(null);
		Mockito.when(user.xpath(QUOTA_VMS_PATH)).thenReturn(null);

		Mockito.when(user.xpath(GROUPS_ID_PATH)).thenReturn("1");

		Group group = Mockito.mock(Group.class);
		Mockito.doReturn(group).when(this.factory).createGroup(client, 1);

		Mockito.when(group.xpath(QUOTA_CPU_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_MEMORY_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_VMS_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_CPU_PATH)).thenReturn(VALUE_UNLIMITED_QUOTA);
		Mockito.when(group.xpath(QUOTA_MEMORY_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_VMS_PATH)).thenReturn(VALUE_UNLIMITED_QUOTA);

		// exercise
		this.plugin.getUserQuota(token);

		// verify
		Mockito.verify(this.factory, Mockito.times(2)).createClient(Mockito.anyString());
		Mockito.verify(this.factory, Mockito.times(1)).getUser(Mockito.eq(userPool), Mockito.anyString());
		Mockito.verify(this.plugin, Mockito.times(1)).getUserQuota(Mockito.eq(token));
		Mockito.verify(user, Mockito.times(7)).xpath(Mockito.anyString());
	}
	
	private OpenNebulaToken createOpenNebulaToken() {
		String provider = null;
		String tokenValue = LOCAL_TOKEN_VALUE;
		String userId = null;
		String userName = FAKE_USER_NAME;
		String signature = null;
		
		OpenNebulaToken token = new OpenNebulaToken(
				provider, 
				tokenValue, 
				userId, 
				userName, 
				signature);
		
		return token;
	}
}
