package cloud.fogbow.ras.core.plugins.interoperability.openstack.compute.v2;

import cloud.fogbow.common.constants.OpenStackConstants;
import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.OpenStackV3User;
import cloud.fogbow.common.models.linkedlists.SynchronizedDoublyLinkedList;
import cloud.fogbow.common.util.connectivity.cloud.openstack.OpenStackHttpClient;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.models.ResourceType;
import cloud.fogbow.ras.api.http.response.ComputeInstance;
import cloud.fogbow.ras.api.http.response.InstanceState;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.OrderState;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.OpenStackStateMapper;
import cloud.fogbow.ras.core.plugins.interoperability.openstack.network.v2.OpenStackNetworkPlugin;
import cloud.fogbow.ras.core.plugins.interoperability.util.LaunchCommandGenerator;
import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SharedOrderHolders.class})
public class OpenStackComputePluginTest {

    private OpenStackComputePlugin computePlugin;
    private OpenStackV3User openStackV3User;
    private LaunchCommandGenerator launchCommandGeneratorMock;
    private OpenStackHttpClient clientMock;
    private Properties propertiesMock;
    private PropertiesHolder propertiesHolderMock;
    private ArgumentCaptor<String> argString = ArgumentCaptor.forClass(String.class);
    private ArgumentCaptor<String> argBodyString = ArgumentCaptor.forClass(String.class);
    private ArgumentCaptor<OpenStackV3User> argCloudToken = ArgumentCaptor.forClass(OpenStackV3User.class);
    private final String defaultNetworkId = "fake-default-network-id";
    private final String imageId = "image-id";
    private final String publicKey = "public-key";
    private final String idKeyName = "493315b3-dd01-4b38-974f-289570f8e7ee";
    private final String bestFlavorId = "best-flavor";
    private final int bestCpu = 2;
    private final int bestMemory = 1024;

    private static final String FAKE_TOKEN_VALUE = "fake-token-value";
    private static final String FAKE_USER_ID = "fake-user-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_PROJECT_ID = "fake-project-id";

    private final int bestDisk = 8;
    private final String privateNetworkId = "fake-private-network-id";
    private final String userData = "userDataFromLauchCommand";
    private final JSONObject rootKeypairJson = generateRootKeyPairJson(idKeyName, publicKey);
    private final List<String> networksId = new ArrayList<String>();
    private final List<String> responseNetworkIds = new ArrayList<String>(networksId);
    private final String idInstanceName = "12345678-dd01-4b38-974f-289570f8e7ee";
    private final String expectedInstanceId = "instance-id-00";
    private final String expectedInstanceIdJson = generateInstaceId(expectedInstanceId);
    private final String localIpAddress = "localIpAddress";
    private final String instanceId = "compute-instance-id";
    private final String requirementTag = "besttag";
    private final String requirementValue = "bestvalue";
    private final int vCPU = 10;
    private final int ram = 15;
    private final int disk = 20;
    private String computeEndpoint;
    private String flavorEndpoint;
    private String flavorExtraSpecsEndpoint;
    private String osKeyPairEndpoint;
    private String hostName = "hostName";
    private String flavorId = "flavorId";
    private final String computeNovaV2UrlKey = "compute-nova-v2-url-key";
    private String openstackStateActive = OpenStackStateMapper.ACTIVE_STATUS;
    private SharedOrderHolders sharedOrderHolders;

    @Before
    public void setUp() throws Exception {
        this.propertiesHolderMock = Mockito.mock(PropertiesHolder.class);
        this.propertiesMock = Mockito.mock(Properties.class);
        this.clientMock = Mockito.mock(OpenStackHttpClient.class);
        this.launchCommandGeneratorMock = Mockito.mock(LaunchCommandGenerator.class);
        this.networksId.add(privateNetworkId);
        this.responseNetworkIds.add(defaultNetworkId);
        this.responseNetworkIds.add(privateNetworkId);

        this.openStackV3User = new OpenStackV3User(FAKE_USER_ID, FAKE_NAME, FAKE_TOKEN_VALUE, FAKE_PROJECT_ID);

        this.computePlugin = Mockito.spy(new OpenStackComputePlugin(this.propertiesMock,
                this.launchCommandGeneratorMock, this.clientMock));
        this.osKeyPairEndpoint = computeNovaV2UrlKey + OpenStackComputePlugin.COMPUTE_V2_API_ENDPOINT
                + this.openStackV3User.getProjectId()
                + OpenStackComputePlugin.SUFFIX_ENDPOINT_KEYPAIRS;
        this.computeEndpoint = computeNovaV2UrlKey + OpenStackComputePlugin.COMPUTE_V2_API_ENDPOINT
                + this.openStackV3User.getProjectId()
                + OpenStackComputePlugin.SERVERS;
        this.flavorEndpoint = this.computeNovaV2UrlKey + OpenStackComputePlugin.COMPUTE_V2_API_ENDPOINT
                + this.openStackV3User.getProjectId()
                + OpenStackComputePlugin.SUFFIX_ENDPOINT_FLAVORS;

        this.flavorExtraSpecsEndpoint = this.computeNovaV2UrlKey + OpenStackComputePlugin.COMPUTE_V2_API_ENDPOINT
                + this.openStackV3User.getProjectId()
                + OpenStackComputePlugin.SUFFIX_ENDPOINT_FLAVORS
                + "/%s"
                + OpenStackComputePlugin.SUFFIX_FLAVOR_EXTRA_SPECS;

        Mockito.when(this.propertiesMock.getProperty(OpenStackComputePlugin.COMPUTE_NOVAV2_URL_KEY))
                .thenReturn(this.computeNovaV2UrlKey);
        Mockito.when(this.propertiesMock.getProperty(OpenStackComputePlugin.DEFAULT_NETWORK_ID_KEY))
                .thenReturn(defaultNetworkId);
        Mockito.when(propertiesHolderMock.getProperties()).thenReturn(propertiesMock);

        this.sharedOrderHolders = Mockito.mock(SharedOrderHolders.class);

        PowerMockito.mockStatic(SharedOrderHolders.class);
        BDDMockito.given(SharedOrderHolders.getInstance()).willReturn(this.sharedOrderHolders);

        Mockito.when(this.sharedOrderHolders.getOrdersList(Mockito.any(OrderState.class)))
                .thenReturn(new SynchronizedDoublyLinkedList<>());
        Mockito.when(this.sharedOrderHolders.getActiveOrdersMap()).thenReturn(new HashMap<>());

    }

    // test case: If a RequestInstance method works as expected
    @Test
    public void testRequestInstance() throws IOException, FogbowException {

        // set up
        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null,
                bestCpu, bestMemory, bestDisk, imageId, null, publicKey, networksId);
        JSONObject computeJson = generateJsonRequest(imageId, bestFlavorId, userData, idKeyName, responseNetworkIds,
                idInstanceName);
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argBodyString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.doReturn(expectedInstanceIdJson).when(this.clientMock).doPostRequest(argString.capture(),
                argBodyString.capture(), argCloudToken.capture());

        // exercise
        String instanceId = this.computePlugin.requestInstance(computeOrder, this.openStackV3User);

        // verify
        Assert.assertEquals(this.argString.getAllValues().get(0), this.osKeyPairEndpoint);
        Assert.assertEquals(this.argCloudToken.getAllValues().get(0), this.openStackV3User);
        JSONAssert.assertEquals(this.argBodyString.getAllValues().get(0).toString(), rootKeypairJson.toString(), false);

        Assert.assertEquals(this.argString.getAllValues().get(1), computeEndpoint);
        Assert.assertEquals(this.argCloudToken.getAllValues().get(1), this.openStackV3User);
        // ToDo: fix the assertion below
        // JSONAssert.assertEquals(this.argBodyString.getAllValues().get(1).toString(), computeJson.toString(), false);

        Assert.assertEquals(expectedInstanceId, instanceId);
    }

    // test case: create instance with extra requirement
    @Test
    public void testRequestInstanceWithRequirements() throws IOException, FogbowException {

        // set up
        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null,
                bestCpu, bestMemory, bestDisk, imageId, null, publicKey, networksId);
        Map<String, String> requirements = new HashMap<>();
        requirements.put(requirementTag, requirementValue);
        computeOrder.setRequirements(requirements);
        JSONObject computeJson = generateJsonRequest(imageId, bestFlavorId, userData, idKeyName, responseNetworkIds,
                idInstanceName);
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argBodyString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.doReturn(expectedInstanceIdJson).when(this.clientMock).doPostRequest(argString.capture(),
                argBodyString.capture(), argCloudToken.capture());

        // exercise
        String instanceId = this.computePlugin.requestInstance(computeOrder, this.openStackV3User);

        // verify
        Assert.assertEquals(this.argString.getAllValues().get(0), this.osKeyPairEndpoint);
        Assert.assertEquals(this.argCloudToken.getAllValues().get(0), this.openStackV3User);
        JSONAssert.assertEquals(this.argBodyString.getAllValues().get(0).toString(), rootKeypairJson.toString(), false);

        Assert.assertEquals(this.argString.getAllValues().get(1), computeEndpoint);
        Assert.assertEquals(this.argCloudToken.getAllValues().get(1), this.openStackV3User);
        // ToDo: fix the assertion below
        // JSONAssert.assertEquals(this.argBodyString.getAllValues().get(1).toString(), computeJson.toString(), false);

        Assert.assertEquals(expectedInstanceId, instanceId);
    }

    // test case: if there is no flavor available with all order requirements, raise exception
    @Test(expected = NoAvailableResourcesException.class)
    public void testRequestInstanceRequirementsNotFound() throws IOException, FogbowException {
        // set up
        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null,
                bestCpu, bestMemory, bestDisk, imageId, null, publicKey, networksId);
        Map<String, String> requirements = new HashMap<>();
        requirements.put(requirementTag, requirementValue);
        requirements.put("additionalReqTag", "additionalReqValue");
        computeOrder.setRequirements(requirements);
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argBodyString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.doReturn(expectedInstanceIdJson).when(this.clientMock).doPostRequest(argString.capture(),
                argBodyString.capture(), argCloudToken.capture());

        // exercise
        String instanceId = this.computePlugin.requestInstance(computeOrder, this.openStackV3User);
    }

    // test case: Check if a getInstance builds a compute instance from http response properly
    @Test
    public void testGetInstance() throws FogbowException, HttpResponseException {
        // set up
        String newComputeEndpoint = this.computeEndpoint + "/" + instanceId;
        String computeInstanceJson = generateComputeInstanceJson(instanceId, hostName, localIpAddress, flavorId, openstackStateActive);
        List<String> ipAddresses = new ArrayList<>();
        ipAddresses.add(localIpAddress);

        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(instanceId);

        ComputeInstance expectedComputeInstance = new ComputeInstance(instanceId, openstackStateActive, hostName, ipAddresses);

        Mockito.when(this.clientMock.doGetRequest(newComputeEndpoint, this.openStackV3User)).thenReturn(computeInstanceJson);
        mockGetFlavorsRequest(flavorId, vCPU, ram, disk);

        // exercise
        ComputeInstance pluginComputeInstance = this.computePlugin.getInstance(computeOrder, this.openStackV3User);

        // verify
        Assert.assertEquals(expectedComputeInstance.getName(), pluginComputeInstance.getName());
        Assert.assertEquals(expectedComputeInstance.getId(), pluginComputeInstance.getId());
        Assert.assertEquals(expectedComputeInstance.getIpAddresses(), pluginComputeInstance.getIpAddresses());
        Assert.assertEquals(expectedComputeInstance.getState(), pluginComputeInstance.getState());
    }

    // test case: If a DeleteInstance method works as expected
    @Test
    public void testDeleteInstance() throws HttpResponseException, FogbowException {
        // set up
        String deleteEndpoint = this.computeEndpoint + "/" + instanceId;
        Mockito.doNothing().when(this.clientMock).doDeleteRequest(this.argString.capture(),
                this.argCloudToken.capture());
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(instanceId);

        // exercise
        this.computePlugin.deleteInstance(computeOrder, this.openStackV3User);

        // verify
        Assert.assertEquals(this.argString.getValue(), deleteEndpoint);
        Assert.assertEquals(this.argCloudToken.getValue(), this.openStackV3User);
    }

    // test case: GetInstance should throw Unauthorized if a http request is Forbidden
    @Test(expected = UnauthorizedRequestException.class)
    public void testGetInstanceOnForbidden() throws FogbowException, HttpResponseException {
        // set up
        String newComputeEndpoint = this.computeEndpoint + "/" + instanceId;
        Mockito.when(this.clientMock.doGetRequest(newComputeEndpoint, this.openStackV3User))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, ""));
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(instanceId);

        // exercise/verify
        this.computePlugin.getInstance(computeOrder, this.openStackV3User);
    }

    // test case: DeleteInstance should return Unauthorized is a http request is Forbidden
    @Test(expected = UnauthorizedRequestException.class)
    public void testDeleteInstanceTestOnForbidden() throws HttpResponseException, FogbowException {
        // set up
        String deleteEndpoint = this.computeEndpoint + "/" + instanceId;
        Mockito.doThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, "")).when(this.clientMock)
                .doDeleteRequest(deleteEndpoint, this.openStackV3User);
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(instanceId);

        // exercise
        this.computePlugin.deleteInstance(computeOrder, this.openStackV3User);
    }

    // test case: Request Instance should throw Unauthenticated if a http request is Anauthorized
    @Test(expected = UnauthenticatedUserException.class)
    public void testRequestInstanceOnAnauthorizedComputePost() throws IOException, FogbowException {
        // set up
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null, bestCpu, bestMemory, bestDisk, "", null, "",
                null);

        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
        Mockito.when(this.clientMock.doPostRequest(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(Mockito.any())).thenReturn("");
        Mockito.when(this.clientMock.doPostRequest(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, ""));

        // exercise
        this.computePlugin.requestInstance(computeOrder, this.openStackV3User);
    }

    // test case: RequestInstance should still work even if there is no public key as parameter
    @Test
    public void testRequestInstanceWhenPublicKeyIsNull() throws IOException, FogbowException {
        // set up
        String publicKey = null;
        String idKeyName = null;
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null,
                bestCpu, bestMemory, bestDisk, imageId, null, publicKey, networksId);
        JSONObject computeJson = generateJsonRequest(imageId, bestFlavorId, userData, idKeyName, responseNetworkIds,
                idInstanceName);

        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argBodyString.capture(), this.argCloudToken.capture()
        )).thenReturn(expectedInstanceIdJson);

        // exercise
        String instanceId = this.computePlugin.requestInstance(computeOrder, this.openStackV3User);

        // verify
        Assert.assertEquals(computeEndpoint, this.argString.getValue());
        Assert.assertEquals(this.openStackV3User, this.argCloudToken.getValue());
        // ToDo: fix the assertion below
        // JSONAssert.assertEquals(computeJson.toString(), this.argBodyString.getValue(), false);
        Assert.assertEquals(expectedInstanceId, instanceId);
    }

    // test case: RequestInstance should throw InvalidParameter when KeyName post returns Bad Request
    @Test(expected = InvalidParameterException.class)
    public void testRequestInstanceOnBadRequestKeyNamePost() throws IOException, FogbowException {
        // set up
        ComputeOrder computeOrder = new ComputeOrder(null, null, null,
                "default", null, bestCpu, bestMemory, bestDisk, imageId, null, publicKey, null);

        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
        Mockito.when(this.clientMock.doPostRequest(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, ""));
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();

        // exercise
        this.computePlugin.requestInstance(computeOrder, this.openStackV3User);
    }

    // test case: Request Instance should throw Unauthenticated when delete key is Anauthorized
    @Test(expected = UnauthenticatedUserException.class)
    public void testRequestInstanceWhenDeleteKeyUnauthorized() throws IOException, FogbowException {
        // set up
        ComputeOrder computeOrder = new ComputeOrder(null, null, null,
                "default", null, bestCpu, bestMemory, bestDisk, imageId, null, publicKey, null);

        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.when(this.clientMock.doPostRequest(argString.capture(), argString.capture(), argCloudToken.capture()
        )).thenReturn(expectedInstanceIdJson);
        Mockito.doThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, "")).when(this.clientMock)
                .doDeleteRequest(Mockito.any(), Mockito.any());

        // exercise
        this.computePlugin.requestInstance(computeOrder, this.openStackV3User);
    }

    // test case: test if Hardware Requirements caching works as expected
    @Test
    public void testRequestInstanceHardwareRequirementsCaching() throws IOException, FogbowException {
        // set up
        ComputeOrder computeOrder = new ComputeOrder(null, null, null,
                "default", null, bestCpu, bestMemory, bestDisk, imageId, null, publicKey, networksId);
        JSONObject computeJson = generateJsonRequest(imageId, bestFlavorId, userData, idKeyName, responseNetworkIds,
                idInstanceName);

        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argBodyString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.doReturn(expectedInstanceIdJson).when(this.clientMock).doPostRequest(argString.capture(),
                argBodyString.capture(), argCloudToken.capture());

        //exercise 1
        String instanceId = this.computePlugin.requestInstance(computeOrder, this.openStackV3User);

        //verify 1
        Assert.assertEquals(this.argString.getAllValues().get(0), this.osKeyPairEndpoint);
        Assert.assertEquals(this.argCloudToken.getAllValues().get(0), this.openStackV3User);
        JSONAssert.assertEquals(this.argBodyString.getAllValues().get(0).toString(), rootKeypairJson.toString(), false);

        Assert.assertEquals(this.argString.getAllValues().get(1), computeEndpoint);
        Assert.assertEquals(this.argCloudToken.getAllValues().get(1), this.openStackV3User);
        // ToDo: fix the assertion below
        //JSONAssert.assertEquals(this.argBodyString.getAllValues().get(1), computeJson.toString(), false);
        Assert.assertEquals(expectedInstanceId, instanceId);

        // exercise 2
        instanceId = this.computePlugin.requestInstance(computeOrder, this.openStackV3User);

        // verify 2
        Assert.assertEquals(this.argString.getAllValues().get(0), this.osKeyPairEndpoint);
        Assert.assertEquals(this.argCloudToken.getAllValues().get(0), this.openStackV3User);
        JSONAssert.assertEquals(this.argBodyString.getAllValues().get(0).toString(), rootKeypairJson.toString(), false);

        Assert.assertEquals(this.argString.getAllValues().get(1), computeEndpoint);
        Assert.assertEquals(this.argCloudToken.getAllValues().get(1), this.openStackV3User);
        // ToDo: fix the assertion below
        // JSONAssert.assertEquals(this.argBodyString.getAllValues().get(1).toString(), computeJson.toString(), false);
        Assert.assertEquals(expectedInstanceId, instanceId);
    }

    // test case: Get Instance should still work even if there is no address field on response
    @Test
    public void testGetInstanceWhenThereIsNoAddressFieldOnResponse() throws FogbowException, HttpResponseException {
        // set up
        String localIpAddress = null;
        String newComputeEndpoint = this.computeEndpoint + "/" + instanceId;
        ComputeInstance expectedComputeInstance = new ComputeInstance(instanceId, openstackStateActive, hostName, new ArrayList());
        String computeInstanceJson = generateComputeInstanceJsonWithoutAddressField(instanceId, hostName, localIpAddress, flavorId, openstackStateActive);
        Mockito.when(this.clientMock.doGetRequest(newComputeEndpoint, this.openStackV3User)).thenReturn(computeInstanceJson);
        mockGetFlavorsRequest(flavorId, vCPU, ram, disk);
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(instanceId);

        // exercise
        ComputeInstance pluginComputeInstance = this.computePlugin.getInstance(computeOrder, this.openStackV3User);

        // verify
        Assert.assertEquals(expectedComputeInstance.getName(), pluginComputeInstance.getName());
        Assert.assertEquals(expectedComputeInstance.getId(), pluginComputeInstance.getId());
        Assert.assertEquals(expectedComputeInstance.getIpAddresses(), pluginComputeInstance.getIpAddresses());
        Assert.assertEquals(expectedComputeInstance.getState(), pluginComputeInstance.getState());
    }

    // test case: Get Instance should still work even if there is no provider network field on response
    @Test
    public void testGetInstanceWhenThereIsNoProviderNetworkFieldOnResponse() throws FogbowException, HttpResponseException {
        // set up
        String newComputeEndpoint = this.computeEndpoint + "/" + instanceId;
        String computeInstanceJson = generateComputeInstanceJsonWithoutProviderNetworkField(instanceId, hostName, localIpAddress, flavorId, openstackStateActive);
        ComputeInstance expectedComputeInstance = new ComputeInstance(instanceId, openstackStateActive, hostName, new ArrayList());

        Mockito.when(this.clientMock.doGetRequest(newComputeEndpoint, this.openStackV3User)).thenReturn(computeInstanceJson);
        mockGetFlavorsRequest(flavorId, vCPU, ram, disk);
        ComputeOrder computeOrder = new ComputeOrder();
        computeOrder.setInstanceId(instanceId);

        // exercise
        ComputeInstance pluginComputeInstance = this.computePlugin.getInstance(computeOrder, this.openStackV3User);

        // verify
        Assert.assertEquals(expectedComputeInstance.getName(), pluginComputeInstance.getName());
        Assert.assertEquals(expectedComputeInstance.getId(), pluginComputeInstance.getId());
        Assert.assertEquals(expectedComputeInstance.getIpAddresses(), pluginComputeInstance.getIpAddresses());
        Assert.assertEquals(expectedComputeInstance.getState(), pluginComputeInstance.getState());
    }

    // test case: Request Instance should throw NoAvailableResources when there is no cpu that meets the criteria
    @Test(expected = NoAvailableResourcesException.class)
    public void testRequestInstanceWhenThereIsNoFlavorAvailableForCPU() throws IOException, FogbowException {
        // set up
        int worst = -1;
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null, bestCpu, bestMemory, bestDisk, imageId, null,
                publicKey, null);

        mockGetFlavorsRequest(bestFlavorId, bestCpu + worst, bestMemory, bestDisk);
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.doReturn(expectedInstanceIdJson).when(this.clientMock).doPostRequest(argString.capture(),
                argString.capture(), argCloudToken.capture());

        // exercise
        this.computePlugin.requestInstance(computeOrder, this.openStackV3User);
    }

    // test case: Request Instance should throw NoAvailableResources when there is no memory that meets the criteria
    @Test(expected = NoAvailableResourcesException.class)
    public void testRequestInstanceWhenThereIsNoFlavorAvailableForMemory() throws IOException, FogbowException {
        // set up
        int worst = -1;
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null, bestCpu, bestMemory, bestDisk, imageId, null,
                publicKey, null);

        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory + worst, bestDisk);
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.doReturn(expectedInstanceIdJson).when(this.clientMock).doPostRequest(argString.capture(),
                argString.capture(), argCloudToken.capture());

        // exercise
        this.computePlugin.requestInstance(computeOrder, this.openStackV3User);
    }

    // test case: Request Instance should throw NoAvailableResources when there is no disk that meets the criteria
    @Test(expected = NoAvailableResourcesException.class)
    public void testRequestInstanceWhenThereIsNoFlavorAvailableForDisk() throws IOException, FogbowException {
        // set up
        int worst = -1;
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null, bestCpu, bestMemory, bestDisk, imageId, null,
                publicKey, null);

        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk + worst);
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.doReturn(expectedInstanceIdJson).when(this.clientMock).doPostRequest(argString.capture(),
                argString.capture(), argCloudToken.capture());

        // exercise
        this.computePlugin.requestInstance(computeOrder, this.openStackV3User);
    }

    // test case: Request Instance should still work even if there is no user data (null)
    @Test
    public void testRequestInstanceWhenThereIsNoUserData() throws IOException, FogbowException {
        // set up
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null, bestCpu, bestMemory, bestDisk, imageId, null,
                publicKey, networksId);
        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argBodyString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        JSONObject computeJson = generateJsonRequest(imageId, bestFlavorId, userData, idKeyName, responseNetworkIds,
                idInstanceName);
        Mockito.doReturn(expectedInstanceIdJson).when(this.clientMock).doPostRequest(argString.capture(),
                argBodyString.capture(), argCloudToken.capture());

        // exercise
        String instanceId = this.computePlugin.requestInstance(computeOrder, this.openStackV3User);

        // verify
        Assert.assertEquals(this.osKeyPairEndpoint, this.argString.getAllValues().get(0));
        Assert.assertEquals(this.openStackV3User, this.argCloudToken.getAllValues().get(0));
        JSONAssert.assertEquals(rootKeypairJson.toString(), this.argBodyString.getAllValues().get(0).toString(), false);

        Assert.assertEquals(computeEndpoint, this.argString.getAllValues().get(1));
        Assert.assertEquals(this.openStackV3User, this.argCloudToken.getAllValues().get(1));
        // ToDo: fix the assertion below
        // JSONAssert.assertEquals(computeJson.toString(), this.argBodyString.getAllValues().get(1).toString(), false);

        Assert.assertEquals(expectedInstanceId, instanceId);
    }

    // test case: Compute networksId should always contain a default network id even if there is no network id in a compute order
    @Test
    public void testRequestInstanceWhenThereIsNoNetworkId() throws IOException, FogbowException {
        // set up
        List<String> networksId = null;
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null, bestCpu, bestMemory, bestDisk, imageId, null,
                publicKey, networksId);
        responseNetworkIds.remove(this.privateNetworkId);

        JSONObject computeJson = generateJsonRequest(imageId, bestFlavorId, userData, idKeyName, responseNetworkIds,
                idInstanceName);

        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argBodyString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.doReturn(expectedInstanceIdJson).when(this.clientMock).doPostRequest(argString.capture(),
                argBodyString.capture(), argCloudToken.capture());

        // exercise
        String instanceId = this.computePlugin.requestInstance(computeOrder, this.openStackV3User);

        // verify
        Assert.assertEquals(this.osKeyPairEndpoint, this.argString.getAllValues().get(0));
        Assert.assertEquals(this.openStackV3User, this.argCloudToken.getAllValues().get(0));
        JSONAssert.assertEquals(rootKeypairJson.toString(), this.argBodyString.getAllValues().get(0).toString(), false);

        Assert.assertEquals(computeEndpoint, this.argString.getAllValues().get(1));
        Assert.assertEquals(this.openStackV3User, this.argCloudToken.getAllValues().get(1));
        JSONAssert.assertEquals(computeJson.toString(), this.argBodyString.getAllValues().get(1).toString(), false);

        Assert.assertEquals(expectedInstanceId, instanceId);
    }

    // test case: Request Instance should throw InvalidParameter if the Get Request on updateFlavors returns Bad Request
    // while getting flavors id
    @Test(expected = InvalidParameterException.class)
    public void testRequestInstanceWhenFlavorsIdRequestException() throws HttpResponseException, FogbowException {
        // set up
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null, bestCpu, bestMemory, bestDisk, imageId, null,
                publicKey, networksId);
        Mockito.when(this.clientMock.doGetRequest(Mockito.any(), Mockito.any()))
                .thenThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, ""));
        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.doReturn(expectedInstanceIdJson).when(this.clientMock).doPostRequest(argString.capture(),
                argString.capture(), argCloudToken.capture());

        // exercise
        this.computePlugin.requestInstance(computeOrder, this.openStackV3User);
    }

    // test case: Request Instance should throw InvalidParameter if the Get Request on updateFlavors returns Bad Request
    // while getting flavor specification
    @Test(expected = InvalidParameterException.class)
    public void testRequestInstanceWhenSpecificFlavorRequestException() throws IOException, FogbowException {
        // set up
        String newEndpoint = flavorEndpoint + "/" + bestFlavorId;
        ComputeOrder computeOrder = new ComputeOrder(null, null, null, "default", null, bestCpu, bestMemory, bestDisk, imageId, null,
                publicKey, networksId);
        mockGetFlavorsRequest(bestFlavorId, bestCpu, bestMemory, bestDisk);
        Mockito.doThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, "")).when(this.clientMock)
                .doGetRequest(newEndpoint, this.openStackV3User);

        Mockito.when(this.clientMock.doPostRequest(this.argString.capture(), this.argString.capture(), this.argCloudToken.capture()
        )).thenReturn("");
        Mockito.when(this.launchCommandGeneratorMock.createLaunchCommand(computeOrder)).thenReturn(userData);
        Mockito.doReturn(idKeyName).doReturn(idInstanceName).when(this.computePlugin).getRandomUUID();
        Mockito.doReturn(expectedInstanceIdJson).when(this.clientMock).doPostRequest(argString.capture(),
                argString.capture(), argCloudToken.capture());

        // exercise
        this.computePlugin.requestInstance(computeOrder, this.openStackV3User);
    }

    /*
     * This method mocks the behavior of a http flavor request by mocking GET"/flavors" and GET"/flavors/id" and adds
     * bestFlavorId as a flavor from this response in addition to other flavors. Besides that, bestFlavorId will be
     * the flavor with best Vcpu, memory and disk from this response. 
     */
    private void mockGetFlavorsRequest(String bestFlavorId, int bestVcpu, int bestMemory, int bestDisk) throws HttpResponseException, FogbowException {

        int qtdFlavors = 100;

        Mockito.when(this.clientMock.doGetRequest(flavorEndpoint, this.openStackV3User))
                .thenReturn(generateJsonFlavors(qtdFlavors, bestFlavorId));

        for (int i = 0; i < qtdFlavors - 1; i++) {
            String flavorId = "flavor" + Integer.toString(i);
            String newEndpoint = flavorEndpoint + "/" + flavorId;
            String newSpecsEndpoint = String.format(this.flavorExtraSpecsEndpoint, flavorId);
            String flavorJson = generateJsonFlavor(
                    flavorId,
                    "nameflavor" + Integer.toString(i),
                    Integer.toString(Math.max(1, bestVcpu - 1 - i)),
                    Integer.toString(Math.max(1, bestMemory - 1 - i)),
                    Integer.toString(Math.max(1, bestDisk - 1 - i)));
            String flavorSpecJson = generateJsonFlavorExtraSpecs("tag" + i, "value" + i);
            Mockito.when(this.clientMock.doGetRequest(newEndpoint, this.openStackV3User))
                    .thenReturn(flavorJson);
            // Mocks /flavors/{flavor_id}/os-extra_specs
            Mockito.when(this.clientMock.doGetRequest(newSpecsEndpoint, this.openStackV3User))
                    .thenReturn(flavorSpecJson);
        }

        String newEndpoint = flavorEndpoint + "/" + bestFlavorId;
        String flavorJson = generateJsonFlavor(
                bestFlavorId,
                "nameflavor" + bestFlavorId,
                Integer.toString(bestVcpu),
                Integer.toString(bestMemory),
                Integer.toString(bestDisk));
        String bestflavorSpecJson = generateJsonFlavorExtraSpecs(this.requirementTag, this.requirementValue);
        String newSpecsEndpoint = String.format(this.flavorExtraSpecsEndpoint, bestFlavorId);

        Mockito.when(this.clientMock.doGetRequest(newEndpoint, this.openStackV3User))
                .thenReturn(flavorJson);
        Mockito.when(this.clientMock.doGetRequest(newSpecsEndpoint, this.openStackV3User))
                .thenReturn(bestflavorSpecJson);
    }

    //mocks GET"/flavors"
    private String generateJsonFlavors(int qtd, String bestFlavorId) {
        Map<String, Object> jsonFlavorsMap = new HashMap<String, Object>();
        List<Map<String, String>> jsonArrayFlavors = new ArrayList<Map<String, String>>();
        for (int i = 0; i < qtd - 1; i++) {
            Map<String, String> flavor = new HashMap<String, String>();
            flavor.put(OpenStackConstants.Compute.ID_KEY_JSON, "flavor" + Integer.toString(i));
            jsonArrayFlavors.add(flavor);
        }

        Map<String, String> flavor = new HashMap<String, String>();
        flavor.put(OpenStackConstants.Compute.ID_KEY_JSON, bestFlavorId);
        jsonArrayFlavors.add(flavor);

        jsonFlavorsMap.put(OpenStackConstants.Compute.FLAVORS_KEY_JSON, jsonArrayFlavors);
        Gson gson = new Gson();
        return gson.toJson(jsonFlavorsMap);
    }

    private String generateJsonFlavor(String id, String name, String vcpu, String memory, String disk) {
        Map<String, Object> flavorMap = new HashMap<String, Object>();
        Map<String, String> flavorAttributes = new HashMap<String, String>();
        flavorAttributes.put(OpenStackConstants.Compute.ID_KEY_JSON, id);
        flavorAttributes.put(OpenStackConstants.Compute.NAME_KEY_JSON, name);
        flavorAttributes.put(OpenStackConstants.Compute.DISK_KEY_JSON, disk);
        flavorAttributes.put(OpenStackConstants.Compute.MEMORY_KEY_JSON, memory);
        flavorAttributes.put(OpenStackConstants.Compute.VCPUS_KEY_JSON, vcpu);
        flavorMap.put(OpenStackConstants.Compute.FLAVOR_KEY_JSON, flavorAttributes);
        return new Gson().toJson(flavorMap);
    }

    private String generateJsonFlavorExtraSpecs(String tag, String value) {
        Map<String, Object> flavorExtraSpecsMap = new HashMap<String, Object>();
        Map<String, String> flavorExtraSpecs = new HashMap<String, String>();
        flavorExtraSpecs.put(tag, value);
        flavorExtraSpecsMap.put(OpenStackConstants.Compute.FLAVOR_EXTRA_SPECS_KEY_JSON, flavorExtraSpecs);
        return new Gson().toJson(flavorExtraSpecsMap);
    }

    private String generateInstaceId(String id) {
        Map<String, String> instanceMap = new HashMap<String, String>();
        instanceMap.put(OpenStackConstants.Compute.ID_KEY_JSON, id);
        Map<String, Object> root = new HashMap<String, Object>();
        root.put(OpenStackConstants.Compute.SERVER_KEY_JSON, instanceMap);
        return new Gson().toJson(root);
    }

    private JSONObject generateRootKeyPairJson(String keyName, String publicKey) {
        JSONObject keypair = new JSONObject();
        keypair.put(OpenStackConstants.Compute.NAME_KEY_JSON, keyName);
        keypair.put(OpenStackConstants.Compute.PUBLIC_KEY_KEY_JSON, publicKey);
        JSONObject root = new JSONObject();
        root.put(OpenStackConstants.Compute.KEY_PAIR_KEY_JSON, keypair);
        return root;
    }

    private JSONObject generateJsonRequest(String imageId, String flavorId, String userData, String keyName, List<String> networkIds, String randomUUID) {
        JSONObject server = new JSONObject();

        // when the user doesn't provide a network, the default network should be added in the request
        if (networkIds.isEmpty()) {
            networkIds.add(defaultNetworkId);
        }

        server.put(OpenStackConstants.Compute.NAME_KEY_JSON, SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + randomUUID);
        server.put(OpenStackConstants.Compute.IMAGE_REFERENCE_KEY_JSON, imageId);
        server.put(OpenStackConstants.Compute.FLAVOR_REFERENCE_KEY_JSON, flavorId);
        server.put(OpenStackConstants.Compute.USER_DATA_KEY_JSON, userData);

        JSONArray networks = new JSONArray();

        for (String id : networkIds) {
            JSONObject netId = new JSONObject();
            netId.put(OpenStackConstants.Compute.UUID_KEY_JSON, id);
            networks.put(netId);
        }
        server.put(OpenStackConstants.Compute.NETWORKS_KEY_JSON, networks);

        if (networkIds.size() > 0) {
            for (String networkId : networkIds) {
                if (!networkId.equals(defaultNetworkId)) {
                    JSONArray securityGroups = new JSONArray();
                    JSONObject securityGroup = new JSONObject();
                    String securityGroupName = OpenStackCloudUtils.getSGNameForPrivateNetwork(networkId);
                    securityGroup.put(OpenStackConstants.Compute.NAME_KEY_JSON, securityGroupName);
                    securityGroups.put(securityGroup);
                    server.put(OpenStackConstants.Compute.SECURITY_GROUPS_KEY_JSON, securityGroups);
                }
            }
        }

        if (keyName != null) {
            server.put(OpenStackConstants.Compute.KEY_NAME_KEY_JSON, keyName);
        }

        JSONObject root = new JSONObject();
        root.put(OpenStackConstants.Compute.SERVER_KEY_JSON, server);
        return root;
    }


    private String generateComputeInstanceJson(String instanceId, String hostName, String localIpAddress, String flavorId, String status) {
        Map<String, Object> root = new HashMap<String, Object>();
        Map<String, Object> computeInstance = new HashMap<String, Object>();
        computeInstance.put(OpenStackConstants.Compute.ID_KEY_JSON, instanceId);
        computeInstance.put(OpenStackConstants.Compute.NAME_KEY_JSON, hostName);

        Map<String, Object> addressField = new HashMap<String, Object>();
        List<Object> providerNetworkArray = new ArrayList<Object>();
        Map<String, String> providerNetwork = new HashMap<String, String>();
        providerNetwork.put(OpenStackConstants.Compute.ADDRESS_KEY_JSON, localIpAddress);
        providerNetworkArray.add(providerNetwork);
        addressField.put(OpenStackConstants.Compute.PROVIDER_KEY_JSON, providerNetworkArray);

        Map<String, Object> flavor = new HashMap<String, Object>();
        flavor.put(OpenStackConstants.Compute.ID_KEY_JSON, flavorId);

        computeInstance.put(OpenStackConstants.Compute.FLAVOR_KEY_JSON, flavor);
        computeInstance.put(OpenStackConstants.Compute.ADDRESSES_KEY_JSON, addressField);
        computeInstance.put(OpenStackConstants.Compute.STATUS_KEY_JSON, status);

        root.put(OpenStackConstants.Compute.SERVER_KEY_JSON, computeInstance);
        return new Gson().toJson(root);
    }

    private String generateComputeInstanceJsonWithoutAddressField(String instanceId, String hostName, String localIpAddress, String flavorId, String status) {
        Map<String, Object> root = new HashMap<String, Object>();
        Map<String, Object> computeInstance = new HashMap<String, Object>();
        computeInstance.put(OpenStackConstants.Compute.ID_KEY_JSON, instanceId);
        computeInstance.put(OpenStackConstants.Compute.NAME_KEY_JSON, hostName);

        List<Object> providerNetworkArray = new ArrayList<Object>();
        Map<String, String> providerNetwork = new HashMap<String, String>();
        providerNetwork.put(OpenStackConstants.Compute.ADDRESS_KEY_JSON, localIpAddress);
        providerNetworkArray.add(providerNetwork);

        Map<String, Object> flavor = new HashMap<String, Object>();
        flavor.put(OpenStackConstants.Compute.ID_KEY_JSON, flavorId);

        computeInstance.put(OpenStackConstants.Compute.FLAVOR_KEY_JSON, flavor);
        computeInstance.put(OpenStackConstants.Compute.STATUS_KEY_JSON, status);

        root.put(OpenStackConstants.Compute.SERVER_KEY_JSON, computeInstance);
        return new Gson().toJson(root);
    }

    private String generateComputeInstanceJsonWithoutProviderNetworkField(String instanceId, String hostName, String localIpAddress, String flavorId, String status) {
        Map<String, Object> root = new HashMap<String, Object>();
        Map<String, Object> computeInstance = new HashMap<String, Object>();
        computeInstance.put(OpenStackConstants.Compute.ID_KEY_JSON, instanceId);
        computeInstance.put(OpenStackConstants.Compute.NAME_KEY_JSON, hostName);

        Map<String, Object> addressField = new HashMap<String, Object>();

        Map<String, Object> flavor = new HashMap<String, Object>();
        flavor.put(OpenStackConstants.Compute.ID_KEY_JSON, flavorId);

        computeInstance.put(OpenStackConstants.Compute.FLAVOR_KEY_JSON, flavor);
        computeInstance.put(OpenStackConstants.Compute.ADDRESSES_KEY_JSON, addressField);
        computeInstance.put(OpenStackConstants.Compute.STATUS_KEY_JSON, status);

        root.put(OpenStackConstants.Compute.SERVER_KEY_JSON, computeInstance);
        return new Gson().toJson(root);
    }
}
