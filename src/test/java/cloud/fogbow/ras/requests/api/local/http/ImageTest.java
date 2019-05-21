package cloud.fogbow.ras.requests.api.local.http;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.common.exceptions.InstanceNotFoundException;
import cloud.fogbow.ras.api.http.CommonKeys;
import cloud.fogbow.ras.api.http.request.Image;
import cloud.fogbow.ras.api.http.response.ImageInstance;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.core.ApplicationFacade;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@WebMvcTest(value = Image.class, secure = false)
@PrepareForTest(ApplicationFacade.class)
public class ImageTest {

    private final String IMAGE_ENDPOINT = "/" + Image.IMAGE_ENDPOINT;

    @Autowired
    private MockMvc mockMvc;

    private ApplicationFacade facade;

    @Before
    public void setUp() throws FogbowException {
        this.facade = Mockito.spy(ApplicationFacade.class);
        PowerMockito.mockStatic(ApplicationFacade.class);
        BDDMockito.given(ApplicationFacade.getInstance()).willReturn(this.facade);
    }

    // test case: Test getAllImages() when no images were found, it must return an empty JSON.
    @Test
    public void testGetAllImagesWhenHasNoData() throws Exception {
        // set up
        List<ImageSummary> imageSummaryList = new ArrayList<>();
        Mockito.doReturn(imageSummaryList).when(this.facade).getAllImages(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        RequestBuilder requestBuilder = createRequestBuilder(IMAGE_ENDPOINT + "/provider/cloud", getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals("{}", result.getResponse().getContentAsString());
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    // test case: Test getAllImages() when there are some images.
    @Test
    public void testGetAllImagesWhenHasData() throws Exception {
        // set up
        List<ImageSummary> imageSummaryList = new ArrayList<>();
        imageSummaryList.add(new ImageSummary("image-id1", "image-name1"));
        imageSummaryList.add(new ImageSummary("image-id2", "image-name2"));
        imageSummaryList.add(new ImageSummary("image-id3", "image-name3"));

        Mockito.doReturn(imageSummaryList).when(this.facade).getAllImages(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        RequestBuilder requestBuilder = createRequestBuilder(IMAGE_ENDPOINT + "/provider/cloud", getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        TypeToken<Map<String, String>> token = new TypeToken<Map<String, String>>() {};
        List<ImageSummary> resultList = new Gson().fromJson(result.getResponse().getContentAsString(), token.getType());
        Assert.assertEquals(3, resultList.size());
    }

    // test case: Test if given an existing image id, the getImageId() returns that image properly.
    @Test
    public void testGetImageById() throws Exception {
        // set up
        String fakeId = "fake-Id-1";
        String imageEndpoint = IMAGE_ENDPOINT + "/provider/cloud/" + fakeId;

        ImageInstance imageInstance = new ImageInstance(fakeId, "fake-name", 1, 1, 1, "READY");

        Mockito.doReturn(imageInstance).when(this.facade).getImage(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString());

        RequestBuilder requestBuilder = createRequestBuilder(imageEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.OK.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());

        ImageInstance resultImageInstance = new Gson().fromJson(result.getResponse().getContentAsString(), ImageInstance.class);
        Assert.assertEquals(imageInstance.getId(), resultImageInstance.getId());
        Assert.assertEquals(imageInstance.getName(), resultImageInstance.getName());
        Assert.assertEquals(imageInstance.getStatus(), resultImageInstance.getStatus());
        Assert.assertEquals(imageInstance.getMinDisk(), resultImageInstance.getMinDisk());
        Assert.assertEquals(imageInstance.getMinRam(), resultImageInstance.getMinRam());
        Assert.assertEquals(imageInstance.getSize(), resultImageInstance.getSize());
    }

    // test case: Test if given an invalid image id, the getImageId() returns just NOT_FOUND.
    @Test
    public void testGetImageWithInvalidId() throws Exception {
        // set up
        String fakeId = "fake-Id-1";
        String imageEndpoint = IMAGE_ENDPOINT + "/" + fakeId;

        Mockito.doThrow(new InstanceNotFoundException()).when(this.facade).getImage(Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        RequestBuilder requestBuilder = createRequestBuilder(imageEndpoint, getHttpHeaders(), "");

        // exercise
        MvcResult result = this.mockMvc.perform(requestBuilder).andReturn();

        // verify
        int expectedStatus = HttpStatus.NOT_FOUND.value();
        Assert.assertEquals(expectedStatus, result.getResponse().getStatus());
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
        String fakeUserToken = "fake-access-id";
        headers.set(CommonKeys.SYSTEM_USER_TOKEN_HEADER_KEY, fakeUserToken);
        return headers;
    }
}
