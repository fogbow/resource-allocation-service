package cloud.fogbow.ras.core.plugins.interoperability.aws.securityrule.v2;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.aws.AwsV2ClientUtil;
import cloud.fogbow.ras.core.plugins.interoperability.aws.image.v2.AwsV2ImagePlugin;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.awssdk.services.ec2.Ec2Client;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AwsV2ClientUtil.class})
public class AwsV2SecurityRuleTest {

    private static final String CLOUD_NAME = "amazon";

    private AwsV2SecurityRulePlugin plugin;

    @Before
    public void setup() {
        String awsConfFilePath = HomeDir.getPath()
                + SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME
                + File.separator
                + CLOUD_NAME
                + File.separator
                + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

        this.plugin = Mockito.spy(new AwsV2SecurityRulePlugin(awsConfFilePath));
    }

    @Test
    public void requestSecurityRule() throws FogbowException {
        // setup
        Ec2Client client = Mockito.mock(Ec2Client.class);
        PowerMockito.mockStatic(AwsV2ClientUtil.class);
        BDDMockito.given(AwsV2ClientUtil.createEc2Client(Mockito.anyString(), Mockito.anyString())).willReturn(client);


    }

    @Test
    public void getSecurityRules() {

    }

    @Test
    public void deleteSecurityRule() {

    }
}
