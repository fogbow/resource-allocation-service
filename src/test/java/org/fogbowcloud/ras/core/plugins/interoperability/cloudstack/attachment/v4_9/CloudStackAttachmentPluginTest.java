package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.attachment.v4_9;

import java.io.File;
import java.util.Properties;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.utils.URIBuilder;
import org.fogbowcloud.ras.core.HomeDir;
import org.fogbowcloud.ras.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.ras.core.exceptions.FogbowRasException;
import org.fogbowcloud.ras.core.exceptions.UnexpectedException;
import org.fogbowcloud.ras.core.models.orders.AttachmentOrder;
import org.fogbowcloud.ras.core.models.tokens.CloudStackToken;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackUrlUtil;
import org.fogbowcloud.ras.util.PropertiesUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.ras.util.connectivity.HttpRequestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudStackUrlUtil.class, HttpRequestUtil.class})
public class CloudStackAttachmentPluginTest {

    private static final String FAKE_USER_ATTRIBUTES = "fake-apikey:fake-secretKey";
    private static final String REQUEST_FORMAT = "%s?command=%s";
    private static final String ID_FIELD = "&id=%s";
    private static final String VM_ID_FIELD = "&virtualmachineid=%s";
    private static final String BASE_ENDPOINT_KEY = "cloudstack_api_url";
    private static final String ATTACHMENT_ID_FORMAT = "%s %s";
    private static final String FAKE_VOLUME_ID = "fake-volume-id";
    private static final String FAKE_JOB_ID = "fake-job-id";
    private static final String FAKE_MEMBER = "fake-member";
    private static final String FAKE_VIRTUAL_MACHINE_ID = "fake-virtual-machine-id";

    private CloudStackAttachmentPlugin plugin;
    private HttpRequestClientUtil client;
    private CloudStackToken token;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(HttpRequestUtil.class);

        this.client = Mockito.mock(HttpRequestClientUtil.class);
        this.plugin = new CloudStackAttachmentPlugin();
        this.plugin.setClient(this.client);
        this.token = new CloudStackToken(FAKE_USER_ATTRIBUTES);
    }

    // test case: When calling the requestInstance method a HTTP GET request must be made with a
    // signed token, returning the id of the Attachment.
    @Test
    public void testAttachRequestInstanceSuccessful() throws HttpResponseException, FogbowRasException, UnexpectedException {
        // set up
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito
                .when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(), Mockito.anyString()))
                .thenCallRealMethod();

        String urlFormat = REQUEST_FORMAT + ID_FIELD + VM_ID_FIELD;
        String baseEndpoint = getBaseEndpointFromCloudStackConf();
        String command = AttachVolumeRequest.ATTACH_VOLUME_COMMAND;
        String id = FAKE_VOLUME_ID;
        String virtualMachineId = FAKE_VIRTUAL_MACHINE_ID;
        String request = String.format(urlFormat, baseEndpoint, command, id, virtualMachineId);
        String response = getAttachVolumeResponse(FAKE_JOB_ID);

        Mockito.when(this.client.doGetRequest(request, this.token)).thenReturn(response);

        // exercise
        AttachmentOrder order = new AttachmentOrder(null, FAKE_MEMBER, FAKE_MEMBER,
                FAKE_VIRTUAL_MACHINE_ID, FAKE_VOLUME_ID, null);
        
        String volumeId = this.plugin.requestInstance(order, this.token);

        // verify
        PowerMockito.verifyStatic(CloudStackUrlUtil.class, VerificationModeFactory.times(1));
        CloudStackUrlUtil.sign(Mockito.any(URIBuilder.class), Mockito.anyString());

        Mockito.verify(this.client, Mockito.times(1)).doGetRequest(Mockito.eq(request),
                Mockito.eq(this.token));

        String expectedId = String.format(ATTACHMENT_ID_FORMAT, FAKE_VOLUME_ID, FAKE_JOB_ID);
        Assert.assertEquals(expectedId, volumeId);
    }

    private String getAttachVolumeResponse(String jobId) {
        String response = "{\"attachvolumeresponse\":{" 
                + "\"jobid\": \"%s\"" 
                + "}}";

        return String.format(response, jobId);
    }

    private String getBaseEndpointFromCloudStackConf() {
        String filePath = HomeDir.getPath() + File.separator
                + DefaultConfigurationConstants.CLOUDSTACK_CONF_FILE_NAME;

        Properties properties = PropertiesUtil.readProperties(filePath);
        return properties.getProperty(BASE_ENDPOINT_KEY);
    }

}
