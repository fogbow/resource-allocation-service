package org.fogbowcloud.manager.requests.api.local.http;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.fogbowcloud.manager.api.http.ImageRequestController;
import org.fogbowcloud.manager.core.ApplicationFacade;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InstanceNotFoundException;
import org.fogbowcloud.manager.core.models.images.Image;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = ImageRequestController.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class ImageRequestControllerTest {

    private final String IMAGE_ENDPOINT = "/" + ImageRequestController.IMAGE_ENDPOINT;

    @Autowired
    private MockMvc mockMvc;

    private ApplicationFacade facade;

    @Before
    public void setUp() throws FogbowManagerException {
        this.facade = spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        given(ApplicationFacade.getInstance()).willReturn(this.facade);
    }

    // test case: Test getAllImages() when no images were found, it must return an empty JSON.
    @Test
    public void testGetAllImagesWhenHasNoData() throws Exception {
        // set up
        Map<String, String> imagesMap = new HashMap<>();
        doReturn(imagesMap).when(this.facade).getAllImages(anyString(), anyString());

        RequestBuilder requestBuilder = createRequestBuilder(IMAGE_ENDPOINT, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        assertEquals("{}", result.getResponse().getContentAsString());
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    // test case: Test getAllImages() when there are some images.
    @Test
    public void testGetAllImagesWhenHasData() throws Exception {
        // set up
        Map<String, String> imagesMap = new HashMap<>();
        imagesMap.put("image-id1", "image-name1");
        imagesMap.put("image-id2", "image-name2");
        imagesMap.put("image-id3", "image-name3");

        doReturn(imagesMap).when(this.facade).getAllImages(anyString(), anyString());

        RequestBuilder requestBuilder = createRequestBuilder(IMAGE_ENDPOINT, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());

        TypeToken<Map<String, String>> token = new TypeToken<Map<String, String>>(){};
        Map<String, String> resultMap = new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());
        assertTrue(resultMap.size() == 3);
    }

    // test case: Test if given an existing image id, the getImage() returns that image properly.
    @Test
    public void testGetImageById() throws Exception {
        // set up
        String fakeId = "fake-Id-1";
        String imageEndpoint = IMAGE_ENDPOINT + "/" + fakeId;

        Image image = new Image(fakeId, "fake-name", 1, 1, 1, "READY");

        doReturn(image).when(this.facade).getImage(anyString(), anyString(), anyString());

        RequestBuilder requestBuilder = createRequestBuilder(imageEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());

        Image resultImage = new Gson().fromJson(result.getResponse().getContentAsString(), Image.class);
        assertEquals(resultImage.getId(), image.getId());
        assertEquals(resultImage.getName(), image.getName());
        assertEquals(resultImage.getStatus(), image.getStatus());
        assertEquals(resultImage.getMinDisk(), image.getMinDisk());
        assertEquals(resultImage.getMinRam(), image.getMinRam());
        assertEquals(resultImage.getSize(), image.getSize());
    }

    // test case: Test if given an invalid image id, the getImage() returns just NOT_FOUND.
    @Test
    public void testGetImageWithInvalidId() throws Exception {
        // set up
        String fakeId = "fake-Id-1";
        String imageEndpoint = IMAGE_ENDPOINT + "/" + fakeId;

        doThrow(new InstanceNotFoundException()).when(this.facade).getImage(anyString(), anyString(), anyString());

        RequestBuilder requestBuilder = createRequestBuilder(imageEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.NOT_FOUND.value();
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    private RequestBuilder createRequestBuilder(String urlTemplate, HttpHeaders headers, String body) {
        return MockMvcRequestBuilders.get(urlTemplate)
                .headers(headers)
                .accept(MediaType.APPLICATION_JSON)
                .content(body)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String fakeFederationTokenValue = "fake-access-id";
        String fakeMemberId = "fake-member-id";
        headers.set(ImageRequestController.FEDERATION_TOKEN_VALUE_HEADER_KEY, fakeFederationTokenValue);
        headers.set(ImageRequestController.MEMBER_ID_HEADER_KEY, fakeMemberId);
        return headers;
    }
}
