package org.fogbowcloud.manager.core.plugins.cloud.util;

import java.util.Properties;

import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.UserData;
import org.fogbowcloud.manager.core.models.tokens.FederationUserToken;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultLaunchCommandGeneratorTest {

    private Properties properties;

    private DefaultLaunchCommandGenerator launchCommandGenerator;

    private final static String MANAGER_PUBLIC_KEY_FILE_PATH = "src/test/resources/fake-manager-public-key";
    private final static String REVERSE_TUNNEL_PRIVATE_IP = "fake-private-ip";
    private final static String REVERSE_TUNNEL_HTTP_PORT = "fake-http-port";
    private final static String EXTRA_USER_DATA_FILE = "fake-extra-user-data-file";
    
    private CloudInitUserDataBuilder.FileType extraUserDataFileType =
            CloudInitUserDataBuilder.FileType.SHELL_SCRIPT;

    @Before
    public void setUp() throws Exception {
        HomeDir.getInstance().setPath("src/test/resources/private");
        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        this.properties.setProperty(
                ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, MANAGER_PUBLIC_KEY_FILE_PATH);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY,
                REVERSE_TUNNEL_PRIVATE_IP);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, REVERSE_TUNNEL_HTTP_PORT);
        this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
    }
    
    // test case: Check the creation of a not empty command from an order.
    @Test
    public void testCreateLaunchCommand() {
    	
    	// set up
        ComputeOrder order = this.createComputeOrder();
        
        // exercise
        String command = this.launchCommandGenerator.createLaunchCommand(order);
        
        // verify
        Assert.assertFalse(command.trim().isEmpty());
    }
    
    // test case: Check the creation of a not empty command from an order without public key.
    @Test
    public void testCreateLaunchCommandWithoutUserPublicKey() {
    	
    	// set up
        ComputeOrder order = this.createComputeOrderWithoutPublicKey();
        
        // exercise
        String command = this.launchCommandGenerator.createLaunchCommand(order);
        
        // verify
        Assert.assertFalse(command.trim().isEmpty());
    }
    
    // test case: Check the creation of a not empty command from an order without extra user data
    @Test
    public void testCreateLaunchCommandWithoutExtraUserData() {
    	
    	// set up
        ComputeOrder order = this.createComputeOrderWithoutExtraUserData();
        
        // exercise
        String command = this.launchCommandGenerator.createLaunchCommand(order);
        
        // verify
        Assert.assertFalse(command.trim().isEmpty());
    }
    
    // test case: Check the addition of extra user data.
    @Test
    public void testAddExtraUserData() {
    	
    	// set up
        CloudInitUserDataBuilder cloudInitUserDataBuilder = CloudInitUserDataBuilder.start();
        
        // exercise
        this.launchCommandGenerator.addExtraUserData(
                cloudInitUserDataBuilder, EXTRA_USER_DATA_FILE, this.extraUserDataFileType);
        
        // verify
        String userData = cloudInitUserDataBuilder.buildUserData();
        Assert.assertTrue(userData.contains(this.extraUserDataFileType.getMimeType()));
        Assert.assertTrue(userData.contains(EXTRA_USER_DATA_FILE));
    }
    
    // test case: Check the addition of extra user data with a null data file content. 
    // extra user data is added only if data file content and data file type are not null.
    @Test
    public void testAddExtraUserDataWithDataFileContentNull() {
    	
    	// set up
        CloudInitUserDataBuilder cloudInitUserDataBuilder = CloudInitUserDataBuilder.start();
        
        // exercise
        this.launchCommandGenerator.addExtraUserData(
                cloudInitUserDataBuilder, null, this.extraUserDataFileType);
        
        // verify
        String userData = cloudInitUserDataBuilder.buildUserData();
        Assert.assertFalse(userData.contains(this.extraUserDataFileType.getMimeType()));
        Assert.assertFalse(userData.contains(EXTRA_USER_DATA_FILE));
    }
    
    // test case: Check the addition of extra user data with a null data file type. 
    // extra user data is added only if data file content and data file type are not null.
    @Test
    public void testAddExtraUserDataWithDataFileTypeNull() {
    	
    	// set up
        CloudInitUserDataBuilder cloudInitUserDataBuilder = CloudInitUserDataBuilder.start();
        
        
        // exercise
        this.launchCommandGenerator.addExtraUserData(
                cloudInitUserDataBuilder, EXTRA_USER_DATA_FILE, null);

        // verify
        String userData = cloudInitUserDataBuilder.buildUserData();
        Assert.assertFalse(userData.contains(this.extraUserDataFileType.getMimeType()));
        Assert.assertFalse(userData.contains(EXTRA_USER_DATA_FILE));
    }
    
    
    // test case: Test the application of token replacements in mime string.
    @Test
    public void testApplyTokensReplacements() {
    	
    	// set up
        ComputeOrder order = this.createComputeOrder();

        String mimeString = "";
        String expectedMimeString = "";

        mimeString += DefaultLaunchCommandGenerator.TOKEN_HOST + System.lineSeparator();
        expectedMimeString += REVERSE_TUNNEL_PRIVATE_IP + System.lineSeparator();

        mimeString += DefaultLaunchCommandGenerator.TOKEN_HOST_HTTP_PORT + System.lineSeparator();
        expectedMimeString += REVERSE_TUNNEL_HTTP_PORT + System.lineSeparator();

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
        
        // exercise
        String replacedMimeString =
                this.launchCommandGenerator.applyTokensReplacements(order, mimeString);
        
        // verify
        Assert.assertEquals(expectedMimeString, replacedMimeString);
    }
    
    // test case: An exception must be thrown when the manager ssh public key file path is empty.   
    @Test(expected = FatalErrorException.class)
    public void testPropertiesWithoutManagerSshPublicKeyFilePath() throws Exception {
    	
    	// set up
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        this.properties.setProperty(
                ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, "");
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY,
                REVERSE_TUNNEL_PRIVATE_IP);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, REVERSE_TUNNEL_HTTP_PORT);
        
        // exercise
        new DefaultLaunchCommandGenerator();
    }
    
    // test case: An exception must be thrown when the reverse tunnel private address key is empty.
    @Test(expected = FatalErrorException.class)
    public void testPropertiesWithoutReverseTunnelPrivateAddress() throws Exception {
    	
    	// set up
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        this.properties.setProperty(
                ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, MANAGER_PUBLIC_KEY_FILE_PATH);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY, "");
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, REVERSE_TUNNEL_HTTP_PORT);
        
        // exercise
        new DefaultLaunchCommandGenerator();
    }
    
    
    // test case: An exception must be thrown when the reverse tunnel http port key is empty.
    @Test(expected = FatalErrorException.class)
    public void testPropertiesWithoutReverseTunnelHttpPort() throws Exception {
    	
    	// set up
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        this.properties.setProperty(
                ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, MANAGER_PUBLIC_KEY_FILE_PATH);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY,
                REVERSE_TUNNEL_PRIVATE_IP);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, "");
        
        // exercise
        new DefaultLaunchCommandGenerator();
    }
    
    // test case: The path to manager ssh public key doesn't exist, so a fatal error exception must be thrown
    @Test(expected = FatalErrorException.class)
    public void testPropertiesWithWrongManagerSshPublicKeyFilePath() throws FatalErrorException {
    	
    	// set up
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
        String emptyManagerPublicKeyFilePath = "src/test/resources/fake-empty-manager-public-key";
        
        this.properties.setProperty(
                ConfigurationConstants.MANAGER_SSH_PUBLIC_KEY_FILE_PATH, emptyManagerPublicKeyFilePath);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_PRIVATE_ADDRESS_KEY,
                REVERSE_TUNNEL_PRIVATE_IP);
        this.properties.setProperty(
                ConfigurationConstants.REVERSE_TUNNEL_HTTP_PORT_KEY, REVERSE_TUNNEL_HTTP_PORT);
        
        // exercise
        new DefaultLaunchCommandGenerator();
    }

    private ComputeOrder createComputeOrder() {
        FederationUserToken federationUserToken = Mockito.mock(FederationUserToken.class);
        UserData userData = new UserData(EXTRA_USER_DATA_FILE, this.extraUserDataFileType);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String publicKey = "fake-public-key";

        ComputeOrder localOrder =
                new ComputeOrder(
                        federationUserToken,
                        requestingMember,
                        providingMember,
                        8,
                        1024,
                        30,
                        imageName,
                        userData,
                        publicKey,
                        null);
        return localOrder;
    }

    private ComputeOrder createComputeOrderWithoutExtraUserData() {
        FederationUserToken federationUserToken = Mockito.mock(FederationUserToken.class);
        UserData userData = new UserData(null, null);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String publicKey = "fake-public-key";

        ComputeOrder localOrder =
                new ComputeOrder(
                        federationUserToken,
                        requestingMember,
                        providingMember,
                        8,
                        1024,
                        30,
                        imageName,
                        userData,
                        publicKey,
                        null);
        return localOrder;
    }

    private ComputeOrder createComputeOrderWithoutPublicKey() {
    	FederationUserToken federationUserToken = Mockito.mock(FederationUserToken.class);
        UserData userData = new UserData(EXTRA_USER_DATA_FILE, this.extraUserDataFileType);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));

        ComputeOrder localOrder =
                new ComputeOrder(
                        federationUserToken,
                        requestingMember,
                        providingMember,
                        8,
                        1024,
                        30,
                        imageName,
                        userData,
                        null,
                        null);
        return localOrder;
    }
    
}
