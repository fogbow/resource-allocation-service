package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import ch.qos.logback.classic.Level;
import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.exceptions.NoAvailableResourcesException;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.api.http.response.quotas.allocation.VolumeAllocation;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.RequestMatcher;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.*;

import static cloud.fogbow.ras.core.TestUtils.FAKE_INSTANCE_ID;

@PrepareForTest({CloudStackUrlUtil.class, DeleteVolumeResponse.class, DeleteVolumeResponse.class,
        GetVolumeResponse.class, DatabaseManager.class, CloudStackCloudUtils.class,
        CreateVolumeResponse.class, GetVolumeResponse.class})
public class CloudStackVolumePluginTest extends BaseUnitTests {

    @Rule
    private ExpectedException expectedException = ExpectedException.none();
    private LoggerAssert loggerTestChecking = new LoggerAssert(CloudStackVolumePlugin.class);

    private CloudStackVolumePlugin plugin;
    private CloudStackHttpClient client;
    private CloudStackUser cloudStackUser;
    private String cloudStackUrl;
    private String zoneId;

    @Before
    public void setUp() throws UnexpectedException, InvalidParameterException {
        String cloudStackConfFilePath = CloudstackTestUtils.CLOUDSTACK_CONF_FILE_PATH;
        Properties properties = PropertiesUtil.readProperties(cloudStackConfFilePath);
        this.cloudStackUrl = properties.getProperty(CloudStackCloudUtils.CLOUDSTACK_URL_CONFIG);
        this.cloudStackUser = CloudstackTestUtils.CLOUD_STACK_USER;
        this.zoneId = properties.getProperty(CloudStackCloudUtils.ZONE_ID_CONFIG);

        this.client = Mockito.mock(CloudStackHttpClient.class);
        this.plugin = Mockito.spy(new CloudStackVolumePlugin(cloudStackConfFilePath));
        this.plugin.setClient(this.client);

        this.testUtils.mockReadOrdersFromDataBase();
        CloudstackTestUtils.ignoringCloudStackUrl();
    }

    // test case: When calling the requestInstance method with secondary methods mocked,
    // it must verify if the buildCreateVolumeRequest, doRequestInstance and updateVolumeOrder are called;
    // this includes the checking in the Cloudstack request.
    @Test
    public void testRequestInstance() throws FogbowException {
        // setup
        CloudStackUser user = CloudstackTestUtils.CLOUD_STACK_USER;
        VolumeOrder order = Mockito.mock(VolumeOrder.class);
        CreateVolumeRequest request = Mockito.mock(CreateVolumeRequest.class);

        Mockito.doReturn(request).when(this.plugin).buildCreateVolumeRequest(Mockito.eq(order), Mockito.eq(user));
        Mockito.doReturn(FAKE_INSTANCE_ID).when(this.plugin).doRequestInstance(Mockito.eq(request), Mockito.eq(user));
        Mockito.doNothing().when(this.plugin).updateVolumeOrder(order);

        // exercise
        this.plugin.requestInstance(order, user);

        // verify
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).buildCreateVolumeRequest(Mockito.eq(order), Mockito.eq(user));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.eq(request), Mockito.eq(user));
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).updateVolumeOrder(Mockito.eq(order));
    }

    // test case: When calling the updateVolumeOrder method with order and new values,
    // it must verify if It updates the volume order.
    @Test
    public void testUpdateVolumeOrder() {
        // setup
        VolumeOrder order = Mockito.mock(VolumeOrder.class);
        int expectedDiskSize = 1024;

        Mockito.doCallRealMethod().when(order).setActualAllocation(Mockito.any(VolumeAllocation.class));
        Mockito.doCallRealMethod().when(order).getActualAllocation();
        Mockito.doReturn(expectedDiskSize).when(order).getVolumeSize();

        // exercise
        this.plugin.updateVolumeOrder(order);

        // verify
        Mockito.verify(order, Mockito.times(TestUtils.RUN_ONCE)).setActualAllocation(Mockito.any(VolumeAllocation.class));
        Assert.assertEquals(expectedDiskSize, order.getActualAllocation().getDisk());
    }

    // test case: When calling the getInstance method with secondary methods mocked,
    // it must verify if the doGetInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testGetInstanceSuccessfully() throws FogbowException {
        // set up
        String instanceId = "instanceId";
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getInstanceId()).thenReturn(instanceId);

        VolumeInstance volumeInstanceExpected = Mockito.mock(VolumeInstance.class);
        Mockito.doReturn(volumeInstanceExpected).when(this.plugin).
                doGetInstance(Mockito.any(), Mockito.eq(this.cloudStackUser));

        GetVolumeRequest request = new GetVolumeRequest.Builder()
                .id(volumeOrder.getInstanceId())
                .build(this.cloudStackUrl);

        // exercise
        VolumeInstance volumeInstance = this.plugin.getInstance(volumeOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(volumeInstanceExpected, volumeInstance);
        RequestMatcher<GetVolumeRequest> matcher = new RequestMatcher.GetVolume(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doGetInstance(Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the doGetInstance method with methods mocked and occurs a HttpResponseException,
    // it must verify if It throws a FogbowException.
    @Test
    public void testDoGetInstanceFailWhenThrowsException() throws FogbowException, HttpResponseException {
        // set up
        GetVolumeRequest request = new GetVolumeRequest.Builder().build(TestUtils.EMPTY_STRING);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.doGetInstance(request, this.cloudStackUser);
    }

    // test case: When calling the doGetInstance method with methods mocked and there is no volumes,
    // it must verify if It throws an UnexpectedException.
    @Test
    public void testDoGetInstanceFailWhenThereIsNoVolume() throws FogbowException, HttpResponseException {
        // set up
        GetVolumeRequest request = new GetVolumeRequest.Builder().build(TestUtils.EMPTY_STRING);

        String responseStr = TestUtils.ANY_VALUE;
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))).
                thenReturn(responseStr);

        GetVolumeResponse response = Mockito.mock(GetVolumeResponse.class);
        List<GetVolumeResponse.Volume> volumes = new ArrayList<>();
        Mockito.when(response.getVolumes()).thenReturn(volumes);

        PowerMockito.mockStatic(GetVolumeResponse.class);
        PowerMockito.when(GetVolumeResponse.fromJson(Mockito.eq(responseStr))).
                thenReturn(response);

        // verify
        this.expectedException.expect(UnexpectedException.class);

        // exercise
        this.plugin.doGetInstance(request, this.cloudStackUser);
    }

    // test case: When calling the buildVolumeInstance method, it must verify if
    // It returns right VolumeInstance.
    @Test
    public void testBuildVolumeInstanceSuccessfully() {
        // set up
        int size = 1 ;
        long sizeInBytes = (long) (size * CloudStackCloudUtils.ONE_GB_IN_BYTES);
        String name = "name";
        String state = "state";
        String id = "id";
        GetVolumeResponse.Volume volume = Mockito.mock(GetVolumeResponse.Volume.class);
        Mockito.when(volume.getSize()).thenReturn(sizeInBytes);
        Mockito.when(volume.getName()).thenReturn(name);
        Mockito.when(volume.getState()).thenReturn(state);
        Mockito.when(volume.getId()).thenReturn(id);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.convertToGigabyte(Mockito.eq(sizeInBytes))).
                thenReturn(size);

        // exercise
        VolumeInstance volumeInstance = this.plugin.buildVolumeInstance(volume);

        // verify
        Assert.assertEquals(size, volumeInstance.getSize());
        Assert.assertEquals(state, volumeInstance.getCloudState());
        Assert.assertEquals(name, volumeInstance.getName());
        Assert.assertEquals(id, volumeInstance.getId());
    }

    // test case: When calling the doGetInstance method with methods mocked,
    // it must verify if It returns a VolumeInstance.
    @Test
    public void testDoGetInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        GetVolumeRequest request = new GetVolumeRequest.Builder().build(TestUtils.EMPTY_STRING);

        String responseStr = TestUtils.ANY_VALUE;
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))).
                thenReturn(responseStr);

        String instanceIdExpected = "instanceIdExpected";
        GetVolumeResponse response = Mockito.mock(GetVolumeResponse.class);
        GetVolumeResponse.Volume volume = Mockito.mock(GetVolumeResponse.Volume.class);
        Mockito.when(volume.getId()).thenReturn(instanceIdExpected);
        List<GetVolumeResponse.Volume> volumes = new ArrayList<>();
        volumes.add(volume);
        Mockito.when(response.getVolumes()).thenReturn(volumes);

        PowerMockito.mockStatic(GetVolumeResponse.class);
        PowerMockito.when(GetVolumeResponse.fromJson(Mockito.eq(responseStr))).
                thenReturn(response);

        VolumeInstance volumeInstanceExpected = Mockito.mock(VolumeInstance.class);
        Mockito.doReturn(volumeInstanceExpected).when(this.plugin).
                buildVolumeInstance(Mockito.eq(volume));

        // exercise
        VolumeInstance volumeInstance = this.plugin.doGetInstance(request, this.cloudStackUser);

        // verify
        Assert.assertEquals(volumeInstanceExpected, volumeInstance);
    }

    // test case: When calling the normalizeInstanceName method with parameter null,
    // it must verify if It returns a name generated.
    @Test
    public void testNormalizeInstanceNameWhenParameterIsNull() {
        // set up
        String nameParameter = null;

        String nameGenaretedExpected = SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX;
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.generateInstanceName()).thenReturn(nameGenaretedExpected);

        // exercise
        String name = this.plugin.normalizeInstanceName(nameParameter);

        // verify
        Assert.assertEquals(nameGenaretedExpected, name);
    }

    // test case: When calling the normalizeInstanceName method, it must verify if
    // It returns the same name of the parameter.
    @Test
    public void testNormalizeInstanceNameWhenParameterIsNotNull() {
        // set up
        String nameExpected = "nameExpected";

        // exercise
        String name = this.plugin.normalizeInstanceName(nameExpected);

        // verify
        Assert.assertEquals(nameExpected, name);
    }

    // test case: When calling the buildVolumeCustomized method, it must verify if
    // It returns a right CreateVolumeRequest.
    @Test
    public void testBuildVolumeCustomizedSuccessfully() throws InvalidParameterException {
        // set up
    String diskOfferingId = "diskOfferingId";
        String nameExpected = "nameExpected";
        int volumeSizeExpected = 1;
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getName()).thenReturn(nameExpected);
        Mockito.when(volumeOrder.getVolumeSize()).thenReturn(volumeSizeExpected);

        CreateVolumeRequest requestExpected = new CreateVolumeRequest.Builder()
                .zoneId(this.zoneId)
                .name(nameExpected)
                .diskOfferingId(diskOfferingId)
                .size(String.valueOf(volumeSizeExpected))
                .build(this.cloudStackUrl);

        // exercise
        CreateVolumeRequest request = this.plugin.buildVolumeCustomized(volumeOrder, diskOfferingId);

        // verify
        Assert.assertEquals(requestExpected.getUriBuilder().toString(), request.getUriBuilder().toString());
    }

    // test case: When calling the getDiskOfferingIdCustomized method and the is no disk offering
    // customized with the size required, it must verify if It returns a null.
    @Test
    public void testGetDiskOfferingIdCustomizedWhenNotFoundDiskOffering() {
        // set up
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering = new ArrayList<>();
        boolean isNotCustomized = false;
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingOne = buildDiskOfferingMocked(isNotCustomized);
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingTwo = buildDiskOfferingMocked(isNotCustomized);

        disksOffering.add(diskOfferingOne);
        disksOffering.add(diskOfferingTwo);

        // exercise
        String diskOfferingIdCustomized = this.plugin.getDiskOfferingIdCustomized(disksOffering);

        // verify
        Assert.assertNull(diskOfferingIdCustomized);
    }

    // test case: When calling the getDiskOfferingIdCustomized method, it must verify if It
    // returns the right disk offering id.
    @Test
    public void testGetDiskOfferingIdCustomizedWhenFoundDiskOffering() {
        // set up
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering = new ArrayList<>();
        boolean isNotCustomized = false;
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingOne = buildDiskOfferingMocked(isNotCustomized);
        boolean isCustomized = true;
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingTwo = buildDiskOfferingMocked(isCustomized);

        disksOffering.add(diskOfferingOne);
        disksOffering.add(diskOfferingTwo);

        // exercise
        String diskOfferingIdCustomized = this.plugin.getDiskOfferingIdCustomized(disksOffering);

        // verify
        Assert.assertEquals(diskOfferingTwo.getId(), diskOfferingIdCustomized);
    }

    // test case: When calling the buildVolumeCompatible method, it must verify if
    // It returns a right CreateVolumeRequest.
    @Test
    public void testBuildVolumeCompatibleSuccessfully() throws InvalidParameterException {
        // set up
        String diskOfferingId = "diskOfferingId";

        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        String nameExpexted = "fogbowname";
        Mockito.when(volumeOrder.getName()).thenReturn(nameExpexted);

        CreateVolumeRequest resquestRequired = new CreateVolumeRequest.Builder()
                .zoneId(this.zoneId)
                .name(nameExpexted)
                .diskOfferingId(diskOfferingId)
                .build(this.cloudStackUrl);

        // exercise
        CreateVolumeRequest request = this.plugin.buildVolumeCompatible(volumeOrder, diskOfferingId);

        // verify
        Assert.assertEquals(resquestRequired.getUriBuilder().toString(), request.getUriBuilder().toString());
    }

    // test case: When calling the getDiskOfferingIdCompatible method and the is no disk offering
    // compatible with the size required, it must verify if It returns a null.
    @Test
    public void testGetDiskOfferingIdCompatibleWhenThereIsNoDisksOffering() {
        // set up
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering = new ArrayList<>();

        int diskSize = 3;
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getVolumeSize()).thenReturn(diskSize);

        // exercise
        String diskOfferingIdCompatible = this.plugin.getDiskOfferingIdCompatible(volumeOrder, disksOffering);

        // verify
        Assert.assertNull(diskOfferingIdCompatible);
    }

    // test case: When calling the getDiskOfferingIdCompatible method and the is no disk offering
    // compatible with the size required, it must verify if It returns a null.
    @Test
    public void testGetDiskOfferingIdCompatibleWhenNotFoundDiskOffering() {
        // set up
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering = new ArrayList<>();
        int diskSizeOne = 1;
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingOne = buildDiskOfferingMocked(diskSizeOne);
        int diskSizeTwo = 2;
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingTwo = buildDiskOfferingMocked(diskSizeTwo);

        disksOffering.add(diskOfferingOne);
        disksOffering.add(diskOfferingTwo);

        int diskSizeDifferent = 3;
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getVolumeSize()).thenReturn(diskSizeDifferent);

        // exercise
        String diskOfferingIdCompatible = this.plugin.getDiskOfferingIdCompatible(volumeOrder, disksOffering);

        // verify
        Assert.assertNull(diskOfferingIdCompatible);
    }

    // test case: When calling the getDiskOfferingIdCompatible method, it must verify if It
    // returns the right disk offering id.
    @Test
    public void testGetDiskOfferingIdCompatibleWhenFoundDiskOffering() {
        // set up
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering = new ArrayList<>();
        int diskSizeOne = 1;
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingOne = buildDiskOfferingMocked(diskSizeOne);
        int diskSizeTwo = 2;
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingTwo = buildDiskOfferingMocked(diskSizeTwo);

        disksOffering.add(diskOfferingOne);
        disksOffering.add(diskOfferingTwo);

        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getVolumeSize()).thenReturn(diskSizeTwo);

        // exercise
        String diskOfferingIdCompatible = this.plugin.getDiskOfferingIdCompatible(volumeOrder, disksOffering);

        // verify
        Assert.assertEquals(diskOfferingTwo.getId(), diskOfferingIdCompatible);
    }

    // test case: When calling the filterDisksOfferingByRequirements method with requirements
    // empty, it must verify if It returns the same  list.
    @Test
    public void testFilterDisksOfferingByRequirementsWhenRequiremetsEmpty() {
        // set up
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Map<String, String> requirements = new HashMap<>();
        Mockito.when(volumeOrder.getRequirements()).thenReturn(requirements);

        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering = new ArrayList<>();

        // exercise
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOfferingsFilted =
                this.plugin.filterDisksOfferingByRequirements(disksOffering, volumeOrder);

        // verify
        Assert.assertEquals(disksOffering, disksOfferingsFilted);
    }

    // test case: When calling the filterDisksOfferingByRequirements method with requirements
    // requested, it must verify if It returns a empty list because there is no disk offering
    // compatible.
    @Test
    public void testFilterDisksOfferingByRequirementsWhenNothingMatch() {
        // set up
        String key = "key";
        String value = "value";
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Map<String, String> requirements = new HashMap<>();
        requirements.put(key, value);
        Mockito.when(volumeOrder.getRequirements()).thenReturn(requirements);

        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering = new ArrayList<>();
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingIncompatibleOne =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingIncompatibleOne.getTags()).thenReturn("anythingone");
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingIncompatibleTwo =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingIncompatibleTwo.getTags()).thenReturn(TestUtils.EMPTY_STRING);

        disksOffering.add(diskOfferingIncompatibleOne);
        disksOffering.add(diskOfferingIncompatibleTwo);

        // exercise
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOfferingsFilted =
                this.plugin.filterDisksOfferingByRequirements(disksOffering, volumeOrder);

        // verify
        Assert.assertTrue(disksOfferingsFilted.isEmpty());
    }

    // test case: When calling the filterDisksOfferingByRequirements method with requirements
    // requested, it must verify if It returns only the disks offering compatible with the requirements.
    @Test
    public void testFilterDisksOfferingByRequirementsWhenFilterRightly() {
        // set up
        String key = "key";
        String value = "value";
        String tagExpected = key + CloudStackCloudUtils.FOGBOW_TAG_SEPARATOR + value;
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Map<String, String> requirements = new HashMap<>();
        requirements.put(key, value);
        Mockito.when(volumeOrder.getRequirements()).thenReturn(requirements);

        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering = new ArrayList<>();
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingCompatible =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingCompatible.getTags()).thenReturn(tagExpected);
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingIncompatibleOne =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingIncompatibleOne.getTags()).thenReturn("anythingone");
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingIncompatibleTwo =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingIncompatibleTwo.getTags()).thenReturn(TestUtils.EMPTY_STRING);


        disksOffering.add(diskOfferingCompatible);
        disksOffering.add(diskOfferingIncompatibleOne);
        disksOffering.add(diskOfferingIncompatibleTwo);

        // exercise
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOfferingsFilted =
                this.plugin.filterDisksOfferingByRequirements(disksOffering, volumeOrder);

        // verify
        int sizeExpected = 1;
        GetAllDiskOfferingsResponse.DiskOffering firstDiskOffering =
                disksOfferingsFilted.listIterator().next();
        Assert.assertEquals(sizeExpected, disksOfferingsFilted.size());
        Assert.assertEquals(diskOfferingCompatible, firstDiskOffering);
    }

    // test case: When calling the buildCreateVolumeRequest method with secondary methods mocked and
    // the disk offering found is compatible with the size, it must verify if It builds
    // the CreateVolumeRequest in the buildVolumeCompatible.
    @Test
    public void testBuildCreateVolumeRequestWhenFindCompatible() throws FogbowException {
        // set up
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);

        List disksOffering = new ArrayList();
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.getDisksOffering(
                this.client, this.cloudStackUser, this.cloudStackUrl)).thenReturn(disksOffering);

        List disksOfferingFilted = new ArrayList<>();
        Mockito.doReturn(disksOfferingFilted).when(this.plugin).filterDisksOfferingByRequirements(
                Mockito.eq(disksOffering), Mockito.eq(volumeOrder));

        String diskOfferingIdCompatible = "id";
        Mockito.doReturn(diskOfferingIdCompatible).when(this.plugin).getDiskOfferingIdCompatible(
                Mockito.eq(volumeOrder), Mockito.eq(disksOfferingFilted));

        CreateVolumeRequest requestExpected = Mockito.mock(CreateVolumeRequest.class);
        Mockito.doReturn(requestExpected).when(this.plugin).buildVolumeCompatible(
                Mockito.eq(volumeOrder), Mockito.eq(diskOfferingIdCompatible));

        // exercise
        CreateVolumeRequest request = this.plugin.buildCreateVolumeRequest(volumeOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(requestExpected, request);
    }

    // test case: When calling the buildCreateVolumeRequest method with secondary methods mocked and
    // the disk offering found the customized, it must verify if It builds the CreateVolumeRequest
    // in the buildVolumeCustomized.
    @Test
    public void testBuildCreateVolumeRequestWhenFindCustomized() throws FogbowException {
        // set up
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);

        List disksOffering = new ArrayList<>();
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.getDisksOffering(
                this.client, this.cloudStackUser, this.cloudStackUrl)).thenReturn(disksOffering);

        List disksOfferingFilted = new ArrayList<>();
        Mockito.doReturn(disksOfferingFilted).when(this.plugin).filterDisksOfferingByRequirements(
                Mockito.eq(disksOffering), Mockito.eq(volumeOrder));

        String diskOfferingIdCompatibleNotFound = null;
        Mockito.doReturn(diskOfferingIdCompatibleNotFound).when(this.plugin).getDiskOfferingIdCompatible(
                Mockito.eq(volumeOrder), Mockito.eq(disksOfferingFilted));

        String diskOfferingIdCustomized = "id";
        Mockito.doReturn(diskOfferingIdCustomized).when(this.plugin).getDiskOfferingIdCustomized(
                 Mockito.eq(disksOfferingFilted));

        CreateVolumeRequest requestExpected = Mockito.mock(CreateVolumeRequest.class);
        Mockito.doReturn(requestExpected).when(this.plugin).buildVolumeCustomized(
                Mockito.eq(volumeOrder), Mockito.eq(diskOfferingIdCustomized));

        // exercise
        CreateVolumeRequest request = this.plugin.buildCreateVolumeRequest(volumeOrder, this.cloudStackUser);

        // verify
        Assert.assertEquals(requestExpected, request);
        this.loggerTestChecking.assertEquals(LoggerAssert.FIRST_POSITION, Level.WARN,
                Messages.Warn.DISK_OFFERING_COMPATIBLE_NOT_FOUND);
    }

    // test case: When calling the buildCreateVolumeRequest method with secondary methods mocked and
    // there is no disk offering found, it must verify if It throws a NoAvailableResourcesException.
    @Test
    public void testBuildCreateVolumeRequestFail() throws FogbowException {
        // set up
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);

        List disksOffering = new ArrayList();
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.getDisksOffering(
                this.client, this.cloudStackUser, this.cloudStackUrl)).thenReturn(disksOffering);

        List disksOfferingFilted = new ArrayList<>();
        Mockito.doReturn(disksOfferingFilted).when(this.plugin).filterDisksOfferingByRequirements(
                Mockito.eq(disksOffering), Mockito.eq(volumeOrder));

        String diskOfferingIdCompatibleNotFound = null;
        Mockito.doReturn(diskOfferingIdCompatibleNotFound).when(this.plugin).getDiskOfferingIdCompatible(
                Mockito.eq(volumeOrder), Mockito.eq(disksOfferingFilted));

        String diskOfferingIdCustomizedNotFound = null;
        Mockito.doReturn(diskOfferingIdCustomizedNotFound).when(this.plugin).getDiskOfferingIdCustomized(
                Mockito.eq(disksOfferingFilted));

        // verify
        this.expectedException.expect(NoAvailableResourcesException.class);

        // exercise
        this.plugin.buildCreateVolumeRequest(volumeOrder, this.cloudStackUser);
        this.loggerTestChecking.assertEquals(LoggerAssert.FIRST_POSITION, Level.WARN,
                Messages.Warn.DISK_OFFERING_COMPATIBLE_NOT_FOUND);
        this.loggerTestChecking.assertEquals(LoggerAssert.SECOND_POSITION, Level.WARN,
                Messages.Warn.DISK_OFFERING_CUSTOMIZED_NOT_FOUND);

    }

    // test case: When calling the doRequestInstance method with secondary methods mocked ,
    // it must verify if It returns a right instanceId.
    @Test
    public void testDoRequestInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        CreateVolumeRequest request = new CreateVolumeRequest.Builder().build(TestUtils.EMPTY_STRING);
        String responseStr = TestUtils.ANY_VALUE;
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))).
                thenReturn(responseStr);

        String instanceIdExpected = "instanceId";
        CreateVolumeResponse response = Mockito.mock(CreateVolumeResponse.class);
        Mockito.when(response.getId()).thenReturn(instanceIdExpected);
        PowerMockito.mockStatic(CreateVolumeResponse.class);
        PowerMockito.when(CreateVolumeResponse.fromJson(Mockito.eq(responseStr))).thenReturn(response);

        // exercise
        String instanceId = this.plugin.doRequestInstance(request, this.cloudStackUser);

        // verify
        Assert.assertEquals(instanceIdExpected, instanceId);
    }

    // test case: When calling the doRequestInstance method with secondary methods mocked and occurs
    // an exception in the doRequest, it must verify if It throws a FogbowException.
    @Test
    public void testDoRequestInstanceFail() throws FogbowException, HttpResponseException {
        // set up
        CreateVolumeRequest request = new CreateVolumeRequest.Builder().build(TestUtils.EMPTY_STRING);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.doRequestInstance(request, this.cloudStackUser);
    }

    // test case: When calling the deleteInstance method with secondary methods mocked,
    // it must verify if the doDeleteInstance is called with the right parameters;
    // this includes the checking of the Cloudstack request.
    @Test
    public void testDeleteInstanceSuccessfully() throws FogbowException {
        // set up
        String instanceId = "instanceId";
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getInstanceId()).thenReturn(instanceId);

        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.any(), Mockito.eq(this.cloudStackUser));

        DeleteVolumeRequest request = new DeleteVolumeRequest.Builder()
                .id(instanceId)
                .build(this.cloudStackUrl);

        // exercise
        this.plugin.deleteInstance(volumeOrder, this.cloudStackUser);

        // verify
        RequestMatcher<DeleteVolumeRequest> matcher = new RequestMatcher.DeleteVolume(request);
        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .doDeleteInstance(Mockito.argThat(matcher), Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the deleteInstance method and occurs an FogbowException,
    // it must verify if It returns a FogbowException.
    @Test(expected = FogbowException.class)
    public void testDeleteInstanceFail() throws FogbowException {
        // set up
        String instanceId = "instanceId";
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getInstanceId()).thenReturn(instanceId);

        Mockito.doThrow(new FogbowException())
                .when(this.plugin).doDeleteInstance(Mockito.any(), Mockito.eq(this.cloudStackUser));

        // exercise
        this.plugin.deleteInstance(volumeOrder, this.cloudStackUser);
    }

    // test case: When calling the doDeleteInstance method with secondary methods mocked,
    // it must verify if It reachs the right method without problems.
    @Test
    public void testDoDeleteInstanceSuccessfully() throws FogbowException, HttpResponseException {
        // set up
        DeleteVolumeRequest request = new DeleteVolumeRequest.Builder().build(TestUtils.EMPTY_STRING);

        String responseStr = TestUtils.ANY_VALUE;
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))).
                thenReturn(responseStr);

        boolean isSuccess = true;
        DeleteVolumeResponse response = Mockito.mock(DeleteVolumeResponse.class);
        Mockito.when(response.isSuccess()).thenReturn(isSuccess);
        PowerMockito.mockStatic(DeleteVolumeResponse.class);
        PowerMockito.when(DeleteVolumeResponse.fromJson(Mockito.eq(responseStr))).thenReturn(response);

        // exercise
        this.plugin.doDeleteInstance(request, this.cloudStackUser);

        // verify
        Mockito.verify(response, Mockito.times(TestUtils.RUN_ONCE)).isSuccess();
    }

    // test case: When calling the doDeleteInstance method with secondary methods mocked and receive
    // a response successless by the cloud, it must verify if It throws an UnexpectedException.
    @Test
    public void testDoDeleteInstanceFailWhenReturnSuccessless() throws FogbowException, HttpResponseException {
        // set up
        DeleteVolumeRequest request = new DeleteVolumeRequest.Builder().build(TestUtils.EMPTY_STRING);

        String responseStr = TestUtils.ANY_VALUE;
        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))).
                thenReturn(responseStr);

        boolean isSuccessless = false;
        String displayTextSuccessless = "successless";
        DeleteVolumeResponse response = Mockito.mock(DeleteVolumeResponse.class);
        Mockito.when(response.isSuccess()).thenReturn(isSuccessless);
        Mockito.when(response.getDisplayText()).thenReturn(displayTextSuccessless);

        PowerMockito.mockStatic(DeleteVolumeResponse.class);
        PowerMockito.when(DeleteVolumeResponse.fromJson(Mockito.eq(responseStr))).thenReturn(response);

        // verify
        this.expectedException.expect(UnexpectedException.class);
        this.expectedException.expectMessage(displayTextSuccessless);

        // exercise
        this.plugin.doDeleteInstance(request, this.cloudStackUser);
    }

    // test case: When calling the doDeleteInstance method with secondary methods mocked and occurs
    // an exception in the doRequest, it must verify if It throws a FogbowException.
    @Test
    public void testDoDeleteInstanceFail() throws FogbowException, HttpResponseException {
        // set up
        DeleteVolumeRequest request = new DeleteVolumeRequest.Builder().build(TestUtils.EMPTY_STRING);

        PowerMockito.mockStatic(CloudStackCloudUtils.class);
        PowerMockito.when(CloudStackCloudUtils.doRequest(Mockito.eq(this.client),
                Mockito.eq(request.getUriBuilder().toString()), Mockito.eq(this.cloudStackUser))).
                thenThrow(CloudstackTestUtils.createBadRequestHttpResponse());

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(CloudstackTestUtils.BAD_REQUEST_MSG);

        // exercise
        this.plugin.doDeleteInstance(request, this.cloudStackUser);
    }

    private GetAllDiskOfferingsResponse.DiskOffering buildDiskOfferingMocked(boolean isCustomized) {
        int diskSize = CloudStackVolumePlugin.CUSTOMIZED_DISK_SIZE_EXPECTED;
        return buildDiskOfferingMocked(isCustomized, diskSize);
    }

    private GetAllDiskOfferingsResponse.DiskOffering buildDiskOfferingMocked(int diskSize) {
        boolean isCustomized = true;
        return buildDiskOfferingMocked(isCustomized, diskSize);
    }

    private GetAllDiskOfferingsResponse.DiskOffering buildDiskOfferingMocked(boolean isCustomized,
                                                                             int diskSize) {
        GetAllDiskOfferingsResponse.DiskOffering diskOffering =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        String diskOfferingId = "id" + System.currentTimeMillis();

        Mockito.when(diskOffering.getId()).thenReturn(diskOfferingId);
        Mockito.when(diskOffering.getDiskSize()).thenReturn(diskSize);
        Mockito.when(diskOffering.isCustomized()).thenReturn(isCustomized);

        return diskOffering;
    }

}
