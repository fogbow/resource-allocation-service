package org.fogbowcloud.ras.core.plugins.interoperability.opennebula;

import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.SystemConstants;
import org.fogbowcloud.ras.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.exceptions.UnauthorizedRequestException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
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
import org.opennebula.client.secgroup.SecurityGroup;
import org.opennebula.client.vnet.VirtualNetwork;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Image.class, SecurityGroup.class, VirtualNetwork.class})
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
        int groupId = 0;

        // exercise
        this.openNebulaClientFactory.createGroup(client, groupId);
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

}
