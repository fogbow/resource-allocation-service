package cloud.fogbow.ras.core.plugins.interoperability.opennebula;

import cloud.fogbow.common.exceptions.*;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.opennebula.OpenNebulaClientFactory;
import cloud.fogbow.ras.constants.SystemConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.group.Group;
import org.opennebula.client.group.GroupPool;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.template.TemplatePool;
import org.opennebula.client.user.User;
import org.opennebula.client.user.UserPool;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vnet.VirtualNetwork;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Iterator;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Image.class, SecurityGroup.class, VirtualNetwork.class, VirtualMachine.class})
public class OpenNebulaClientFactoryTest {

    private static final String CLOUD_NAME = "opennebula";
    private String openenbulaConfFilePath;
    private OpenNebulaClientFactory openNebulaClientFactory;

    @Before
    public void setUp() {
        this.openenbulaConfFilePath = HomeDir.getPath() + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME +
                File.separator + CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;
        this.openNebulaClientFactory = Mockito.spy(new OpenNebulaClientFactory(openenbulaConfFilePath));
    }

    // test case: success case
    @Test
    public void testCreateClient() throws UnexpectedException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        // exercise
        Assert.assertTrue(client instanceof Client);
    }

    // test case: error the in the client creation
    @Test(expected = UnexpectedException.class)
    public void testCreateClientClientConfigurationException() throws UnexpectedException {
        // exercise
        this.openNebulaClientFactory.createClient(null);
    }

    // test case: success case
    @Test
    public void testCreateGroup() throws UnexpectedException, UnauthorizedRequestException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        int groupId = 0;
        Group groupExpected = Mockito.mock(Group.class);

        GroupPool groupPool = Mockito.mock(GroupPool.class);
        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.when(oneResponse.isError()).thenReturn(Boolean.FALSE);
        Mockito.when(groupPool.info()).thenReturn(oneResponse);
        Mockito.doReturn(groupExpected).when(groupPool).getById(Mockito.eq(groupId));
        Mockito.doReturn(groupPool).when(this.openNebulaClientFactory).generateOnePool(Mockito.eq(client), Mockito.eq(GroupPool.class));

        // exercise
        Group group = this.openNebulaClientFactory.createGroup(client, groupId);

        // verify
        Assert.assertEquals(groupExpected, group);
        Mockito.verify(group, Mockito.times(1)).info();
    }

    // test case: throws exception when does not find the group
    @Test(expected = UnauthorizedRequestException.class)
    public void testCreateGroupUnauthorized() throws UnexpectedException, UnauthorizedRequestException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");

        GroupPool groupPool = Mockito.mock(GroupPool.class);
        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.when(oneResponse.isError()).thenReturn(Boolean.FALSE);
        Mockito.when(groupPool.info()).thenReturn(oneResponse);
        Mockito.doReturn(groupPool).when(this.openNebulaClientFactory).generateOnePool(Mockito.eq(client), Mockito.eq(GroupPool.class));
        int groupId = 0;

        // exercise
        this.openNebulaClientFactory.createGroup(client, groupId);
    }

    // test case: success case
    @Test
    public void testCreateImagePool() throws UnexpectedException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        ImagePool imagePoolExpected = Mockito.mock(ImagePool.class);

        Mockito.doReturn(imagePoolExpected).when(this.openNebulaClientFactory).generateOnePool(Mockito.eq(client), Mockito.eq(ImagePool.class));

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.FALSE).when(oneResponse).isError();

        Mockito.doReturn(oneResponse).when(imagePoolExpected).infoAll();

        // exercise
        ImagePool imagePool = this.openNebulaClientFactory.createImagePool(client);

        // verify
        Assert.assertEquals(imagePoolExpected, imagePool);
    }

    // test case: throws UnexpectedException
    @Test(expected = UnexpectedException.class)
    public void testCreateImagePoolUnexpectedException() throws UnexpectedException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        ImagePool imagePoolExpected = Mockito.mock(ImagePool.class);

        Mockito.doReturn(imagePoolExpected).when(this.openNebulaClientFactory).generateOnePool(Mockito.eq(client), Mockito.eq(ImagePool.class));

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();

        Mockito.doReturn(oneResponse).when(imagePoolExpected).infoAll();

        // exercise
        this.openNebulaClientFactory.createImagePool(client);
    }

    // test case: success case
    @Test
    public void testCreateTemplatePool() throws UnexpectedException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        TemplatePool templatePoolExpected = Mockito.mock(TemplatePool.class);

        Mockito.doReturn(templatePoolExpected).when(this.openNebulaClientFactory).generateOnePool(Mockito.eq(client), Mockito.eq(TemplatePool.class));

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.FALSE).when(oneResponse).isError();

        Mockito.doReturn(oneResponse).when(templatePoolExpected).infoAll();

        // exercise
        TemplatePool templatePool = this.openNebulaClientFactory.createTemplatePool(client);

        // verify
        Assert.assertEquals(templatePoolExpected, templatePool);
    }

    // test case: throws UnexpectedException
    @Test(expected = UnexpectedException.class)
    public void testCreateTemplatePoolUnexpectedException() throws UnexpectedException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        TemplatePool templatePoolExpected = Mockito.mock(TemplatePool.class);

        Mockito.doReturn(templatePoolExpected).when(this.openNebulaClientFactory).generateOnePool(Mockito.eq(client), Mockito.eq(TemplatePool.class));

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();

        Mockito.doReturn(oneResponse).when(templatePoolExpected).infoAll();

        // exercise
        this.openNebulaClientFactory.createTemplatePool(client);
    }

    // test case: success case
    @Test
    public void testCreateUserPool() throws UnexpectedException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        UserPool userPoolExpected = Mockito.mock(UserPool.class);

        Mockito.doReturn(userPoolExpected).when(this.openNebulaClientFactory).generateOnePool(Mockito.eq(client), Mockito.eq(UserPool.class));

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.FALSE).when(oneResponse).isError();

        Mockito.doReturn(oneResponse).when(userPoolExpected).info();

        // exercise
        UserPool userPool = this.openNebulaClientFactory.createUserPool(client);

        // verify
        Assert.assertEquals(userPoolExpected, userPool);
    }

    // test case: throws UnexpectedException
    @Test(expected = UnexpectedException.class)
    public void testCreateUserPoolUnexpectedException() throws UnexpectedException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        UserPool userPoolExpected = Mockito.mock(UserPool.class);

        Mockito.doReturn(userPoolExpected).when(this.openNebulaClientFactory).generateOnePool(Mockito.eq(client), Mockito.eq(UserPool.class));

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();

        Mockito.doReturn(oneResponse).when(userPoolExpected).info();

        // exercise
        this.openNebulaClientFactory.createUserPool(client);
    }

    // test case: success case
    @Test
    public void testGetUser() throws UnexpectedException, UnauthorizedRequestException {
        // set up
        String userName = "userName";

        UserPool userPool = Mockito.mock(UserPool.class);
        Iterator<User> itertUser = Mockito.mock(Iterator.class);
        Mockito.when(userPool.iterator()).thenReturn(itertUser);
        Mockito.when(itertUser.hasNext()).thenReturn(true, true, false);
        User userOne = Mockito.mock(User.class);
        Mockito.when(userOne.getName()).thenReturn("anything");
        User userTwo = Mockito.mock(User.class);
        Mockito.when(userTwo.getName()).thenReturn(userName);

        Mockito.when(itertUser.next()).thenReturn(userOne, userTwo);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.FALSE).when(oneResponse).isError();

        Mockito.doReturn(oneResponse).when(userTwo).info();

        // exercise
        User user = this.openNebulaClientFactory.getUser(userPool, userName);

        // verify
        Assert.assertEquals(userTwo, user);
    }

    // test case: throws UnexpectedException
    @Test(expected = UnexpectedException.class)
    public void testGetUserUnexpectedException() throws UnexpectedException, UnauthorizedRequestException {
        // set up
        String userName = "userName";

        UserPool userPool = Mockito.mock(UserPool.class);
        Iterator<User> itertUser = Mockito.mock(Iterator.class);
        Mockito.when(userPool.iterator()).thenReturn(itertUser);
        Mockito.when(itertUser.hasNext()).thenReturn(true, true, false);
        User userOne = Mockito.mock(User.class);
        Mockito.when(userOne.getName()).thenReturn("anything");
        User userTwo = Mockito.mock(User.class);
        Mockito.when(userTwo.getName()).thenReturn(userName);

        Mockito.when(itertUser.next()).thenReturn(userOne, userTwo);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();

        Mockito.doReturn(oneResponse).when(userTwo).info();

        // exercise
        this.openNebulaClientFactory.getUser(userPool, userName);
    }

    // test case: throws UnauthorizedRequestException
    @Test(expected = UnauthorizedRequestException.class)
    public void testGetUserUnauthorizedRequestException() throws UnexpectedException, UnauthorizedRequestException {
        // set up
        String userName = "userName";

        UserPool userPool = Mockito.mock(UserPool.class);
        Iterator<User> itertUser = Mockito.mock(Iterator.class);
        Mockito.when(userPool.iterator()).thenReturn(itertUser);
        Mockito.when(itertUser.hasNext()).thenReturn(true, true, false);
        User userOne = Mockito.mock(User.class);
        Mockito.when(userOne.getName()).thenReturn("anything");
        User userTwo = Mockito.mock(User.class);
        Mockito.when(userTwo.getName()).thenReturn("something");

        Mockito.when(itertUser.next()).thenReturn(userOne, userTwo);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();

        Mockito.doReturn(oneResponse).when(userTwo).info();

        // exercise
        this.openNebulaClientFactory.getUser(userPool, userName);
    }

    // test case : success case
    @Test
    public void testCreateSecurityGroup() throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        String securityGroupIp = "securityGroupId";

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.FALSE).when(oneResponse).isError();

        SecurityGroup securityGroupExpected = Mockito.mock(SecurityGroup.class);
        Mockito.doReturn(oneResponse).when(securityGroupExpected).info();

        Mockito.doReturn(securityGroupExpected).when(this.openNebulaClientFactory).generateOnePoolElement(Mockito.eq(client), Mockito.eq(securityGroupIp), Mockito.eq(SecurityGroup.class));

        // exercise
        SecurityGroup securityGroup = this.openNebulaClientFactory.createSecurityGroup(client, securityGroupIp);

        // verify
        Assert.assertEquals(securityGroupExpected, securityGroup);
    }

    // test case : throw UnauthorizedRequestException exception
    @Test(expected = UnauthorizedRequestException.class)
    public void testCreateSecurityGroupUnauthorizedRequestException() throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        String securityGroupIp = "securityGroupId";

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        Mockito.doReturn(OpenNebulaClientFactory.RESPONSE_NOT_AUTHORIZED).when(oneResponse).getErrorMessage();

        SecurityGroup securityGroupExpected = Mockito.mock(SecurityGroup.class);
        Mockito.doReturn(oneResponse).when(securityGroupExpected).info();

        Mockito.doReturn(securityGroupExpected).when(this.openNebulaClientFactory).generateOnePoolElement(Mockito.eq(client), Mockito.eq(securityGroupIp), Mockito.eq(SecurityGroup.class));

        // exercise
        this.openNebulaClientFactory.createSecurityGroup(client, securityGroupIp);
    }

    // test case : throw InstanceNotFoundException exception
    @Test(expected = InstanceNotFoundException.class)
    public void testCreateSecurityGroupInstanceNotFoundException() throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        String securityGroupIp = "securityGroupId";

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        String anything = "";
        Mockito.doReturn(anything).when(oneResponse).getErrorMessage();

        SecurityGroup securityGroupExpected = Mockito.mock(SecurityGroup.class);
        Mockito.doReturn(oneResponse).when(securityGroupExpected).info();

        Mockito.doReturn(securityGroupExpected).when(this.openNebulaClientFactory).generateOnePoolElement(Mockito.eq(client), Mockito.eq(securityGroupIp), Mockito.eq(SecurityGroup.class));

        // exercise
        this.openNebulaClientFactory.createSecurityGroup(client, securityGroupIp);
    }

    // test case : success case
    @Test
    public void testCreateVirtualMachine() throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        String virtualMachineIp = "virtualMachineId";

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.FALSE).when(oneResponse).isError();

        VirtualMachine virtualMachineExpected = Mockito.mock(VirtualMachine.class);
        Mockito.doReturn(oneResponse).when(virtualMachineExpected).info();

        Mockito.doReturn(virtualMachineExpected).when(this.openNebulaClientFactory).generateOnePoolElement(Mockito.eq(client), Mockito.eq(virtualMachineIp), Mockito.eq(VirtualMachine.class));

        // exercise
        VirtualMachine virtualMachine = this.openNebulaClientFactory.createVirtualMachine(client, virtualMachineIp);

        // verify
        Assert.assertEquals(virtualMachineExpected, virtualMachine);
    }

    // test case : throw UnauthorizedRequestException exception
    @Test(expected = UnauthorizedRequestException.class)
    public void testCreateVirtualMachineUnauthorizedRequestException() throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        String virtualMachineIp = "virtualMachineId";

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        Mockito.doReturn(OpenNebulaClientFactory.RESPONSE_NOT_AUTHORIZED).when(oneResponse).getErrorMessage();

        VirtualMachine virtualMachineExpected = Mockito.mock(VirtualMachine.class);
        Mockito.doReturn(oneResponse).when(virtualMachineExpected).info();

        Mockito.doReturn(virtualMachineExpected).when(this.openNebulaClientFactory).generateOnePoolElement(Mockito.eq(client), Mockito.eq(virtualMachineIp), Mockito.eq(VirtualMachine.class));

        // exercise
        this.openNebulaClientFactory.createVirtualMachine(client, virtualMachineIp);
    }

    // test case : throw InstanceNotFoundException exception
    @Test(expected = InstanceNotFoundException.class)
    public void testCreateVirtualMachineInstanceNotFoundException() throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        String virtualMachineIp = "virtualMachineId";

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        String anything = "";
        Mockito.doReturn(anything).when(oneResponse).getErrorMessage();

        VirtualMachine virtualMachineExpected = Mockito.mock(VirtualMachine.class);
        Mockito.doReturn(oneResponse).when(virtualMachineExpected).info();

        Mockito.doReturn(virtualMachineExpected).when(this.openNebulaClientFactory).generateOnePoolElement(Mockito.eq(client), Mockito.eq(virtualMachineIp), Mockito.eq(VirtualMachine.class));

        // exercise
        this.openNebulaClientFactory.createVirtualMachine(client, virtualMachineIp);
    }

    // test case : success case
    @Test
    public void testCreateVirtualNetwork() throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        String virtualNetworkIp = "virtualNetworkId";

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.FALSE).when(oneResponse).isError();

        VirtualNetwork virtualNetworkExpected = Mockito.mock(VirtualNetwork.class);
        Mockito.doReturn(oneResponse).when(virtualNetworkExpected).info();

        Mockito.doReturn(virtualNetworkExpected).when(this.openNebulaClientFactory).generateOnePoolElement(Mockito.eq(client), Mockito.eq(virtualNetworkIp), Mockito.eq(VirtualNetwork.class));

        // exercise
        VirtualNetwork virtualNetwork = this.openNebulaClientFactory.createVirtualNetwork(client, virtualNetworkIp);

        // verify
        Assert.assertEquals(virtualNetworkExpected, virtualNetwork);
    }

    // test case : throw UnauthorizedRequestException exception
    @Test(expected = UnauthorizedRequestException.class)
    public void testCreateVirtualNetworkUnauthorizedRequestException() throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        String virtualNetworkIp = "virtualNetworkId";

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        Mockito.doReturn(OpenNebulaClientFactory.RESPONSE_NOT_AUTHORIZED).when(oneResponse).getErrorMessage();

        VirtualNetwork virtualNetworkExpected = Mockito.mock(VirtualNetwork.class);
        Mockito.doReturn(oneResponse).when(virtualNetworkExpected).info();

        Mockito.doReturn(virtualNetworkExpected).when(this.openNebulaClientFactory).generateOnePoolElement(Mockito.eq(client), Mockito.eq(virtualNetworkIp), Mockito.eq(VirtualNetwork.class));

        // exercise
        this.openNebulaClientFactory.createVirtualNetwork(client, virtualNetworkIp);
    }

    // test case : throw InstanceNotFoundException exception
    @Test(expected = InstanceNotFoundException.class)
    public void testCreateVirtualNetworkInstanceNotFoundException() throws UnexpectedException, UnauthorizedRequestException, InstanceNotFoundException, InvalidParameterException {
        // set up
        Client client = this.openNebulaClientFactory.createClient("");
        String virtualNetworkIp = "virtualNetworkId";

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        String anything = "";
        Mockito.doReturn(anything).when(oneResponse).getErrorMessage();

        VirtualNetwork virtualNetworkExpected = Mockito.mock(VirtualNetwork.class);
        Mockito.doReturn(oneResponse).when(virtualNetworkExpected).info();

        Mockito.doReturn(virtualNetworkExpected).when(this.openNebulaClientFactory).generateOnePoolElement(Mockito.eq(client), Mockito.eq(virtualNetworkIp), Mockito.eq(VirtualNetwork.class));

        // exercise
        this.openNebulaClientFactory.createVirtualNetwork(client, virtualNetworkIp);
    }

    // test case: success case
    @Test
    public void testAllocateImage() throws InvalidParameterException {
        // exercise
        String template = "";
        int datastoreId = 10;
        String messageExpected = "message";
        int indMessageExpected = 1;
        PowerMockito.mockStatic(Image.class);

        Client client = Mockito.mock(Client.class);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.FALSE).when(oneResponse).isError();
        Mockito.doReturn(messageExpected).when(oneResponse).getMessage();
        Mockito.doReturn(indMessageExpected).when(oneResponse).getIntMessage();

        PowerMockito.when(Image.allocate(Mockito.eq(client), Mockito.eq(template), Mockito.eq(datastoreId))).thenReturn(oneResponse);

        // exercise
        String message = this.openNebulaClientFactory.allocateImage(client, template, datastoreId);

        // verify
        Assert.assertEquals(messageExpected, message);
        PowerMockito.verifyStatic(Image.class, Mockito.times(1));
        Image.chmod(Mockito.eq(client), Mockito.eq(indMessageExpected), Mockito.eq(OpenNebulaClientFactory.CHMOD_PERMISSION_744));
    }

    // test case: throw InvalidParameterException
    @Test(expected = InvalidParameterException.class)
    public void testAllocateImageInvalidParameterException() throws InvalidParameterException {
        // exercise
        String template = "";
        int datastoreId = 10;
        String messageExpected = "message";
        int indMessageExpected = 1;
        PowerMockito.mockStatic(Image.class);

        Client client = Mockito.mock(Client.class);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        Mockito.doReturn(messageExpected).when(oneResponse).getMessage();
        Mockito.doReturn(indMessageExpected).when(oneResponse).getIntMessage();

        PowerMockito.when(Image.allocate(Mockito.eq(client), Mockito.eq(template), Mockito.eq(datastoreId))).thenReturn(oneResponse);

        // exercise
        this.openNebulaClientFactory.allocateImage(client, template, datastoreId);
    }

    // test case: success case
    @Test
    public void testAllocateSecurityGroup() throws InvalidParameterException {
        // exercise
        String template = "";
        String messageExpected = "message";
        int indMessageExpected = 1;
        PowerMockito.mockStatic(SecurityGroup.class);

        Client client = Mockito.mock(Client.class);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.FALSE).when(oneResponse).isError();
        Mockito.doReturn(messageExpected).when(oneResponse).getMessage();
        Mockito.doReturn(indMessageExpected).when(oneResponse).getIntMessage();

        PowerMockito.when(SecurityGroup.allocate(Mockito.eq(client), Mockito.eq(template))).thenReturn(oneResponse);

        // exercise
        String message = this.openNebulaClientFactory.allocateSecurityGroup(client, template);

        // verify
        Assert.assertEquals(messageExpected, message);
        PowerMockito.verifyStatic(Image.class, Mockito.times(1));
        SecurityGroup.chmod(Mockito.eq(client), Mockito.eq(indMessageExpected), Mockito.eq(OpenNebulaClientFactory.CHMOD_PERMISSION_744));
    }

    // test case: throw InvalidParameterException
    @Test(expected = InvalidParameterException.class)
    public void testAllocateSecurityGroupInvalidParameterException() throws InvalidParameterException {
        // exercise
        String template = "";
        String messageExpected = "message";
        int indMessageExpected = 1;
        PowerMockito.mockStatic(SecurityGroup.class);

        Client client = Mockito.mock(Client.class);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        Mockito.doReturn(messageExpected).when(oneResponse).getMessage();
        Mockito.doReturn(indMessageExpected).when(oneResponse).getIntMessage();

        PowerMockito.when(SecurityGroup.allocate(Mockito.eq(client), Mockito.eq(template))).thenReturn(oneResponse);

        // exercise
        this.openNebulaClientFactory.allocateSecurityGroup(client, template);
    }

    // test case: success case
    @Test
    public void testAllocateVirtualNetwork() throws InvalidParameterException {
        // exercise
        String template = "";
        String messageExpected = "message";
        int indMessageExpected = 1;
        PowerMockito.mockStatic(VirtualNetwork.class);

        Client client = Mockito.mock(Client.class);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.FALSE).when(oneResponse).isError();
        Mockito.doReturn(messageExpected).when(oneResponse).getMessage();
        Mockito.doReturn(indMessageExpected).when(oneResponse).getIntMessage();

        PowerMockito.when(VirtualNetwork.allocate(Mockito.eq(client), Mockito.eq(template))).thenReturn(oneResponse);

        // exercise
        String message = this.openNebulaClientFactory.allocateVirtualNetwork(client, template);

        // verify
        Assert.assertEquals(messageExpected, message);
        PowerMockito.verifyStatic(Image.class, Mockito.times(1));
        VirtualNetwork.chmod(Mockito.eq(client), Mockito.eq(indMessageExpected), Mockito.eq(OpenNebulaClientFactory.CHMOD_PERMISSION_744));
    }

    // test case: throw InvalidParameterException
    @Test(expected = InvalidParameterException.class)
    public void testAllocateVirtualNetworkInvalidParameterException() throws InvalidParameterException {
        // exercise
        String template = "";
        String messageExpected = "message";
        int indMessageExpected = 1;
        PowerMockito.mockStatic(VirtualNetwork.class);

        Client client = Mockito.mock(Client.class);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        Mockito.doReturn(messageExpected).when(oneResponse).getMessage();
        Mockito.doReturn(indMessageExpected).when(oneResponse).getIntMessage();

        PowerMockito.when(VirtualNetwork.allocate(Mockito.eq(client), Mockito.eq(template))).thenReturn(oneResponse);

        // exercise
        this.openNebulaClientFactory.allocateVirtualNetwork(client, template);
    }

    // test case: success case
    @Test
    public void testAllocateVirtualMachine() throws InvalidParameterException, NoAvailableResourcesException, QuotaExceededException {
        // exercise
        String template = "";
        String messageExpected = "message";
        int indMessageExpected = 1;
        PowerMockito.mockStatic(VirtualMachine.class);

        Client client = Mockito.mock(Client.class);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.FALSE).when(oneResponse).isError();
        Mockito.doReturn(messageExpected).when(oneResponse).getMessage();
        Mockito.doReturn(indMessageExpected).when(oneResponse).getIntMessage();

        PowerMockito.when(VirtualMachine.allocate(Mockito.eq(client), Mockito.eq(template))).thenReturn(oneResponse);

        // exercise
        String message = this.openNebulaClientFactory.allocateVirtualMachine(client, template);

        // verify
        Assert.assertEquals(messageExpected, message);
        PowerMockito.verifyStatic(Image.class, Mockito.times(1));
        VirtualMachine.chmod(Mockito.eq(client), Mockito.eq(indMessageExpected), Mockito.eq(OpenNebulaClientFactory.CHMOD_PERMISSION_744));
    }

    // test case: throw InvalidParameterException
    @Test(expected = InvalidParameterException.class)
    public void testAllocateVirtualMachineInvalidParameterException() throws InvalidParameterException, NoAvailableResourcesException, QuotaExceededException {
        // exercise
        String template = "";
        String messageExpected = "message";
        int indMessageExpected = 1;
        PowerMockito.mockStatic(VirtualMachine.class);

        Client client = Mockito.mock(Client.class);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        Mockito.doReturn(messageExpected).when(oneResponse).getErrorMessage();
        Mockito.doReturn(indMessageExpected).when(oneResponse).getIntMessage();

        PowerMockito.when(VirtualMachine.allocate(Mockito.eq(client), Mockito.eq(template))).thenReturn(oneResponse);

        // exercise
        this.openNebulaClientFactory.allocateVirtualMachine(client, template);
    }

    // test case: throw QuotaExceededException
    @Test(expected = QuotaExceededException.class)
    public void testAllocateVirtualMachineQuotaExceededException() throws InvalidParameterException, NoAvailableResourcesException, QuotaExceededException {
        // exercise
        String template = "";
        String messageExpected = OpenNebulaClientFactory.FIELD_RESPONSE_LIMIT + "message" + OpenNebulaClientFactory.FIELD_RESPONSE_QUOTA;
        int indMessageExpected = 1;
        PowerMockito.mockStatic(VirtualMachine.class);

        Client client = Mockito.mock(Client.class);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        Mockito.doReturn(messageExpected).when(oneResponse).getErrorMessage();
        Mockito.doReturn(indMessageExpected).when(oneResponse).getIntMessage();

        PowerMockito.when(VirtualMachine.allocate(Mockito.eq(client), Mockito.eq(template))).thenReturn(oneResponse);

        // exercise
        this.openNebulaClientFactory.allocateVirtualMachine(client, template);
    }

    // test case: throw NoAvailableResourcesException
    @Test(expected = NoAvailableResourcesException.class)
    public void testAllocateVirtualMachineNoAvailableResourcesException() throws InvalidParameterException, NoAvailableResourcesException, QuotaExceededException {
        // exercise
        String template = "";
        String messageExpected = OpenNebulaClientFactory.RESPONSE_NOT_ENOUGH_FREE_MEMORY + "message";
        int indMessageExpected = 1;
        PowerMockito.mockStatic(VirtualMachine.class);

        Client client = Mockito.mock(Client.class);

        OneResponse oneResponse = Mockito.mock(OneResponse.class);
        Mockito.doReturn(Boolean.TRUE).when(oneResponse).isError();
        Mockito.doReturn(messageExpected).when(oneResponse).getErrorMessage();
        Mockito.doReturn(indMessageExpected).when(oneResponse).getIntMessage();

        PowerMockito.when(VirtualMachine.allocate(Mockito.eq(client), Mockito.eq(template))).thenReturn(oneResponse);

        // exercise
        this.openNebulaClientFactory.allocateVirtualMachine(client, template);
    }

}
