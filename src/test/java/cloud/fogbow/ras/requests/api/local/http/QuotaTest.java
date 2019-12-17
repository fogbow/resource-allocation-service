package cloud.fogbow.ras.requests.api.local.http;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.gson.Gson;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.request.Quota;
import cloud.fogbow.ras.api.http.response.quotas.ResourceQuota;
import cloud.fogbow.ras.api.http.response.quotas.allocation.ResourceAllocation;
import cloud.fogbow.ras.core.ApplicationFacade;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;

@PrepareForTest({ ApplicationFacade.class, DatabaseManager.class })
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(Quota.class)
public class QuotaTest extends BaseUnitTests {

    private static final String QUOTA_ENDPOINT_FORMAT = "/ras/quota/%s/%s";

    @Autowired
    private MockMvc mockMvc;

    private ApplicationFacade facade;

    @Before
    public void setUp() throws FogbowException {
        this.testUtils.mockReadOrdersFromDataBase();
        this.facade = this.testUtils.mockApplicationFacade();
    }
    
    // test case: When executing a GET request for the "/ras/quota" endpoint with a
    // valid provide ID and cloud name, it must invoke the getAllQuotas method by
    // returning the Status OK and the content of ResourceQuota class in JSON
    // format.
    @Test
    public void testGetAllQuotas() throws Exception {
        // set up
        String providerId = TestUtils.LOCAL_MEMBER_ID;
        String cloudName = TestUtils.DEFAULT_CLOUD_NAME;
        String userToken = TestUtils.FAKE_TOKEN_VALUE;

        ResourceQuota resourceQuota = this.testUtils.createAvailableQuota();
        Mockito.doReturn(resourceQuota).when(this.facade).getResourceQuota(Mockito.eq(providerId),
                Mockito.eq(cloudName), Mockito.eq(userToken));

        String endpoint = String.format(QUOTA_ENDPOINT_FORMAT, providerId, cloudName);
        String expected = this.testUtils.getResponseContent(resourceQuota);

        // exercise
        this.mockMvc
                .perform(
                        MockMvcRequestBuilders.get(endpoint).header(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, userToken))
                // verify
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(expected));
    }
    
}
