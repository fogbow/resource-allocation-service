package cloud.fogbow.ras.core.plugins.interoperability.util;

import cloud.fogbow.common.models.FederationUser;
import cloud.fogbow.ras.constants.ConfigurationPropertyDefaults;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Properties;

public class DefaultLaunchCommandGeneratorTest {

    private Properties properties;

    private DefaultLaunchCommandGenerator launchCommandGenerator;

    private static final String EXTRA_USER_DATA_FILE = "fake-extra-user-data-file";

    private static final CloudInitUserDataBuilder.FileType EXTRA_USER_DATA_FILE_TYPE =
            CloudInitUserDataBuilder.FileType.SHELL_SCRIPT;

    @Before
    public void setUp() throws Exception {

        PropertiesHolder propertiesHolder = PropertiesHolder.getInstance();
        this.properties = propertiesHolder.getProperties();
        this.properties.setProperty(ConfigurationPropertyKeys.XMPP_JID_KEY, "localidentity-member");
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
                cloudInitUserDataBuilder, EXTRA_USER_DATA_FILE, this.EXTRA_USER_DATA_FILE_TYPE);

        // verify
        String userData = cloudInitUserDataBuilder.buildUserData();
        Assert.assertTrue(userData.contains(this.EXTRA_USER_DATA_FILE_TYPE.getMimeType()));
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
                cloudInitUserDataBuilder, null, this.EXTRA_USER_DATA_FILE_TYPE);

        // verify
        String userData = cloudInitUserDataBuilder.buildUserData();
        Assert.assertFalse(userData.contains(this.EXTRA_USER_DATA_FILE_TYPE.getMimeType()));
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
        Assert.assertFalse(userData.contains(this.EXTRA_USER_DATA_FILE_TYPE.getMimeType()));
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
                ConfigurationPropertyDefaults.SSH_COMMON_USER + System.lineSeparator();

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
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationPropertyKeys.XMPP_JID_KEY));
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationPropertyKeys.XMPP_JID_KEY));
        String publicKey = "fake-public-key";
        String instanceName = "fake-instance-name";

        ComputeOrder localOrder =
                new ComputeOrder(
                        federationUser,
                        requestingMember,
                        providingMember,
                        "default", instanceName,
                        8,
                        1024,
                        30,
                        imageName,
                        createExampleUserData(),
                        publicKey,
                        null);
        return localOrder;
    }

    private ComputeOrder createComputeOrderWithoutExtraUserData() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationPropertyKeys.XMPP_JID_KEY));
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationPropertyKeys.XMPP_JID_KEY));
        String publicKey = "fake-public-key";
        String instanceName = "fake-instance-name";

        ComputeOrder localOrder =
                new ComputeOrder(
                        federationUser,
                        requestingMember,
                        providingMember,
                        "default", instanceName,
                        8,
                        1024,
                        30,
                        imageName,
                        null,
                        publicKey,
                        null);
        return localOrder;
    }

    private ComputeOrder createComputeOrderWithoutPublicKey() {
        FederationUser federationUser = Mockito.mock(FederationUser.class);
        String imageName = "fake-image-name";
        String requestingMember =
                String.valueOf(this.properties.get(ConfigurationPropertyKeys.XMPP_JID_KEY));
        String providingMember =
                String.valueOf(this.properties.get(ConfigurationPropertyKeys.XMPP_JID_KEY));
        String instanceName = "fake-instance-name";

        ComputeOrder localOrder =
                new ComputeOrder(
                        federationUser,
                        requestingMember,
                        providingMember,
                        "default", instanceName,
                        8,
                        1024,
                        30,
                        imageName,
                        createExampleUserData(),
                        null,
                        null);
        return localOrder;
    }

    private ArrayList<UserData> createExampleUserData() {
        UserData userDataScript = new UserData(EXTRA_USER_DATA_FILE, EXTRA_USER_DATA_FILE_TYPE, "fake-tag");
        ArrayList<UserData> userDataScripts = new ArrayList<UserData>();
        userDataScripts.add(userDataScript);
        return userDataScripts;
    }

}
