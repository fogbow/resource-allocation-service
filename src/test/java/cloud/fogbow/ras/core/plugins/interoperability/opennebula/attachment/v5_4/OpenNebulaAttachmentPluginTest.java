package cloud.fogbow.ras.core.plugins.interoperability.opennebula.attachment.v5_4;

import cloud.fogbow.common.exceptions.InternalServerErrorException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.vm.VirtualMachine;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudUser;
import cloud.fogbow.ras.api.http.response.AttachmentInstance;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.SharedOrderHolders;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.datastore.DatabaseManager;
import cloud.fogbow.ras.core.models.orders.AttachmentOrder;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.VolumeOrder;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaBaseTests;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.opennebula.OpenNebulaStateMapper;

@PrepareForTest({ DatabaseManager.class, OpenNebulaClientUtil.class, VirtualMachine.class })
public class OpenNebulaAttachmentPluginTest extends OpenNebulaBaseTests {

    private static final String ANOTHER_DEVICE_PATH = "/sda";
    private static final String DEFAULT_DEVICE_PATH = "/dev/sdb";
    private static final String FAKE_INSTANCE_ID = "1";
    private static final String FAKE_VIRTUAL_MACHINE_ID = "1";
    private static final String FAKE_VOLUME_ID = "1";
    private static final String LOCAL_TOKEN_VALUE = "user:password";
    private static final String VIRTUAL_MACHINE_CONTENT = "<DISK_ID>1</DISK_ID>";

    private OpenNebulaAttachmentPlugin plugin;
    private AttachmentOrder attachmentOrder;
    private VirtualMachine virtualMachine;
    private String template;
    private OneResponse response;
    private CloudUser cloudUser;
    private Client client;

    @Before
    public void setUp() throws FogbowException {
        super.setUp();
        this.plugin = Mockito.spy(new OpenNebulaAttachmentPlugin(this.openNebulaConfFilePath));
        this.attachmentOrder = createAttachmentOrder();

        this.virtualMachine = Mockito.mock(VirtualMachine.class);
        this.response = Mockito.mock(OneResponse.class);
        this.client = Mockito.mock(Client.class);
        this.template = this.generateAttachmentTemplate();
        this.cloudUser = this.createCloudUser();

        PowerMockito.when(OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.anyString()))
                .thenReturn(this.virtualMachine);
        Mockito.when(this.virtualMachine.info()).thenReturn(this.response);
        Mockito.when(this.response.getMessage()).thenReturn(VIRTUAL_MACHINE_CONTENT);
    }

    // test case: When calling the isReady method with the cloud states USED or
    // USED_PERS, this means that the state of attachment is READY and it must
    // return true.
    @Test
    public void testIsReady() {
        // set up
        String[] cloudStates = { OpenNebulaStateMapper.USED_STATE,
                OpenNebulaStateMapper.ATTACHMENT_USED_PERSISTENT_STATE };

        String cloudState;
        for (int i = 0; i < cloudStates.length; i++) {
            cloudState = cloudStates[i];

            // exercise
            boolean status = this.plugin.isReady(cloudState);

            // verify
            Assert.assertTrue(status);
        }
    }

    // test case: When calling the isReady method with the cloud states ERROR, this
    // means that the state of attachment is FAILED and it must return false.
    @Test
    public void testIsReadyFail() {
        // set up
        String cloudState = OpenNebulaStateMapper.DEFAULT_ERROR_STATE;

        // exercise
        boolean status = this.plugin.isReady(cloudState);

        // verify
        Assert.assertFalse(status);
    }

    // test case: When calling the hasFailed method with the cloud states ERROR,
    // this means that the state of attachment is FAILED and it must return true.
    @Test
    public void testHasFailed() {
        // set up
        String cloudState = OpenNebulaStateMapper.DEFAULT_ERROR_STATE;

        // exercise
        boolean status = this.plugin.hasFailed(cloudState);

        // verify
        Assert.assertTrue(status);
    }

    // test case: When calling the hasFailed method with the cloud states USED or
    // USED_PERS, this means that the state of attachment is READY and it must
    // return false.
    @Test
    public void testHasFailedFail() {
        // set up
        String[] cloudStates = { OpenNebulaStateMapper.USED_STATE,
                OpenNebulaStateMapper.ATTACHMENT_USED_PERSISTENT_STATE };

        String cloudState;
        for (int i = 0; i < cloudStates.length; i++) {
            cloudState = cloudStates[i];

            // exercise
            boolean status = this.plugin.hasFailed(cloudState);

            // verify
            Assert.assertFalse(status);
        }
    }

    // test case: When invoking the requestInstance method, with a valid client
    // and template, a virtual machine will be instantiated to attach a volume image
    // disk and return the attached disk ID
    @Test
    public void testRequestInstance() throws FogbowException {
        // set up
        String deviceTarget = OpenNebulaAttachmentPlugin.DEFAULT_TARGET;
        Mockito.doReturn(deviceTarget).when(this.plugin).normalizeDeviceTarget(this.attachmentOrder);

        Mockito.doNothing().when(this.plugin).doRequestInstance(Mockito.any(Client.class),
                Mockito.eq(this.attachmentOrder.getComputeId()), Mockito.eq(this.attachmentOrder.getVolumeId()),
                Mockito.eq(this.template));

        // exercise
        this.plugin.requestInstance(this.attachmentOrder, this.cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE))
                .normalizeDeviceTarget(Mockito.eq(this.attachmentOrder));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doRequestInstance(Mockito.any(Client.class),
                Mockito.eq(this.attachmentOrder.getComputeId()), Mockito.eq(this.attachmentOrder.getVolumeId()),
                Mockito.eq(this.template));
    }

    // test case: When calling the doRequestInstance method with a valid template,
    // OpenNebulaClientUtil should retrieve the VM, call the diskAttach method and
    // return a VirtualMachine object with the attached disk.
    @Test
    public void testDoRequestInstance() throws FogbowException {
        // set up
        Mockito.when(this.virtualMachine.diskAttach(this.template)).thenReturn(this.response);
        Mockito.when(this.response.isError()).thenReturn(false);

        String computeId = this.attachmentOrder.getComputeId();

        // exercise
        this.plugin.doRequestInstance(this.client, computeId, this.attachmentOrder.getVolumeId(), this.template);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(computeId));

        Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
        Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).diskAttach(Mockito.eq(this.template));
    }

    // test case: When calling the doRequestInstance method, with an invalid
    // instance ID or an invalid token, an error will occur and an
    // InvalidParameterException will be thrown.
    @Test
    public void testDoRequestInstanceFail() throws FogbowException {
        // set up
        Mockito.when(this.virtualMachine.diskAttach(this.template)).thenReturn(this.response);
        Mockito.when(this.response.isError()).thenReturn(true);

        String fakeVolumeId = this.attachmentOrder.getVolumeId();
        String computeId = this.attachmentOrder.getComputeId();
        String expectedMessage = String.format(Messages.Log.ERROR_WHILE_ATTACHING_VOLUME_S_WITH_RESPONSE_S, fakeVolumeId,
                VIRTUAL_MACHINE_CONTENT);

        try {
            // exercise
            this.plugin.doRequestInstance(this.client, computeId, fakeVolumeId, this.template);
            Assert.fail();
        } catch (InvalidParameterException e) {
            // Verify
            PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(computeId));

            Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
            Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).getMessage();
            Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE))
                    .diskAttach(Mockito.eq(this.template));

            Assert.assertEquals(expectedMessage, e.getMessage());
        }
    }
    
    // test case: When calling the normalizeDeviceTarget method, with an
    // attachmentOrder containing a default device path, it must be returned the
    // expected device target.
    @Test
    public void testNormalizeDeviceTargetForDefaultDevicePath() {
        // set up
        String[] pathSplited = DEFAULT_DEVICE_PATH.split(OpenNebulaAttachmentPlugin.DEVICE_PATH_SEPARATOR);
        String expected = pathSplited[OpenNebulaAttachmentPlugin.TARGET_INDEX];
        
        // exercise
        String target = this.plugin.normalizeDeviceTarget(this.attachmentOrder);
        
        // verify
        Assert.assertEquals(expected, target);
    }
    
    // test case: When calling the normalizeDeviceTarget method, with an
    // attachmentOrder containing a device path different from the default, it must
    // be returned the default device target.
    @Test
    public void testNormalizeDeviceTargetForAnotherDevicePath() {
        // set up
        AttachmentOrder attachmentOrder = createAttachmentOrder(ANOTHER_DEVICE_PATH);
        String expected = OpenNebulaAttachmentPlugin.DEFAULT_TARGET;

        // exercise
        String target = this.plugin.normalizeDeviceTarget(attachmentOrder);

        // verify
        Assert.assertEquals(expected, target);
    }
    

    // test case: When calling the deleteInstance method, with an attachmentOrder
    // and a valid cloudUser, the volume image disk associated to a virtual machine
    // will be detached.
    @Test
    public void testDeleteInstance() throws FogbowException {
        // set up
        String computeId = this.attachmentOrder.getComputeId();
        String instanceId = this.attachmentOrder.getInstanceId();

        Mockito.doNothing().when(this.plugin).doDeleteInstance(Mockito.any(Client.class), Mockito.eq(computeId),
                Mockito.eq(instanceId));

        // exercise
        this.plugin.deleteInstance(this.attachmentOrder, this.cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doDeleteInstance(Mockito.any(Client.class),
                Mockito.eq(computeId), Mockito.eq(instanceId));
    }

    // test case: When calling the deleteInstance method with an nonexistent
    // AttachmentOrder, an InstanceNotFoundException will be thrown.
    @Test
    public void testDeleteInstanceFail() throws FogbowException {
        // set up
        AttachmentOrder attachmentOrder = null;

        try {
            // exercise
            this.plugin.deleteInstance(attachmentOrder, this.cloudUser);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(e.getMessage(), Messages.Exception.INSTANCE_NOT_FOUND);
        }
    }

    // test case: When calling the doDeleteInstance method, with a valid
    // attachmentOrder and client, the VM associated to the order should be
    // retrieved and the the method diskDetach should be called successfully 
    // passing the order volumeId relating to the disk image used.
    @Test
    public void testDoDeleteInstance() throws FogbowException {
        // set up
        String volumeId = this.attachmentOrder.getVolumeId();
        String computeId = this.attachmentOrder.getComputeId();
        int imageId = Integer.parseInt(volumeId);

        Mockito.when(this.virtualMachine.diskDetach(imageId)).thenReturn(this.response);
        Mockito.when(this.response.isError()).thenReturn(false);

        // exercise
        this.plugin.doDeleteInstance(this.client, computeId, volumeId);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(computeId));

        Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).diskDetach(Mockito.eq(imageId));
        Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
    }

    // test case: When calling the doDeleteInstance method, with an invalid
    // attachmentOrder or cloudUser, an InternalServerErrorException will occur
    @Test
    public void testDoDeleteInstanceFail() throws FogbowException {
        // set up
        String volumeId = this.attachmentOrder.getVolumeId();
        String computeId = this.attachmentOrder.getComputeId();
        int imageId = Integer.parseInt(volumeId);

        String expectedMessage = String.format(Messages.Log.ERROR_WHILE_DETACHING_VOLUME_S, imageId,
                VIRTUAL_MACHINE_CONTENT);

        Mockito.when(this.virtualMachine.diskDetach(imageId)).thenReturn(this.response);
        Mockito.when(this.response.isError()).thenReturn(true);

        try {
            // exercise
            this.plugin.doDeleteInstance(this.client, computeId, volumeId);
            Assert.fail();
        } catch (InternalServerErrorException e) {
            // verify
            Assert.assertEquals(e.getMessage(), expectedMessage);
        }

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(computeId));

        Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).diskDetach(Mockito.eq(imageId));
        Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).isError();
        Mockito.verify(this.response, Mockito.times(TestUtils.RUN_ONCE)).getMessage();
    }

    // test case: When calling the getInstance method, with the instance ID and a
    // valid token, a set of images will be loaded and the specific instance of the
    // image must be loaded.
    @Test
    public void testGetInstance() throws FogbowException {
        // set up
        String fakeInstanceId = this.attachmentOrder.getInstanceId();

        Mockito.doReturn(new AttachmentInstance(fakeInstanceId)).when(this.plugin).doGetInstance(
                Mockito.any(Client.class), Mockito.eq(fakeInstanceId), Mockito.eq(this.attachmentOrder.getComputeId()),
                Mockito.eq(this.attachmentOrder.getVolumeId()));

        // exercise
        this.plugin.getInstance(this.attachmentOrder, this.cloudUser);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.createClient(Mockito.anyString(), Mockito.eq(this.cloudUser.getToken()));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).doGetInstance(Mockito.any(Client.class),
                Mockito.eq(fakeInstanceId), Mockito.eq(this.attachmentOrder.getComputeId()),
                Mockito.eq(this.attachmentOrder.getVolumeId()));
    }

    // test case: When calling the getInstance method with a nonexistent
    // AttachmentOrder an InstanceNotFoundException will be thrown.
    @Test
    public void testGetInstanceFail() throws FogbowException {
        // set up
        AttachmentOrder attachmentOrder = null;

        // exercise
        try {
            this.plugin.getInstance(attachmentOrder, this.cloudUser);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            Assert.assertEquals(e.getMessage(), Messages.Exception.INSTANCE_NOT_FOUND);
        }
    }

    // test case: When calling the getInstance method, with the instance ID and a
    // valid token, a set of images will be loaded and the specific instance of the
    // image must be loaded.
    @Test
    public void testDoGetInstance() throws FogbowException {
        // set up
        String computeId = this.attachmentOrder.getComputeId();
        String volumeId = this.attachmentOrder.getVolumeId();

        String deviceTarget = OpenNebulaAttachmentPlugin.DEFAULT_TARGET;
        String deviceTargetPath = String.format(OpenNebulaAttachmentPlugin.TARGET_PATH_FORMAT, volumeId);
        Mockito.when(this.virtualMachine.xpath(Mockito.eq(deviceTargetPath))).thenReturn(deviceTarget);

        String imageState = OpenNebulaStateMapper.USED_STATE;
        Mockito.doReturn(imageState).when(this.plugin).getImageState(Mockito.eq(this.client), Mockito.eq(volumeId));

        // exercise
        this.plugin.doGetInstance(this.client, this.attachmentOrder.getInstanceId(), computeId, volumeId);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.getVirtualMachine(Mockito.any(Client.class), Mockito.eq(computeId));

        Mockito.verify(this.virtualMachine, Mockito.times(TestUtils.RUN_ONCE)).xpath(Mockito.eq(deviceTargetPath));

        Mockito.verify(this.plugin, Mockito.times(TestUtils.RUN_ONCE)).getImageState(Mockito.eq(client),
                Mockito.eq(volumeId));
    }
    
    // test case: When calling the getImageState method, with a valid client and
    // volumeId, it must verify that he was successfully called.
    @Test
    public void testGetImageState() throws FogbowException {
        // set up
        String volumeId = FAKE_VOLUME_ID;
        int diskId = Integer.parseInt(volumeId);

        ImagePool imagePool = Mockito.mock(ImagePool.class);
        Mockito.when(OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class))).thenReturn(imagePool);

        Image image = Mockito.mock(Image.class);
        Mockito.when(imagePool.getById(diskId)).thenReturn(image);
        Mockito.when(image.shortStateStr()).thenReturn(OpenNebulaStateMapper.USED_STATE);

        // exercise
        this.plugin.getImageState(this.client, volumeId);

        // verify
        PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
        OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class));

        Mockito.verify(imagePool, Mockito.times(TestUtils.RUN_ONCE)).getById(Mockito.eq(diskId));
        Mockito.verify(image, Mockito.times(TestUtils.RUN_ONCE)).shortStateStr();
    }
    
    // test case: When calling the getImageState method, with an invalid volumeId,
    // it must verify if the InstanceNotFoundException was thrown with the expected
    // error message.
    @Test
    public void testGetImageStateFail() throws FogbowException {
        // set up
        String volumeId = FAKE_VOLUME_ID;
        int diskId = Integer.parseInt(volumeId);

        ImagePool imagePool = Mockito.mock(ImagePool.class);
        Mockito.when(OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class))).thenReturn(imagePool);

        Mockito.when(imagePool.getById(diskId)).thenReturn(null);
        String expected = Messages.Exception.INSTANCE_NOT_FOUND;

        try {
            // exercise
            this.plugin.getImageState(this.client, volumeId);
            Assert.fail();
        } catch (InstanceNotFoundException e) {
            // verify
            PowerMockito.verifyStatic(OpenNebulaClientUtil.class, Mockito.times(TestUtils.RUN_ONCE));
            OpenNebulaClientUtil.getImagePool(Mockito.any(Client.class));

            Mockito.verify(imagePool, Mockito.times(TestUtils.RUN_ONCE)).getById(Mockito.eq(diskId));
            Assert.assertEquals(expected, e.getMessage());
        }
    }

    private String generateAttachmentTemplate() {
        String template = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" 
                + "<TEMPLATE>\n"
                + "    <DISK>\n" 
                + "        <IMAGE_ID>1</IMAGE_ID>\n" 
                + "        <TARGET>hdb</TARGET>\n"
                + "    </DISK>\n" 
                + "</TEMPLATE>\n";
        
        return template;
    }

    private AttachmentOrder createAttachmentOrder(String...args) {
        ComputeOrder computeOrder = this.testUtils.createLocalComputeOrder();
        computeOrder.setInstanceId(FAKE_VIRTUAL_MACHINE_ID);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(computeOrder.getId(), computeOrder);

        VolumeOrder volumeOrder = this.testUtils.createLocalVolumeOrder();
        volumeOrder.setInstanceId(FAKE_VOLUME_ID);
        SharedOrderHolders.getInstance().getActiveOrdersMap().put(volumeOrder.getId(), volumeOrder);

        String device = args.length == 0 ? DEFAULT_DEVICE_PATH : args[0];
        AttachmentOrder attachmentOrder = this.testUtils.createAttachmentOrder(computeOrder, volumeOrder, device);
        attachmentOrder.setInstanceId(FAKE_INSTANCE_ID);
        return attachmentOrder;
    }

    private CloudUser createCloudUser() {
        String userId = null;
        String userName = TestUtils.FAKE_USER_NAME;
        String tokenValue = LOCAL_TOKEN_VALUE;
        return new CloudUser(userId, userName, tokenValue);
    }
}
