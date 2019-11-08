package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.volume.v4_9;

import ch.qos.logback.classic.Level;
import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.PropertiesUtil;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackHttpClient;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.api.http.response.VolumeInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.BaseUnitTests;
import cloud.fogbow.ras.core.LoggerAssert;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackCloudUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudStackUrlMatcher;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.RequestMatcher;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.*;

import static cloud.fogbow.common.constants.CloudStackConstants.Volume.*;

@PrepareForTest({CloudStackUrlUtil.class, DeleteVolumeResponse.class, DeleteVolumeResponse.class,
        GetVolumeResponse.class, DatabaseManager.class, CloudStackCloudUtils.class,
        CreateVolumeResponse.class})
public class CloudStackVolumePluginTest extends BaseUnitTests {

    private static final String REQUEST_FORMAT = "%s?command=%s";
    private static final String RESPONSE_FORMAT = "&response=%s";
    private static final String ID_FIELD = "&id=%s";
    private static final String EMPTY_INSTANCE = "";

    private static final int BYTE = 1;
    private static final int KILOBYTE = 1024 * BYTE;
    private static final int MEGABYTE = 1024 * KILOBYTE;
    private static final int GIGABYTE = 1024 * MEGABYTE;

    private static final String DEFAULT_STATE = "Ready";
    private static final String DEFAULT_DISPLAY_TEXT =
            "A description of the error will be shown if the success field is equal to false.";

    private static final String FAKE_DISK_OFFERING_ID = "fake-disk-offering-id";
    private static final String FAKE_ID = "fake-id";
    private static final String FAKE_JOB_ID = "fake-job-id";
    private static final String FAKE_NAME = "fake-name";
    private static final String FAKE_TAGS = "tag1:value1,tag2:value2";
    private static final String FAKE_MEMBER = "fake-member";
    private static final String FAKE_CLOUD_NAME = "cloud-name";

    private static final String JSON_FORMAT = "json";
    private static final String RESPONSE_KEY = "response";
    private static final String COMMAND_KEY = "command";
    private static final String DISK_OFFERING_ID_KEY = DISK_OFFERING_ID_KEY_JSON;
    private static final String NAME_KEY = NAME_KEY_JSON;
    private static final String SIZE_KEY = SIZE_KEY_JSON;
    private static final String ZONE_ID_KEY = ZONE_ID_KEY_JSON;

    private static final int COMPATIBLE_SIZE = 1;
    private static final int CUSTOMIZED_SIZE = 2;
    private static final int STANDARD_SIZE = 0;

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

    @Test
    public void testBuildVolumeCustomized() {
        // set up
        // exercise
        // verify
    }

    // test case: When calling the getDiskOfferingIdCustomized method and the is no disk offering
    // customized with the size required, it must verify if It returns a null.
    @Test
    public void testGetDiskOfferingIdCustomizedWhenNotFoundDiskOffering() {
        // set up
        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering = new ArrayList<>();
        int diskSizeOne = CloudStackVolumePlugin.CUSTOMIZED_DISK_SIZE_EXPECTED;
        String diskOfferingIdOne = "idOne";
        boolean isNotCustomized = false;
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingOne =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingOne.getId()).thenReturn(diskOfferingIdOne);
        Mockito.when(diskOfferingOne.getDiskSize()).thenReturn(diskSizeOne);
        Mockito.when(diskOfferingOne.isCustomized()).thenReturn(isNotCustomized);
        int diskSizeTwo = CloudStackVolumePlugin.CUSTOMIZED_DISK_SIZE_EXPECTED;
        String diskOfferingIdTwo = "idTwo";
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingTwo =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingTwo.getId()).thenReturn(diskOfferingIdTwo);
        Mockito.when(diskOfferingTwo.getDiskSize()).thenReturn(diskSizeTwo);
        Mockito.when(diskOfferingTwo.isCustomized()).thenReturn(isNotCustomized);

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
        int diskSizeOne = CloudStackVolumePlugin.CUSTOMIZED_DISK_SIZE_EXPECTED;
        String diskOfferingIdOne = "idOne";
        boolean isNotCustomized = false;
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingOne =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingOne.getId()).thenReturn(diskOfferingIdOne);
        Mockito.when(diskOfferingOne.getDiskSize()).thenReturn(diskSizeOne);
        Mockito.when(diskOfferingOne.isCustomized()).thenReturn(isNotCustomized);
        int diskSizeTwo = CloudStackVolumePlugin.CUSTOMIZED_DISK_SIZE_EXPECTED;
        String diskOfferingIdTwo = "idTwo";
        boolean isCustomized = true;
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingTwo =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingTwo.getId()).thenReturn(diskOfferingIdTwo);
        Mockito.when(diskOfferingTwo.getDiskSize()).thenReturn(diskSizeTwo);
        Mockito.when(diskOfferingTwo.isCustomized()).thenReturn(isCustomized);

        disksOffering.add(diskOfferingOne);
        disksOffering.add(diskOfferingTwo);

        // exercise
        String diskOfferingIdCustomized = this.plugin.getDiskOfferingIdCustomized(disksOffering);

        // verify
        Assert.assertEquals(diskOfferingIdTwo, diskOfferingIdCustomized);
    }

    // test case: When calling the buildVolumeCompatible method, it must verify if
    // It returns a right CreateVolumeRequest.
    @Test
    public void testBuildVolumeCompatibleSuccessfully() throws InvalidParameterException {
        // set up
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        String diskOfferingId = "diskOfferingId";

        String nameExpexted = "fogbowname";
        Mockito.doReturn(nameExpexted).when(this.plugin).generateFogbowName();

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
        String diskOfferingIdOne = "idOne";
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingOne =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingOne.getId()).thenReturn(diskOfferingIdOne);
        Mockito.when(diskOfferingOne.getDiskSize()).thenReturn(diskSizeOne);
        int diskSizeTwo = 2;
        String diskOfferingIdTwo = "idTwo";
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingTwo =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingTwo.getId()).thenReturn(diskOfferingIdTwo);
        Mockito.when(diskOfferingTwo.getDiskSize()).thenReturn(diskSizeTwo);

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
        String diskOfferingIdOne = "idOne";
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingOne =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingOne.getId()).thenReturn(diskOfferingIdOne);
        Mockito.when(diskOfferingOne.getDiskSize()).thenReturn(diskSizeOne);
        int diskSizeTwo = 2;
        String diskOfferingIdTwo = "idTwo";
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingTwo =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingTwo.getId()).thenReturn(diskOfferingIdTwo);
        Mockito.when(diskOfferingTwo.getDiskSize()).thenReturn(diskSizeTwo);

        disksOffering.add(diskOfferingOne);
        disksOffering.add(diskOfferingTwo);

        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Mockito.when(volumeOrder.getVolumeSize()).thenReturn(diskSizeTwo);

        // exercise
        String diskOfferingIdCompatible = this.plugin.getDiskOfferingIdCompatible(volumeOrder, disksOffering);

        // verify
        Assert.assertEquals(diskOfferingIdTwo, diskOfferingIdCompatible);
    }

    // test case: When calling the filterDisksOfferingByRequirements method with requirements
    // empty, it must verify if It returns the same  list.
    @Test
    public void testFilterDisksOfferingByRequirementsWhenRequiremetsEmpty() {
        // set up
        VolumeOrder volumeOrder = Mockito.mock(VolumeOrder.class);
        Map<String, String> requirements = new HashedMap();
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
        Map<String, String> requirements = new HashedMap();
        requirements.put(key, value);
        Mockito.when(volumeOrder.getRequirements()).thenReturn(requirements);

        List<GetAllDiskOfferingsResponse.DiskOffering> disksOffering = new ArrayList<>();
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingIncompatibleOne =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingIncompatibleOne.getTags()).thenReturn("anythingone");
        GetAllDiskOfferingsResponse.DiskOffering diskOfferingIncompatibleTwo =
                Mockito.mock(GetAllDiskOfferingsResponse.DiskOffering.class);
        Mockito.when(diskOfferingIncompatibleTwo.getTags()).thenReturn("");

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
        Map<String, String> requirements = new HashedMap();
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
        Mockito.when(diskOfferingIncompatibleTwo.getTags()).thenReturn("");


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
        Mockito.doReturn(disksOffering).when(this.plugin).getDisksOffering(Mockito.eq(this.cloudStackUser));

        List disksOfferingFilted = new ArrayList();
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

        List disksOffering = new ArrayList();
        Mockito.doReturn(disksOffering).when(this.plugin).getDisksOffering(Mockito.eq(this.cloudStackUser));

        List disksOfferingFilted = new ArrayList();
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
        Mockito.doReturn(disksOffering).when(this.plugin).getDisksOffering(Mockito.eq(this.cloudStackUser));

        List disksOfferingFilted = new ArrayList();
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
        CreateVolumeRequest request = new CreateVolumeRequest.Builder().build("");

        String responseStr = "anything";
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
        CreateVolumeRequest request = new CreateVolumeRequest.Builder().build("");

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
        DeleteVolumeRequest request = new DeleteVolumeRequest.Builder().build("");

        String responseStr = "anything";
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
        DeleteVolumeRequest request = new DeleteVolumeRequest.Builder().build("");

        String responseStr = "anything";
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
        DeleteVolumeRequest request = new DeleteVolumeRequest.Builder().build("");

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

    // # ------------ old code ------------ #

    // test case: When calling the requestInstance method with a size compatible with the
    // orchestrator's disk offering, HTTP GET requests must be made with a signed cloudUser, one to get
    // the compatible disk offering Id attached to the requisition, and another to create a volume
    // of compatible size, returning the id of the VolumeInstance object.
    @Test
    public void testCreateRequestInstanceSuccessfulWithDiskSizeCompatible()
            throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT;
        String command = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, this.cloudStackUrl, command, jsonFormat);

        String id = FAKE_DISK_OFFERING_ID;
        int diskSize = COMPATIBLE_SIZE;
        boolean customized = false;
        String diskOfferings = getListDiskOfferrings(id, diskSize, customized, FAKE_TAGS);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(diskOfferings);

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, CREATE_VOLUME_COMMAND);
        expectedParams.put(RESPONSE_KEY, JSON_FORMAT);
        expectedParams.put(ZONE_ID_KEY, this.plugin.getZoneId());
        expectedParams.put(NAME_KEY, FAKE_NAME);
        expectedParams.put(DISK_OFFERING_ID_KEY, FAKE_DISK_OFFERING_ID);
        expectedParams.put(SIZE_KEY, new Long(diskSize * GIGABYTE).toString());

        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams, SIZE_KEY);

        String response = getCreateVolumeResponse(FAKE_ID, FAKE_JOB_ID);
        Mockito.when(
                this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(this.cloudStackUser)))
                .thenReturn(response);

        // exercise
        VolumeOrder order =
                new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, "default", FAKE_NAME, COMPATIBLE_SIZE);
        String volumeId = this.plugin.requestInstance(order, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(2));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(request),
                Mockito.eq(this.cloudStackUser));

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.argThat(urlMatcher),
                Mockito.eq(this.cloudStackUser));

        String expectedId = FAKE_ID;
        Assert.assertEquals(expectedId, volumeId);
    }

    // test case: When calling the requestInstance method to get a size customized by the
    // orchestrator's disk offering, HTTP GET requests must be made with a signed cloudUser, one to get
    // the standard disk offering Id attached to the requisition, and another to create a volume of
    // customized size, returning the id of the VolumeInstance object.
    @Test
    public void testCreateRequestInstanceSuccessfulWithDiskSizeCustomized()
            throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT;
        String command = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, this.cloudStackUrl, command, jsonFormat);

        String id = FAKE_DISK_OFFERING_ID;
        int diskSize = STANDARD_SIZE;
        boolean customized = true;
        String diskOfferings = getListDiskOfferrings(id, diskSize, customized, FAKE_TAGS);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(diskOfferings);

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, CREATE_VOLUME_COMMAND);
        expectedParams.put(RESPONSE_KEY, JSON_FORMAT);
        expectedParams.put(ZONE_ID_KEY, this.plugin.getZoneId());
        expectedParams.put(NAME_KEY, FAKE_NAME);
        expectedParams.put(DISK_OFFERING_ID_KEY, FAKE_DISK_OFFERING_ID);
        expectedParams.put(SIZE_KEY, new Long(diskSize * GIGABYTE).toString());

        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams, SIZE_KEY);

        String response = getCreateVolumeResponse(FAKE_ID, FAKE_JOB_ID);
        Mockito.when(
                this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(this.cloudStackUser)))
                .thenReturn(response);

        // exercise
        VolumeOrder order =
                new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, "default", FAKE_NAME, CUSTOMIZED_SIZE);
        String volumeId = this.plugin.requestInstance(order, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(2));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(request),
                Mockito.eq(this.cloudStackUser));

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.argThat(urlMatcher),
                Mockito.eq(this.cloudStackUser));

        String expectedId = FAKE_ID;
        Assert.assertEquals(expectedId, volumeId);
    }

    @Test
    public void testCreateRequestInstanceSuccessfulWithRequirements() throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT;
        String command = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, this.cloudStackUrl, command, jsonFormat);

        String id = FAKE_DISK_OFFERING_ID;
        int diskSize = COMPATIBLE_SIZE;
        boolean customized = false;
        String diskOfferings = getListDiskOfferrings(id, diskSize, customized, FAKE_TAGS);
        Map<String, String> fakeRequirements = new HashMap<>();
        fakeRequirements.put("tag1", "value1");

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(diskOfferings);

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, CREATE_VOLUME_COMMAND);
        expectedParams.put(RESPONSE_KEY, JSON_FORMAT);
        expectedParams.put(ZONE_ID_KEY, this.plugin.getZoneId());
        expectedParams.put(NAME_KEY, FAKE_NAME);
        expectedParams.put(DISK_OFFERING_ID_KEY, FAKE_DISK_OFFERING_ID);
        expectedParams.put(SIZE_KEY, new Long(diskSize * GIGABYTE).toString());

        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams, SIZE_KEY);

        String response = getCreateVolumeResponse(FAKE_ID, FAKE_JOB_ID);
        Mockito.when(
                this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(this.cloudStackUser)))
                .thenReturn(response);

        // exercise
        VolumeOrder order =
                new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, FAKE_CLOUD_NAME, FAKE_NAME, COMPATIBLE_SIZE);
        order.setRequirements(fakeRequirements);
        String volumeId = this.plugin.requestInstance(order, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(2));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(request),
                Mockito.eq(this.cloudStackUser));

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.argThat(urlMatcher),
                Mockito.eq(this.cloudStackUser));

        String expectedId = FAKE_ID;
        Assert.assertEquals(expectedId, volumeId);
    }

    @Test(expected = FogbowException.class)
    public void testCreateRequestInstanceFailNoRequirements() throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT;
        String command = GetAllDiskOfferingsRequest.LIST_DISK_OFFERINGS_COMMAND;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, this.cloudStackUrl, command, jsonFormat);

        String id = FAKE_DISK_OFFERING_ID;
        int diskSize = COMPATIBLE_SIZE;
        boolean customized = false;
        String diskOfferings = getListDiskOfferrings(id, diskSize, customized, FAKE_TAGS);
        Map<String, String> fakeRequirements = new HashMap<>();
        fakeRequirements.put("tag3", "value3");

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(diskOfferings);

        Map<String, String> expectedParams = new HashMap<>();
        expectedParams.put(COMMAND_KEY, CREATE_VOLUME_COMMAND);
        expectedParams.put(RESPONSE_KEY, JSON_FORMAT);
        expectedParams.put(ZONE_ID_KEY, this.plugin.getZoneId());
        expectedParams.put(NAME_KEY, FAKE_NAME);
        expectedParams.put(DISK_OFFERING_ID_KEY, FAKE_DISK_OFFERING_ID);
        expectedParams.put(SIZE_KEY, new Long(diskSize * GIGABYTE).toString());

        CloudStackUrlMatcher urlMatcher = new CloudStackUrlMatcher(expectedParams, SIZE_KEY);

        String response = getCreateVolumeResponse(FAKE_ID, FAKE_JOB_ID);
        Mockito.when(
                this.client.doGetRequest(Mockito.argThat(urlMatcher), Mockito.eq(this.cloudStackUser)))
                .thenReturn(response);

        // exercise
        VolumeOrder order =
                new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, FAKE_CLOUD_NAME, FAKE_NAME, COMPATIBLE_SIZE);
        order.setRequirements(fakeRequirements);
        String volumeId = this.plugin.requestInstance(order, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(2));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(request),
                Mockito.eq(this.cloudStackUser));
    }

    // test case: When calling the requestInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testCreateRequestInstanceThrowUnauthorizedRequestException()
            throws FogbowException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        try {
            // exercise
            VolumeOrder order =
                    new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, "default", FAKE_NAME, CUSTOMIZED_SIZE);
            this.plugin.requestInstance(order, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When try to request instance with an ID of the volume that do not exist, an
    // InstanceNotFoundException must be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testCreateRequestInstanceThrowNotFoundException()
            throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        try {
            // exercise
            VolumeOrder order =
                    new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, "default", FAKE_NAME, CUSTOMIZED_SIZE);
            this.plugin.requestInstance(order, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When calling the requestInstance method passing some invalid argument, an
    // FogbowException must be thrown.
    @Test(expected = FogbowException.class)
    public void testCreateRequestInstanceThrowFogbowException()
            throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, null));

        try {
            // exercise
            VolumeOrder order =
                    new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, "default", FAKE_NAME, CUSTOMIZED_SIZE);
            this.plugin.requestInstance(order, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When calling the requestInstance method with a unauthenticated user, an
    // UnauthenticatedUserException must be thrown.
    @Test(expected = UnauthenticatedUserException.class)
    public void testCreateRequestInstanceThrowUnauthenticatedUserException()
            throws FogbowException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        try {
            // exercise
            VolumeOrder order =
                    new VolumeOrder(null, FAKE_MEMBER, FAKE_MEMBER, "default", FAKE_NAME, CUSTOMIZED_SIZE);
            this.plugin.requestInstance(order, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When calling the getInstance method, an HTTP GET request must be made with a
    // signed cloudUser, which returns a response in the JSON format for the retrieval of the
    // VolumeInstance object.
    @Test
    public void testGetInstanceRequestSuccessful()
            throws HttpResponseException, FogbowException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + ID_FIELD;
        String command = GetVolumeRequest.LIST_VOLUMES_COMMAND;
        String id = FAKE_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, this.cloudStackUrl, command, jsonFormat, id);

        String name = FAKE_NAME;
        String size = new Long(COMPATIBLE_SIZE * GIGABYTE).toString();
        String state = DEFAULT_STATE;
        String volume = getVolumeResponse(id, name, size, state);
        String response = getListVolumesResponse(volume);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(response);

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        // exercise
        VolumeInstance recoveredInstance = this.plugin.getInstance(volumeOrder, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Assert.assertEquals(id, recoveredInstance.getId());
        Assert.assertEquals(name, recoveredInstance.getName());
        Assert.assertEquals(COMPATIBLE_SIZE, recoveredInstance.getSize());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, this.cloudStackUser);
    }

    // test case: When calling the getInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testGetInstanceThrowUnauthorizedRequestException()
            throws UnexpectedException, FogbowException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.getInstance(volumeOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When try to get an instance with an ID that does not exist, an
    // InstanceNotFoundException must be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testGetInstanceThrowNotFoundException()
            throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.getInstance(volumeOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When calling the getInstance method with a unauthenticated user, an
    // UnauthenticatedUserException must be thrown.
    @Test(expected = UnauthenticatedUserException.class)
    public void testGetInstanceThrowUnauthenticatedUserException()
            throws UnexpectedException, FogbowException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.getInstance(volumeOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When calling the getInstance method passing some invalid argument, an
    // FogbowException must be thrown.
    @Test(expected = FogbowException.class)
    public void testGetInstanceThrowFogbowException()
            throws UnexpectedException, FogbowException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, null));

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.getInstance(volumeOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When calling the getInstance method and an HTTP GET request returns a failure
    // response in JSON format, an UnexpectedException must be thrown.
    @Test(expected = UnexpectedException.class)
    public void testGetInstanceThrowUnexpectedException()
            throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + ID_FIELD;
        String command = GetVolumeRequest.LIST_VOLUMES_COMMAND;
        String id = FAKE_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, this.cloudStackUrl, command, jsonFormat, id);

        String volume = EMPTY_INSTANCE;
        String response = getListVolumesResponse(volume);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(response);

        PowerMockito.mockStatic(GetVolumeResponse.class);
        PowerMockito.when(GetVolumeResponse.fromJson(response)).thenCallRealMethod();

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.getInstance(volumeOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));

            PowerMockito.verifyStatic(GetVolumeResponse.class, VerificationModeFactory.times(1));
            GetVolumeResponse.fromJson(Mockito.eq(response));
        }
    }

    // test case: When calling the deleteInstance method, an HTTP GET request must be made with a
    // signed cloudUser, which returns a response in the JSON format.
    @Test
    public void testDeleteInstanceRequestSuccessful()
            throws HttpResponseException, FogbowException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + ID_FIELD;
        String command = DELETE_VOLUME_COMMAND;
        String id = FAKE_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, this.cloudStackUrl, command, jsonFormat, id);

        boolean success = true;
        String response = getDeleteVolumeResponse(success);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(response);

        PowerMockito.mockStatic(DeleteVolumeResponse.class);
        PowerMockito.when(DeleteVolumeResponse.fromJson(response)).thenCallRealMethod();

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        // exercise
        this.plugin.deleteInstance(volumeOrder, this.cloudStackUser);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(request, cloudStackUser);

        PowerMockito.verifyStatic(DeleteVolumeResponse.class, VerificationModeFactory.times(1));
        DeleteVolumeResponse.fromJson(Mockito.eq(response));
    }

    // test case: When calling the deleteInstance method with a user without permission, an
    // UnauthorizedRequestException must be thrown.
    @Test(expected = UnauthorizedRequestException.class)
    public void testDeleteInstanceThrowUnauthorizedRequestException()
            throws UnexpectedException, FogbowException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_FORBIDDEN, null));

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.getInstance(volumeOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When try to delete an instance with an ID that does not exist, an
    // InstanceNotFoundException must be thrown.
    @Test(expected = InstanceNotFoundException.class)
    public void testDeleteInstanceThrowNotFoundException()
            throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_NOT_FOUND, null));

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.getInstance(volumeOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When calling the deleteInstance method with a unauthenticated user, an
    // UnauthenticatedUserException must be thrown.
    @Test(expected = UnauthenticatedUserException.class)
    public void testDeleteInstanceThrowUnauthenticatedUserException()
            throws UnexpectedException, FogbowException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(this.client.doGetRequest(Mockito.anyString(),
                Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_UNAUTHORIZED, null));

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.getInstance(volumeOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When calling the deleteInstance method passing some invalid argument, an
    // FogbowException must be thrown.
    @Test(expected = FogbowException.class)
    public void testDeleteInstanceThrowFogbowException()
            throws UnexpectedException, FogbowException, HttpResponseException {

        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        Mockito.when(
                this.client.doGetRequest(Mockito.anyString(), Mockito.any(CloudStackUser.class)))
                .thenThrow(new HttpResponseException(HttpStatus.SC_BAD_REQUEST, null));

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.getInstance(volumeOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));
        }
    }

    // test case: When calling the deleteInstance method and an HTTP GET request returns a failure
    // response in JSON format, an UnexpectedException must be thrown.
    @Test(expected = UnexpectedException.class)
    public void testDeleteInstanceThrowUnexpectedException()
            throws HttpResponseException, FogbowException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + RESPONSE_FORMAT + ID_FIELD;
        String command = DELETE_VOLUME_COMMAND;
        String id = FAKE_ID;
        String jsonFormat = JSON_FORMAT;
        String request = String.format(urlFormat, this.cloudStackUrl, command, jsonFormat, id);

        boolean success = false;
        String response = getDeleteVolumeResponse(success);

        Mockito.when(this.client.doGetRequest(request, this.cloudStackUser)).thenReturn(response);

        PowerMockito.mockStatic(DeleteVolumeResponse.class);
        PowerMockito.when(DeleteVolumeResponse.fromJson(response)).thenCallRealMethod();

        VolumeOrder volumeOrder = new VolumeOrder();
        volumeOrder.setInstanceId(FAKE_ID);

        try {
            // exercise
            this.plugin.deleteInstance(volumeOrder, this.cloudStackUser);
        } finally {
            // verify
            PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
            CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

            Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.anyString(),
                    Mockito.any(CloudStackUser.class));

            PowerMockito.verifyStatic(DeleteVolumeResponse.class, VerificationModeFactory.times(1));
            DeleteVolumeResponse.fromJson(Mockito.eq(response));
        }
    }

    private String getListDiskOfferrings(String id, int diskSize, boolean customized, String tags) {
        String response = "{\"listdiskofferingsresponse\":{" + "\"diskoffering\":[{"
                + "\"id\": \"%s\","
                + "\"disksize\": %s,"
                + "\"iscustomized\": %s,"
                + "\"tags\": \"%s\""
                + "}]}}";

        return String.format(response, id, diskSize, customized, tags);
    }

    private String getCreateVolumeResponse(String id, String jobId) {
        String response = "{\"createvolumeresponse\":{"
                + "\"id\": \"%s\", "
                + "\"jobid\": \"%s\""
                + "}}";

        return String.format(response, id, jobId);
    }

    private String getListVolumesResponse(String volume) {
        String response = "{\"listvolumesresponse\":{\"volume\":[%s]}}";

        return String.format(response, volume);
    }

    private String getVolumeResponse(String id, String name, String size, String state) {
        String response = "{\"id\":\"%s\","
                + "\"name\":\"%s\","
                + "\"size\":\"%s\","
                + "\"state\":\"%s\""
                + "}";

        return String.format(response, id, name, size, state);
    }

    private String getDeleteVolumeResponse(boolean success) {
        String value = String.valueOf(success);
        String response = "{\"deletevolumeresponse\":{"
                + "\"displaytext\": \"%s\","
                + "\"success\": \"%s\""
                + "}}";

        return String.format(response, DEFAULT_DISPLAY_TEXT, value);
    }

}
