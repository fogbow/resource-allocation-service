package org.fogbowcloud.manager.core.plugins.cloud.openstack;


import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.HttpResponseException;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.images.Image;
import org.fogbowcloud.manager.core.models.tokens.Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class OpenStackImagePluginTest {

    private static final String FAKE_IMAGE_ID = "fake-image-id";
    private static final String FAKE_IMAGE_NAME = "fake-image-name";
    private static final long FAKE_SIZE = 10l;
    private static final long FAKE_MIN_DISK = 1l;
    private static final long FAKE_MIN_RAM = 2l;
    private static final String STATUS = "status";
    
    private OpenStackImagePlugin plugin;
    private Token token;
    
    @Before
    public void setUp() {
        HomeDir.getInstance().setPath("src/test/resources/private");
        this.plugin = Mockito.spy(new OpenStackImagePlugin());
        this.token = Mockito.mock(Token.class);
    }
    
    @Test
    public void getAllImagesTest() throws FogbowManagerException, UnexpectedException, HttpResponseException {
        Map<String, String> allImages = new HashMap<>();
        allImages.put("fake-image-id", "fake-image-name");
        Mockito.doReturn(allImages).when(this.plugin).getImageNameAndIdMapFromAllAvailableImages(Mockito.any(Token.class), Mockito.anyString());
        Assert.assertEquals(allImages, this.plugin.getAllImages(this.token));
    }
    
    @Test (expected = UnexpectedException.class)
    public void getAllImagesTestThrowUnexpectedException() throws FogbowManagerException, UnexpectedException {
        Mockito.doThrow(UnexpectedException.class).when(this.plugin).getAllImages(Mockito.any(Token.class));
        this.plugin.getAllImages(this.token);
    }
    
    @Test (expected = FogbowManagerException.class)
    public void getAllImagesTestThrowFogbowManagerException() throws FogbowManagerException, UnexpectedException {
        Mockito.doThrow(FogbowManagerException.class).when(this.plugin).getAllImages(Mockito.any(Token.class));
        this.plugin.getAllImages(this.token);
    }
    
    @Test
    public void getImageTest() throws FogbowManagerException, UnexpectedException {
        Image image = new Image(FAKE_IMAGE_ID, FAKE_IMAGE_NAME, FAKE_SIZE, FAKE_MIN_DISK, FAKE_MIN_RAM, STATUS);
        Mockito.doReturn(image).when(this.plugin).getImage(Mockito.anyString(), Mockito.any(Token.class));
        Assert.assertEquals(image, this.plugin.getImage(FAKE_IMAGE_ID, this.token));        
    }
    
    @Test (expected = UnexpectedException.class)
    public void getImageThrowUnexpectedExceptionTest() throws FogbowManagerException, UnexpectedException {
        Mockito.doThrow(UnexpectedException.class).when(this.plugin).getImage(Mockito.anyString(), Mockito.any(Token.class));
        this.plugin.getImage(FAKE_IMAGE_ID, this.token);
    }
    
    @Test (expected = FogbowManagerException.class)
    public void getImageThrowFogbowManagerExceptionTest() throws FogbowManagerException, UnexpectedException {
        Mockito.doThrow(FogbowManagerException.class).when(this.plugin).getImage(Mockito.anyString(), Mockito.any(Token.class));
        this.plugin.getImage(FAKE_IMAGE_ID, this.token);
    }

}
