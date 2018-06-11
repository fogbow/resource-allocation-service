package org.fogbowcloud.manager.core.plugins.cloud.compute;

import java.io.IOException;
import java.util.Properties;

import org.fogbowcloud.manager.core.exceptions.PropertyNotSpecifiedException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.token.FederationUser;
import org.fogbowcloud.manager.core.plugins.cloud.compute.util.CloudInitUserDataBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultLaunchCommandGeneratorTest {

    private Properties properties;

    private DefaultLaunchCommandGenerator launchCommandGenerator;

    private String managerPublicKeyFilePath = "src/test/resources/fake-manager-public-key";
    private String reverseTunnelPrivateIp = "fake-private-ip";
    private String reverseTunnelHttpPort = "fake-http-port";

    private String extraUserDataFile = "fake-extra-user-data-file";
    private CloudInitUserDataBuilder.FileType extraUserDataFileType =
            CloudInitUserDataBuilder.FileType.SHELL_SCRIPT;

    @Before
    public void setUp() throws Exception {
        HomeDir.getInstance().setPath("src/test/resources/private");
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        this.properties.setProperty(
                ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, this.managerPublicKeyFilePath);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY,
                this.reverseTunnelPrivateIp);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, this.reverseTunnelHttpPort);
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
    }

    @Test
    public void testCreateLaunchCommand() {
        ComputeOrder order = this.createComputeOrder();
        String command = this.launchCommandGenerator.createLaunchCommand(order);

        Assert.assertFalse(command.trim().isEmpty());
    }

    @Test
    public void testCreateLaunchCommandWithoutUserPublicKey() {
        ComputeOrder order = this.createComputeOrderWithoutPublicKey();
        String command = this.launchCommandGenerator.createLaunchCommand(order);

        Assert.assertFalse(command.trim().isEmpty());
    }

    @Test
    public void testCreateLaunchCommandWithoutExtraUserData() {
        ComputeOrder order = this.createComputeOrderWithoutExtraUserData();
        String command = this.launchCommandGenerator.createLaunchCommand(order);

        Assert.assertFalse(command.trim().isEmpty());
    }

    @Test
    public void testAddExtraUserData() {
        CloudInitUserDataBuilder cloudInitUserDataBuilder = CloudInitUserDataBuilder.start();

        this.launchCommandGenerator.addExtraUserData(
                cloudInitUserDataBuilder, this.extraUserDataFile, this.extraUserDataFileType);

        String userData = cloudInitUserDataBuilder.buildUserData();

        Assert.assertTrue(userData.contains(this.extraUserDataFileType.getMimeType()));
        Assert.assertTrue(userData.contains(this.extraUserDataFile));
    }

    @Test
    public void testAddExtraUserDataWithDataFileNull() {
        CloudInitUserDataBuilder cloudInitUserDataBuilder = CloudInitUserDataBuilder.start();

        this.launchCommandGenerator.addExtraUserData(
                cloudInitUserDataBuilder, null, this.extraUserDataFileType);

        String userData = cloudInitUserDataBuilder.buildUserData();

        Assert.assertFalse(userData.contains(this.extraUserDataFileType.getMimeType()));
        Assert.assertFalse(userData.contains(this.extraUserDataFile));
    }

    @Test
    public void testAddExtraUserDataWithDataFileTypeNull() {
        CloudInitUserDataBuilder cloudInitUserDataBuilder = CloudInitUserDataBuilder.start();

        this.launchCommandGenerator.addExtraUserData(
                cloudInitUserDataBuilder, this.extraUserDataFile, null);

        String userData = cloudInitUserDataBuilder.buildUserData();

        Assert.assertFalse(userData.contains(this.extraUserDataFileType.getMimeType()));
        Assert.assertFalse(userData.contains(this.extraUserDataFile));
    }

    @Test
    public void testApplyTokensReplacements() {
        ComputeOrder order = this.createComputeOrder();

        String mimeString = "";
        String expectedMimeString = "";

        mimeString += DefaultLaunchCommandGenerator.TOKEN_HOST + System.lineSeparator();
        expectedMimeString += this.reverseTunnelPrivateIp + System.lineSeparator();

        mimeString += DefaultLaunchCommandGenerator.TOKEN_HOST_HTTP_PORT + System.lineSeparator();
        expectedMimeString += this.reverseTunnelHttpPort + System.lineSeparator();

        mimeString += DefaultLaunchCommandGenerator.TOKEN_HOST_SSH_PORT + System.lineSeparator();
        expectedMimeString +=
                DefaultLaunchCommandGenerator.DEFAULT_SSH_HOST_PORT + System.lineSeparator();

        mimeString += DefaultLaunchCommandGenerator.TOKEN_ID + System.lineSeparator();
        expectedMimeString += order.getId() + System.lineSeparator();

        mimeString +=
                DefaultLaunchCommandGenerator.TOKEN_MANAGER_SSH_PUBLIC_KEY + System.lineSeparator();
        expectedMimeString += "fake-manager-public-key" + System.lineSeparator();

        mimeString += DefaultLaunchCommandGenerator.TOKEN_SSH_USER + System.lineSeparator();
        expectedMimeString +=
                DefaultConfigurationConstants.SSH_COMMON_USER + System.lineSeparator();

        mimeString +=
                DefaultLaunchCommandGenerator.TOKEN_USER_SSH_PUBLIC_KEY + System.lineSeparator();
        expectedMimeString += order.getPublicKey() + System.lineSeparator();

        String replacedMimeString =
                this.launchCommandGenerator.applyTokensReplacements(order, mimeString);

        Assert.assertEquals(expectedMimeString, replacedMimeString);
    }

    @Test(expected = PropertyNotSpecifiedException.class)
    public void testPropertiesWithoutManagerSshPublicKeyFilePath() throws Exception {
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY,
                this.reverseTunnelPrivateIp);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, this.reverseTunnelHttpPort);
        new DefaultLaunchCommandGenerator();
    }

    @Test(expected = PropertyNotSpecifiedException.class)
    public void testPropertiesWithoutReverseTunnelPrivateAddress() throws Exception {
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        this.properties.setProperty(
                ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, this.managerPublicKeyFilePath);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, this.reverseTunnelHttpPort);
        new DefaultLaunchCommandGenerator();
    }

    @Test(expected = PropertyNotSpecifiedException.class)
    public void testPropertiesWithoutReverseTunnelHttpPort() throws Exception {
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        this.properties.setProperty(
                ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, this.managerPublicKeyFilePath);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY,
                this.reverseTunnelPrivateIp);
        new DefaultLaunchCommandGenerator();
    }

    @Test(expected = PropertyNotSpecifiedException.class)
    public void testManagerSshPublicKeyPathEmpty()
            throws PropertyNotSpecifiedException, IOException {
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        this.properties.setProperty(ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, "");
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY,
                this.reverseTunnelPrivateIp);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, this.reverseTunnelHttpPort);
        new DefaultLaunchCommandGenerator();
    }

    @Test(expected = PropertyNotSpecifiedException.class)
    public void testManagerSshPublicKeyEmpty() throws PropertyNotSpecifiedException, IOException {
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        String emptyManagerPublicKeyFilePath = "src/test/resources/fake-empty-manager-public-key";
        this.properties.setProperty(
                ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, emptyManagerPublicKeyFilePath);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY,
                this.reverseTunnelPrivateIp);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, this.reverseTunnelHttpPort);
        new DefaultLaunchCommandGenerator();
    }

    @Test(expected = PropertyNotSpecifiedException.class)
    public void testReverseTunnelPrivateIpEmpty()
            throws PropertyNotSpecifiedException, IOException {
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        this.properties.setProperty(
                ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, this.managerPublicKeyFilePath);
        this.properties.setProperty(ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY, "");
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, this.reverseTunnelHttpPort);
        new DefaultLaunchCommandGenerator();
    }

    @Test(expected = PropertyNotSpecifiedException.class)
    public void testReverseHttpPortIpEmpty() throws PropertyNotSpecifiedException, IOException {
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        this.properties.setProperty(
                ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, this.managerPublicKeyFilePath);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY,
                this.reverseTunnelPrivateIp);
        this.properties.setProperty(ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, "");
        new DefaultLaunchCommandGenerator();
    }

    private ComputeOrder createComputeOrder() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        UserData userData = new UserData(this.extraUserDataFile, this.extraUserDataFileType);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String publicKey = "fake-public-key";

        ComputeOrder localOrder =
                new ComputeOrder(
                        federationUser,
                        requestingMember,
                        providingMember,
                        8,
                        1024,
                        30,
                        imageName,
                        userData,
                        publicKey);
        return localOrder;
    }

    private ComputeOrder createComputeOrderWithoutExtraUserData() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        UserData userData = new UserData(null, null);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String publicKey = "fake-public-key";

        ComputeOrder localOrder =
                new ComputeOrder(
                		federationUser,
                        requestingMember,
                        providingMember,
                        8,
                        1024,
                        30,
                        imageName,
                        userData,
                        publicKey);
        return localOrder;
    }

    private ComputeOrder createComputeOrderWithoutPublicKey() {
    	FederationUser federationUser = Mockito.mock(FederationUser.class);
        UserData userData = new UserData(this.extraUserDataFile, this.extraUserDataFileType);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));

        ComputeOrder localOrder =
                new ComputeOrder(
                		federationUser,
                        requestingMember,
                        providingMember,
                        8,
                        1024,
                        30,
                        imageName,
                        userData,
                        null);
        return localOrder;
    }
    
}
