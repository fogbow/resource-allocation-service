package cloud.fogbow.ras.core.plugins.interoperability.opennebula.quota.v5_4;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.group.Group;
import org.opennebula.client.user.User;
import org.opennebula.client.user.UserPool;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.CloudToken;
import cloud.fogbow.ras.api.http.response.quotas.ComputeQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenNebulaClientUtil.class})
public class OpenNebulaComputeQuotaPluginTest {

	private static final String EIGHT_CPU_VALUE = "8";
	private static final String FAKE_USER_ID = "fake-user-id";
	private static final String FIVE_VMS = "5";
	private static final String GROUPS_ID_PATH = "GROUPS/ID";
	private static final String LOCAL_TOKEN_VALUE = "user:password";
	private static final String MEMORY_VALUE_2MB = "2048";
	private static final String MEMORY_VALUE_16MB = "16384";
	private static final String MEMORY_VALUE_32MB = "32768";
	private static final String MEMORY_VALUE_64MB = "65536";
	private static final String ONE_ID_VALUE = "1";
	private static final String ONE_VM_VALUE = "1";
	private static final String QUOTA_VMS_PATH = "VM_QUOTA/VM/VMS";
	private static final String QUOTA_CPU_USED_PATH = "VM_QUOTA/VM/CPU_USED";
	private static final String QUOTA_MEMORY_USED_PATH = "VM_QUOTA/VM/MEMORY_USED";
	private static final String QUOTA_VMS_USED_PATH = "VM_QUOTA/VM/VMS_USED";
	private static final String QUOTA_CPU_PATH = "VM_QUOTA/VM/CPU";
	private static final String QUOTA_MEMORY_PATH = "VM_QUOTA/VM/MEMORY";
	private static final String SIX_CPU_VALUE = "6";
	private static final String SIXTEEN_CPU_VALUE = "16";
	private static final String TEN_VMS_VALUE = "10";
	private static final String TWENTY_VMS_VALUE = "20";
	private static final String TWO_CPU_VALUE = "2";
	private static final String VALUE_DEFAULT_QUOTA = "-1";
	private static final String VALUE_UNLIMITED_QUOTA = "-2";

	private static final int INSTANCES = 4;
	private static final int RAM = 30720;
	private static final int VCPU = 6;
	

	private OpenNebulaComputeQuotaPlugin plugin;

	@Before
	public void setUp() {
		this.plugin = Mockito.spy(new OpenNebulaComputeQuotaPlugin());
	}
	
	// test case: When invoking the getUserQuota method, with a valid client, a
	// collection of users must be loaded next to a group of users, and their data
	// will be collected to calculate the available quotas.
	@Test
	public void testGetUserQuotaSuccessful() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		UserPool userPool = Mockito.mock(UserPool.class);
		BDDMockito.given(OpenNebulaClientUtil.getUserPool(Mockito.eq(client))).willReturn(userPool);

		User user = Mockito.mock(User.class);
		BDDMockito.given(OpenNebulaClientUtil.getUser(Mockito.eq(userPool), Mockito.anyString())).willReturn(user);

		Iterator<User> userIterator = Mockito.mock(Iterator.class);
		Mockito.when(userPool.iterator()).thenReturn(userIterator);
		Mockito.when(userIterator.hasNext()).thenReturn(true, false);
		Mockito.when(userIterator.next()).thenReturn(user);

		OneResponse response = Mockito.mock(OneResponse.class);
		Mockito.when(user.info()).thenReturn(response);
		Mockito.when(response.isError()).thenReturn(false);

		Mockito.when(user.xpath(QUOTA_CPU_USED_PATH)).thenReturn(TWO_CPU_VALUE);
		Mockito.when(user.xpath(QUOTA_MEMORY_USED_PATH)).thenReturn(MEMORY_VALUE_2MB);
		Mockito.when(user.xpath(QUOTA_VMS_USED_PATH)).thenReturn(ONE_VM_VALUE);
		Mockito.when(user.xpath(QUOTA_CPU_PATH)).thenReturn(EIGHT_CPU_VALUE);
		Mockito.when(user.xpath(QUOTA_MEMORY_PATH)).thenReturn(MEMORY_VALUE_32MB);
		Mockito.when(user.xpath(QUOTA_VMS_PATH)).thenReturn(FIVE_VMS);

		Mockito.when(user.xpath(GROUPS_ID_PATH)).thenReturn(ONE_ID_VALUE);

		Group group = Mockito.mock(Group.class);
		BDDMockito.given(OpenNebulaClientUtil.getGroup(Mockito.eq(client), Mockito.anyInt())).willReturn(group);

		Mockito.when(group.xpath(QUOTA_CPU_USED_PATH)).thenReturn(SIX_CPU_VALUE);
		Mockito.when(group.xpath(QUOTA_MEMORY_USED_PATH)).thenReturn(MEMORY_VALUE_16MB);
		Mockito.when(group.xpath(QUOTA_VMS_USED_PATH)).thenReturn(TEN_VMS_VALUE);
		Mockito.when(group.xpath(QUOTA_CPU_PATH)).thenReturn(SIXTEEN_CPU_VALUE);
		Mockito.when(group.xpath(QUOTA_MEMORY_PATH)).thenReturn(MEMORY_VALUE_64MB);
		Mockito.when(group.xpath(QUOTA_VMS_PATH)).thenReturn(TWENTY_VMS_VALUE);

		ComputeAllocation expected = new ComputeAllocation(VCPU, RAM, INSTANCES);
		
		CloudToken token = createCloudToken();
		
		// exercise
		ComputeQuota computeQuota = this.plugin.getUserQuota(token);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getUser(Mockito.eq(userPool), Mockito.anyString());
		
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
	public void testGetUserQuotaWithDefaultValuesInUserResources() throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		UserPool userPool = Mockito.mock(UserPool.class);
		BDDMockito.given(OpenNebulaClientUtil.getUserPool(Mockito.eq(client))).willReturn(userPool);

		User user = Mockito.mock(User.class);
		BDDMockito.given(OpenNebulaClientUtil.getUser(Mockito.eq(userPool), Mockito.anyString())).willReturn(user);

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
		Mockito.when(user.xpath(QUOTA_VMS_PATH)).thenReturn(ONE_VM_VALUE);

		Mockito.when(user.xpath(GROUPS_ID_PATH)).thenReturn(ONE_ID_VALUE);

		Group group = Mockito.mock(Group.class);
		BDDMockito.given(OpenNebulaClientUtil.getGroup(Mockito.eq(client), Mockito.anyInt())).willReturn(group);

		Mockito.when(group.xpath(QUOTA_CPU_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_MEMORY_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_VMS_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_CPU_PATH)).thenReturn(VALUE_UNLIMITED_QUOTA);
		Mockito.when(group.xpath(QUOTA_MEMORY_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_VMS_PATH)).thenReturn(null);

		CloudToken token = createCloudToken();
		
		// exercise
		this.plugin.getUserQuota(token);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getUser(Mockito.eq(userPool), Mockito.anyString());

		Mockito.verify(this.plugin, Mockito.times(1)).getUserQuota(Mockito.eq(token));
		Mockito.verify(user, Mockito.times(7)).xpath(Mockito.anyString());
	}
	
	// test case: the computation of the remaining value of computing quotas must
	// remain accessible even with the return null or of data as unlimited value
	// quota for groups resource.
	@Test
	public void testGetUserQuotaWithUnlimitedValuesInGroupResources()
			throws FogbowException {
		// set up
		Client client = Mockito.mock(Client.class);
		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		BDDMockito.given(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.willReturn(client);

		UserPool userPool = Mockito.mock(UserPool.class);
		BDDMockito.given(OpenNebulaClientUtil.getUserPool(Mockito.eq(client))).willReturn(userPool);

		User user = Mockito.mock(User.class);
		BDDMockito.given(OpenNebulaClientUtil.getUser(Mockito.eq(userPool), Mockito.anyString())).willReturn(user);

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

		Mockito.when(user.xpath(GROUPS_ID_PATH)).thenReturn(ONE_ID_VALUE);

		Group group = Mockito.mock(Group.class);
		BDDMockito.given(OpenNebulaClientUtil.getGroup(Mockito.eq(client), Mockito.anyInt())).willReturn(group);

		Mockito.when(group.xpath(QUOTA_CPU_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_MEMORY_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_VMS_USED_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_CPU_PATH)).thenReturn(VALUE_UNLIMITED_QUOTA);
		Mockito.when(group.xpath(QUOTA_MEMORY_PATH)).thenReturn(null);
		Mockito.when(group.xpath(QUOTA_VMS_PATH)).thenReturn(VALUE_UNLIMITED_QUOTA);

		CloudToken token = createCloudToken();
		
		// exercise
		this.plugin.getUserQuota(token);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString());
		
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, VerificationModeFactory.times(1));
		OpenNebulaClientUtil.getUser(Mockito.eq(userPool), Mockito.anyString());
		
		Mockito.verify(this.plugin, Mockito.times(1)).getUserQuota(Mockito.eq(token));
		Mockito.verify(user, Mockito.times(7)).xpath(Mockito.anyString());
	}
	
	private CloudToken createCloudToken() {
		String provider = null;
		String tokenValue = LOCAL_TOKEN_VALUE;
		String userId = FAKE_USER_ID;
		CloudToken token = new CloudToken(provider, userId, tokenValue);
		return token;
	}
}
