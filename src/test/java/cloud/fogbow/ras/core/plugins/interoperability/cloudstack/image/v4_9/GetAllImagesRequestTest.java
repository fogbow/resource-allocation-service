package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.image.v4_9;

import cloud.fogbow.common.constants.CloudStackConstants;
import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Assert;
import org.junit.Test;

public class GetAllImagesRequestTest {

    // test case: When calling the build method with imageId parameter filled,
    // it must verify if It generates the right URL.
    @Test
    public void testBuildWithParameterImageId() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.Image.LIST_TEMPLATES_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String imageId = "imageId";
        String executableTemplatesValue = CloudStackConstants.Image.EXECUTABLE_TEMPLATES_VALUE;

        String imageIdStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Image.ID_KEY_JSON, imageId);
        String executableTemplatesValueStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Image.TEMPLATE_FILTER_KEY_JSON, executableTemplatesValue);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                executableTemplatesValueStructureUrl,
                imageIdStructureUrl
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        GetAllImagesRequest getAllImagesRequest = new GetAllImagesRequest.Builder()
                .id(imageId)
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String getAllImagesRequestUrl = getAllImagesRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, getAllImagesRequestUrl);
    }

    // test case: When calling the build method with imageId parameter empty,
    // it must verify if It generates the right URL.
    @Test
    public void testBuildWithParameterImageIdEmpty() throws InvalidParameterException {
        // set up
        URIBuilder uriBuilder = CloudStackUrlUtil.createURIBuilder(
                CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT,
                CloudStackConstants.Image.LIST_TEMPLATES_COMMAND);
        String urlBaseExpected = uriBuilder.toString();
        String executableTemplatesValue = CloudStackConstants.Image.EXECUTABLE_TEMPLATES_VALUE;

        String executableTemplatesValueStructureUrl = String.format(
                "%s=%s", CloudStackConstants.Image.TEMPLATE_FILTER_KEY_JSON, executableTemplatesValue);

        String[] urlStructure = new String[] {
                urlBaseExpected,
                executableTemplatesValueStructureUrl
        };
        String urlExpectedStr = String.join(
                CloudstackTestUtils.AND_OPERATION_URL_PARAMETER, urlStructure);

        // exercise
        GetAllImagesRequest getAllImagesRequest = new GetAllImagesRequest.Builder()
                .build(CloudstackTestUtils.CLOUDSTACK_URL_DEFAULT);
        String getAllImagesRequestUrl = getAllImagesRequest.getUriBuilder().toString();

        // verify
        Assert.assertEquals(urlExpectedStr, getAllImagesRequestUrl);
    }

    // test case: When calling the build method with a null parameter,
    // it must verify if It throws an InvalidParameterException.
    @Test(expected = InvalidParameterException.class)
    public void testBuildFail() throws InvalidParameterException {
        // exercise and verify
        new GetAllImagesRequest.Builder().build(null);
    }

}
