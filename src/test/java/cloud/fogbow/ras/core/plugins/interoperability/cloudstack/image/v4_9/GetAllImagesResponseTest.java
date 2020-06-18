package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.image.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class GetAllImagesResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the fromJson method, it must verify if It returns the right GetAllImagesResponse.
    @Test
    public void testFromJsonSuccessfully() throws Exception {
        // set up
        String id = "1";
        String name = "name";
        int size = 1;
        String getCreateNetworkResponseJson = CloudstackTestUtils.createGetAllImagesResponseJson(id, name, size);

        // execute
        GetAllImagesResponse getAllImagesResponse =
                GetAllImagesResponse.fromJson(getCreateNetworkResponseJson);

        // verify
        GetAllImagesResponse.Image image = getAllImagesResponse.getImages().get(0);
        Assert.assertEquals(id, image.getId());
        Assert.assertEquals(name, image.getName());
        Assert.assertEquals(size, image.getSize());
    }

    // test case: When calling the fromJson method with empty json response,
    // it must verify if It returns the GetAllImagesResponse with empty image list.
    @Test
    public void testFromJsonWithEmptyList() throws Exception {
        // set up
        String getAllImagesEmptyResponseJson = CloudstackTestUtils.createGetAllImagesEmptyResponseJson();

        // execute
        GetAllImagesResponse getAllImagesResponse =
                GetAllImagesResponse.fromJson(getAllImagesEmptyResponseJson);

        // verify
        Assert.assertTrue(getAllImagesResponse.getImages().isEmpty());
    }

    // test case: When calling the fromJson method with error json response,
    // it must verify if It throws a FogbowException.
    @Test
    public void testFromJsonFail() throws IOException, FogbowException {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String getAllImagesErrorResponseJson = CloudstackTestUtils
                .createGetAllImagesErrorResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        GetAllImagesResponse.fromJson(getAllImagesErrorResponseJson);
    }

}
