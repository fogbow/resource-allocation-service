package org.fogbowcloud.ras.core.plugins.interoperability.util;

import java.util.Properties;

import org.fogbowcloud.ras.core.PropertiesHolder;
import org.fogbowcloud.ras.core.constants.ConfigurationConstants;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.models.orders.ComputeOrder;
import org.fogbowcloud.ras.core.models.orders.UserData;
import org.fogbowcloud.ras.core.models.tokens.FederationUserToken;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DefaultLaunchCommandGeneratorTest {

    private Properties properties;

    private DefaultLaunchCommandGenerator launchCommandGenerator;

    private final static String EXTRA_USER_DATA_FILE = "fake-extra-user-data-file";

    private CloudInitUserDataBuilder.FileType extraUserDataFileType =
            CloudInitUserDataBuilder.FileType.SHELL_SCRIPT;

    @Before
    public void setUp() throws Exception {

        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.setProperty(ConfigurationConstants.XMPP_JID_KEY, "localidentity-member");
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

        mimeString += DefaultLaunchCommandGenerator.TOKEN_ID + System.lineSeparator();
        expectedMimeString += order.getId() + System.lineSeparator();

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

    private ComputeOrder createComputeOrder() {
        FederationUserToken federationUserToken = Mockito.mock(FederationUserToken.class);
        UserData userData = new UserData(EXTRA_USER_DATA_FILE, this.extraUserDataFileType);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationConstants.XMPP_JID_KEY));
        String publicKey = "fake-public-key";
        String instanceName = "fake-instance-name";

        ComputeOrder localOrder =
                new ComputeOrder(
                        federationUserToken,
                        requestingMember,
                        providingMember,
                        instanceName,
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
        String instanceName = "fake-instance-name";

        ComputeOrder localOrder =
                new ComputeOrder(
                        federationUserToken,
                        requestingMember,
                        providingMember,
                        instanceName,
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
        String instanceName = "fake-instance-name";

        ComputeOrder localOrder =
                new ComputeOrder(
                        federationUserToken,
                        requestingMember,
                        providingMember,
                        instanceName,
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
