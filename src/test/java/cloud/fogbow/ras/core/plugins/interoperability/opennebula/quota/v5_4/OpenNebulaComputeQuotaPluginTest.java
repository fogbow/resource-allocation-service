package cloud.fogbow.ras.core.plugins.interoperability.opennebula.quota.v5_4;

import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.user.User;
import org.opennebula.client.user.UserPool;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ComputeAllocation;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;

@PrepareForTest({OpenNebulaClientUtil.class, DatabaseManager.class})
public class OpenNebulaComputeQuotaPluginTest extends OpenNebulaBaseTests {

	private static final String CPU_MAX_VALUE = "8";
	private static final String CPU_USED_VALUE = "2";
	private static final String FRACTION_RESOURCE_USED_VALUE = "1.5";
	private static final String MEMORY_USED_VALUE = "2048";
	private static final String MEMORY_MAX_VALUE = "32768";
	private static final String QUOTA_CPU_USED_PATH = OpenNebulaComputeQuotaPlugin.QUOTA_CPU_USED_PATH;
	private static final String QUOTA_MEMORY_USED_PATH = OpenNebulaComputeQuotaPlugin.QUOTA_MEMORY_USED_PATH;
	private static final String QUOTA_VMS_USED_PATH = OpenNebulaComputeQuotaPlugin.QUOTA_VMS_USED_PATH;
	private static final String QUOTA_CPU_PATH = OpenNebulaComputeQuotaPlugin.QUOTA_CPU_PATH;
	private static final String QUOTA_MEMORY_PATH = OpenNebulaComputeQuotaPlugin.QUOTA_MEMORY_PATH;
	private static final String QUOTA_VMS_PATH = OpenNebulaComputeQuotaPlugin.QUOTA_VMS_PATH;
	private static final String VMS_MAX_VALUE = "5";
	private static final String VMS_USED_VALUE = "1";

	private static final int CPU_EXPECTED = 6;

	private OpenNebulaComputeQuotaPlugin plugin;
	private User user;

	@Before
	public void setUp() throws FogbowException {
	    super.setUp();

		this.plugin = Mockito.spy(new OpenNebulaComputeQuotaPlugin(this.openNebulaConfFilePath));
		this.user = Mockito.mock(User.class);
	}
	
	// test case: When invoking the getUserQuota method, with a valid client, a
	// collection of users must be loaded, with valid data to be collected to
	// calculate the available quotas.
	@Test
	public void testGetUserQuota() throws FogbowException {
		// set up
		UserPool userPool = Mockito.mock(UserPool.class);

		Mockito.doReturn(new ComputeAllocation()).when(this.plugin).getTotalAllocation(this.user);
		Mockito.doReturn(new ComputeAllocation()).when(this.plugin).getUsedAllocation(this.user);

		PowerMockito.mockStatic(OpenNebulaClientUtil.class);
		Mockito.when(OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.anyString()))
				.thenReturn(this.client);
		Mockito.when(OpenNebulaClientUtil.getUserPool(Mockito.any(Client.class))).thenReturn(userPool);
		Mockito.when(OpenNebulaClientUtil.getUser(Mockito.eq(userPool), Mockito.anyString())).thenReturn(this.user);

		// exercise
		this.plugin.getUserQuota(this.cloudUser);

		// verify
		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getUserPool(Mockito.any(Client.class));

		PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
		OpenNebulaClientUtil.getUser(Mockito.eq(userPool), Mockito.eq(this.cloudUser.getId()));

		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getTotalAllocation(Mockito.eq(this.user));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getUsedAllocation(Mockito.eq(this.user));
	}
	
	// test case: When invoking the getTotalAllocation method, with a valid user,
    // return the respective quota values encapsulated in a ComputeAllocation object
	@Test
	public void testGetTotalAllocation() {
		// set up
		Mockito.when(this.user.xpath(QUOTA_CPU_PATH)).thenReturn(CPU_MAX_VALUE);
		Mockito.when(this.user.xpath(QUOTA_MEMORY_PATH)).thenReturn(MEMORY_MAX_VALUE);
		Mockito.when(this.user.xpath(QUOTA_VMS_PATH)).thenReturn(VMS_MAX_VALUE);
		Mockito.doReturn(CPU_EXPECTED).when(this.plugin).convertToInteger(Mockito.anyString());

		// exercise
		ComputeAllocation computeAllocation = this.plugin.getTotalAllocation(this.user);

		// verify
		Mockito.verify(this.user, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(QUOTA_CPU_PATH));
		Mockito.verify(this.user, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(QUOTA_MEMORY_PATH));
		Mockito.verify(this.user, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(QUOTA_VMS_PATH));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_THRICE)).convertToInteger(Mockito.anyString());

		Assert.assertEquals(CPU_EXPECTED, computeAllocation.getvCPU());
	}

	// test case: When invoking the getUsedAllocation method, with a valid user,
	// return the respective quota values encapsulated in a ComputeAllocation object
	@Test
	public void testGetUsedAllocation() {
		// set up
		Mockito.when(this.user.xpath(QUOTA_CPU_USED_PATH)).thenReturn(CPU_USED_VALUE);
		Mockito.when(this.user.xpath(QUOTA_MEMORY_USED_PATH)).thenReturn(MEMORY_USED_VALUE);
		Mockito.when(this.user.xpath(QUOTA_VMS_USED_PATH)).thenReturn(VMS_USED_VALUE);
		Mockito.doReturn(CPU_EXPECTED).when(this.plugin).convertToInteger(Mockito.anyString());

		// exercise
		ComputeAllocation computeAllocation = this.plugin.getUsedAllocation(this.user);

		// verify
		Mockito.verify(this.user, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(QUOTA_CPU_USED_PATH));
		Mockito.verify(this.user, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(QUOTA_MEMORY_USED_PATH));
		Mockito.verify(this.user, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(QUOTA_VMS_USED_PATH));
		Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_THRICE)).convertToInteger(Mockito.anyString());

		Assert.assertEquals(CPU_EXPECTED, computeAllocation.getvCPU());
	}

	// test case: When invoking the testConvertToInteger method, with an integer-convertible String,
	// return the respective round int value or 0 otherwise
	@Test
    public void testConvertToInteger() {
	    // set up
		int expectedIntSuccess = 2;
		int expectedIntFail = 0;

		// exercise
		int intSuccess = this.plugin.convertToInteger(FRACTION_RESOURCE_USED_VALUE);
		int intFail = this.plugin.convertToInteger("");

		// verify
		Assert.assertEquals(expectedIntSuccess, intSuccess);
		Assert.assertEquals(expectedIntFail, intFail);
	}
}
