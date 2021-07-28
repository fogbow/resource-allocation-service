package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.compute.v1;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.models.GoogleCloudUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.googlecloud.GoogleCloudHttpClient;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudPluginUtils;
import cloud.fogbow.ras.core.plugins.interoperability.googlecloud.util.GoogleCloudStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;

@PrepareForTest({GoogleCloudPluginUtils.class, DatabaseManager.class})
public class GoogleCloudComputePluginTest extends BaseUnitTests {

    private static final String CLOUD_NAME = "google";
    private static final String PROJECT_ID = "projectId";
    private static final String ZONE = "default-zone";
    private static final String INSTANCE_ID = "instanceId";
    private static final String ANY_VALUE = "anything";
    private GoogleCloudComputePlugin plugin;
    private LaunchCommandGenerator launchCommandGenerator;
    private GoogleCloudHttpClient client;
    
    @Before
    public void setUp() throws FogbowException {
        this.testUtils.mockReadOrdersFromDataBase();
        this.launchCommandGenerator = Mockito.mock(LaunchCommandGenerator.class);
        this.client = Mockito.mock(GoogleCloudHttpClient.class);
        
        String googleCloudConfFilePath = HomeDir.getPath() 
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator 
                + CLOUD_NAME 
                + File.separator 
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        
        this.plugin = Mockito.spy(new GoogleCloudComputePlugin(googleCloudConfFilePath, launchCommandGenerator, client));
    }

    // test case: When calling the stopInstance method, with a compute order and
    // cloud user valid, the instance in the cloud must be stopped correctly.
    @Test
    public void testStopInstance() throws FogbowException {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        order.setInstanceId(INSTANCE_ID);
        
        GoogleCloudUser cloudUser = Mockito.mock(GoogleCloudUser.class);
        Mockito.when(cloudUser.getProjectId()).thenReturn(PROJECT_ID);

        // exercise
        this.plugin.stopInstance(order, cloudUser);
        
        // verify
        String expectedRequestBody = "";
        // Reference: https://cloud.google.com/compute/docs/instances/stop-start-instance#stop_a_vm
        String expectedUrl = 
                GoogleCloudConstants.BASE_COMPUTE_API_URL + GoogleCloudConstants.COMPUTE_ENGINE_V1_ENDPOINT +  
                GoogleCloudConstants.PROJECT_ENDPOINT + GoogleCloudConstants.ENDPOINT_SEPARATOR + PROJECT_ID + 
                GoogleCloudConstants.ZONES_ENDPOINT + GoogleCloudConstants.ENDPOINT_SEPARATOR + ZONE + 
                GoogleCloudConstants.INSTANCES_ENDPOINT + GoogleCloudConstants.ENDPOINT_SEPARATOR + INSTANCE_ID + 
                GoogleCloudConstants.ENDPOINT_SEPARATOR + GoogleCloudConstants.Compute.STOP_ENDPOINT;
        
        Mockito.verify(this.client).doPostRequest(expectedUrl, expectedRequestBody, cloudUser);
    }
    
    // test case: When calling the resumeInstance method, with a compute order and
    // cloud user valid, the instance in the cloud must be started correctly.
    @Test
    public void testResumeInstance() throws FogbowException {
        // set up
        ComputeOrder order = this.testUtils.createLocalComputeOrder();
        order.setInstanceId(INSTANCE_ID);
        
        GoogleCloudUser cloudUser = Mockito.mock(GoogleCloudUser.class);
        Mockito.when(cloudUser.getProjectId()).thenReturn(PROJECT_ID);

        // exercise
        this.plugin.resumeInstance(order, cloudUser);
        
        // verify
        String expectedRequestBody = "";
        // Reference: https://cloud.google.com/compute/docs/instances/stop-start-instance#restart_a_stopped_vm_that_doesnt_have_an_encrypted_disk
        String expectedUrl = 
                GoogleCloudConstants.BASE_COMPUTE_API_URL + GoogleCloudConstants.COMPUTE_ENGINE_V1_ENDPOINT +  
                GoogleCloudConstants.PROJECT_ENDPOINT + GoogleCloudConstants.ENDPOINT_SEPARATOR + PROJECT_ID + 
                GoogleCloudConstants.ZONES_ENDPOINT + GoogleCloudConstants.ENDPOINT_SEPARATOR + ZONE + 
                GoogleCloudConstants.INSTANCES_ENDPOINT + GoogleCloudConstants.ENDPOINT_SEPARATOR + INSTANCE_ID + 
                GoogleCloudConstants.ENDPOINT_SEPARATOR + GoogleCloudConstants.Compute.START_ENDPOINT;
        
        Mockito.verify(this.client).doPostRequest(expectedUrl, expectedRequestBody, cloudUser);
    }
    
    // test case: When calling the isStopped method with the cloud state TERMINATED,
    // this means that the state of compute is STOPPED and it must return true.
    @Test
    public void testIsStopped() throws FogbowException {
        // set up
        String cloudState = GoogleCloudStateMapper.TERMINATED_STATE;

        // exercise
        boolean status = this.plugin.isStopped(cloudState);

        // verify
        Assert.assertTrue(status);
    }
    
    // test case: When calling the isStopped method with the cloud states different
    // from TERMINATED, this means that the state of compute is not STOPPED and it must
    // return false.
    @Test
    public void testIsNotStopped() throws FogbowException {
        // set up
        String[] cloudStates = { ANY_VALUE, GoogleCloudStateMapper.PROVISIONING_STATE, GoogleCloudStateMapper.STAGING_STATE,
          GoogleCloudStateMapper.RUNNING_STATE, GoogleCloudStateMapper.STOPPING_STATE, GoogleCloudStateMapper.REPAIRING_STATE,
          GoogleCloudStateMapper.SUSPENDING_STATE, GoogleCloudStateMapper.SUSPENDED_STATE};

        for (String cloudState : cloudStates) {
            // exercise
            boolean status = this.plugin.isStopped(cloudState);

            // verify
            Assert.assertFalse(status);
        }
    }
}
